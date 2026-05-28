# sip-gateway 统一 Envelope 协议化方案

> 版本：2.0 草案 | 日期：2026-05-28 | 关联：[SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md)（**主纲领**）、[GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md)、[LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) v2.5、[PROTOCOL-LAYERING-MATRIX.md](../../architecture/PROTOCOL-LAYERING-MATRIX.md)

> **本文档承载 envelope schema 与命令映射表的具体语义**。模块拓扑、SPI 形态、HTTP path、配置前缀的最终决策见 [SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md)。

> **v1.0 → v2.0 变更（与父聚合方案对齐）**：
> - type 命名升为三段式 `<protocol>.<Group>.<Name>`，1.8.0 起强制（GB28181 全表加 `gb28181.` 前缀）
> - 模块拓扑改为 `sip-gateway/` 父聚合 + `gateway-core` + `gateway-gb28181` + BOM + starter，envelope/SPI 落 `gateway-core`、命令表/forwarder 落 `gateway-gb28181`
> - `CommandHandler` 接口签名去掉 sender 形参，改为单参数 `(GatewayCommand) → GatewayCommandResult`
> - 新增 `ProtocolModule` SPI 作为协议适配器自报命名空间的入口
> - `InviteContextStore` 改为 `TransactionContextStore<String, InviteContext>` 的具体化，`TransactionContextStore<K,V>` 成为 gateway-core 泛型基类
> - HTTP path 全部改前缀 `/gateway/*`（核心分发） + `/gateway/{protocol}/*`（协议特殊端点）
> - 配置前缀拆为 `gateway.*`（核心） + `gateway.{protocol}.*`（协议子前缀）

> **目标**：把 `SipCommandController` 从"6 个固定端点 + 5 个固定 DTO"改造成**协议无关的薄壳**——业务方调一个接口承载所有 cmdType，回调走一个 envelope 承载所有事件。新增协议命令零改动 controller、零改动 BusinessNotifier 接口签名。**新增协议（ONVIF/GT1078/...）只追加 `gateway-{proto}` 子模块，本文档与父聚合方案契约保持稳定。**

---

## 一、设计目标

| 维度 | 现状 | 目标 |
|------|------|------|
| HTTP 出站 | 6 个 `@PostMapping` + 5 个固定 DTO（`InviteStartRequest` / `PtzRequest` / `CatalogQueryRequest` / `ByeRequest` / `InviteResponseRequest`） | 1 个 `POST /gateway/command`（gateway-core） + 1 个 `POST /gateway/gb28181/invite/response`（gateway-gb28181 协议特殊端点） |
| 入站回调 | `BusinessNotifier` 3 个方法（`deviceOnline` / `inviteIncoming` / `alarm`） | `BusinessNotifier#notify(GatewayEvent)` 1 个方法承载 35+ 事件 |
| 协议演进 | 加 cmdType 要改 controller + 加 DTO + 加 BusinessNotifier 接口 + 改 forwarder | 加 cmdType = `Gb28181CommandSpecs` 表里加 1 行 OR 加 1 个 `@CommandMapping` 方法 + 加 1 行 `Gb28181EventForwarder.emit` 映射；**加协议**=新建 `gateway-{proto}` 子模块、实现 `ProtocolModule`、core 0 改动 |
| 网关职责 | 协议解析 + 业务字段语义参与（`InviteStartRequest.streamMode`、`PtzRequest.cmd` 都是协议字段） | 网关只做：① 命令路由 ② 事务/Dialog 维护 ③ 跨节点回包 ④ 幂等 ⑤ 推送 envelope |
| 业务侧依赖面 | 直接依赖 `gb28181-common.StreamModeEnum`、`PTZControlEnum`（编译期耦合） | 业务侧用 `Map<String,Object>` payload，按文档约定字段填值；可选引入 `gb28181-common` 拿强类型 |
| Handler 文件数 | — | **1 张表 ~40 行 + 1~2 个白名单聚合类 ~20 个方法**，Java 总行数 ~250（vs 散落 59 个 handler 类约 1500 行） |

**核心命题**：网关是"信令转发 + 事务维护"层，**不解析 payload 业务字段**。它只关心 type 路由到哪个 handler，handler 才把 payload 翻译成具体协议参数。多协议扩展通过新增 `gateway-{proto}` 子模块 + 实现 `ProtocolModule` SPI 完成，**`gateway-core` 永远协议中立**（CI 强制纯度检查）。

---

## 二、统一 Envelope 设计

### 2.1 出站 envelope（业务 → gateway）

```java
public record GatewayCommand(
    /** 命令类型：protocol.Group.Name 三段式，如 "gb28181.Query.Catalog"、"gb28181.Control.Ptz"、"gb28181.Invite.Play" */
    String type,

    /** 设备 GB28181 编码（INVITE 回包等基于 callId 的命令该字段为 null） */
    String deviceId,

    /** 协议参数，按 type 对应的 schema 填值（详见 §五映射表） */
    Map<String, Object> payload,

    /** 业务侧追踪 ID，gateway 透传到回调 envelope 的 traceId 字段 */
    String requestId
) {}
```

```java
public record GatewayCommandResult(
    /** 关联键：sn（Query/Subscribe）或 callId（Invite/Bye）。业务侧用此键关联回调 */
    String correlationId,

    /** 命令类型，回执给业务侧确认无歧义 */
    String type,

    /** 处理节点（多节点部署排查用） */
    String nodeId
) {}
```

### 2.2 入站 envelope（gateway → 业务）

```java
public record GatewayEvent(
    /** 事件类型：protocol.Group.Name 三段式，如 "gb28181.Lifecycle.Online"、"gb28181.Notify.Alarm"、"gb28181.Response.Catalog" */
    String type,

    /** 设备 GB28181 编码（部分 Session.* 事件没有 deviceId，仅有 callId） */
    String deviceId,

    /** 关联键：sn（Response.*）或 callId（Session.*），不适用时 null */
    String correlationId,

    /** 事件发生时间（毫秒，gateway 节点时钟） */
    long timestampMs,

    /** 事件载荷（Map<String,Object>，业务侧按 type 反序列化） */
    Map<String, Object> payload,

    /** gateway 节点标识 */
    String nodeId
) {}
```

```java
public interface BusinessNotifier {
    /**
     * 业务方实现：把 event 推到 HTTP/MQ/Webhook。
     * <p><strong>必须异步</strong>，否则会阻塞 SIP 事件线程导致设备超时重传。
     */
    void notify(GatewayEvent event);
}
```

### 2.3 命名规则

```
type ::= <protocol>.<Group>.<Name>

protocol ∈ { gb28181 | onvif | gt1078 | rtsp | ... }    小写、与 ProtocolModule#protocol() 一致
Group    ∈ { Query | Subscribe | Control | Config | Invite | Device
           | Lifecycle | Notify | Response | Session }   GB28181 命名空间内的分组
Name     := 与 GBT-2022 cmdType 严格一致（首字母大写驼峰，如 Catalog、PtzCmd、MobilePosition）
```

**为什么三段式**：
- `protocol` 位预留多协议扩展（ONVIF/GT1078 加入时同名命令不冲突，详见 [SIP-GATEWAY-AGGREGATION-PLAN §五](SIP-GATEWAY-AGGREGATION-PLAN.md#L390)）
- `Group` 直接对照 GBT-28181-2022 §A.2.x 章节，业务侧好做菜单分组（查询 / 控制 / 配置 / 订阅）
- gateway 内部用 `protocol` 决定 ProtocolModule 路由，`Group` 决定子路由策略（Invite 走 Dialog 链路、Query 走 sn 异步链路）
- 比扁平 `gb28181_Query_Catalog` 更易过滤和监控（按 `protocol` 和 `Group` 二维聚合 metrics）

**示例**：

| 命令含义 | type |
|---------|------|
| GB28181 设备目录查询 | `gb28181.Query.Catalog` |
| GB28181 PTZ 控制 | `gb28181.Control.Ptz` |
| GB28181 INVITE 实时点播 | `gb28181.Invite.Play` |
| GB28181 设备上线事件 | `gb28181.Lifecycle.Online` |
| GB28181 报警通知 | `gb28181.Notify.Alarm` |
| GB28181 INVITE 200 OK 回执 | `gb28181.Session.InviteOk` |
| GB28181 目录查询响应 | `gb28181.Response.Catalog` |
| ONVIF 设备发现（1.10 占位） | `onvif.Discovery.Probe` |
| GT1078 通道开启（1.11 占位） | `gt1078.Channel.Open` |

> 1.8.0 兼容期 shim：`GatewayDispatchController` 收到无协议前缀的老 type 时自动补 `gb28181.` 前缀并 warn，1.10.0 移除。详见 [SIP-GATEWAY-AGGREGATION-PLAN §5.3](SIP-GATEWAY-AGGREGATION-PLAN.md#L416)。

---

## 三、SPI 抽象与注册表（混合架构：表驱动 + 注解白名单）

### 3.1 设计原则

62 个出站方法呈现明显的 80/20 分布：

- **80%（~39 个）**：签名简单（`(deviceId, 0~3 个标量参数)`），无重载、无默认值、无字段变换 → **走静态表**
- **20%（~20 个）**：有重载（`gb28181.Query.RecordInfo` String/long、`gb28181.Subscribe.Refresh` 含/不含 content）、强制字段（`gb28181.Control.AlarmReset` 必须 `cmd="ResetAlarm"`）、复杂枚举多态（`gb28181.Control.Ptz` hex 与枚举两路）、默认值（`gb28181.Invite.Play.streamMode=UDP`）、Date 类型转换（`gb28181.Query.AlarmQuery`）、多枚举组合（`gb28181.Control.FI/Preset/Cruise` 系列） → **走 `@CommandMapping` 注解方法**

两种机制注册到同一 `CommandHandlerRegistry`，注解优先（同 type 时白名单覆盖表条目）。

> **跨协议聚合**：每个协议适配器通过 `ProtocolModule#commandSpecs()` 向核心声明自己的表，`CommandHandlerRegistry` 启动期跨模块合并。详见 [SIP-GATEWAY-AGGREGATION-PLAN §四](SIP-GATEWAY-AGGREGATION-PLAN.md#L262)。

### 3.2 CommandSpec：表驱动核心数据结构

```java
/** 描述一条静态命令映射 */
public record CommandSpec(
    String type,                    // 例如 "gb28181.Query.Catalog"（必须以 ProtocolModule#protocol() + "." 开头）
    Class<?> senderClass,           // 启动期通过 ApplicationContext.getBean(senderClass) 解析；
                                    //   GB28181 时 = ServerCommandSender.class，未来 ONVIF = OnvifCommandClient.class
    String methodName,              // 例如 "deviceCatalogQuery"
    List<ParamBinding> bindings     // 参数绑定列表（顺序 = 方法形参顺序）
) {}

/**
 * 参数绑定 DSL：
 *   "deviceId"                          → cmd.deviceId()
 *   "callId"                            → payload.callId（顶层别名 callId 优先）
 *   "interval"                          → payload.interval as String
 *   "expires:int"                       → payload.expires 转 Integer
 *   "speed:int?128"                     → payload.speed 转 Integer，缺省 128
 *   "streamMode:StreamModeEnum?UDP"     → JSON.to(StreamModeEnum, payload.streamMode)，缺省 UDP
 *   "osdInfo:OsdConfig$OsdInfo"         → JSON.to(嵌套 class, payload.osdInfo)
 *   "dragZoom:DragZoom"                 → JSON.to(DragZoom, payload.dragZoom)
 */
public record ParamBinding(
    String source,                  // "deviceId" | "callId" | "payload.<field>"
    String fieldName,               // 字段名（payload 取值用）
    Class<?> targetType,            // 目标类型，反射调用前转换
    Object defaultValue             // 缺省值，null 表示必填（缺失抛 400）
) {}
```

### 3.3 ReflectiveCommandHandler：表驱动 handler

启动期为每条 `CommandSpec` 构造一个 `ReflectiveCommandHandler` 实例，注册到 `CommandHandlerRegistry`：

```java
public final class ReflectiveCommandHandler implements CommandHandler {

    private final CommandSpec spec;
    private final Object sender;                // 启动期由 Registry 通过 spec.senderClass() 解析
    private final Method targetMethod;          // 启动期一次性反射查找

    @Override public String type() { return spec.type(); }

    @Override
    public GatewayCommandResult handle(GatewayCommand cmd) {
        Object[] args = bindArgs(cmd);
        try {
            String correlationId = (String) targetMethod.invoke(sender, args);
            return new GatewayCommandResult(correlationId, spec.type(), nodeId());
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    private Object[] bindArgs(GatewayCommand cmd) {
        Object[] args = new Object[spec.bindings().size()];
        for (int i = 0; i < args.length; i++) {
            ParamBinding b = spec.bindings().get(i);
            Object raw = switch (b.source()) {
                case "deviceId" -> cmd.deviceId();
                case "callId"   -> Optional.ofNullable(cmd.payload().get("callId"))
                                          .orElse(cmd.deviceId());  // 兼容
                default         -> cmd.payload().get(b.fieldName());
            };
            if (raw == null && b.defaultValue() != null) raw = b.defaultValue();
            if (raw == null) throw new ResponseStatusException(BAD_REQUEST,
                    "missing field: " + b.fieldName() + " for type " + spec.type());
            args[i] = JSON.to(b.targetType(), raw);
        }
        return args;
    }
}
```

### 3.4 GB28181 命令表（Gb28181CommandSpecs）：覆盖 ~39 个简单命令

> **位置**：`gateway-gb28181/.../handler/Gb28181CommandSpecs.java`，由 `Gb28181Module#commandSpecs()` 暴露给 `CommandHandlerRegistry`。

```java
public final class Gb28181CommandSpecs {
    public static List<CommandSpec> declare() {
        return List.of(
            // ===== Query (11) =====
            spec("gb28181.Query.DeviceInfo",       "deviceInfoQuery",         arg("deviceId")),
            spec("gb28181.Query.DeviceStatus",     "deviceStatusQuery",       arg("deviceId")),
            spec("gb28181.Query.Catalog",          "deviceCatalogQuery",      arg("deviceId")),
            spec("gb28181.Query.PresetQuery",      "devicePresetQuery",       arg("deviceId")),
            spec("gb28181.Query.MobilePosition",   "deviceMobilePositionQuery", arg("deviceId"), arg("interval")),
            spec("gb28181.Query.PtzPosition",      "devicePtzPositionQuery",  arg("deviceId")),
            spec("gb28181.Query.SdCardStatus",     "deviceSdCardStatusQuery", arg("deviceId")),
            spec("gb28181.Query.HomePosition",     "deviceHomePositionQuery", arg("deviceId")),
            spec("gb28181.Query.CruiseTrackList",  "deviceCruiseTrackListQuery", arg("deviceId")),
            spec("gb28181.Query.CruiseTrack",      "deviceCruiseTrackQuery",  arg("deviceId"), arg("number:int")),
            spec("gb28181.Query.ConfigDownload",   "deviceConfigDownload",    arg("deviceId"), arg("configType")),

            // ===== Subscribe (3) =====
            spec("gb28181.Subscribe.Catalog",      "deviceCatalogSubscribe",  arg("deviceId"), arg("expires:int"), arg("eventType")),
            spec("gb28181.Subscribe.PtzPosition",  "devicePtzPositionSubscribe", arg("deviceId"), arg("expires:int")),
            spec("gb28181.Subscribe.Unsubscribe",  "unsubscribe",             arg("callId")),

            // ===== Control (10) =====
            spec("gb28181.Control.Reboot",         "deviceControlReboot",     arg("deviceId")),
            spec("gb28181.Control.Record",         "deviceControlRecord",     arg("deviceId"), arg("recordCmd")),
            spec("gb28181.Control.Guard",          "deviceControlGuardCmd",   arg("deviceId"), arg("guardCmd")),
            spec("gb28181.Control.IFrame",         "deviceControlIFrame",     arg("deviceId")),
            spec("gb28181.Control.HomePosition",   "deviceControlHomePosition", arg("deviceId"),
                                                                       arg("enabled"), arg("resetTime"), arg("presetIndex")),
            spec("gb28181.Control.PtzPrecise",     "deviceControlPtzPrecise", arg("deviceId"),
                                                                       arg("pan:double"), arg("tilt:double"), arg("zoom:double")),
            spec("gb28181.Control.FormatSDCard",   "deviceControlFormatSDCard", arg("deviceId"), arg("sdNumber:int")),
            spec("gb28181.Control.ScanSpeed",      "deviceControlScanSpeed",  arg("deviceId"), arg("groupNumber:int"), arg("speed:int")),
            spec("gb28181.Control.DragZoomIn",     "deviceControlDragZoomIn", arg("deviceId"), arg("dragZoom:DragZoom")),
            spec("gb28181.Control.DragZoomOut",    "deviceControlDragZoomOut", arg("deviceId"), arg("dragZoom:DragZoom")),

            // ===== Config (11) =====
            spec("gb28181.Config.BasicParam",      "deviceConfig",            arg("deviceId"),
                                                                       arg("name"), arg("expiration"),
                                                                       arg("heartBeatInterval"), arg("heartBeatCount")),
            spec("gb28181.Config.Osd",             "deviceConfigOsd",         arg("deviceId"), arg("osdInfo:OsdConfig$OsdInfo")),
            spec("gb28181.Config.VideoAlarmRecord","deviceConfigVideoAlarmRecord", arg("deviceId"), arg("config:VideoAlarmRecordConfig")),
            spec("gb28181.Config.AlarmReport",     "deviceConfigAlarmReport", arg("deviceId"), arg("config:AlarmReportConfig")),
            spec("gb28181.Config.SvacEncode",      "deviceConfigSvacEncode",  arg("deviceId"), arg("config:SvacEncodeConfig")),
            spec("gb28181.Config.SvacDecode",      "deviceConfigSvacDecode",  arg("deviceId"), arg("config:SvacDecodeConfig")),
            spec("gb28181.Config.VideoParamAttr",  "deviceConfigVideoParamAttribute", arg("deviceId"), arg("config:VideoParamAttributeConfig")),
            spec("gb28181.Config.VideoParamOpt",   "deviceConfigVideoParamOpt", arg("deviceId"), arg("config:VideoParamOptConfig")),
            spec("gb28181.Config.VideoRecordPlan", "deviceConfigVideoRecordPlan", arg("deviceId"), arg("config:VideoRecordPlanConfig")),
            spec("gb28181.Config.PictureMask",     "deviceConfigPictureMask", arg("deviceId"), arg("config:PictureMaskConfig")),
            spec("gb28181.Config.FrameMirror",     "deviceConfigFrameMirror", arg("deviceId"), arg("config:FrameMirrorConfig")),

            // ===== Invite (1) =====
            spec("gb28181.Invite.Bye",             "deviceBye",               arg("callId")),

            // ===== Device (3) =====
            spec("gb28181.Device.Upgrade",         "deviceUpgrade",           arg("deviceId"),
                                                                       arg("firmware"), arg("fileURL"),
                                                                       arg("manufacturer"), arg("sessionId")),
            spec("gb28181.Device.SnapShot",        "deviceSnapShot",          arg("deviceId"),
                                                                       arg("snapNum:int"), arg("interval:int"),
                                                                       arg("uploadURL"), arg("sessionId")),
            spec("gb28181.Device.Broadcast",       "deviceBroadcast",         arg("deviceId"))
        );
    }

    private static CommandSpec spec(String type, String methodName, ParamBinding... bindings) {
        return new CommandSpec(type, ServerCommandSender.class, methodName, List.of(bindings));
    }
    // arg(...) helper：见仓库 ParamBinding 工厂方法
}
```

**总计 39 行表条目，对应 39 个简单命令**。新增/修改命令 = 改这一张表 1 行，不动其他文件。**所有 type 严格以 `gb28181.` 开头**，启动期 Registry 会校验与 `Gb28181Module#protocol()` 一致。

### 3.5 注解白名单：覆盖 ~20 个复杂命令

> **位置**：`gateway-gb28181/.../handler/Gb28181WhitelistHandlers.java`。`@CommandMapping` 方法签名：`(GatewayCommand) → String`，sender 通过 Spring 注入到 bean。

```java
@Component
@RequiredArgsConstructor
public class Gb28181WhitelistHandlers {

    private final ServerCommandSender sender;          // ★ 通过构造器注入到 bean，不再走方法形参

    @CommandMapping("gb28181.Control.Ptz")
    public String ptz(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        if (p.containsKey("hex")) {
            return sender.deviceControlPtzCmd(cmd.deviceId(), (String) p.get("hex"));
        }
        return sender.deviceControlPtzCmd(cmd.deviceId(),
                JSON.to(PTZControlEnum.class, p.get("cmd")),
                ((Number) p.getOrDefault("speed", 128)).intValue());
    }

    @CommandMapping("gb28181.Control.AlarmReset")
    public String alarmReset(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlAlarm(cmd.deviceId(), "ResetAlarm",
                (String) p.get("alarmMethod"), (String) p.get("alarmType"));
    }

    @CommandMapping("gb28181.Control.FI")
    public String fi(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceControlFI(cmd.deviceId(),
                JSON.to(FocusIrisControlEnum.class, p.get("cmd")),
                ((Number) p.getOrDefault("speed", 128)).intValue());
    }

    @CommandMapping("gb28181.Control.Preset")
    public String preset(GatewayCommand cmd) { /* 同上模式 */ }

    @CommandMapping("gb28181.Control.Cruise")
    public String cruise(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Control.CruiseSpeedOrTime")
    public String cruiseSpeedOrTime(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Control.Scan")
    public String scan(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Control.Auxiliary")
    public String auxiliary(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Control.TargetTrack")
    public String targetTrack(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Query.RecordInfo")
    public String recordInfo(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        // 用 long 重载（业务侧统一传时间戳，避免 String 格式歧义）
        return sender.deviceRecordInfoQuery(cmd.deviceId(),
                ((Number) p.get("startTime")).longValue(),
                ((Number) p.get("endTime")).longValue());
    }

    @CommandMapping("gb28181.Query.AlarmQuery")
    public String alarmQuery(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceAlarmQuery(cmd.deviceId(),
                new Date(((Number) p.get("startTime")).longValue()),
                new Date(((Number) p.get("endTime")).longValue()),
                (String) p.get("alarmLevel"),
                (String) p.get("alarmMethod"),
                (String) p.get("alarmType"));
    }

    @CommandMapping("gb28181.Subscribe.MobilePosition")
    public String mobilePositionSubscribe(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Subscribe.Alarm")
    public String alarmSubscribe(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Subscribe.Refresh")
    public String refresh(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        String callId = (String) p.get("callId");
        int expires = ((Number) p.get("expires")).intValue();
        if (p.containsKey("content")) {
            return sender.refreshSubscribe(callId, (String) p.get("content"), expires);
        }
        return sender.refreshSubscribe(callId, expires);
    }

    @CommandMapping("gb28181.Invite.Play")
    public String invitePlay(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInvitePlay(cmd.deviceId(),
                (String) p.get("mediaIp"),
                ((Number) p.get("mediaPort")).intValue(),
                JSON.to(StreamModeEnum.class, p.getOrDefault("streamMode", "UDP")));
    }

    @CommandMapping("gb28181.Invite.Playback")
    public String invitePlayback(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Invite.Talk")
    public String inviteTalk(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Invite.Download")
    public String inviteDownload(GatewayCommand cmd) { /* ... */ }

    @CommandMapping("gb28181.Invite.PlaybackControl")
    public String invitePlaybackControl(GatewayCommand cmd) {
        Map<String, Object> p = cmd.payload();
        return sender.deviceInvitePlayBackControl(cmd.deviceId(),
                JSON.to(PlayActionEnums.class, p.get("action")));
    }

    @CommandMapping("gb28181.Invite.Ack")
    public String ack(GatewayCommand cmd) {
        String callId = (String) cmd.payload().get("callId");
        return callId != null ? sender.deviceAck(cmd.deviceId(), callId) : sender.deviceAck(cmd.deviceId());
    }
}
```

**总计 ~20 个方法，分布在 1~2 个聚合类**（按 Group 分文件也可：`Gb28181ControlWhitelistHandlers` / `Gb28181InviteWhitelistHandlers` / `Gb28181QueryWhitelistHandlers`）。所有 type 严格 `gb28181.` 前缀。

### 3.6 CommandHandler 接口与注解

```java
// gateway-core/api/CommandHandler.java
public interface CommandHandler {
    String type();
    /** 单参数：sender 在实现侧持有，对外 SPI 不感知具体协议 */
    GatewayCommandResult handle(GatewayCommand cmd);
}

// gateway-core/api/CommandMapping.java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMapping {
    /** 命令 type，如 "gb28181.Control.Ptz"。必须三段式。 */
    String value();

    /**
     * 是否显式覆盖 ProtocolModule 注册的同 type 表条目。
     * 默认 false：覆盖时启动期仅 WARN，不阻断。
     * 建议覆盖时设为 true，作为意图声明。
     */
    boolean overrideTable() default false;
}
```

`MethodInvokerHandler` 启动期校验注解方法签名：参数个数 = 1、参数类型 = `GatewayCommand`、返回类型 = `String`，不符合 fail-fast。

### 3.7 协议适配器 SPI 与注册表

```java
// gateway-core/api/ProtocolModule.java
public interface ProtocolModule {
    String protocol();                          // "gb28181" / "onvif" / ...
    Collection<CommandSpec> commandSpecs();     // 该协议的全部静态命令表
    default int order() { return 0; }
}
```

```java
// gateway-gb28181/handler/Gb28181Module.java
@Component
@RequiredArgsConstructor
public class Gb28181Module implements ProtocolModule {

    private final ServerCommandSender sender;

    @Override public String protocol() { return "gb28181"; }
    @Override public Collection<CommandSpec> commandSpecs() {
        return Gb28181CommandSpecs.declare();   // 39 行
    }
}
```

```java
// gateway-core/core/CommandHandlerRegistry.java
@Component
public class CommandHandlerRegistry {

    private final Map<String, CommandHandler> handlers;

    public CommandHandlerRegistry(ApplicationContext ctx,
                                  List<ProtocolModule> modules) {
        Map<String, CommandHandler> all = new HashMap<>();
        Map<String, String> typeOwner = new HashMap<>();

        // 1) 各 ProtocolModule 注册的静态表
        for (ProtocolModule m : sortByOrder(modules)) {
            for (CommandSpec spec : m.commandSpecs()) {
                if (!spec.type().startsWith(m.protocol() + ".")) {
                    throw new IllegalStateException(
                        "ProtocolModule '" + m.protocol() + "' declared spec '"
                        + spec.type() + "' not under its namespace");
                }
                if (typeOwner.containsKey(spec.type())) {
                    throw new IllegalStateException(
                        "Duplicate type '" + spec.type() + "': "
                        + typeOwner.get(spec.type()) + " vs " + m.protocol());
                }
                Object sender = ctx.getBean(spec.senderClass());
                all.put(spec.type(), new ReflectiveCommandHandler(spec, sender));
                typeOwner.put(spec.type(), m.protocol());
            }
        }

        // 2) 扫所有 Spring bean 的 @CommandMapping 方法
        for (Object bean : ctx.getBeansWithAnnotation(Component.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                CommandMapping ann = method.getAnnotation(CommandMapping.class);
                if (ann == null) continue;
                if (typeOwner.containsKey(ann.value()) && !ann.overrideTable()) {
                    log.warn("@CommandMapping('{}') silently overrides table entry from '{}'. "
                            + "Set overrideTable=true to declare intent.",
                            ann.value(), typeOwner.get(ann.value()));
                }
                all.put(ann.value(), new MethodInvokerHandler(ann.value(), bean, method));
                typeOwner.put(ann.value(), "annotation:" + bean.getClass().getSimpleName());
            }
        }

        this.handlers = Map.copyOf(all);
        log.info("CommandHandlerRegistry ready: {} types from {} modules", handlers.size(), modules.size());
    }

    public CommandHandler require(String type) {
        CommandHandler h = handlers.get(type);
        if (h == null) throw new ResponseStatusException(NOT_FOUND, "unknown command type: " + type);
        return h;
    }
}
```

### 3.8 GatewayDispatchController（极简化）

```java
// gateway-core/web/GatewayDispatchController.java
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayDispatchController {

    private final GatewayProperties props;
    private final CommandHandlerRegistry registry;
    private final RestTemplate gatewayForwardRestTemplate;
    private final Set<String> knownProtocols;     // 启动期 = registry 内所有 ProtocolModule#protocol()

    @PostMapping("/command")
    public GatewayCommandResult dispatch(@RequestBody GatewayCommand cmd) {
        String type = cmd.type();
        // 1.8.0 兼容 shim：无协议前缀时默认补 gb28181.（1.10.0 移除）
        boolean hasPrefix = type.indexOf('.') > 0
                && knownProtocols.contains(type.substring(0, type.indexOf('.')));
        if (!hasPrefix) {
            log.warn("type '{}' missing protocol prefix; falling back to 'gb28181.{}'. "
                   + "This compat shim will be removed in 1.10.0.", type, type);
            type = "gb28181." + type;
            cmd = cmd.withType(type);
        }
        return registry.require(type).handle(cmd);
    }

    @GetMapping("/whoami")
    public Map<String, String> whoami() {
        return Map.of("nodeId", props.getNodeId());
    }
}
```

**Controller 总代码量**：从 134 行 → ~50 行；新增 cmdType 永远不需要改这个文件。**协议特殊端点**（如 GB28181 INVITE 回包）单独留在 `gateway-gb28181/web/Gb28181InviteResponseController.java`，路径 `POST /gateway/gb28181/invite/response`。

---

## 四、入站事件聚合

### 4.1 Gb28181EventForwarder

> **位置**：`gateway-gb28181/forwarder/Gb28181EventForwarder.java`。替换 1.7.x 时期的 `SipEventForwarder`（仅实现 3 个 listener），改为实现 4 个 listener 共 35 个方法。每个方法 1~2 行：

```java
@Component
@RequiredArgsConstructor
public class Gb28181EventForwarder
        implements DeviceLifecycleListener, DeviceNotifyListener,
                   DeviceSessionListener, DeviceResponseListener {

    private final BusinessNotifier notifier;
    private final InviteContextStore inviteContextStore;
    private final GatewayProperties coreProps;            // gateway.* 节点身份
    private final Gb28181GatewayProperties gb28181Props;  // gateway.gb28181.* 协议子前缀
    private Cache<String, Boolean> processedInvites;       // INVITE 重传幂等

    private void emit(String type, String deviceId, String correlationId, Object payload) {
        notifier.notify(new GatewayEvent(
                type, deviceId, correlationId,
                System.currentTimeMillis(),
                JSON.parseObject(JSON.toJSONString(payload), Map.class),
                coreProps.getNodeId()));
    }

    // ========== Lifecycle (5) ==========
    @Override public void onDeviceRegister(String d, RegisterInfo i) { emit("gb28181.Lifecycle.Register", d, null, i); }
    @Override public void onRegisterChallenge(String d) { emit("gb28181.Lifecycle.RegisterChallenge", d, null, Map.of()); }
    @Override public void onDeviceOnline(String d, SipTransaction t) { emit("gb28181.Lifecycle.Online", d, null, t); }
    @Override public void onDeviceOffline(String d, RegisterInfo i, SipTransaction t) {
        emit("gb28181.Lifecycle.Offline", d, null, Map.of("registerInfo", i, "transaction", t));
    }
    @Override public void onRemoteAddressChanged(String d, RemoteAddressInfo r) { emit("gb28181.Lifecycle.RemoteAddressChanged", d, null, r); }

    // ========== Notify (7) ==========
    @Override public void onAlarmNotify(String d, DeviceAlarmNotify n) { emit("gb28181.Notify.Alarm", d, null, n); }
    @Override public void onKeepalive(String d, DeviceKeepLiveNotify n) { emit("gb28181.Notify.Keepalive", d, null, n); }
    @Override public void onMediaStatus(String d, MediaStatusNotify n) { emit("gb28181.Notify.MediaStatus", d, null, n); }
    @Override public void onMobilePositionNotify(String d, MobilePositionNotify n) { emit("gb28181.Notify.MobilePosition", d, null, n); }
    @Override public void onUpgradeResult(String d, UpgradeResultNotify n) { emit("gb28181.Notify.UpgradeResult", d, null, n); }
    @Override public void onSnapShotFinished(String d, UploadSnapShotFinishedNotify n) { emit("gb28181.Notify.SnapShotFinished", d, null, n); }
    @Override public void onVideoUploadNotify(String d, VideoUploadNotify n) { emit("gb28181.Notify.VideoUpload", d, null, n); }

    // ========== Session (7) ==========
    @Override public void onInviteTrying(String d, String c) { emit("gb28181.Session.InviteTrying", d, c, Map.of()); }
    @Override public void onInviteOk(String d, String c) { emit("gb28181.Session.InviteOk", d, c, Map.of()); }
    @Override public void onInviteFailure(String d, String c, int s) { emit("gb28181.Session.InviteFailure", d, c, Map.of("statusCode", s)); }
    @Override public void onAck(String d, String c, int s) { emit("gb28181.Session.Ack", d, c, Map.of("statusCode", s)); }
    @Override public void onBye(String d) { emit("gb28181.Session.Bye", d, null, Map.of()); }
    @Override public void onByeError(String d, String e) { emit("gb28181.Session.ByeError", d, null, Map.of("error", e)); }

    @Override
    public void onServerInvite(String callId, String fromUserId, String toUserId,
                               String rawSdp, GbSessionDescription sdp, String ctxKey) {
        // INVITE 幂等：UDP 重传按 callId 去重（关键不变量，保留）
        if (processedInvites.asMap().putIfAbsent(callId, Boolean.TRUE) != null) return;

        // 写入跨节点路由（关键不变量，保留）
        inviteContextStore.save(callId, new InviteContext(coreProps.getNodeId(), ctxKey),
                gb28181Props.getInviteContextTtlMs());

        emit("gb28181.Session.ServerInvite", null, callId, Map.of(
                "fromUserId", fromUserId, "toUserId", toUserId,
                "rawSdp", rawSdp, "sdp", sdp, "ctxKey", ctxKey));
    }

    // ========== Response (16) ==========
    @Override public void onCatalogResponse(String d, String sn, DeviceResponse r) { emit("gb28181.Response.Catalog", d, sn, r); }
    @Override public void onDeviceInfoResponse(String d, String sn, DeviceInfo i) { emit("gb28181.Response.DeviceInfo", d, sn, i); }
    @Override public void onDeviceInfoError(String d, String reason) { emit("gb28181.Response.DeviceInfoError", d, null, Map.of("reason", reason)); }
    @Override public void onDeviceInfoRequest(String d, String c) { emit("gb28181.Response.DeviceInfoRequest", d, null, Map.of("content", c)); }
    @Override public void onDeviceStatusResponse(String d, String sn, DeviceStatus s) { emit("gb28181.Response.DeviceStatus", d, sn, s); }
    @Override public void onRecordInfoResponse(String d, String sn, DeviceRecord r) { emit("gb28181.Response.RecordInfo", d, sn, r); }
    @Override public void onPtzPositionResponse(String d, PTZPositionResponse r) { emit("gb28181.Response.PtzPosition", d, null, r); }
    @Override public void onSdCardStatusResponse(String d, SDCardStatusResponse r) { emit("gb28181.Response.SdCardStatus", d, null, r); }
    @Override public void onHomePositionResponse(String d, HomePositionResponse r) { emit("gb28181.Response.HomePosition", d, null, r); }
    @Override public void onCruiseTrackListResponse(String d, CruiseTrackListResponse r) { emit("gb28181.Response.CruiseTrackList", d, null, r); }
    @Override public void onCruiseTrackResponse(String d, CruiseTrackResponse r) { emit("gb28181.Response.CruiseTrack", d, null, r); }
    @Override public void onConfigResponse(String d, String sn, DeviceConfigResponse r) { emit("gb28181.Response.Config", d, sn, r); }
    @Override public void onConfigDownloadResponse(String d, DeviceConfigDownloadResponse r) { emit("gb28181.Response.ConfigDownload", d, null, r); }
    @Override public void onPresetQueryResponse(String d, PresetQueryResponse r) { emit("gb28181.Response.PresetQuery", d, null, r); }
    @Override public void onSubscribeResponse(String d, String c, int s) { emit("gb28181.Response.Subscribe", d, c, Map.of("statusCode", s)); }
    @Override public void onNotifyUpdate(String d, DeviceOtherUpdateNotify n) { emit("gb28181.Response.NotifyUpdate", d, null, n); }
}
```

### 4.2 emit() 序列化策略

`fastjson2` 的 `JSON.toJSONString(obj)` → `JSON.parseObject(json, Map.class)` 是常见 entity → Map 转换路径，避免直接放 Java 对象（业务方反序列化时拿不到原始 entity 类）。

性能考量：
- entity 平均字段 < 30，序列化耗时 ~50μs，相比 SIP/HTTP IO 可忽略
- 后续可换 `JSON.toJSONObject(obj)` 直接拿 `JSONObject`（兼容 Map 接口），节省一次 String 中转

---

## 五、全量映射表

### 5.1 出站命令（59 个：39 走表 + 20 走白名单）

> **路由列说明**：📋 = 走 `CommandSpecs.declare()` 表条目；✍️ = 走 `@CommandMapping` 注解方法

| 路由 | Group | Type | ServerCommandSender 方法 | payload 必填 | payload 可选 | 返回 correlationId |
|:----:|-------|------|--------------------------|-------------|--------------|-------------------|
| 📋 | **Query** | `gb28181.Query.DeviceInfo` | `deviceInfoQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.DeviceStatus` | `deviceStatusQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.Catalog` | `deviceCatalogQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.PresetQuery` | `devicePresetQuery(d)` | — | — | sn |
| ✍️ | Query | `gb28181.Query.RecordInfo` | `deviceRecordInfoQuery(d, long, long)` | `startTime`(long), `endTime`(long) | — | sn |
| 📋 | Query | `gb28181.Query.MobilePosition` | `deviceMobilePositionQuery(d, interval)` | `interval`(string) | — | sn |
| ✍️ | Query | `gb28181.Query.AlarmQuery` | `deviceAlarmQuery(d, Date, Date, ...)` | `startTime`(long ms), `endTime`(long ms) | `alarmLevel`, `alarmMethod`, `alarmType` | sn |
| 📋 | Query | `gb28181.Query.PtzPosition` | `devicePtzPositionQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.SdCardStatus` | `deviceSdCardStatusQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.HomePosition` | `deviceHomePositionQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.CruiseTrackList` | `deviceCruiseTrackListQuery(d)` | — | — | sn |
| 📋 | Query | `gb28181.Query.CruiseTrack` | `deviceCruiseTrackQuery(d, number)` | `number`(int) | — | sn |
| 📋 | Query | `gb28181.Query.ConfigDownload` | `deviceConfigDownload(d, configType)` | `configType` | — | sn |
| 📋 | **Subscribe** | `gb28181.Subscribe.Catalog` | `deviceCatalogSubscribe(d, expires, eventType)` | `expires`(int), `eventType` | — | callId |
| ✍️ | Subscribe | `gb28181.Subscribe.MobilePosition` | `deviceMobilePositionSubscribe(...)` | `interval`, `expires`(int), `eventType` | — | callId |
| ✍️ | Subscribe | `gb28181.Subscribe.Alarm` | `deviceAlarmSubscribe(...)` | `expires`(int), `eventType` | `alarmLevel`, `alarmMethod`, `alarmType`, `startTime`, `endTime` | callId |
| 📋 | Subscribe | `gb28181.Subscribe.PtzPosition` | `devicePtzPositionSubscribe(d, expires)` | `expires`(int) | — | callId |
| ✍️ | Subscribe | `gb28181.Subscribe.Refresh` | `refreshSubscribe(callId, expires)` 或 `refreshSubscribe(callId, content, expires)` | `callId`, `expires`(int) | `content` | callId |
| 📋 | Subscribe | `gb28181.Subscribe.Unsubscribe` | `unsubscribe(callId)` | `callId` | — | callId |
| ✍️ | **Control** | `gb28181.Control.Ptz` | `deviceControlPtzCmd(d, ptz, speed)` 或 `(d, hex)` | `cmd`(PTZControlEnum) 或 `hex` | `speed`(int, 默认 128) | sn |
| ✍️ | Control | `gb28181.Control.FI` | `deviceControlFI(d, ...)` | `cmd`(FocusIrisEnum), `speed`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.Preset` | `deviceControlPreset(d, ...)` | `cmd`(PresetEnum), `presetIndex`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.Cruise` | `deviceControlCruise(d, ...)` | `cmd`(CruiseEnum), `groupNumber`(int), `presetIndex`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.CruiseSpeedOrTime` | `deviceControlCruiseSpeedOrTime(...)` | `groupNumber`(int), `cmd`(CruiseSpeedTimeEnum), `value`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.Scan` | `deviceControlScan(d, group, ...)` | `groupNumber`(int) | — | sn |
| 📋 | Control | `gb28181.Control.ScanSpeed` | `deviceControlScanSpeed(d, group, speed)` | `groupNumber`(int), `speed`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.Auxiliary` | `deviceControlAuxiliary(d, ...)` | `auxiliaryNumber`(int), `cmd`(AuxiliaryEnum) | — | sn |
| 📋 | Control | `gb28181.Control.Reboot` | `deviceControlReboot(d)` | — | — | sn |
| 📋 | Control | `gb28181.Control.Record` | `deviceControlRecord(d, recordCmd)` | `recordCmd` | — | sn |
| 📋 | Control | `gb28181.Control.Guard` | `deviceControlGuardCmd(d, guardCmdStr)` | `guardCmd` | — | sn |
| ✍️ | Control | `gb28181.Control.AlarmReset` | `deviceControlAlarm(d, "ResetAlarm", method, type)` | `alarmMethod`, `alarmType` | — | sn |
| 📋 | Control | `gb28181.Control.IFrame` | `deviceControlIFrame(d)` | — | — | sn |
| 📋 | Control | `gb28181.Control.DragZoomIn` | `deviceControlDragZoomIn(d, dragZoom)` | `dragZoom`(对象) | — | sn |
| 📋 | Control | `gb28181.Control.DragZoomOut` | `deviceControlDragZoomOut(d, dragZoom)` | `dragZoom`(对象) | — | sn |
| 📋 | Control | `gb28181.Control.HomePosition` | `deviceControlHomePosition(d, enabled, resetTime, presetIndex)` | `enabled`, `resetTime`, `presetIndex` | — | sn |
| 📋 | Control | `gb28181.Control.PtzPrecise` | `deviceControlPtzPrecise(d, pan, tilt, zoom)` | `pan`(double), `tilt`(double), `zoom`(double) | — | sn |
| 📋 | Control | `gb28181.Control.FormatSDCard` | `deviceControlFormatSDCard(d, sdNumber)` | `sdNumber`(int) | — | sn |
| ✍️ | Control | `gb28181.Control.TargetTrack` | `deviceControlTargetTrack(d, mode, deviceId2, ...)` | `mode`, `deviceId2` | 其他可选目标参数 | sn |
| 📋 | **Config** | `gb28181.Config.BasicParam` | `deviceConfig(d, name, expiration, ...)` | `name`, `expiration`, `heartBeatInterval`, `heartBeatCount` | — | sn |
| 📋 | Config | `gb28181.Config.Osd` | `deviceConfigOsd(d, osdInfo)` | `osdInfo`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.VideoAlarmRecord` | `deviceConfigVideoAlarmRecord(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.AlarmReport` | `deviceConfigAlarmReport(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.SvacEncode` | `deviceConfigSvacEncode(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.SvacDecode` | `deviceConfigSvacDecode(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.VideoParamAttr` | `deviceConfigVideoParamAttribute(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.VideoParamOpt` | `deviceConfigVideoParamOpt(d, ...)` | `config`(对象, GB28181-2016 兼容) | — | sn |
| 📋 | Config | `gb28181.Config.VideoRecordPlan` | `deviceConfigVideoRecordPlan(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.PictureMask` | `deviceConfigPictureMask(d, ...)` | `config`(对象) | — | sn |
| 📋 | Config | `gb28181.Config.FrameMirror` | `deviceConfigFrameMirror(d, ...)` | `config`(对象) | — | sn |
| ✍️ | **Invite** | `gb28181.Invite.Play` | `deviceInvitePlay(d, sdpIp, port, streamMode)` | `mediaIp`, `mediaPort`(int) | `streamMode`(StreamModeEnum, 默认 UDP) | callId |
| ✍️ | Invite | `gb28181.Invite.Playback` | `deviceInvitePlayBack(d, sdpIp, port, ..., streamMode)` | `mediaIp`, `mediaPort`, `startTime`, `endTime` | `streamMode`(默认 UDP) | callId |
| ✍️ | Invite | `gb28181.Invite.Talk` | `deviceInviteTalk(d, sdpIp, port, streamMode)` | `mediaIp`, `mediaPort` | `streamMode`(默认 UDP) | callId |
| ✍️ | Invite | `gb28181.Invite.Download` | `deviceInviteDownload(d, sdpIp, port, ..., streamMode)` | `mediaIp`, `mediaPort`, `startTime`, `endTime`, `downloadSpeed` | `streamMode` | callId |
| ✍️ | Invite | `gb28181.Invite.PlaybackControl` | `deviceInvitePlayBackControl(d, action)` | `action`(PlayActionEnums) | — | callId |
| 📋 | Invite | `gb28181.Invite.Bye` | `deviceBye(callId)` | `callId` | — | callId |
| ✍️ | Invite | `gb28181.Invite.Ack` | `deviceAck(d)` 或 `deviceAck(d, callId)` | — | `callId` | callId 或 null |
| 📋 | **Device** | `gb28181.Device.Upgrade` | `deviceUpgrade(d, firmware, fileURL, manufacturer, sessionId)` | `firmware`, `fileURL`, `manufacturer`, `sessionId` | — | sn |
| 📋 | Device | `gb28181.Device.SnapShot` | `deviceSnapShot(d, snapNum, interval, uploadURL, sessionId)` | `snapNum`(int), `interval`(int), `uploadURL`, `sessionId` | — | sn |
| 📋 | Device | `gb28181.Device.Broadcast` | `deviceBroadcast(d)` | — | — | sn |

**统计**：39 走表 📋 + 20 走白名单 ✍️ = 59 条命令。

> **不暴露**：`deviceAckBySipUri`（内部协议补偿，业务侧无场景）、`send(CommandContext)`（底层 hook）

### 5.2 入站事件（35 个 GatewayEvent）

| Group | Type | 来源 listener 方法 | payload 字段 |
|-------|------|------------------|------------|
| **Lifecycle** (5) | `gb28181.Lifecycle.Register` | onDeviceRegister | `RegisterInfo` 全字段 |
| Lifecycle | `gb28181.Lifecycle.RegisterChallenge` | onRegisterChallenge | 空 |
| Lifecycle | `gb28181.Lifecycle.Online` | onDeviceOnline | `transaction`(SipTransaction) |
| Lifecycle | `gb28181.Lifecycle.Offline` | onDeviceOffline | `registerInfo`, `transaction` |
| Lifecycle | `gb28181.Lifecycle.RemoteAddressChanged` | onRemoteAddressChanged | `RemoteAddressInfo` 全字段 |
| **Notify** (7) | `gb28181.Notify.Alarm` | onAlarmNotify | `DeviceAlarmNotify` 全字段 |
| Notify | `gb28181.Notify.Keepalive` | onKeepalive | `DeviceKeepLiveNotify` 全字段 |
| Notify | `gb28181.Notify.MediaStatus` | onMediaStatus | `MediaStatusNotify` |
| Notify | `gb28181.Notify.MobilePosition` | onMobilePositionNotify | `MobilePositionNotify` |
| Notify | `gb28181.Notify.UpgradeResult` | onUpgradeResult | `UpgradeResultNotify` |
| Notify | `gb28181.Notify.SnapShotFinished` | onSnapShotFinished | `UploadSnapShotFinishedNotify` |
| Notify | `gb28181.Notify.VideoUpload` | onVideoUploadNotify | `VideoUploadNotify` |
| **Session** (7) | `gb28181.Session.InviteTrying` | onInviteTrying | 空（correlationId=callId）|
| Session | `gb28181.Session.InviteOk` | onInviteOk | 空 |
| Session | `gb28181.Session.InviteFailure` | onInviteFailure | `statusCode`(int) |
| Session | `gb28181.Session.Ack` | onAck | `statusCode`(int) |
| Session | `gb28181.Session.Bye` | onBye | 空 |
| Session | `gb28181.Session.ByeError` | onByeError | `error`(string) |
| Session | `gb28181.Session.ServerInvite` | onServerInvite | `fromUserId`, `toUserId`, `rawSdp`, `sdp`(GbSessionDescription), `ctxKey` |
| **Response** (16) | `gb28181.Response.Catalog` | onCatalogResponse | `DeviceResponse`（含 sumNum/deviceItems） |
| Response | `gb28181.Response.DeviceInfo` | onDeviceInfoResponse | `DeviceInfo` |
| Response | `gb28181.Response.DeviceInfoError` | onDeviceInfoError | `reason`(string) |
| Response | `gb28181.Response.DeviceInfoRequest` | onDeviceInfoRequest | `content`(string，INFO 类原始 body) |
| Response | `gb28181.Response.DeviceStatus` | onDeviceStatusResponse | `DeviceStatus` |
| Response | `gb28181.Response.RecordInfo` | onRecordInfoResponse | `DeviceRecord` |
| Response | `gb28181.Response.PtzPosition` | onPtzPositionResponse | `PTZPositionResponse` |
| Response | `gb28181.Response.SdCardStatus` | onSdCardStatusResponse | `SDCardStatusResponse` |
| Response | `gb28181.Response.HomePosition` | onHomePositionResponse | `HomePositionResponse` |
| Response | `gb28181.Response.CruiseTrackList` | onCruiseTrackListResponse | `CruiseTrackListResponse` |
| Response | `gb28181.Response.CruiseTrack` | onCruiseTrackResponse | `CruiseTrackResponse` |
| Response | `gb28181.Response.Config` | onConfigResponse | `DeviceConfigResponse`（仅结果码） |
| Response | `gb28181.Response.ConfigDownload` | onConfigDownloadResponse | `DeviceConfigDownloadResponse` |
| Response | `gb28181.Response.PresetQuery` | onPresetQueryResponse | `PresetQueryResponse` |
| Response | `gb28181.Response.Subscribe` | onSubscribeResponse | `statusCode`(int) |
| Response | `gb28181.Response.NotifyUpdate` | onNotifyUpdate | `DeviceOtherUpdateNotify` |

> **特殊端点**（不进 envelope）：`POST /gateway/gb28181/invite/response` 是事务回包基础设施，依赖 `SipTransactionRegistry` + 跨节点路由，单独留在 `gateway-gb28181/web/Gb28181InviteResponseController.java`。详见 [SIP-GATEWAY-AGGREGATION-PLAN §六](SIP-GATEWAY-AGGREGATION-PLAN.md#L425)。

---

## 六、模块结构变化

### 6.1 目标结构

> **本节描述 envelope 与命令表在 sip-gateway 父聚合下的归属。完整模块拓扑（含 BOM/starter）见 [SIP-GATEWAY-AGGREGATION-PLAN §二、§三](SIP-GATEWAY-AGGREGATION-PLAN.md#L75)。**

**gateway-core**（协议中立内核）：

```
gateway-core/src/main/java/io/github/lunasaw/sipgateway/core/
├── api/
│   ├── envelope/
│   │   ├── GatewayCommand.java                  # 入参 envelope
│   │   ├── GatewayCommandResult.java            # 出参 envelope
│   │   └── GatewayEvent.java                    # 回调 envelope
│   ├── BusinessNotifier.java                    # 单方法 notify(GatewayEvent)
│   ├── CommandHandler.java                      # SPI 接口（无 sender 形参）
│   ├── CommandMapping.java                      # 注解
│   ├── CommandSpec.java                         # 表条目数据结构（含 senderClass 字段）
│   ├── ParamBinding.java                        # 参数绑定 DSL
│   ├── ProtocolModule.java                      # ★ 协议适配器自报命名空间的 SPI
│   └── TransactionContextStore.java             # 泛型事务存储基类
├── core/
│   ├── CommandHandlerRegistry.java              # 跨协议聚合（启动期合并表 + 注解，fail-fast）
│   ├── ReflectiveCommandHandler.java            # 表条目运行期适配（持有协议 sender）
│   ├── MethodInvokerHandler.java                # 注解方法运行期适配
│   └── PayloadCodec.java                        # fastjson2 二次反序列化封装
├── notifier/
│   ├── NoopBusinessNotifier.java                # 默认日志实现，启动 warn
│   └── AbstractProtocolBusinessNotifier.java    # 可选基类（按 protocol 分发）
├── web/
│   └── GatewayDispatchController.java           # POST /gateway/command + GET /gateway/whoami
└── config/
    ├── GatewayProperties.java                   # gateway.* 协议中立字段
    └── GatewayCoreAutoConfiguration.java
```

**gateway-gb28181**（GB28181 适配器）：

```
gateway-gb28181/src/main/java/io/github/lunasaw/sipgateway/gb28181/
├── handler/
│   ├── Gb28181CommandSpecs.java                 # ⭐ 39 行表条目，type 全部带 gb28181. 前缀
│   ├── Gb28181Module.java                       # ★ implements ProtocolModule
│   └── Gb28181WhitelistHandlers.java            # ⭐ ~20 个 @CommandMapping 方法
├── forwarder/
│   └── Gb28181EventForwarder.java               # 4 listener × 35 emit
├── store/
│   ├── InviteContextStore.java                  # extends TransactionContextStore<String, InviteContext>
│   ├── InviteContext.java                       # record(nodeId, ctxKey)
│   └── InMemoryInviteContextStore.java          # Caffeine 默认实现，启动 warn
├── web/
│   └── Gb28181InviteResponseController.java     # POST /gateway/gb28181/invite/response
├── config/
│   ├── Gb28181GatewayProperties.java            # gateway.gb28181.* 子前缀
│   └── Gb28181GatewayAutoConfiguration.java     # @ConditionalOnClass(ServerCommandSender.class)
└── dto/
    └── InviteResponseRequest.java               # 仅保留事务回包 DTO
```

**删除**（vs 1.7.x 参考实现）：`dto/CatalogQueryRequest.java`、`dto/InviteStartRequest.java`、`dto/PtzRequest.java`、`dto/ByeRequest.java`、`forwarder/SipEventForwarder.java`（被 `Gb28181EventForwarder` 取代）。

### 6.2 文件计数对比

| 部分 | 现状 | 目标 | 增量 |
|------|------|------|------|
| Controller 路径 | 6 + 1 = 7 | 1（gateway-core 分发） + 1（gb28181 INVITE 回包） + 1（whoami） = 3 | -4 |
| 固定 DTO | 5 | 1（仅 `InviteResponseRequest`） | -4 |
| Envelope record（gateway-core） | 0 | 3 | +3 |
| ProtocolModule SPI（gateway-core） | 0 | 1 | +1 |
| 命令表条目（gateway-gb28181） | 0 | 1 个 `Gb28181CommandSpecs.java`（39 行表条目） | +1 文件 |
| 注解白名单方法（gateway-gb28181） | 0 | 1~2 个聚合类 × ~20 个 `@CommandMapping` 方法 | +1~2 文件 |
| Forwarder listener 方法实现 | 3 | 35 | +32 |
| BusinessNotifier 方法 | 3 | 1 | -2 |

**净增 Java 文件 ≈ 12 个，净增源代码行数 ≈ 280 行**（vs 1.7.x 参考实现 +59 个 handler 类、+1500 行）。

新增 cmdType 路径：

- 简单 GB28181 命令 → 改 `Gb28181CommandSpecs.java` 加 1 行（**0 个新文件**）
- 复杂 GB28181 命令 → `Gb28181WhitelistHandlers` 加 1 个 `@CommandMapping` 方法（**0 个新文件**）
- **新协议**（如 ONVIF） → 新建 `gateway-onvif` 子模块、加 `OnvifModule implements ProtocolModule` 与 `OnvifCommandSpecs`、starter 加一行依赖（**gateway-core 与本文档 0 改动**）

> Controller 永远稳定，新增 cmdType / 新增协议都是纯 SPI 加法，零破坏。

---

## 七、HTTP 协议细节

### 7.1 请求示例

```http
POST /gateway/command HTTP/1.1
Content-Type: application/json

{
  "type": "gb28181.Query.Catalog",
  "deviceId": "34020000001320000001",
  "payload": {},
  "requestId": "trace-abc-123"
}
```

```http
POST /gateway/command HTTP/1.1
Content-Type: application/json

{
  "type": "gb28181.Control.Ptz",
  "deviceId": "34020000001320000001",
  "payload": {
    "cmd": "RIGHT",
    "speed": 200
  },
  "requestId": "trace-abc-124"
}
```

```http
POST /gateway/command HTTP/1.1
Content-Type: application/json

{
  "type": "gb28181.Invite.Play",
  "deviceId": "34020000001320000001",
  "payload": {
    "mediaIp": "10.0.0.5",
    "mediaPort": 30001,
    "streamMode": "UDP"
  },
  "requestId": "trace-abc-125"
}
```

### 7.2 响应示例

```json
{
  "correlationId": "1234567890",
  "type": "gb28181.Query.Catalog",
  "nodeId": "node-1"
}
```

### 7.3 错误码（保持现状语义）

| HTTP | 场景 | 业务侧动作 |
|------|------|-----------|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | type 不存在 | 修正 type 字符串 |
| 410 | 事务已终止/超时（INVITE / 订阅 / ...） | 重新发起原始命令 |
| 502 | 跨节点路由 nodeAddressMap 暂未刷新 | 200ms × 3 短重试 |
| 503 | 转发失败 / store 后端不可达 | 短重试 |
| 504 | （仅 sync 模式预留） | 重试或降级异步 |

### 7.4 回调示例

```json
{
  "type": "gb28181.Lifecycle.Register",
  "deviceId": "34020000001320000001",
  "correlationId": null,
  "timestampMs": 1748390400000,
  "payload": {
    "deviceId": "34020000001320000001",
    "expires": 3600,
    "remoteIp": "10.0.0.100",
    "remotePort": 5060,
    "transport": "UDP"
  },
  "nodeId": "node-1"
}
```

```json
{
  "type": "gb28181.Response.Catalog",
  "deviceId": "34020000001320000001",
  "correlationId": "1234567890",
  "timestampMs": 1748390401234,
  "payload": {
    "deviceId": "34020000001320000001",
    "sumNum": 4,
    "deviceItems": [
      { "deviceId": "...", "name": "...", "status": "ON", ... }
    ]
  },
  "nodeId": "node-1"
}
```

### 7.5 GB28181 INVITE 异步回包

GB28181 设备主动 INVITE 时业务方需异步准备 SDP，通过协议特殊端点回包：

```http
POST /gateway/gb28181/invite/response HTTP/1.1
Content-Type: application/json

{
  "callId": "abc123@10.0.0.100",
  "sdp": "v=0\r\no=...",
  "statusCode": 200
}
```

`Gb28181InviteResponseController` 内部从 `InviteContextStore` 取 `{nodeId, ctxKey}`：本节点直接调 `ResponseCmd.sendResponse`；跨节点则通过 `gateway.nodes` HTTP 转发。详见 [LAYERED-ARCHITECTURE §4.3](../../architecture/LAYERED-ARCHITECTURE.md#L134)。

### 7.6 1.8.0 兼容期 shim

业务方仍按老 type（`Query.Catalog` 不带前缀）调用时，`GatewayDispatchController` 自动补 `gb28181.` 前缀并 warn：

```
WARN  type 'Query.Catalog' missing protocol prefix; falling back to 'gb28181.Query.Catalog'.
      This compat shim will be removed in 1.10.0.
```

shim 仅作用于 `cmd.type`，HTTP 路径强制改 `/gateway/*`，老 `/sip/*` 不再可用。详见 [SIP-GATEWAY-AGGREGATION-PLAN §5.3](SIP-GATEWAY-AGGREGATION-PLAN.md#L416)。

---

## 八、SPI 扩展性

### 8.1 业务方扩展（自定义 cmdType）

业务方可在自己的 Spring 上下文里加 `@Component` 持有 `@CommandMapping` 注解方法，自动并入注册表：

```java
@Component
@RequiredArgsConstructor
class CustomBusinessHandlers {

    private final ServerCommandSender sender;     // 或业务侧自定义 sender bean

    @CommandMapping("gb28181.Custom.GroupQuery")
    public String groupQuery(GatewayCommand cmd) {
        // 业务自有 SIP 命令拼装逻辑（必要时直接用 sip-common 底层 API）
        ...
    }
}
```

**type 命名约束**：业务方自定义 type 也必须遵循三段式 `<protocol>.<Group>.<Name>`，且 `protocol` 必须为已知协议（`gb28181 / onvif / ...`）；如需引入业务专属协议命名空间（`mybiz.*`），需同时实现 `ProtocolModule` 并注册到 Spring 上下文。

### 8.2 替换/覆盖

加 `@Primary` 或同 `type()` 后用 `@ConditionalOnMissingBean` 覆盖默认 handler。覆盖 `ProtocolModule` 注册的内置 type 时，建议显式 `@CommandMapping(value="...", overrideTable=true)` 声明意图，避免启动期 WARN。

### 8.3 协议演进

GBT-2022 → GBT-2025 时新增 cmdType 路径：

1. `gb28181-common` 加 entity
2. `gb28181-server` 加 handler / listener 方法
3. `gateway-gb28181` 加 1 行 `Gb28181CommandSpecs` 表条目（简单命令）或 1 个 `@CommandMapping` 方法（复杂命令） + 1 行 `Gb28181EventForwarder.emit(...)`
4. **不需要**改 `GatewayDispatchController`、`BusinessNotifier` 接口签名、envelope record、`ProtocolModule` SPI、本文档的 §二~§四

### 8.4 多协议扩展（加协议）

新增 ONVIF/GT1078 路径：

1. 新建 `sip-gateway/gateway-{proto}/` 子模块
2. 实现 `ProtocolModule`（自报 `protocol()` + `commandSpecs()`）
3. 实现 `{Proto}EventForwarder` 桥接协议事件源到 `BusinessNotifier`
4. 协议特殊端点（如 ONVIF 订阅续订）放本协议 web 子包，路径 `POST /gateway/{proto}/...`
5. `sip-gateway-bom` 增加坐标，`sip-gateway-spring-boot-starter` 增加 `<dependency>`，`AutoConfiguration.imports` 加一行
6. **gateway-core 不动；本文档不动**

详见 [SIP-GATEWAY-AGGREGATION-PLAN §十二](SIP-GATEWAY-AGGREGATION-PLAN.md#L630)。

---

## 九、迁移策略

### 9.1 落地节奏（已确认：一次性全量提交）

按 [SIP-GATEWAY-AGGREGATION-PLAN §九](SIP-GATEWAY-AGGREGATION-PLAN.md#L460) Stage 0~6 执行，本文档承担 Stage 2~3 envelope 与命令表的代码细节：

1. 新增 envelope 三件套（`GatewayCommand` / `GatewayCommandResult` / `GatewayEvent`），落 `gateway-core/api/envelope/`
2. 新增核心抽象：`CommandHandler` 接口（单参数）、`@CommandMapping` 注解（含 `overrideTable`）、`CommandSpec`（含 `senderClass`）/ `ParamBinding` / `ProtocolModule`，落 `gateway-core/api/`
3. 落 `Gb28181CommandSpecs.java`（**1 个文件，39 行表条目，type 全部带 `gb28181.` 前缀**）+ `ReflectiveCommandHandler` 表条目运行期适配
4. 落 `Gb28181WhitelistHandlers.java`（**1~2 个聚合类，~20 个 `@CommandMapping` 方法，签名 `(GatewayCommand) → String`**）+ `MethodInvokerHandler` 注解运行期适配
5. 落 `Gb28181Module implements ProtocolModule` 自报 `"gb28181"` 命名空间 + `commandSpecs()` 调用 `Gb28181CommandSpecs.declare()`
6. 落 `CommandHandlerRegistry`（启动期跨协议合并表 + 注解，type 重复 fail-fast，类型前缀校验）
7. 重写 `GatewayDispatchController`（`gateway-core`：1 个 `/gateway/command` + `/gateway/whoami` + 1.8.0 兼容 shim）
8. 落 `Gb28181InviteResponseController`（`gateway-gb28181`：`POST /gateway/gb28181/invite/response`，跨节点路由不变）
9. 重写 `Gb28181EventForwarder`（4 listener × 35 方法，每方法 1 行 emit，全部带 `gb28181.` 前缀）
10. 重写 `BusinessNotifier` 接口（3 方法 → 1 方法 `notify(GatewayEvent)`）+ 提供 `AbstractProtocolBusinessNotifier` 可选基类
11. `NoopBusinessNotifier` / `InMemoryInviteContextStore` 默认实现 + 启动 warn
12. CHANGELOG 1.8.0 单独章节，列出 cmdType 老→新对照（94 条）+ HTTP path 老→新对照（7 条）+ 配置前缀 deprecation

### 9.2 测试覆盖

- **`CommandSpec` 表条目单测**：每条 spec 各 1 用例（mock `ServerCommandSender`，断言反射调用参数顺序、类型转换、缺省值生效），共 **39 个用例**
- **白名单方法单测**：每个 `@CommandMapping` 方法 1 用例（断言重载选择、强制字段、默认值正确），共 **~20 个用例**
- **`Gb28181EventForwarder` 单测**：35 个 listener 方法各 1 用例（断言 emit 出的 `GatewayEvent.type` 三段式正确、`correlationId` / `payload` 正确），并保留 INVITE 幂等老用例
- **`GatewayDispatchController` 集成测**：分发未知 type → 404、payload 缺失 → 400、合法分发 → handler 被调用、跨节点 forward 不变；1.8.0 兼容 shim 命中时 WARN 日志可观察
- **`Gb28181InviteResponseController` 集成测**：本节点回包、跨节点转发、store miss → 410、store 故障 → 503
- **`CommandHandlerRegistry` 启动期校验**：① type 重复（含跨协议）fail-fast；② 注解覆盖表条目时日志 WARN 提示，`overrideTable=true` 时不 warn；③ 每条目反射 `Method.findMethod` 命中目标方法（无歧义）；④ ProtocolModule 自报 protocol 与 spec.type 前缀不一致 fail-fast
- **starter 集成测**：空白 spring-boot 项目引 `sip-gateway-spring-boot-starter` 启动正常；exclusions 排除 `gateway-gb28181` 后 `gb28181.*` type 返回 404；缺 `ServerCommandSender` 时整个 GB28181 模块自动跳过

### 9.3 兼容性

- **破坏性变更**：
  - `BusinessNotifier` 接口从 3 方法变 1 方法
  - 老 6 个 HTTP 路径删除（`/sip/invite/start` 等），全部改 `/gateway/*`
  - type 字符串改三段式（兼容 shim 一版后移除）
  - 配置前缀部分键名搬到 `gateway.gb28181.*`（保留一版 deprecation 别名）
- **CHANGELOG 必须**：列出所有 type、HTTP path、配置 key 的老→新对照
- **不提供过渡 sugar**：避免长期维护两套语义。仅保留 1.8.0 ~ 1.9.x 一版兼容 shim，1.10.0 移除

### 9.4 文档同步

- **本计划**：写明 envelope 设计 + 全量映射 + 业务侧示例代码（HTTP / Java），与父聚合方案对齐
- **[SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md)**：主纲领，决定模块拓扑、SPI 形态、HTTP path、配置前缀；本文档严格遵循
- **[GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md)**：执行手册，承接 1.7.3 前置 + 代码迁移步骤；与本文档保持一致
- **[LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) §6**：sip-gateway 段落改写为"sip-proxy 提供的聚合 starter"
- **[PROTOCOL-LAYERING-MATRIX.md](../../architecture/PROTOCOL-LAYERING-MATRIX.md)**：在 §三 / §四 末尾追加"L3 网关 envelope 表"小节，cmdType 表格再扩一列指向本文档对应行（type 列改三段式）

---

## 十、决策点（已确认）

| # | 问题 | 决策 |
|---|------|------|
| 1 | type 命名风格 | `<protocol>.<Group>.<Name>` 三段式（`gb28181.Query.Catalog` / `gb28181.Control.Ptz`） |
| 2 | payload 序列化形态 | `Map<String, Object>` + 文档约定字段；fastjson2 二次反序列化 |
| 3 | 出站接口同步语义 | 全异步：返回 `correlationId`，业务侧靠回调匹配 |
| 4 | 落代码节奏 | 一次性全量提交（单 PR） |
| 5 | 模块拓扑 | sip-gateway 父聚合 + gateway-core + gateway-gb28181 + BOM + starter（详见 [SIP-GATEWAY-AGGREGATION-PLAN](SIP-GATEWAY-AGGREGATION-PLAN.md)） |
| 6 | CommandHandler 接口签名 | 单参数 `(GatewayCommand) → GatewayCommandResult`，sender 在实现侧持有 |
| 7 | 协议适配器注册 | 实现 `ProtocolModule#protocol() / commandSpecs()` 自报命名空间 |
| 8 | HTTP 路径前缀 | `/gateway/command`（核心） + `/gateway/{protocol}/...`（协议特殊端点） |
| 9 | 配置前缀 | `gateway.*`（核心） + `gateway.{protocol}.*`（协议子前缀） |
| 10 | 1.8.0 兼容 shim | 自动补 `gb28181.` 前缀一版（1.10 移除） |

---

## 十一、风险与权衡

| 风险 | 影响 | 缓解 |
|------|------|------|
| 业务方拼 payload 出错（字段名/类型） | 调用失败、错误码不直观 | handler 内做严格校验 + 错误信息含 type 与字段路径；提供 OpenAPI/JSON Schema 文档生成 |
| 扩展 handler 误用 type 重复 | 启动期被覆盖、运行期路由错乱 | Registry 启动期 fail-fast 检测 type 重复（含跨协议） |
| ProtocolModule 自报 protocol 与 spec.type 前缀不一致 | 启动期 type 路由错乱 | Registry 启动期断言每个 spec.type 必须以 `module.protocol() + "."` 开头，否则抛 `IllegalStateException` |
| 业务侧二次反序列化遗漏字段 | payload 字段更新后业务侧静默丢值 | type 升级走 `<protocol>.<Group>.<Name>@v2` 显式版本，老 type 保留兼容期 |
| Map 序列化丢类型信息（枚举/日期） | fastjson2 `@JSONField(format=)` 等注解失效 | gateway 侧统一约定时间戳用毫秒 long、枚举用大写 String、自定义类型在文档说明；推荐业务侧用 `JSON.to(TargetClass.class, payload)` 一步反序列化 |
| BusinessNotifier 单方法导致业务侧分支爆炸 | 业务侧 switch type 写一坨 | `gateway-core` 提供 `AbstractProtocolBusinessNotifier` 按 protocol 分发；业务侧可在协议子层再按 group 拆方法（`onLifecycle/onNotify/onSession/onResponse`） |
| 表 vs 注解两套机制并存 | 业务方加新 cmdType 时不知该走哪套 | 文档明确判定准则：① "deviceId + ≤4 个标量/无重载/无默认值/无强制字段" → 走表；② 其他 → 注解。`Gb28181CommandSpecs` 文件首注释复述本规则 |
| 表驱动反射性能（每命令 1 次反射调用） | 微开销 | 启动期一次性 `Method.invoke` 句柄缓存；JIT 内联后 ≈ 直接调用；P99 增量 < 10μs，相对 SIP/HTTP IO 可忽略 |
| 表条目类型字符串拼写错（如 `int` 写成 `Int`） | 启动期失败 | `ParamBinding` 解析阶段对类型 token 做白名单校验（`int/long/double/String/<EnumClass>/<EntityClass>$<Inner>`），未识别立即抛 `IllegalArgumentException`；CI 集成测包含全表加载用例 |
| 注解方法签名约束（必须 `(GatewayCommand) → String`） | 写错签名启动期才发现 | `MethodInvokerHandler` 启动期校验签名：参数个数=1、参数类型=`GatewayCommand`、返回值=`String`，不符抛 fail-fast；提供 IDE-friendly 编译期 annotation processor 作为可选 P2 增强 |
| 业务方误用注解覆盖了内置表条目 | 默认行为被业务侧无意改写 | Registry 启动期发现注解 type ∈ 表条目 type 集合时输出 WARN 日志；建议 `@CommandMapping(value="...", overrideTable=true)` 显式声明意图 |
| 1.8.0 兼容 shim（自动补 `gb28181.` 前缀）让用户惰于迁移 | 1.10.0 删 shim 时炸一片 | shim 路径每次触发都打 WARN；CHANGELOG 1.8.0/1.9.0 都提醒；1.10.0 实现移除并在 RELEASE NOTES 单独章节列出 |

---

## 十二、与现有 1.8.0 计划的衔接

[SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md) 是 1.8.0 主纲领，决定形态；本文档承担 envelope 与命令映射表的具体语义；[GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md) v1.3+ 是执行手册。

时间线：

- **1.7.3** ✅：`ServerSessionEvent.rawSdp` 字段补齐（gateway 模块化前置）
- **1.8.0**：模块化 + envelope 协议化 + 父聚合，**一次到位**
  - sip-gateway 父聚合 + 4 子模块（core / gb28181 / bom / starter）
  - 59 出站 = 39 表条目 + 20 注解白名单方法（type 全部带 `gb28181.` 前缀）
  - 35 入站事件（emit 全部带 `gb28181.` 前缀）
  - HTTP path 全部改 `/gateway/*`
  - 配置前缀拆 `gateway.*` + `gateway.gb28181.*`

---

## 十三、后续计划占位

| 版本 | 范围 |
|------|------|
| 1.9.0 | gateway-gb28181-redis 扩展模块（RedisInviteContextStore + 候选 RedisDeviceSessionCache） |
| 1.10.0 | **gateway-onvif** 子模块（ONVIF SOAP Discovery + Imaging + PTZ）；同时移除 1.8.0 兼容 shim |
| 1.11.0 | **gateway-gt1078** 子模块（GT1078 私有 TCP 长连接） |
| 1.12.0 | gateway-rtsp 子模块（RTSP 直连） |
| 待定 | sip-gateway-webhook（HttpWebhookBusinessNotifier，HMAC-SHA256 签名）作为可选 starter |
| 待定 | sip-gateway-discovery（K8s Endpoints / Nacos `gateway.nodes` 动态发现） |
| 待定 | 同步语义补齐（`?sync=true&timeoutMs=5000` 选项 + Future 超时 504） |
| 待定 | OpenAPI/Swagger 自动生成 HTTP 文档 |
| 待定 | 内置 metrics（按 protocol 维度聚合 INVITE 成功率 / 跨节点转发耗时 / store 命中率） |

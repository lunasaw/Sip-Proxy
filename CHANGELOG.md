# Changelog

本文档记录 sip-proxy 各版本的对外可见变更。版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [1.8.0] - 2026-05-29

### 🚨 BREAKING CHANGES — 老 `gb28181-gateway` 模块下线 + 1.8.0 兼容 shim 移除

本期是 1.8.0 **最终版**，一次性切换到新 sip-gateway 父聚合形态：

- **`gb28181-gateway/` 模块整体删除**：1.7.x 业务方网关参考实现完全下线，请迁移到 `sip-gateway-spring-boot-starter`
- **包名搬家**：`io.github.lunasaw.gbproxy.gateway.*` → `io.github.lunasaw.sipgateway.{core,gb28181}.*`
- **HTTP 路径改前缀**：`/sip/*` → `/gateway/*`（老路径全部删除）
- **type 强制三段式**：`GatewayDispatchController` 不再做"无前缀自动补 `gb28181.`"shim，未知 type 直接 404
- **`BusinessNotifier` 接口签名变更**：3 方法（`deviceOnline` / `inviteIncoming` / `alarm`）→ 1 方法 `notify(GatewayEvent)` envelope
- **配置 namespace 重组**：部分 `gateway.*` 键名搬到 `gateway.gb28181.*`

### ✨ Features — sip-gateway 父聚合 + envelope 协议化（[doc/plans/1.8.0/](doc/plans/1.8.0/)）

把寄居在 `gb28181-test/...gateway/` 的"业务方网关"参考实现升级为正式 Maven 父聚合 `sip-gateway/`，业务方通过 `sip-gateway-spring-boot-starter` 一键接入。新增 `gateway-core` 协议中立内核 + `gateway-gb28181` 协议适配器，未来加协议（ONVIF/GT1078）只需新建子模块、starter `AutoConfiguration.imports` 加一行、`gateway-core` 与本期文档零改动。

**新增模块拓扑：**

```
sip-proxy/sip-gateway/                                ← 父聚合（packaging=pom）
├── gateway-core/                                     ← 协议中立内核
├── gateway-gb28181/                                  ← GB28181 协议适配器
├── sip-gateway-bom/                                  ← 依赖管理 BOM
└── sip-gateway-spring-boot-starter/                  ← 一键接入 Starter
```

**核心抽象（`gateway-core`）：**

- **Envelope 三件套**（`io.github.lunasaw.sipgateway.core.api.envelope`）
  - `GatewayCommand{type, deviceId, payload, requestId}`：业务 → gateway 入参
  - `GatewayCommandResult{correlationId, type, nodeId}`：gateway → 业务出参
  - `GatewayEvent{type, deviceId, correlationId, timestampMs, payload, nodeId}`：gateway → 业务回调
- **SPI 接口**（7 个）
  - `CommandHandler`、`@CommandMapping`、`CommandSpec`、`ParamBinding`、`ProtocolModule`、`TransactionContextStore<K,V>`、`BusinessNotifier`
- **核心实现**（4 个）
  - `CommandHandlerRegistry` 跨协议聚合，启动期 fail-fast（type 重复 / 命名空间不一致 / 注解签名错）；分两步装配（构造期注册 ProtocolModule 静态表，`SmartInitializingSingleton.afterSingletonsInstantiated()` 扫注解）避免循环依赖
  - `ReflectiveCommandHandler` 表驱动（启动期反射查找方法）
  - `MethodInvokerHandler` 注解方法运行期适配
  - `PayloadCodec` fastjson2 二次反序列化封装
- **Web 层** `GatewayDispatchController`
  - `POST /gateway/command` 协议中立分发（未知 type → 404）
  - `GET /gateway/whoami` 节点身份探测
- **Notifier**：`NoopBusinessNotifier`（默认日志，启动 warn）+ `AbstractProtocolBusinessNotifier`（按 protocol 分发）

**GB28181 适配器（`gateway-gb28181`）：**

- `Gb28181Module implements ProtocolModule` 自报 `"gb28181"` 命名空间
- `Gb28181CommandSpecs.declare()` 静态命令表（30 条简单命令）
- `Gb28181WhitelistHandlers`（17 个 `@CommandMapping` 复杂命令：PTZ/FI/Cruise/Invite/AlarmQuery 等）
- `Gb28181EventForwarder` 实现 4 个 listener × 35 个 emit 方法
- `InviteContextStore extends TransactionContextStore<String, InviteContext>` + `InMemoryInviteContextStore` 默认实现（启动 warn 提示生产换 Redis）
- `Gb28181InviteResponseController` 异步回包：`POST /gateway/gb28181/invite/response`，跨节点路由（410/502/503 错误码契约）
- AutoConfig 双重守门：`@ConditionalOnClass(ServerCommandSender)` + `@ConditionalOnBean(ServerCommandSender)`

**type 命名规则（三段式）：**

```
type ::= <protocol>.<Group>.<Name>

protocol ∈ { gb28181 | onvif | gt1078 | rtsp | ... }    与 ProtocolModule#protocol() 一致
Group    ∈ { Query | Subscribe | Control | Config | Invite | Device | Lifecycle | Notify | Response | Session }
Name     := 与 GBT-2022 cmdType 严格一致
```

示例：`gb28181.Query.Catalog`、`gb28181.Control.Ptz`、`gb28181.Invite.Play`、`gb28181.Lifecycle.Online`、`gb28181.Notify.Alarm`、`gb28181.Session.ServerInvite`、`gb28181.Response.Catalog`。

**配置 namespace 分层：**

```yaml
gateway:
  node-id: node-1
  nodes:                          # 多节点部署用，跨节点 INVITE 回包路由
    node-1: http://10.0.0.1:8080
  forward-timeout-ms: 3000
  gb28181:
    invite-context-ttl-ms: 30000
    invite-idempotency-window-ms: 5000
```

**业务方接入示例：**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
    <version>1.8.0</version>
</dependency>
```

```java
// Application.java
@SpringBootApplication
@EnableSipServer
public class MyGatewayApp {
    public static void main(String[] args) { SpringApplication.run(MyGatewayApp.class, args); }
}

// 业务方实现 BusinessNotifier 推送 envelope（异步）
@Component
public class HttpWebhookNotifier extends AbstractProtocolBusinessNotifier {
    @Override
    @Async
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        // 推到 HTTP / MQ / Webhook
    }
}
```

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

### 🛡️ CI 守门

- 新增 `scripts/check-gateway-core-purity.sh`：在 `mvn verify` 阶段检查 `gateway-core/src/main/java` 的 import 语句不含 GB28181/ONVIF/GT1078/RTSP 等协议关键字，强制核心壳协议中立。
- `CommandHandlerRegistry` 启动期 fail-fast：① type 重复（含跨协议）② ProtocolModule 自报 protocol 与 spec.type 前缀不一致 ③ `@CommandMapping` 方法签名不符 `(GatewayCommand) → String`。

### 📦 Migration Guide（必读）

**1.7.x 用户必须做的迁移：**

1. 删除 `gb28181-gateway` 依赖：

   ```diff
   - <dependency>
   -     <groupId>io.github.lunasaw</groupId>
   -     <artifactId>gb28181-gateway</artifactId>
   - </dependency>
   + <dependency>
   +     <groupId>io.github.lunasaw</groupId>
   +     <artifactId>sip-gateway-spring-boot-starter</artifactId>
   + </dependency>
   ```

2. 包名 / 接口替换：

   ```diff
   - import io.github.lunasaw.gbproxy.gateway.api.BusinessNotifier;
   - import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
   - import io.github.lunasaw.gbproxy.gateway.store.InMemoryInviteContextStore;
   + import io.github.lunasaw.sipgateway.core.api.BusinessNotifier;
   + import io.github.lunasaw.sipgateway.core.config.GatewayProperties;
   + import io.github.lunasaw.sipgateway.gb28181.store.InMemoryInviteContextStore;
   ```

3. `BusinessNotifier` 接口从 3 方法改为 1 方法 envelope：

   ```diff
   - public class MyNotifier implements BusinessNotifier {
   -     public void deviceOnline(String deviceId, RegisterInfo info) { ... }
   -     public void inviteIncoming(String callId, ...) { ... }
   -     public void alarm(String deviceId, DeviceAlarmNotify notify) { ... }
   - }
   + public class MyNotifier extends AbstractProtocolBusinessNotifier {
   +     @Override
   +     @Async
   +     protected void onProtocolEvent(String protocol, GatewayEvent event) {
   +         switch (event.type()) {
   +             case "gb28181.Lifecycle.Online" -> ...
   +             case "gb28181.Session.ServerInvite" -> ...
   +             case "gb28181.Notify.Alarm" -> ...
   +         }
   +     }
   + }
   ```

4. HTTP 调用路径全部改：`/sip/*` → `/gateway/command`（envelope 化）+ `/gateway/gb28181/invite/response`（INVITE 回包）

5. type 必须三段式（如 `gb28181.Query.Catalog`），1.8.0 起未知前缀直接 404，无 fallback。

### 🔧 协议层小升级（1.7.3 内含）

`ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)` 已在 1.7.x 中落地。1.8.0 的 `gb28181.Session.ServerInvite` envelope 直接透传 rawSdp，业务侧需要把原始 SDP 转给 ZLM/SRS 推流时取此字段，避免 `GbSessionDescription` 反向序列化丢字段。

### 🔄 仓库版本号

仓库版本从 `1.7.2` 整体升级到 `1.8.0`，模块从 14 个调整为 13 个（删除老 `gb28181-gateway`，新增 4 个 `sip-gateway/*` 子模块）。

---

## [1.7.0] - 2026-05-25

### 🚨 BREAKING CHANGES — 出站 Dialog 维护（[doc/plans/1.7.0/OUTBOUND-DIALOG-PLAN.md](doc/plans/1.7.0/OUTBOUND-DIALOG-PLAN.md)）

修复出站 BYE 不携带 to-tag 导致设备返回 `481 Call leg/Transaction does not exist` 的协议合规问题。同源同病的 SUBSCRIBE 续订 / 退订也一并改造为 dialog-aware 路径，避免长期被掩盖的协议错误（详见方案文档 v1.2 §3.2.10–§3.2.14）。

**API 变更：**

- `ServerCommandSender.deviceBye(String deviceId, String callId)` → `deviceBye(String callId)`：deviceId 已包含在 dialog 中，无需再传。
- `ClientCommandSender.sendByeCommand(FromDevice, ToDevice)` → `sendByeCommand(String callId)`：client 主动 BYE 同样要求 dialog 已建立。
- `SipSender.doByeRequest(FromDevice, ToDevice)` **直接删除**：改为 `doByeRequest(String callId)`，必须先有已建立的 dialog。无 dialog 时抛 `DialogNotFoundException`。**直接删除而非 deprecated 桥接**，让编译期一次性暴露所有调用点。
- `CommandContext.forAckBye(role, from, to, callId, "BYE")` → `CommandContext.forBye(role, callId)`：BYE 场景退役 forAckBye，ACK 场景仍可用 forAckBye。

**新增 SUBSCRIBE 续订 / 退订 API：**

- `ServerCommandSender.refreshSubscribe(String callId, int expires)` / `refreshSubscribe(callId, content, expires)` / `unsubscribe(callId)`
- `ClientCommandSender.refreshSubscribe(...)` / `unsubscribe(...)`
- `SipSender.doSubscribeRefresh(String callId, String content, int expires)`
- `CommandContext.forSubscribeRefresh(String role, String callId, String content, int expires)`

### ✨ Features

- 新增 `DialogRegistry`（进程内出站 dialog 注册表，[sip-common/.../DialogRegistry.java](sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/DialogRegistry.java)），承载 INVITE / SUBSCRIBE 两类 entry，含 `expiresAtMs` / `kind` 元数据。
- 新增 `DialogNotFoundException`，让 BYE / SUBSCRIBE refresh 调用错误在第一时间暴露而不是被 481 掩盖。
- 新增 `SipMessageTransmitter.transmitStateful(...)` / `transmitStatefulPreRegister(...)` —— 走 ClientTransaction，JAIN-SIP 自动建立 Dialog；先注册 dialog 再 sendRequest，覆盖同机回环 / 极低延迟链路下的响应竞态。
- INVITE / SUBSCRIBE 改走有状态发送（client 与 server 同步生效，共用同一 strategy），自动建立 JAIN-SIP Dialog。
- INVITE 200 OK 的 ACK 改用 `dialog.sendAck`，与 BYE 路径对称（`InviteResponseProcessor.sendAck`）。
- `AbstractSipListener.processDialogTerminated` 钩子接入 `DialogRegistry.remove`，BYE 后自动清理（INVITE 主清理路径）。
- 新增 `DialogRegistryCleaner`（`@Scheduled` 60s 跑一次），覆盖 SUBSCRIBE 自然过期无 `DialogTerminatedEvent` 的场景（RFC 6665 §4.4.1 case 3）。

### 🐛 Bug Fixes

- 修复出站 BYE 不携带 to-tag 导致设备返回 `481 Call leg/Transaction does not exist` 的协议合规问题。问题对 server 主动 BYE 与 client 主动 BYE 同源同病，本次一并修复。
- 修复 SUBSCRIBE 续订 / 退订使用 stateless 路径长期产生孤儿订阅的隐性脏数据问题（设备端订阅不断重复创建，浪费带宽）。
- 修复 `InvitePlayFlowTest.invitePlay_thenBye_shouldEndSession` 因不断言 BYE 状态码 / dialog 清理掩盖上述 bug 的测试盲区，新增 `SubscribeRefreshFlowTest` 覆盖 SUBSCRIBE 续订 / 退订路径。

### 📦 Migration Guide

**Server 侧 BYE：**

```diff
- commandSender.deviceBye(deviceId, callId);
+ commandSender.deviceBye(callId);
```

**Client 侧 BYE：**

```diff
- ClientCommandSender.sendByeCommand(fromDevice, toDevice);
+ ClientCommandSender.sendByeCommand(callId);
```

**SUBSCRIBE 续订 / 退订（新增 API，业务方按需替换历史"重发新 SUBSCRIBE"逻辑）：**

```java
String callId = commandSender.deviceCatalogSubscribe(deviceId, 3600, "Catalog");
// ... 业务运行 ...
commandSender.refreshSubscribe(callId, 3600);   // 续订
// ... 不再需要 ...
commandSender.unsubscribe(callId);               // 退订
```

业务方对 `deviceBye` / `sendByeCommand` / `refreshSubscribe` / `unsubscribe` 调用应增加 try-catch：

```java
try {
    commandSender.deviceBye(callId);
} catch (DialogNotFoundException e) {
    // dialog 已不存在（如对端先发 BYE / INVITE 还未 200 OK / callId 错误）
    log.warn("BYE 失败：dialog 不存在", e);
}
```

---

## [1.5.0] - 2026-05-24

### Added — Listener 化业务接口分层（[doc/architecture/LISTENER-LAYERED-DESIGN.md](doc/architecture/LISTENER-LAYERED-DESIGN.md)）

业务方接入完全脱离 SIP 协议细节，统一为 listener interface 模式：

**Client 端（设备侧）新增 5 个 listener 接口 + 一站式适配基类**
- `gb28181-client/api/QueryListener` — 14 个查询方法（Catalog/DeviceInfo/DeviceStatus/RecordInfo/Alarm/ConfigDownload×2/Preset/MobilePosition/PTZPosition/SDCardStatus/HomePosition/CruiseTrack(List)），返回非 null 时 Adapter 自动回包
- `gb28181-client/api/ControlListener` — 14 个控制 hook（13 个 cmdType=DeviceControl 子类型 + 1 个 Keepalive），fire-and-forget
- `gb28181-client/api/ConfigListener` — 5 个配置 hook（SnapShotConfig / OsdConfig / AlarmReportConfig / VideoAlarmRecordConfig / DeviceConfigControl），用 `Class<?> → Consumer` 显式映射避免未来父子化重构时的 instanceof 顺序陷阱
- `gb28181-client/api/SubscribeListener` — 3 个订阅 hook（Catalog/Alarm/MobilePosition），fire-and-forget，200 OK 由协议层同步返回，listener 不能拒绝订阅
- `gb28181-client/api/NotifyListener` — Broadcast 通知 hook
- `gb28181-client/api/ClientGb28181Adapter` — 一站式适配基类，业务方继承即获得所有 hook，按需 override

**Client 端新增 6 个 L1 协议事件**
- `eventbus/event/ClientQueryEvent` — rootType=Query 的统一外层事件，多态承载 19 类 query payload
- `eventbus/event/ClientControlEvent` — rootType=Control + cmdType=DeviceControl
- `eventbus/event/ClientKeepaliveEvent` — cmdType=Keepalive 独立事件（语义是状态上报，与控制指令拆开）
- `eventbus/event/ClientConfigEvent` — cmdType=DeviceConfig
- `eventbus/event/ClientSubscribeEvent` — method=SUBSCRIBE
- `eventbus/event/ClientNotifyEvent` — rootType=Notify

**Client 端新增 ClientListenerAdapter 内部分发器**
- `eventbus/internal/ClientListenerAdapter` — 监听 6 个 L1 事件，按 payload 类型分发到 listener 接口方法
- Query listener 通过 `ObjectProvider#getIfUnique()` 强制单 bean，多实例 fail fast；缺失时首次告警一次后静默走默认空响应
- Control / Config / Subscribe / Notify listener 用 `List<>` 注入，全部调用（观察者模式，业务+metrics+audit 可同时监听）
- supplier 强转 ToDevice 失败时给明确错误信息而非裸 ClassCastException

**Client 端新增协议层订阅注册表**
- `transmit/request/subscribe/SubscribeRegistry` — SUBSCRIBE 处理器在发出 200 OK 时直接调 `SubscribeRegistry.put()` 维护，业务方无感知

**Server 端新增 4 个 listener 接口 + 一站式适配基类**
- `gb28181-server/api/DeviceResponseListener` — 14 个应答 hook（设备主动应答 query 类）
- `gb28181-server/api/DeviceNotifyListener` — 6 个主动通知 hook
- `gb28181-server/api/DeviceLifecycleListener` — 5 个生命周期 hook（注册/挑战/在线/离线/远端地址变更）
- `gb28181-server/api/DeviceSessionListener` — 7 个 INVITE/BYE/ACK 状态机 hook
- `gb28181-server/api/ServerGb28181Adapter` — 一站式适配基类

**Server 端新增 ServerListenerAdapter**
- `transmit/event/internal/ServerListenerAdapter` — 监听 32 个底层 typed `Device*Event` / `ServerInviteEvent`，转发到 4 个 listener 接口

### Removed — Client 端旧业务接口与 10 个 query/config/subscribe 事件

按 v1.5.0 一刀切策略，client 端业务方旧接入方式全部删除：

**删除的 4 个旧业务接口**
- `transmit/request/message/MessageRequestHandler` —— 12 个方法
- `transmit/request/message/CustomMessageRequestHandler` —— 默认空实现
- `transmit/request/message/handler/control/DeviceControlRequestHandler` —— 13 个方法
- `transmit/request/subscribe/SubscribeRequestHandler` + 默认实现 `DefaultSubscribeProcessor`

**删除的 10 个旧 client 事件**
- `eventbus/event/ClientPtzPositionQueryEvent`
- `eventbus/event/ClientSdCardStatusQueryEvent`
- `eventbus/event/ClientHomePositionQueryEvent`
- `eventbus/event/ClientCruiseTrackListQueryEvent`
- `eventbus/event/ClientCruiseTrackQueryEvent`
- `eventbus/event/ClientSnapShotConfigEvent`
- `eventbus/event/ClientOsdConfigEvent`
- `eventbus/event/ClientAlarmReportConfigEvent`
- `eventbus/event/ClientVideoAlarmRecordConfigEvent`
- `eventbus/event/ClientAlarmSubscribeEvent`

**保留的 8 个 SIP method 系 client 事件**（与 SIP method 强绑定，不属于 MESSAGE 体系，继续作为协议层规范 API）
- `ClientInviteEvent` / `ClientByeEvent` / `ClientAckEvent` / `ClientCancelEvent` / `ClientInfoEvent`
- `ClientRegisterChallengeEvent` / `ClientRegisterFailureEvent` / `ClientRegisterSuccessEvent`

### Changed — Client handler 全部改为发布外层事件

15 个 `*MessageClientHandler` 不再持有 `MessageRequestHandler` 引用，改为只 `parseXml + publishEvent`。

- `MessageClientHandlerAbstract` —— 删除 `@ConditionalOnBean(MessageRequestHandler.class)` 注解 + `messageRequestHandler` 字段 + 构造器入参
- `SubscribeHandlerAbstract` —— 删除 `subscribeRequestHandler` 字段 + 构造器入参
- 15 个具体子类 —— 构造器同步精简
- `DeviceControlMessageHandler` —— 保留 13 个 XML 子标签 → Class 映射，分发改为 `publishEvent(new ClientControlEvent(...))`
- `DeviceConfigControlMessageHandler` —— 4 个 config 子分支统一发 `ClientConfigEvent`
- `BroadcastNotifyMessageHandler` —— 改为发 `ClientNotifyEvent`
- `KeepaliveMessageClientHandler` —— 改为发 `ClientKeepaliveEvent`
- 2 个 SUBSCRIBE handler —— 协议层内化 `SubscribeRegistry.put()`，发布 `ClientSubscribeEvent`

### Server 端非破坏性扩展（保留 32 个 typed Device*Event）

由于 server 端的 32 个 typed event 在业务侧已广泛使用，且 typed 设计（每个事件携带 typed payload）比 4 个 generic 事件 + instanceof 更安全，本次 v1.5.0 的 server 端改造采用**加性策略**：

- ✅ 新增 4 个 listener 接口 + Adapter 作为业务方一站式入口
- ✅ 32 个底层 `Device*Event` 全部保留，作为协议层规范 API
- ✅ ServerListenerAdapter 内部转发：业务方既可继续用 `@EventListener Device*Event`，也可用新 listener 接口
- ❌ 与设计文档 §3.4 的「删除 31 个 Device*Event」目标存在偏差，此偏差专为保护现有业务侧（voglander 等）兼容性而做，列为已知 trade-off

### Migration Guide（业务侧 v1.4.0 → v1.5.0）

详见 [doc/architecture/LISTENER-MIGRATION-GUIDE.md](doc/architecture/LISTENER-MIGRATION-GUIDE.md)。

业务侧约 20-30 个类受影响（以 voglander 为基准），集中在原 `MessageRequestHandler` / `DeviceControlRequestHandler` / `SubscribeRequestHandler` 实现类与对应 `@EventListener` 散点。

## [1.4.0] - 2026-05-24

### Added — GB/T 28181-2022 协议扩展

按 [doc/GBT-28181-2022-IMPLEMENTATION-PLAN.md](doc/GBT-28181-2022-IMPLEMENTATION-PLAN.md) 落地，13 个阶段全部完成：

**P0 优先级（独立流程，可单独跑通）**
- §9.13 设备软件升级：`DeviceUpgradeControl` 控制 + `UpgradeResultNotify` 结果通知
  - sender：`ServerCommandSender.deviceUpgrade(...)` / `ClientCommandSender.sendUpgradeResultNotify(...)`
  - 事件：`DeviceUpgradeResultEvent`
  - cmdType 新增：`DeviceUpgradeResult`
- §9.14 图像抓拍：`SnapShotConfig`（cmdType=DeviceConfig）+ `UploadSnapShotFinishedNotify`
  - sender：`ServerCommandSender.deviceSnapShot(...)` / `ClientCommandSender.sendSnapShotFinishedNotify(...)`
  - 事件：`DeviceSnapShotFinishedEvent` / `ClientSnapShotConfigEvent`
  - cmdType 新增：`UploadSnapShotFinished`

**P1 优先级（沿用现有 MessageCommandStrategy）**
- A.2.3.1.11 / A.2.4.13 / A.2.6.15 PTZ 精准控制 + 精确状态：
  - `DeviceControlPTZPrecise`、`PTZPositionQuery`、`PTZPositionResponse`
  - sender：`deviceControlPtzPrecise(...)`, `devicePtzPositionQuery(...)`, `sendPtzPositionResponse(...)`
  - 事件：`DevicePtzPositionEvent` / `ClientPtzPositionQueryEvent`
  - cmdType 新增：`PTZPosition`
- A.2.4.14 / A.2.6.16 / A.2.3.1.13 存储卡状态查询 + 格式化：
  - `SDCardStatusQuery`、`SDCardStatusResponse`、`DeviceControlSDCardFormat`
  - sender：`deviceSdCardStatusQuery(...)`, `deviceControlFormatSDCard(...)`, `sendSdCardStatusResponse(...)`
  - 事件：`DeviceSdCardStatusEvent` / `ClientSdCardStatusQueryEvent`
  - cmdType 新增：`SDCardStatus`

**P2 优先级（涉及新 CmdType 路由分支）**
- A.2.4.10 / A.2.6.12 / A.2.3.1.10 看守位查询 + 控制：
  - `HomePositionQuery`、`HomePositionResponse`（控制实体复用现有 `DeviceControlPosition`）
  - sender：`deviceControlHomePosition(...)`, `deviceHomePositionQuery(...)`, `sendHomePositionResponse(...)`
  - 事件：`DeviceHomePositionEvent` / `ClientHomePositionQueryEvent`
  - cmdType 新增：`HomePositionQuery`
- A.2.4.11 / A.2.4.12 / A.2.6.13 / A.2.6.14 巡航轨迹查询：
  - `CruiseTrackListQuery`、`CruiseTrackQuery`、`CruiseTrackListResponse`、`CruiseTrackResponse`
  - sender：`deviceCruiseTrackListQuery(...)`, `deviceCruiseTrackQuery(...)`, `sendCruiseTrackListResponse(...)`, `sendCruiseTrackResponse(...)`
  - 事件：`DeviceCruiseTrackEvent`（type=LIST/SINGLE）/ `ClientCruiseTrackListQueryEvent` / `ClientCruiseTrackQueryEvent`
  - cmdType 新增：`CruiseTrackListQuery`、`CruiseTrackQuery`
- A.2.3.1.7 / A.2.3.1.8 / A.2.3.1.9 强制关键帧 + 拉框放大/缩小 server sender 补齐
  - sender：`deviceControlIFrame(...)`, `deviceControlDragZoomIn(...)`, `deviceControlDragZoomOut(...)`
  - client 入站路由原已存在
- A.2.3.1.14 目标跟踪：
  - `DeviceControlTargetTrack`（含 TargetArea 内嵌结构）
  - sender：`deviceControlTargetTrack(...)`
  - client 接口扩展：`DeviceControlRequestHandler.handleTargetTrack(...)`
- §9.11.1 / §9.11.2 报警事件订阅与通知：
  - sender：`deviceAlarmSubscribe(...)`, `sendAlarmNotify(...)`
  - 客户端订阅入站：`SubscribeAlarmQueryMessageHandler` → `ClientAlarmSubscribeEvent`
  - **附带修复**：`SubscribeCommandStrategy` 现在正确从 CommandContext.extras 提取
    `subscribeInfo` 并透传，之前 SUBSCRIBE 请求会因 `subscribeInfo is null` 失败

**P3 优先级（多消息会话或外部依赖）**
- A.2.3.2 设备配置扩展（代表性子集）：OSD / VideoAlarmRecord / AlarmReport
  - 实体：`OsdConfig`、`VideoAlarmRecordConfig`、`AlarmReportConfig`（`entity/control/cfg/`）
  - sender：`deviceConfigOsd(...)`, `deviceConfigVideoAlarmRecord(...)`, `deviceConfigAlarmReport(...)`
  - 事件：`ClientOsdConfigEvent` / `ClientVideoAlarmRecordConfigEvent` / `ClientAlarmReportConfigEvent`
  - 入站路由：`DeviceConfigControlMessageHandler` 通过 XML 子标签分发
  - 其余子配置（VideoRecordPlan / PictureMask / FrameMirror / SVAC* / VideoParamAttribute）按相同模式扩展即可
- §9.9 视音频文件下载：sender `deviceInviteDownload(...)` + `InviteEntity.getInviteDownloadBody(...)` 支持 `s=Download` + `a=downloadspeed`
- §9.8.3.2 INFO MANSRTSP 结构化：`ManSrtspRequest` + `ManSrtspParser`，
  `ClientInfoEvent` 新增 `contentType` / `parsed` 字段（保留 `content` 字段兼容）
- §9.12.2 语音对讲：`InviteSessionNameEnum.TALK` + `InviteEntity.getInviteTalkBody(...)`
  + sender `deviceInviteTalk(...)`，audio-only sendonly SDP（PCMA/8000）

### Added — 通用基础设施
- `CmdTypeEnum` 新增枚举值：`DEVICE_UPGRADE_RESULT`、`UPLOAD_SNAP_SHOT_FINISHED`、
  `PTZ_POSITION`、`SD_CARD_STATUS`、`HOME_POSITION_QUERY`、
  `CRUISE_TRACK_LIST_QUERY`、`CRUISE_TRACK_QUERY`
- `InviteSessionNameEnum` 新增 `TALK` / `DOWNLOAD`，`isValid` 同步扩展
- `DeviceControlRequestHandler` 接口默认方法扩展：`handleDeviceUpgrade`、
  `handlePtzPreciseCtrl`、`handleFormatSDCard`、`handleTargetTrack`
  （default 空实现，业务方按需 override）

### Fixed
- `SubscribeCommandStrategy.doSend(...)` 之前忽略 `CommandContext.extras["subscribeInfo"]`，
  导致所有带 SubscribeInfo 的 SUBSCRIBE（订阅类命令）抛 `subscribeInfo is null`。
  目录订阅（`deviceCatalogSubscribe`）虽然代码路径存在，但旧代码不会真正生效。

## [1.3.0] - 2026-05-24

### BREAKING CHANGES

本次为协议解耦主版本升级，**不保留兼容期逻辑**。详见
[doc/plans/1.3.0/PROTOCOL-DECOUPLING-PLAN.md](doc/plans/1.3.0/PROTOCOL-DECOUPLING-PLAN.md)。

- **`SipUtils.parseSdp()` 返回类型变更**：由 `GbSessionDescription` 变为标准
  `SdpSessionDescription`，y= / f= 字段剥离逻辑下沉到 gb28181-common。
  GB28181 接入方需改用 `GbSdpUtils.parseGbSdp()`。
- **`SipUtils.generateGB28181Code` / `SipUtils.genSsrc` 迁移**：从 sip-common
  迁到 `gb28181-common/io.github.lunasaw.gb28181.common.entity.utils.GbUtil`。
- **`GbSessionDescription` / `GbSipDate` 包路径变更**：
  - `io.github.lunasaw.sip.common.entity.GbSessionDescription`
    → `io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription`
  - `io.github.lunasaw.sip.common.entity.GbSipDate`
    → `io.github.lunasaw.gb28181.common.entity.GbSipDate`
- **校时配置 key 替换**：`sip.gb28181.time-sync.*` → `sip.common.time-sync.*`
  （含 `.enabled`、`.ntp-sync-interval` 等所有子键）。
- **默认 UserAgent 变更**：由 `LunaSaw-GB28181-Proxy` 改为 `sip-proxy`。
  网络对端看到的 User-Agent 头会变化。
  如需保持原值，配置 `sip.common.user-agent: LunaSaw-GB28181-Proxy`。

### Added

- 新增 `SipCommonProperties.userAgent` 字段（默认 `sip-proxy`）。
- 新增 `SipCommonContextHolder`，为 `SipMessageTransmitter` /
  `FromDevice` 等静态调用点提供 `SipCommonProperties.userAgent` 的访问入口。
- `gb28181-common` 新增 `GbSdpUtils.parseGbSdp(String)`，承接原 sip-common
  的 GB SDP 解析职责。
- `gb28181-common/GbUtil` 新增 `generateGB28181Code` / `genSsrc`。
- 新增 CI 脚本 `scripts/check-sip-common-purity.sh`，校验 sip-common
  不含 GB28181 关键词。

### Removed

- 删除 `sip-common` 中的 `SubscribeHolder` / `SubscribeTask`（全仓库零调用，确认死代码）。
- 删除 `SipUtils.generateGB28181Code` / `SipUtils.genSsrc`。
- 删除 `SipUtils.parseSdp` 中的 y= / f= 剥离逻辑。
- 移除配置 key `sip.gb28181.time-sync.*`。

### Migration

| 变更点 | 迁移方式 |
|--------|---------|
| `GbSessionDescription` import | IDE 自动修复至 `io.github.lunasaw.gb28181.common.entity.sdp` |
| `GbSipDate` import | IDE 自动修复至 `io.github.lunasaw.gb28181.common.entity` |
| `SipUtils.parseSdp` 强转处 | 改用 `GbSdpUtils.parseGbSdp(...)`（编译期签名变更强制迁移） |
| `SipUtils.generateGB28181Code` / `genSsrc` | 改用 `GbUtil.generateGB28181Code` / `GbUtil.genSsrc` |
| 配置 `sip.gb28181.time-sync.*` | 改为 `sip.common.time-sync.*` |
| `SubscribeHolder` / `SubscribeTask` | 仓库核对确认无调用方；如有外部业务方依赖，自行复制保留 |
| 默认 UserAgent 还原 | 配置 `sip.common.user-agent: LunaSaw-GB28181-Proxy` |

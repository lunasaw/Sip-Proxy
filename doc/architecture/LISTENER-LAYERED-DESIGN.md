# Listener 化业务接口分层设计

> 版本：1.3 | 日期：2026-05-24 | 状态：1.5.0 已落地 | 优先级：高
>
> 📌 **运行时事实**（每行 cmdType 的 L0 类 / L1 事件 / L2 Listener 方法对应表）请参见
> [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md) —— 本文档侧重设计动机与决策，
> 矩阵文档侧重当前代码的逐行落地。新增 cmdType 时**先改矩阵，再改代码**。
>
> v1.3 修订（基于代码事实核查）：
> - **修正 §3.2 KeepaliveControl 论据**：实际继承链为 `KeepaliveControl → ControlBase → DeviceControlBase`，并非"不继承 DeviceControlBase"。独立 `ClientKeepaliveEvent` 的合理性改由"L0 handler 按 cmdType 分流"承担，而非类型差异
> - **修正 §3.3.1 / §3.3.3 Config 继承关系**：`SnapShotConfig` / `OsdConfig` / `AlarmReportConfig` / `VideoAlarmRecordConfig` / `DeviceConfigControl` 全部直接 extends `DeviceControlBase`，是兄弟节点而非父子。改用显式 `Class<?> → Consumer` 映射，规避未来重构成父子关系时的 instanceof 顺序陷阱
> - **修正 §7.2 改造前/后对比**：`DeviceControlMessageHandler` 早已使用 `HandlerEntry + BiConsumer` 模式，改造工作仅"BiConsumer 调用 → publishEvent"一行替换
> - **校准数字**：MessageRequestHandler 12 方法（原 11）、MessageClientHandlerAbstract 15 子类（原 17/13）、Client*Event 18 个（原 19）、Server Device*Event 32 个（原 31）、TestDeviceControlHandler 67 行（原 55）
> - **澄清 §6.1 broadcastNotify 路径**：`BroadcastNotifyMessageHandler` 当前已存在（15 个子类之一），改造方式与其他 handler 一致——把 `messageRequestHandler.broadcastNotify(...)` 调用替换为 `publishEvent(new ClientNotifyEvent(...))`，并在 Adapter 内分发到 `NotifyListener.onBroadcastNotify`
> - **新增 §10.1 三条风险**：ToDevice 强转、业务侧 bean 扫描路径、Adapter 与 supplier 协议
>
> v1.2 修订：
> - **新增 §3.5「自下而上的运行时视图」**：补齐入站消息从网卡 → SIP Stack → handler → 协议事件 → Adapter → listener 的完整链路图，并给出 Catalog 查询端到端时序与 7 条运行时不变量
>
> v1.1 修订（基于评审报告）：
> - **硬伤修复**：payload 父类降为 `XmlBean`（query 类继承不统一）、Java 17 兼容（去掉 switch pattern matching）、SUBSCRIBE 改 fire-and-forget（避免 SIP 事务超时）、`@ConditionalOnBean(MessageRequestHandler.class)` 级联清理在同一 PR 完成
> - **遗漏补齐**：`SubscribeRequestHandler` 接口、INVITE trying 状态、注册挑战 payload、`*ErrorEvent` 错误语义、`RemoteAddress` 路由信息全部纳入新 listener 体系
> - **PR 重排**：原 PR-2 + PR-3 合并为单个破坏性 PR，避免出现"中间状态不可用"
> - **新增**：sip-gateway / voglander 业务侧迁移影响章节
>
> 关联：[PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md)（运行时落地矩阵）、[REFACTOR-HANDLER-TO-SPRING-EVENT.md](../plans/1.3.0/REFACTOR-HANDLER-TO-SPRING-EVENT.md)、[GBT-28181-2022-IMPLEMENTATION-PLAN.md](GBT-28181-2022-IMPLEMENTATION-PLAN.md)、[LISTENER-MIGRATION-GUIDE.md](LISTENER-MIGRATION-GUIDE.md)

---

## 一、问题背景

### 1.1 v1.4.0 后的架构不一致现状

GB28181-2022 协议扩展（13 个阶段）落地后，client 端入站 message handler 出现两套并行的业务扩展机制：

| 模式 | 适用范围 | 业务方接入方式 |
|---|---|---|
| **接口回调**（v1.0 遗留） | Catalog/DeviceInfo/DeviceStatus/RecordInfo/Alarm/ConfigDownload/Preset/MobilePosition | `MessageRequestHandler` + `DeviceControlRequestHandler` 接口实现 + `@Primary` 覆盖默认实现 |
| **Spring Event**（v1.3.0 + v1.4.0 新增） | PTZPosition/SDCardStatus/HomePosition/CruiseTrack/SnapShotConfig/OsdConfig/AlarmReport/VideoAlarmRecord/AlarmSubscribe | `@EventListener` 监听 `Client*Event` |

两套机制并存导致：

1. **业务方心智负担**：同样是"客户端响应平台请求"，PTZ 控制走接口、PTZPosition 查询走事件，规则不统一
2. **接口被迫全方法实现**：`MessageRequestHandler` 12 个方法 + `DeviceControlRequestHandler` 13 个方法，业务方哪怕只关心其中一个，也得空实现剩余所有方法
3. **测试代码膨胀**：`TestMessageRequestHandler` (99 行) + `TestDeviceControlHandler` (67 行) + `TestClientEventHandler` (156 行) + `TestServerEventHandler` (61 行) = 四处响应逻辑分散，共 ~383 行
4. **新增协议规则不统一**：阶段 1-2 的 DeviceUpgrade/SnapShotConfig 走接口；阶段 3-13 走事件 —— 取决于实现者口味
5. **跨切监听难**：接口被业务方占用，框架层无法在 query/control 周边做统一 metrics/logging/tracing

### 1.2 v1.3.0 事件总线的初衷

`REFACTOR-HANDLER-TO-SPRING-EVENT.md` 已明确 v1.3.0 方向：

> 业务对接 = `@EventListener`，不再新增 `*Handler` 业务回调接口

但因为 `MessageRequestHandler` 涉及"同步返回响应数据"语义（Catalog 查询需要返回设备列表给框架打包），未一并改造。本方案补齐这块，并把 `DeviceControlRequestHandler` / `SubscribeRequestHandler` 也一并收编。

---

## 二、设计目标

### 2.1 业务方视角 SLA

业务方接入这个框架时应该感受到：

1. **不需要懂 SIP**：不知道什么是 rootType=Query / Control / cmdType=Catalog，只看方法名
2. **按需接入**：只 override 关心的方法，不写一行空方法
3. **类型安全**：拿到的是强类型 `DeviceControlPtz` 而不是 `Object`，无 cast 无 instanceof
4. **一站式**：一个 bean 涵盖所有想接入的 hook，不需要写多个 bean
5. **可监控**：框架有协议事件（query 计数、慢查询告警等）跨切层

### 2.2 框架视角约束

1. **零兼容包袱**：`MessageRequestHandler` / `DeviceControlRequestHandler` / `CustomMessageRequestHandler` / `SubscribeRequestHandler` / 10 个独立 `Client*Event` 全删
2. **协议事件保留**：跨切层仍可监听协议层事件
3. **新增协议成本固定**：加一个 cmdType 只需「加 listener 一个 hook + 改 Adapter 一行 if 分支」
4. **server / client 对称**：两侧使用同一套层级模型
5. **Java 17 兼容**：所有派发逻辑使用 `instanceof` pattern + 字符串 switch（Java 14+），不依赖 Java 21 sealed switch pattern

---

## 三、三层架构

```
┌──────────────────────────────────────────────────────────┐
│  Layer 3: 业务方代码                                       │
│  @Component                                                │
│  class MyDeviceImpl implements QueryListener,             │
│                                  ControlListener {         │
│      DeviceResponse onCatalogQuery(...)  ← 只写关心的     │
│      void onPtzControl(...)                                │
│  }                                                         │
└──────────────────┬───────────────────────────────────────┘
                   │ implements N 个 listener interface
                   │ Adapter 通过 ObjectProvider<Xxx> 自动注入
┌──────────────────▼───────────────────────────────────────┐
│  Layer 2: 业务接口 + Adapter（gb28181-client/api）        │
│                                                            │
│  Listener 接口（业务方实现）：                             │
│    QueryListener      / ControlListener                   │
│    ConfigListener     / SubscribeListener                 │
│    NotifyListener                                          │
│                                                            │
│  Adapter（框架内部，业务方零感知）：                       │
│    ClientListenerAdapter @EventListener × 5              │
│      instanceof + cmdType switch → fan-out 到 listener    │
│      query 类自动回包                                      │
└──────────────────┬───────────────────────────────────────┘
                   │ Spring ApplicationEvent
┌──────────────────▼───────────────────────────────────────┐
│  Layer 1: 协议层事件（gb28181-client/eventbus）           │
│                                                            │
│  ClientQueryEvent      ← rootType=Query                   │
│  ClientControlEvent    ← rootType=Control, DeviceControl  │
│  ClientConfigEvent     ← rootType=Control, DeviceConfig   │
│  ClientSubscribeEvent  ← method=SUBSCRIBE                 │
│  ClientNotifyEvent     ← rootType=Notify (含 Broadcast)   │
│                                                            │
│  → 任何 bean 可监听用于跨切（metrics / audit / tracing）   │
└──────────────────┬───────────────────────────────────────┘
                   │ publishEvent
┌──────────────────▼───────────────────────────────────────┐
│  Layer 0: 协议解析（MessageHandler 系列）                  │
│                                                            │
│  CatalogQueryMessageClientHandler                         │
│  PtzPositionQueryMessageClientHandler                     │
│  DeviceControlMessageHandler        (XML 子标签 → Class)  │
│  DeviceConfigControlMessageHandler  (XML 子标签 → Class)  │
│  ...                                                       │
│                                                            │
│  parseXml → publishEvent(外层事件 with 多态 payload)       │
└──────────────────────────────────────────────────────────┘
```

### 3.1 Layer 0：协议解析（最薄一层）

每个 `*MessageClientHandler` 只做两件事：
1. 反序列化 XML → 强类型 entity
2. 发对应的外层 Layer 1 事件

```java
@Component
@RequiredArgsConstructor
public class CatalogQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public static final String CMD_TYPE = "Catalog";
    private final ApplicationEventPublisher publisher;

    @Override public String getRootType() { return QUERY; }
    @Override public String getCmdType()  { return CMD_TYPE; }

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceQuery query = parseXml(DeviceQuery.class);
        DeviceSession session = getDeviceSession(event);
        publisher.publishEvent(new ClientQueryEvent(this,
                session.getUserId(), session.getSipId(), query));
    }
}
```

**关键变化**：handler 不再持有 `MessageRequestHandler` / `DeviceControlRequestHandler` 引用，不再调业务接口，不再自动发响应 —— 这些职责挪到 Adapter。

> ⚠️ **Control / Config handler 例外**：`DeviceControlMessageHandler` 和 `DeviceConfigControlMessageHandler` 因 cmdType 单一但 XML 子标签多态（PTZCmd / TeleBoot / RecordCmd / ... 共 13 种 control；OsdConfig / SnapShotConfig / ... 共 N 种 config），**XML 子标签 → 具体 Class 的映射逻辑仍保留在 handler 内**，只是 `parseObj` 之后改为 `publishEvent(new ClientControlEvent(this, userId, typedCmd))`，把 typed payload 交给 Adapter 做 `instanceof` 分发。这样 Adapter 只需识别 Java 类型，不需要重复 XML 字符串匹配。

### 3.2 Layer 1：协议层事件（5 类）

按 SIP rootType + method 分类，承载多态 payload。

```java
package io.github.lunasaw.gbproxy.client.eventbus.event;

/**
 * 平台主动查询：rootType=Query，cmdType ∈ {Catalog/DeviceInfo/DeviceStatus/RecordInfo/Alarm/...}
 *
 * payload 父类是 XmlBean 而非 DeviceBase ——
 * gb28181-common 中 DeviceRecordQuery / DeviceAlarmQuery / MobilePositionQuery / PresetQuery /
 * DeviceMobileQuery / DeviceInfoQuery / DeviceStatusQuery / CatalogQuery / RecordInfoQuery /
 * AlarmQuery / ConfigDownloadQuery / TalkQuery 直接 extends XmlBean，
 * 只有 DeviceQuery / PTZPositionQuery / SDCardStatusQuery / HomePositionQuery /
 * CruiseTrackQuery / CruiseTrackListQuery / DeviceConfigDownload 经过 DeviceBase。
 *
 * 不在 v1.5.0 范围内统一这两条继承链 —— 那是 v2.0 的事。
 */
@Getter
public class ClientQueryEvent extends ApplicationEvent {
    private final String userId;       // 当前客户端 ID
    private final String sipId;        // 平台 ID
    private final XmlBean query;       // 多态：DeviceQuery / DeviceRecordQuery / DeviceAlarmQuery / PTZPositionQuery / ...

    public ClientQueryEvent(Object source, String userId, String sipId, XmlBean query) {
        super(source);
        this.userId = userId;
        this.sipId = sipId;
        this.query = query;
    }
}

/** 平台控制：rootType=Control，cmdType=DeviceControl */
@Getter
public class ClientControlEvent extends ApplicationEvent {
    private final String userId;
    private final DeviceControlBase command;  // 多态：DeviceControlPtz / DeviceControlTeleBoot / ...
                                              // 注：13 个 control 类全部直接 extends DeviceControlBase。
                                              // KeepaliveControl 虽然也间接继承 DeviceControlBase，
                                              // 但走独立的 ClientKeepaliveEvent 路径，不会进入这个事件。
}

/** 平台 Keepalive：rootType=Control，cmdType=Keepalive
 *
 *  独立事件而非合并入 ClientControlEvent ——
 *  ⚠️ v1.3 事实修正：`KeepaliveControl` 实际通过 `ControlBase` 间接继承 `DeviceControlBase`
 *  （继承链：KeepaliveControl → ControlBase → DeviceControlBase → DeviceBase → XmlBean），
 *  所以"类型不同"不是拆分理由。真正的拆分理由有两个：
 *    (a) **语义不同**：心跳是状态上报，不是控制指令；业务方对它的处理逻辑（刷新 lastSeen）
 *        与 PTZ 等控制完全不同
 *    (b) **L0 分流安全**：`KeepaliveMessageClientHandler`（cmdType=Keepalive）和
 *        `DeviceControlMessageHandler`（cmdType=DeviceControl）是两个独立 L0 handler，
 *        发不同事件，Adapter 在 L2 不必再做 instanceof 区分（避免 KeepaliveControl
 *        被 ClientControlEvent.command 字段的 DeviceControlBase 类型误吞）
 */
@Getter
public class ClientKeepaliveEvent extends ApplicationEvent {
    private final String userId;
    private final KeepaliveControl keepalive;
}

/** 平台配置：rootType=Control，cmdType=DeviceConfig */
@Getter
public class ClientConfigEvent extends ApplicationEvent {
    private final String userId;
    private final DeviceControlBase config;   // 多态：SnapShotConfig / OsdConfig / AlarmReportConfig /
                                              // VideoAlarmRecordConfig / DeviceConfigControl。
                                              // ⚠️ v1.3 事实修正：5 个 config 类全部**直接** extends
                                              // DeviceControlBase，互为兄弟节点（不是父子关系）。
                                              // 这意味着 instanceof 顺序当前不敏感，但任何未来重构
                                              // （比如把 4 个具体 config 改成 extends DeviceConfigControl）
                                              // 都会引入顺序陷阱，因此 Adapter 用 Class<?> → Consumer
                                              // 显式映射，参见 §3.3.3。
}

/** 平台订阅：method=SUBSCRIBE */
@Getter
public class ClientSubscribeEvent extends ApplicationEvent {
    private final String userId;
    private final String sipId;
    private final Integer expires;
    private final XmlBean body;               // 多态：DeviceQuery (Catalog 订阅) / DeviceAlarmQuery (报警订阅) / ...
}

/** 平台通知（客户端方向：Broadcast 语音广播是核心场景） */
@Getter
public class ClientNotifyEvent extends ApplicationEvent {
    private final String userId;
    private final XmlBean notify;
}
```

### 3.3 Layer 2：业务接口 + Adapter

#### 3.3.1 Listener 接口设计（5 个，全 default 方法）

```java
package io.github.lunasaw.gbproxy.client.api;

/**
 * 平台查询监听器：
 * - 方法返回响应对象 = 框架自动回包
 * - 返回 null = 框架按默认空响应（或不回包，取决于具体 cmdType 的 SIP 语义）
 *
 * 多 listener 处理策略（v1.1 修订）：
 * - **强制单 bean** —— Adapter 通过 ObjectProvider#getIfUnique() 注入
 * - 业务方注册 0 个 listener：所有查询走默认空响应
 * - 业务方注册 1 个 listener：正常分发
 * - 业务方注册 ≥2 个 listener：启动期抛 NoUniqueBeanDefinitionException（fail fast）
 *
 * 改动原因：原 firstNonNull 设计依赖 Spring 注入顺序，多 listener 时返回行为
 * 静默且不确定（mock / 真实 listener 同时存在的场景容易踩坑）。强制单实例
 * 更符合"业务方零心智"目标。如果业务方真有多 listener 聚合需求，由其自己
 * 在 Layer 1 协议事件做合并即可。
 */
public interface QueryListener {
    default DeviceResponse        onCatalogQuery(String platformId, DeviceQuery q)            { return null; }
    default DeviceInfo            onDeviceInfoQuery(String platformId, DeviceQuery q)         { return null; }
    default DeviceStatus          onDeviceStatusQuery(String platformId, DeviceQuery q)       { return null; }
    default DeviceRecord          onRecordInfoQuery(String platformId, DeviceRecordQuery q)   { return null; }
    default DeviceAlarmNotify     onAlarmQuery(String platformId, DeviceAlarmQuery q)         { return null; }
    default DeviceConfigResponse  onConfigDownloadQuery(String platformId, DeviceConfigDownload q) { return null; }
    default PresetQueryResponse   onPresetQuery(String platformId, PresetQuery q)             { return null; }
    default MobilePositionNotify  onMobilePositionQuery(String platformId, MobilePositionQuery q) { return null; }
    default PTZPositionResponse   onPtzPositionQuery(String platformId, PTZPositionQuery q)   { return null; }
    default SDCardStatusResponse  onSdCardStatusQuery(String platformId, SDCardStatusQuery q) { return null; }
    default HomePositionResponse  onHomePositionQuery(String platformId, HomePositionQuery q) { return null; }
    default CruiseTrackListResponse onCruiseTrackListQuery(String platformId, CruiseTrackListQuery q) { return null; }
    default CruiseTrackResponse   onCruiseTrackQuery(String platformId, CruiseTrackQuery q)   { return null; }
}

/**
 * 平台控制监听器：fire-and-forget。
 * 所有方法返回 void，因为控制命令不需要业务返回数据（200 OK 由协议层自动回）。
 *
 * 多 listener 策略：全部调用（观察者模式，metrics / audit / 业务可同时监听同一控制命令）。
 */
public interface ControlListener {
    default void onPtzControl(String platformId, DeviceControlPtz cmd)        {}
    default void onTeleBoot(String platformId, DeviceControlTeleBoot cmd)     {}
    default void onRecord(String platformId, DeviceControlRecordCmd cmd)      {}
    default void onGuard(String platformId, DeviceControlGuard cmd)           {}
    default void onAlarmReset(String platformId, DeviceControlAlarm cmd)      {}
    default void onIFrame(String platformId, DeviceControlIFame cmd)          {}
    default void onDragIn(String platformId, DeviceControlDragIn cmd)         {}
    default void onDragOut(String platformId, DeviceControlDragOut cmd)       {}
    default void onHomePositionControl(String platformId, DeviceControlPosition cmd) {}
    default void onDeviceUpgrade(String platformId, DeviceUpgradeControl cmd) {}
    default void onPtzPrecise(String platformId, DeviceControlPTZPrecise cmd) {}
    default void onFormatSdCard(String platformId, DeviceControlSDCardFormat cmd) {}
    default void onTargetTrack(String platformId, DeviceControlTargetTrack cmd) {}
    default void onKeepalive(String platformId, KeepaliveControl cmd)         {}
}

/**
 * 平台配置监听器：cmdType=DeviceConfig 的子集。
 *
 * ⚠️ v1.3 事实修正：5 个 config 类（SnapShotConfig / OsdConfig / AlarmReportConfig /
 * VideoAlarmRecordConfig / DeviceConfigControl）全部直接 extends DeviceControlBase，
 * 互为兄弟节点。当前 instanceof 顺序不会引发"父类先匹配"陷阱，但 Adapter 仍采用
 * **Class<?> → Consumer 显式映射**实现分发（见 §3.3.3），原因有三：
 *   (1) 一致性：与 Control 同结构的 instanceof 链对照，但 Config 子类后续可能
 *       重构成 extends DeviceConfigControl（合乎 OOP 直觉），届时 instanceof 顺序
 *       敏感，提前用映射表规避未来踩坑
 *   (2) 可枚举：单元测试可以遍历映射表断言"每个 config 类都有 listener 方法"
 *   (3) 可读：13/N 个 if-else 链不如一张映射表直观
 */
public interface ConfigListener {
    default void onSnapShotConfig(String platformId, SnapShotConfig cfg)               {}
    default void onOsdConfig(String platformId, OsdConfig cfg)                          {}
    default void onAlarmReportConfig(String platformId, AlarmReportConfig cfg)         {}
    default void onVideoAlarmRecordConfig(String platformId, VideoAlarmRecordConfig cfg) {}
    default void onBasicParamConfig(String platformId, DeviceConfigControl cfg)        {}
    // 后续阶段 10 其他子配置（PictureMask / FrameMirror / SVAC* / VideoParamAttribute）按相同模式扩展
}

/**
 * 平台订阅监听器：fire-and-forget 通知语义（v1.1 修订）。
 *
 * 设计权衡：原方案让 listener 返回 SubscribeResult 决定 200/403，但 SIP 事务必须
 * 在毫秒级返回 ACK，业务方一旦 @Async 化或查 DB 决定接受/拒绝，事务就会超时。
 * 因此 200 OK 一律由协议层 handler 同步返回（沿用现有 SubscribeAlarmQueryMessageHandler
 * 的逻辑），listener 只接收"已接受订阅"通知。
 *
 * 真要做接受/拒绝决策（业务认证、限流），可在 Layer 1 协议事件层
 * （ClientSubscribeEvent）通过 BeanPostProcessor / 全局拦截器实现，不下放到 listener。
 *
 * 同时收编原 SubscribeRequestHandler 的两个方法：
 * - putSubscribe(userId, subscribeInfo)：由协议层在发出 200 OK 时直接维护 SubscribeInfo 表
 * - getDeviceSubscribe(deviceQuery)：催发订阅 NOTIFY 内容时由业务自行触发，无需框架回调
 */
public interface SubscribeListener {
    default void onCatalogSubscribe(String platformId, Integer expires, DeviceQuery q)              {}
    default void onAlarmSubscribe(String platformId, Integer expires, DeviceAlarmQuery q)           {}
    default void onMobilePositionSubscribe(String platformId, Integer expires, DeviceMobileQuery q) {}
}

/**
 * 平台通知监听器（客户端方向：Broadcast 语音广播是核心场景）。
 */
public interface NotifyListener {
    default void onBroadcastNotify(String platformId, DeviceBroadcastNotify n) {}
}
```

#### 3.3.2 ClientGb28181Adapter（一站式便利基类）

```java
package io.github.lunasaw.gbproxy.client.api;

/**
 * 一站式适配基类：业务方继承它即可获得所有 hook。
 *
 * 等价于：implements QueryListener, ControlListener, ConfigListener, SubscribeListener, NotifyListener
 *
 * 不想全 hook 的业务方可以选择性 implements 几个 interface。
 */
public abstract class ClientGb28181Adapter
        implements QueryListener, ControlListener, ConfigListener, SubscribeListener, NotifyListener {
}
```

> 原 §3.3.2 `SubscribeResult` 类在 v1.1 中删除（见 §3.3.1 SubscribeListener 设计变更）。

#### 3.3.3 ClientListenerAdapter（框架内部分发器，Java 17 兼容形式）

```java
package io.github.lunasaw.gbproxy.client.eventbus.internal;

@Component
@RequiredArgsConstructor
@Slf4j
class ClientListenerAdapter {

    /** Query 必须唯一，多实例 fail fast */
    private final ObjectProvider<QueryListener>           queryListenerProvider;

    /** Control / Config / Subscribe / Notify 允许多实例，全部调用 */
    private final ObjectProvider<List<ControlListener>>   controlListeners;
    private final ObjectProvider<List<ConfigListener>>    configListeners;
    private final ObjectProvider<List<SubscribeListener>> subscribeListeners;
    private final ObjectProvider<List<NotifyListener>>    notifyListeners;

    private final ClientDeviceSupplier supplier;

    // ============ Query 分发：返回非 null 即自动回包 ============

    @EventListener
    void dispatch(ClientQueryEvent event) {
        QueryListener l = queryListenerProvider.getIfUnique();
        if (l == null) return;  // 0 个 listener：跳过；≥2 个：Spring 启动期已 fail

        FromDevice from = supplier.getClientFromDevice();
        ToDevice to = (ToDevice) supplier.getDevice(event.getSipId());
        XmlBean q = event.getQuery();

        // DeviceQuery 携带 cmdType 字段，按字符串区分
        if (q instanceof DeviceQuery dq) {
            switch (dq.getCmdType()) {
                case "Catalog" -> respondCatalog(l, event, dq, from, to);
                case "DeviceInfo" -> respondDeviceInfo(l, event, dq, from, to);
                case "DeviceStatus" -> respondDeviceStatus(l, event, dq, from, to);
                default -> log.debug("未识别的 DeviceQuery cmdType: {}", dq.getCmdType());
            }
            return;
        }
        // 其余 query 类型直接按 Java 类型分发（Java 17 instanceof pattern）
        if (q instanceof DeviceRecordQuery rq) {
            DeviceRecord resp = l.onRecordInfoQuery(event.getSipId(), rq);
            if (resp != null) ClientCommandSender.sendDeviceRecordCommand(from, to, resp);
        } else if (q instanceof DeviceAlarmQuery aq) {
            DeviceAlarmNotify resp = l.onAlarmQuery(event.getSipId(), aq);
            if (resp != null) ClientCommandSender.sendAlarmQueryResponse(from, to, resp);
        } else if (q instanceof PTZPositionQuery pq) {
            PTZPositionResponse resp = l.onPtzPositionQuery(event.getSipId(), pq);
            if (resp != null) ClientCommandSender.sendPtzPositionResponse(from, to, resp);
        } else if (q instanceof SDCardStatusQuery sq) {
            SDCardStatusResponse resp = l.onSdCardStatusQuery(event.getSipId(), sq);
            if (resp != null) ClientCommandSender.sendSdCardStatusResponse(from, to, resp);
        } else if (q instanceof HomePositionQuery hq) {
            HomePositionResponse resp = l.onHomePositionQuery(event.getSipId(), hq);
            if (resp != null) ClientCommandSender.sendHomePositionResponse(from, to, resp);
        } else if (q instanceof CruiseTrackListQuery cq) {
            CruiseTrackListResponse resp = l.onCruiseTrackListQuery(event.getSipId(), cq);
            if (resp != null) ClientCommandSender.sendCruiseTrackListResponse(from, to, resp);
        } else if (q instanceof CruiseTrackQuery cq) {
            CruiseTrackResponse resp = l.onCruiseTrackQuery(event.getSipId(), cq);
            if (resp != null) ClientCommandSender.sendCruiseTrackResponse(from, to, resp);
        } else if (q instanceof MobilePositionQuery mq) {
            MobilePositionNotify resp = l.onMobilePositionQuery(event.getSipId(), mq);
            if (resp != null) ClientCommandSender.sendMobilePositionNotify(from, to, resp);
        } else if (q instanceof PresetQuery pq) {
            PresetQueryResponse resp = l.onPresetQuery(event.getSipId(), pq);
            if (resp != null) ClientCommandSender.sendPresetQueryResponse(from, to, resp);
        } else if (q instanceof DeviceConfigDownload cd) {
            DeviceConfigResponse resp = l.onConfigDownloadQuery(event.getSipId(), cd);
            if (resp != null) ClientCommandSender.sendConfigDownloadResponse(from, to, resp);
        } else {
            log.debug("未识别的查询类型: {}", q.getClass().getSimpleName());
        }
    }

    private void respondCatalog(QueryListener l, ClientQueryEvent event, DeviceQuery q,
                                FromDevice from, ToDevice to) {
        DeviceResponse resp = l.onCatalogQuery(event.getSipId(), q);
        if (resp != null) {
            resp.setSn(q.getSn());
            ClientCommandSender.sendCatalogCommand(from, to, resp);
        }
    }
    // respondDeviceInfo / respondDeviceStatus 同形 ...

    // ============ Control 分发：fire-and-forget，所有 listener 都接收 ============

    @EventListener
    void dispatch(ClientControlEvent event) {
        for (ControlListener l : safeList(controlListeners)) {
            DeviceControlBase cmd = event.getCommand();
            // instanceof 链：所有 control 类继承自 DeviceControlBase，无父子重合，顺序无关
            if (cmd instanceof DeviceControlPtz c)              l.onPtzControl(event.getUserId(), c);
            else if (cmd instanceof DeviceControlTeleBoot c)    l.onTeleBoot(event.getUserId(), c);
            else if (cmd instanceof DeviceControlRecordCmd c)   l.onRecord(event.getUserId(), c);
            else if (cmd instanceof DeviceControlGuard c)       l.onGuard(event.getUserId(), c);
            else if (cmd instanceof DeviceControlAlarm c)       l.onAlarmReset(event.getUserId(), c);
            else if (cmd instanceof DeviceControlIFame c)       l.onIFrame(event.getUserId(), c);
            else if (cmd instanceof DeviceControlDragIn c)      l.onDragIn(event.getUserId(), c);
            else if (cmd instanceof DeviceControlDragOut c)     l.onDragOut(event.getUserId(), c);
            else if (cmd instanceof DeviceControlPosition c)    l.onHomePositionControl(event.getUserId(), c);
            else if (cmd instanceof DeviceUpgradeControl c)     l.onDeviceUpgrade(event.getUserId(), c);
            else if (cmd instanceof DeviceControlPTZPrecise c)  l.onPtzPrecise(event.getUserId(), c);
            else if (cmd instanceof DeviceControlSDCardFormat c) l.onFormatSdCard(event.getUserId(), c);
            else if (cmd instanceof DeviceControlTargetTrack c) l.onTargetTrack(event.getUserId(), c);
            else log.debug("未识别的控制命令: {}", cmd.getClass().getSimpleName());
        }
    }

    @EventListener
    void dispatch(ClientKeepaliveEvent event) {
        for (ControlListener l : safeList(controlListeners)) {
            l.onKeepalive(event.getUserId(), event.getKeepalive());
        }
    }

    // ============ Config 分发（v1.3：用 Class<?> → Consumer 映射表替代 instanceof 链）============

    /**
     * 5 个 config 类当前是 DeviceControlBase 的兄弟节点，instanceof 顺序不敏感。
     * 但用 Class.equals 精确匹配 + 显式映射表，是为了：
     *   - 一致性：未来若把具体类改 extends DeviceConfigControl，无需调顺序
     *   - 可测：单元测试遍历此表断言"5 类全覆盖"
     */
    private static final Map<Class<?>, BiConsumer<ConfigListener, ConfigDispatchCtx>> CONFIG_DISPATCH = Map.of(
            SnapShotConfig.class,         (l, c) -> l.onSnapShotConfig(c.userId, (SnapShotConfig) c.cfg),
            OsdConfig.class,              (l, c) -> l.onOsdConfig(c.userId, (OsdConfig) c.cfg),
            AlarmReportConfig.class,      (l, c) -> l.onAlarmReportConfig(c.userId, (AlarmReportConfig) c.cfg),
            VideoAlarmRecordConfig.class, (l, c) -> l.onVideoAlarmRecordConfig(c.userId, (VideoAlarmRecordConfig) c.cfg),
            DeviceConfigControl.class,    (l, c) -> l.onBasicParamConfig(c.userId, (DeviceConfigControl) c.cfg)
    );

    private record ConfigDispatchCtx(String userId, DeviceControlBase cfg) {}

    @EventListener
    void dispatch(ClientConfigEvent event) {
        DeviceControlBase cfg = event.getConfig();
        BiConsumer<ConfigListener, ConfigDispatchCtx> dispatch = CONFIG_DISPATCH.get(cfg.getClass());
        if (dispatch == null) {
            log.debug("未识别的配置: {}", cfg.getClass().getSimpleName());
            return;
        }
        ConfigDispatchCtx ctx = new ConfigDispatchCtx(event.getUserId(), cfg);
        for (ConfigListener l : safeList(configListeners)) {
            dispatch.accept(l, ctx);
        }
    }

    // ============ Subscribe 分发（v1.1 改 fire-and-forget，无返回值）============

    @EventListener
    void dispatch(ClientSubscribeEvent event) {
        for (SubscribeListener l : safeList(subscribeListeners)) {
            XmlBean body = event.getBody();
            if (body instanceof DeviceQuery dq && "Catalog".equals(dq.getCmdType())) {
                l.onCatalogSubscribe(event.getSipId(), event.getExpires(), dq);
            } else if (body instanceof DeviceAlarmQuery aq) {
                l.onAlarmSubscribe(event.getSipId(), event.getExpires(), aq);
            } else if (body instanceof DeviceMobileQuery mq) {
                l.onMobilePositionSubscribe(event.getSipId(), event.getExpires(), mq);
            }
        }
    }

    // ============ Notify 分发 ============

    @EventListener
    void dispatch(ClientNotifyEvent event) {
        for (NotifyListener l : safeList(notifyListeners)) {
            if (event.getNotify() instanceof DeviceBroadcastNotify n) {
                l.onBroadcastNotify(event.getUserId(), n);
            } else {
                log.debug("未识别的通知: {}", event.getNotify().getClass().getSimpleName());
            }
        }
    }

    // ============ 工具 ============

    private static <T> List<T> safeList(ObjectProvider<List<T>> provider) {
        List<T> list = provider.getIfAvailable();
        return list != null ? list : List.of();
    }
}
```

> **设计选择说明**：
> - Query 用 `getIfUnique()` 而非 `firstNonNull` —— 多实例静默不确定的风险大于灵活性收益
> - Control / Config / Subscribe / Notify 用 `List` —— 这些是观察者，多 bean 同时监听是合法场景（业务 + metrics + audit）
> - **Config 分发用 Class<?> → Consumer 映射表**（v1.3 改动）—— 当前 5 个 config 类是 DeviceControlBase 的兄弟节点，instanceof 顺序不敏感；用映射表是为了一致性、可枚举测试与未来重构鲁棒性，参见 ConfigListener javadoc
> - **Control 分发暂留 instanceof 链**（v1.3 保留现状）—— 13 个 control 类同样互为兄弟节点，顺序无关；如果未来子类爆炸到 20+，可在内部重构为映射表，对外接口不变
> - **`supplier.getDevice(sipId)` 强转 ToDevice 的安全性**（v1.3 新增）—— `ClientDeviceSupplier` 的契约是"client 角色发外呼时的目标 = ToDevice"，所有官方实现（含 `DefaultClientDeviceSupplier`）都遵守。如业务方覆写 supplier 返回了 `FromDevice` 或其他类型，会 ClassCastException —— 此约束需写进 `ClientDeviceSupplier` 的 javadoc 同步收紧

### 3.4 Server 端对称设计

server 端目前已是事件驱动（`DeviceCatalogEvent` / `DeviceAlarmEvent` / `DeviceKeepaliveEvent` / ... 共 32 个事件类，含 INVITE/BYE/ACK 状态机 + 注册挑战 + 远端地址变更等），同样收敛为外层事件 + listener 接口。

**v1.1 修订**：原方案的 4 个 listener 粒度过粗，丢失关键 payload 信息。重新拆分为下列形态。

```java
package io.github.lunasaw.gbproxy.server.api;

/**
 * 设备应答（query 类响应 + 错误响应）
 */
public interface DeviceResponseListener {
    default void onCatalogResponse(String deviceId, DeviceCatalog catalog)        {}
    default void onDeviceInfoResponse(String deviceId, DeviceInfo info)           {}
    default void onDeviceInfoError(String deviceId, String reason)                {} // 保留 DeviceInfoErrorEvent 语义
    default void onDeviceInfoRequest(String deviceId)                             {} // 保留 DeviceInfoRequestEvent
    default void onDeviceStatusResponse(String deviceId, DeviceStatus status)     {}
    default void onRecordInfoResponse(String deviceId, DeviceRecord record)       {}
    default void onPtzPositionResponse(String deviceId, PTZPositionResponse resp) {}
    default void onSdCardStatusResponse(String deviceId, SDCardStatusResponse resp) {}
    default void onHomePositionResponse(String deviceId, HomePositionResponse resp) {}
    default void onCruiseTrackResponse(String deviceId, CruiseTrackResponse resp) {}
    default void onCruiseTrackListResponse(String deviceId, CruiseTrackListResponse resp) {}
    default void onConfigResponse(String deviceId, ConfigDownloadResponse resp)   {}
    default void onSubscribeResponse(String deviceId, DeviceSubscribe resp)       {}
    default void onNotifyUpdate(String deviceId, DeviceUpdateNotify notify)       {} // DeviceNotifyUpdateEvent
}

/**
 * 设备主动通知（alarm/keepalive/upgrade/snapshot 等）
 */
public interface DeviceNotifyListener {
    default void onAlarmNotify(String deviceId, DeviceAlarmNotify notify)              {}
    default void onKeepalive(String deviceId, KeepaliveNotify notify)                  {}
    default void onMediaStatus(String deviceId, MediaStatusNotify notify)              {}
    default void onMobilePositionNotify(String deviceId, MobilePositionNotify notify)  {}
    default void onUpgradeResult(String deviceId, UpgradeResultNotify notify)          {}
    default void onSnapShotFinished(String deviceId, UploadSnapShotFinishedNotify notify) {}
}

/**
 * 设备生命周期（注册 / 在线 / 离线 / 远端地址变更 / 注册挑战）
 *
 * v1.1 修订：保留 register/challenge/remoteAddress 的 payload，
 * 不再简化为 onDeviceOnline(String deviceId)。
 */
public interface DeviceLifecycleListener {
    /** 设备首次注册成功 */
    default void onDeviceRegister(String deviceId, RegisterContext ctx)            {}
    /** 注册挑战（digest auth），业务方可拦截改写凭据 */
    default void onRegisterChallenge(String deviceId, ChallengeContext ctx)        {}
    /** 设备进入在线状态（已通过注册 + keepalive 验活） */
    default void onDeviceOnline(String deviceId)                                   {}
    /** 设备离线（超时 / 主动注销） */
    default void onDeviceOffline(String deviceId, OfflineReason reason)            {}
    /** 设备远端地址变化（IP/端口漂移，常见于 NAT 重连） */
    default void onRemoteAddressChanged(String deviceId, String oldAddr, String newAddr) {}
}

/**
 * INVITE / BYE / 流媒体会话
 *
 * v1.1 修订：保留 trying 中间状态、bye/ack 错误语义。
 */
public interface DeviceSessionListener {
    /** INVITE 进行中（180/183/100 trying） */
    default void onInviteTrying(String deviceId, InviteProgress progress)          {}
    /** INVITE 成功（200 OK + ACK 完成） */
    default void onInviteOk(InviteSession session)                                 {}
    /** INVITE 失败（4xx/5xx/6xx） */
    default void onInviteFailure(InviteFailure failure)                            {}
    /** 收到 ACK（INVITE 三向握手最后一步） */
    default void onAck(String deviceId, String callId)                             {}
    /** BYE 正常结束 */
    default void onBye(String deviceId, String callId)                             {}
    /** BYE 异常（对端 4xx/timeout） */
    default void onByeError(String deviceId, String callId, String reason)         {}
    /** server 端发起 INVITE 的状态（比如下行点播） */
    default void onServerInvite(ServerInviteContext ctx)                           {}
}

/**
 * 一站式适配基类
 */
public abstract class ServerGb28181Adapter
        implements DeviceResponseListener, DeviceNotifyListener,
                   DeviceLifecycleListener, DeviceSessionListener {
}
```

> 上述 `RegisterContext` / `ChallengeContext` / `OfflineReason` / `InviteProgress` / `InviteFailure` / `InviteSession` / `ServerInviteContext` 是从被删除的 31 个 `Device*Event` / `ServerInviteEvent` 中萃取出来的纯 payload 对象（去掉 ApplicationEvent 包装），由 `gb28181-server/api/dto/` 持有。

### 3.5 入站消息流转（自下而上的运行时视图）

§3.0 ~ §3.4 的结构图表达「层与层是什么关系」。本节补齐运行时视角：一条 SIP 报文从网卡进来，**完整经过哪些类、产生哪些事件、最终落到哪个业务方法**，以及（query 分支）响应又是怎么沿原路返回。下述描述对应 v1.5.0 落地后的最终形态，不涉及任何兼容路径。

#### 3.5.1 Client 端（设备侧）入站调用链

```
                         ┌─────────────────────────────┐
                         │  网卡 / TCP·UDP socket       │
                         │  byte[] → SIPRequest         │
                         └──────────────┬──────────────┘
                                        ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L-1  SIP 传输层（sip-common）                                      │
   │  AbstractSipListener.processRequest(RequestEvent)                  │
   │   • TraceId 注入 / 透传                                             │
   │   • 按 SIP method 路由                                              │
   └──────────────┬──────────────┬──────────────┬──────────────────────┘
                  ▼              ▼              ▼
   ┌──────────────────────┐ ┌────────────┐ ┌────────────────────────────┐
   │ MessageRequest       │ │ Subscribe  │ │ Register / Invite /        │
   │ Processor            │ │ Request    │ │ Bye / Ack / Cancel / Info  │
   │ (按 cmdType 二次派发) │ │ Processor  │ │ RequestProcessor           │
   │                      │ │ (同步 200) │ │ (直接发 SIP method 系事件) │
   └──────────┬───────────┘ └────┬───────┘ └────────────┬───────────────┘
              ▼                  ▼                      │
   ┌────────────────────────────────────────────┐       │
   │  L0  XML 解析层（gb28181-client/.../message│       │
   │       /handler）                            │       │
   │   *MessageClientHandler.handForEvt(event)   │       │
   │     • parseXml(...) → 强类型 entity         │       │
   │     • DeviceControl/Config:                 │       │
   │         XML 子标签 → 具体 Class             │       │
   │     • publisher.publishEvent(L1 外层事件)   │       │
   └───────────────────────┬────────────────────┘       │
                           ▼                            ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L1  协议事件总线（ApplicationEvent，6 个外层 + 8 个 SIP method 系）│
   │   ClientQueryEvent      ClientControlEvent     ClientConfigEvent   │
   │   ClientSubscribeEvent  ClientNotifyEvent      ClientKeepaliveEvent│
   │   ClientRegister*Event  ClientInviteEvent      ClientByeEvent ...  │
   │                                                                    │
   │   ▶ 广播：所有 @EventListener 都收到（业务 / Adapter / metrics）   │
   └─────────┬────────────────────┬────────────────────┬───────────────┘
             ▼                    ▼                    ▼
   ┌──────────────────────┐ ┌────────────┐ ┌────────────────────────────┐
   │ L2  Adapter（框架内） │ │ Metrics    │ │ Tracing / Audit            │
   │ ClientListenerAdapter │ │ Counter++  │ │ TraceId 落库               │
   │  • Query: getIfUnique │ │ 慢查询告警 │ │ 协议事件原样落盘           │
   │  • 其他: safeList     │ │            │ │                            │
   │  • payload instanceof │ │            │ │  (业务方零侵入)            │
   │    → typed listener  │ │            │ │                            │
   └─────────┬─────────────┘ └────────────┘ └────────────────────────────┘
             ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L3  业务方实现（按需 implements）                                  │
   │   QueryListener    .onCatalogQuery(...)        → DeviceResponse?   │
   │   ControlListener  .onPtzControl(...)          → void              │
   │   ConfigListener   .onOsdConfig(...)           → void              │
   │   SubscribeListener.onCatalogSubscribe(...)    → void              │
   │   NotifyListener   .onBroadcastNotify(...)     → void              │
   └─────────┬──────────────────────────────────────────────────────────┘
             │  Query 分支：返回非 null
             ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  Adapter 内部回包（仅 Query 分支走这条路）                          │
   │   resp.setSn(query.getSn())                                        │
   │   ClientCommandSender.sendXxxCommand(from, to, resp)               │
   │     ─── 走 outbound 路径，构造 SIP MESSAGE 应答                    │
   └─────────┬──────────────────────────────────────────────────────────┘
             ▼
                              （回到 SIP 传输层，发回平台）
```

#### 3.5.2 Server 端（平台侧）入站调用链

server 端**不存在「query 同步回包」语义**——设备的应答全部是异步 NOTIFY/MESSAGE。因此所有 listener 一律 fire-and-forget，无需 Adapter 回包。

```
                         ┌─────────────────────────────┐
                         │  网卡 → SIPRequest           │
                         └──────────────┬──────────────┘
                                        ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L-1  SIP 传输层（共享 sip-common）                                 │
   │       AbstractSipListener → 各 method Processor                    │
   └──┬────────────┬────────────┬────────────┬────────────┬─────────────┘
      ▼            ▼            ▼            ▼            ▼
   ┌──────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
   │MESSAGE│ │ REGISTER │ │  INVITE  │ │   BYE    │ │ ACK / CANCEL │
   │Process│ │ Process  │ │ Process  │ │ Process  │ │ / SUBSCRIBE  │
   └───┬───┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │   / NOTIFY   │
       ▼          │            │            │       └──────┬───────┘
   ┌──────────┐   │            │            │              │
   │ L0 XML   │   │            │            │              │
   │ 解析层    │   │            │            │              │
   │ *Message │   │            │            │              │
   │ Server   │   │            │            │              │
   │ Handler  │   │            │            │              │
   └────┬─────┘   │            │            │              │
        ▼         ▼            ▼            ▼              ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L1  server 协议事件（4 类外层 + DeviceEvent 基类）                 │
   │   ServerQueryResponseEvent ← Catalog/DeviceInfo/DeviceStatus/      │
   │                              RecordInfo/PTZPosition/SDCardStatus/  │
   │                              HomePosition/CruiseTrack/Config/      │
   │                              SubscribeResponse/NotifyUpdate        │
   │   ServerNotifyEvent        ← Alarm/Keepalive/MediaStatus/          │
   │                              MobilePosition/UpgradeResult/         │
   │                              SnapShotFinished                      │
   │   ServerLifecycleEvent     ← Register/Online/Offline/Challenge/    │
   │                              RemoteAddressChanged                  │
   │   ServerSessionEvent       ← InviteTrying/Ok/Failure/Ack/Bye/      │
   │                              ByeError/ServerInvite                 │
   └─────────┬────────────────────┬────────────────────┬───────────────┘
             ▼                    ▼                    ▼
   ┌──────────────────────┐ ┌────────────┐ ┌────────────────────────────┐
   │ L2 ServerListener    │ │ DeviceState│ │ Audit / Metrics / Tracing  │
   │    Adapter           │ │ Cache 更新 │ │                            │
   │  按 payload Class    │ │ (在线状态  │ │                            │
   │  instanceof → 路由   │ │ 自动维护)  │ │                            │
   └─────────┬────────────┘ └────────────┘ └────────────────────────────┘
             ▼
   ┌────────────────────────────────────────────────────────────────────┐
   │  L3  业务方实现（全部 fire-and-forget）                             │
   │   DeviceResponseListener   ← 设备应答（query 类响应）               │
   │   DeviceNotifyListener     ← 设备主动通知                           │
   │   DeviceLifecycleListener  ← 注册 / 在线 / 离线 / NAT 漂移          │
   │   DeviceSessionListener    ← INVITE / BYE / ACK 状态机              │
   └────────────────────────────────────────────────────────────────────┘
```

#### 3.5.3 端到端时序：平台 Catalog 查询（query 分支完整回路）

下行入站 → 业务计算 → 上行回包，跨 8 个组件：

```
平台              SIP Stack    Message      Catalog        Spring Event   ClientListener   QueryListener     ClientCommand
                              RequestProc   QueryHandler   Bus            Adapter          (业务实现)         Sender
 │                   │           │              │              │              │                 │                  │
 │── MESSAGE q ─────►│           │              │              │              │                 │                  │
 │                   │── 200 OK ─┤  (SIP 协议层 ACK，与下方主链并行，毫秒级返回)               │                  │
 │◄──────────────────│           │              │              │              │                 │                  │
 │                   │── route ─►│── cmdType ──►│              │              │                 │                  │
 │                   │           │   =Catalog   │── parseXml ──┤              │                 │                  │
 │                   │           │              │              │              │                 │                  │
 │                   │           │              │── publish ──►│              │                 │                  │
 │                   │           │              │  ClientQuery │              │                 │                  │
 │                   │           │              │  Event       │── @EventLst ►│                 │                  │
 │                   │           │              │              │              │── getIfUnique ─►│                  │
 │                   │           │              │              │              │  + instanceof   │                  │
 │                   │           │              │              │              │── onCatalog ───►│                  │
 │                   │           │              │              │              │   Query(...)    │                  │
 │                   │           │              │              │              │                 │── 业务计算 ──┐   │
 │                   │           │              │              │              │                 │              │   │
 │                   │           │              │              │              │◄── return resp ─│◄─────────────┘   │
 │                   │           │              │              │              │                 │                  │
 │                   │           │              │              │              │── resp.setSn(q.getSn())            │
 │                   │           │              │              │              │── sendCatalogCommand(from,to,resp)►│
 │                   │           │              │              │              │                 │                  │
 │◄── MESSAGE Catalog body (设备列表) ─────────────────────────────────────────────────────────────────────────────│
 │                                                                                                                  │
```

#### 3.5.4 关键运行时不变量

1. **SIP 事务 ACK 与业务响应解耦**：SIP 协议层的 200 OK 由 SIP Stack / 协议 handler 同步返回（毫秒级，不依赖 listener）；业务响应（含 Catalog 设备列表）走独立的上行 SIP MESSAGE，从 Adapter 异步发出。这是为什么 `SubscribeListener` 必须 fire-and-forget——业务无权决定 200/403。
2. **响应只在 Adapter 发送，不在 handler 发送**：L0 handler 的职责严格限定为「parseXml + publishEvent」，回包逻辑全部聚拢到 L2 Adapter。结果是 metrics / audit 监听同一个 L1 事件即可观测「响应是否发出、耗时多少」，业务侵入零。
3. **L1 事件多订阅者并行**：Adapter、metrics、audit、业务自定义跨切层都监听同一个 `ClientQueryEvent`，互不干扰。Spring 默认同步发布，需要异步监听者自行加 `@Async`。
4. **DeviceControl / DeviceConfig 双重派发的职责切分**：cmdType=`DeviceControl` 单一，但 XML 子标签（`PTZCmd`/`TeleBoot`/...）决定具体 Class——
   - **L0 handler 内**：完成 XML 字符串 → Java Class 的映射（懂 XML 结构）
   - **L2 Adapter 内**：用 `instanceof` 把 typed payload 路由到 listener 方法（懂 Java 类型）
   两次派发互不重叠，且 listener 接口完全不感知 XML。
5. **Adapter 是无状态单例**：`@Component` 注册一次，整个进程共用。它持有 `ObjectProvider<...>` 的延迟引用，启动期不强求 listener bean 存在；运行期每次事件到达再 `getIfUnique()` / `getIfAvailable()` 取最新引用。
6. **Query bean 唯一约束在 Spring 启动期 fail fast**：业务方注册 ≥2 个 `QueryListener`，`getIfUnique()` 会让上下文启动失败——保证「响应是哪个 listener 给出的」永远确定。其他 listener 走 `safeList(List<>)` 全量调用，是观察者模式语义，可任意叠加。
7. **TraceId 透传**：L-1 SIP 传输层注入的 TraceId 必须随 ApplicationEvent payload 透传到 L3 业务方法；如果业务侧用 `@Async` 异步消费，需在自定义 `Executor` 里手工接力 `TraceContext`（参见 sip-common 的 `TracingExecutor` 实现）。

---

## 四、业务方代码示例

### 4.1 接入设备网关（client 角色）

```java
@Component
@RequiredArgsConstructor
public class MyDeviceGatewayImpl implements QueryListener, ControlListener {

    private final DeviceRegistry deviceRegistry;
    private final PtzCommandQueue ptzQueue;

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery q) {
        var devices = deviceRegistry.listChannels(q.getDeviceId());
        var resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), q.getSn(), q.getDeviceId());
        resp.setDeviceItemList(devices.stream().map(this::toItem).toList());
        return resp;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery q) {
        var info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), q.getSn(), q.getDeviceId());
        info.setDeviceName(deviceRegistry.getName(q.getDeviceId()));
        info.setManufacturer("MyCompany");
        info.setResult("OK");
        return info;
    }

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        ptzQueue.enqueue(cmd.getDeviceId(), cmd.getPtzCmd());
    }

    // 不关心 DeviceUpgrade/TargetTrack/各种 Config —— 一行不写
}
```

### 4.2 接入监控平台（server 角色）

```java
@Component
@RequiredArgsConstructor
public class MyPlatformImpl implements DeviceNotifyListener, DeviceLifecycleListener {

    private final BusinessNotifier notifier;
    private final DeviceStatusRepository repo;

    @Override
    public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {
        notifier.pushAlarm(deviceId, notify);
    }

    @Override
    public void onKeepalive(String deviceId, KeepaliveNotify notify) {
        repo.refreshLastSeen(deviceId);
    }

    @Override
    public void onDeviceRegister(String deviceId, RegisterContext ctx) {
        repo.upsertRegistration(deviceId, ctx.getRemoteAddr(), ctx.getExpires());
    }

    @Override
    public void onRemoteAddressChanged(String deviceId, String oldAddr, String newAddr) {
        repo.updateRemoteAddr(deviceId, newAddr);
    }

    @Override
    public void onDeviceOffline(String deviceId, OfflineReason reason) {
        repo.markOffline(deviceId, reason);
    }
}
```

### 4.3 测试场景（一站式 Mock）

```java
@Component @Primary
public class TestClientImpl extends ClientGb28181Adapter {

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery q) {
        var item = new DeviceItem();
        item.setDeviceId(q.getDeviceId());
        item.setName("TestChannel");
        var resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), q.getSn(), q.getDeviceId());
        resp.setDeviceItemList(List.of(item));
        return resp;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery q) {
        var info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), q.getSn(), q.getDeviceId());
        info.setDeviceName("TestDevice");
        info.setResult("OK");
        return info;
    }

    @Override
    public PTZPositionResponse onPtzPositionQuery(String platformId, PTZPositionQuery q) {
        var resp = new PTZPositionResponse(q.getSn(), q.getDeviceId());
        resp.setPan(180.0); resp.setTilt(30.0); resp.setZoom(2.0);
        return resp;
    }

    // 测试关心啥就 override 啥；不关心的留 default null/no-op
}
```

### 4.4 跨切监控（不打扰业务）

```java
@Component
@RequiredArgsConstructor
public class Gb28181Metrics {

    private final MeterRegistry meterRegistry;

    @EventListener
    public void countQueries(ClientQueryEvent event) {
        meterRegistry.counter("gb28181.client.query",
                "cmd", event.getQuery().getClass().getSimpleName()).increment();
    }

    @EventListener
    public void countControls(ClientControlEvent event) {
        meterRegistry.counter("gb28181.client.control",
                "cmd", event.getCommand().getClass().getSimpleName()).increment();
    }
}
```

业务接口和协议事件**正交**，不冲突。

---

## 五、删除清单（不留兼容）

### 5.1 client 端删除

```
gb28181-client/
  transmit/request/message/
  - MessageRequestHandler.java                        ❌ 接口
  - CustomMessageRequestHandler.java                  ❌ 接口默认实现
  - MessageClientHandlerAbstract.java                 ✏️ 修改：删除 @ConditionalOnBean(MessageRequestHandler.class)
                                                          删除 messageRequestHandler 字段
                                                          删除构造器 messageRequestHandler 入参
                                                          所有 15 个继承类的构造器同步精简（其中
                                                          DeviceControlMessageHandler 与 DeviceConfigControlMessageHandler
                                                          已不依赖 messageRequestHandler 字段，仅需删入参）
  transmit/request/message/handler/control/
  - DeviceControlRequestHandler.java                  ❌ 接口
  transmit/request/subscribe/
  - SubscribeRequestHandler.java                      ❌ 接口（v1.1 新增到删除列表）
  - SubscribeHandlerAbstract.java                     ✏️ 修改：删除对 SubscribeRequestHandler 的依赖
                                                          putSubscribe 逻辑下沉到 handler 内部直接维护
  - DefaultSubscribeProcessor.java                    ✏️ 修改：移除 @Autowired SubscribeRequestHandler
  eventbus/event/
  - ClientSnapShotConfigEvent.java                    ❌
  - ClientPtzPositionQueryEvent.java                  ❌
  - ClientSdCardStatusQueryEvent.java                 ❌
  - ClientHomePositionQueryEvent.java                 ❌
  - ClientCruiseTrackListQueryEvent.java              ❌
  - ClientCruiseTrackQueryEvent.java                  ❌
  - ClientAlarmSubscribeEvent.java                    ❌
  - ClientOsdConfigEvent.java                         ❌
  - ClientVideoAlarmRecordConfigEvent.java            ❌
  - ClientAlarmReportConfigEvent.java                 ❌

(共 14 个文件删除：4 个接口 + 10 个 Client*Event；3 个文件修改；MessageClientHandlerAbstract 实际波及 15 个具体子类的构造器签名)
```

### 5.2 server 端删除（v1.1：保留 payload 不丢信息）

```
gb28181-server/
  transmit/event/
  - DeviceCatalogEvent.java                           → DeviceResponseListener.onCatalogResponse
  - DeviceInfoEvent.java                              → DeviceResponseListener.onDeviceInfoResponse
  - DeviceInfoErrorEvent.java                         → DeviceResponseListener.onDeviceInfoError (保留语义)
  - DeviceInfoRequestEvent.java                       → DeviceResponseListener.onDeviceInfoRequest (保留语义)
  - DeviceStatusEvent.java                            → DeviceResponseListener.onDeviceStatusResponse
  - DeviceAlarmEvent.java                             → DeviceNotifyListener.onAlarmNotify
  - DeviceKeepaliveEvent.java                         → DeviceNotifyListener.onKeepalive
  - DeviceRecordEvent.java                            → DeviceResponseListener.onRecordInfoResponse
  - DeviceUpgradeResultEvent.java                     → DeviceNotifyListener.onUpgradeResult
  - DeviceSnapShotFinishedEvent.java                  → DeviceNotifyListener.onSnapShotFinished
  - DevicePtzPositionEvent.java                       → DeviceResponseListener.onPtzPositionResponse
  - DeviceSdCardStatusEvent.java                      → DeviceResponseListener.onSdCardStatusResponse
  - DeviceHomePositionEvent.java                      → DeviceResponseListener.onHomePositionResponse
  - DeviceCruiseTrackEvent.java                       → DeviceResponseListener.onCruiseTrackResponse / onCruiseTrackListResponse
  - DeviceMediaStatusEvent.java                       → DeviceNotifyListener.onMediaStatus
  - DeviceMobilePositionEvent.java                    → DeviceNotifyListener.onMobilePositionNotify
  - DeviceConfigEvent.java                            → DeviceResponseListener.onConfigResponse
  - DeviceInviteOkEvent.java                          → DeviceSessionListener.onInviteOk
  - DeviceInviteFailureEvent.java                     → DeviceSessionListener.onInviteFailure
  - DeviceInviteTryingEvent.java                      → DeviceSessionListener.onInviteTrying (v1.1 保留)
  - DeviceByeEvent.java                               → DeviceSessionListener.onBye
  - DeviceByeErrorEvent.java                          → DeviceSessionListener.onByeError (v1.1 保留语义)
  - DeviceAckEvent.java                               → DeviceSessionListener.onAck (v1.1 独立保留)
  - DeviceOnlineEvent.java                            → DeviceLifecycleListener.onDeviceOnline
  - DeviceOfflineEvent.java                           → DeviceLifecycleListener.onDeviceOffline (含 OfflineReason)
  - DeviceRegisterEvent.java                          → DeviceLifecycleListener.onDeviceRegister (含 RegisterContext)
  - DeviceRegisterChallengeEvent.java                 → DeviceLifecycleListener.onRegisterChallenge (含 ChallengeContext)
  - DeviceRemoteAddressEvent.java                     → DeviceLifecycleListener.onRemoteAddressChanged (含 old/new addr)
  - DeviceSubscribeResponseEvent.java                 → DeviceResponseListener.onSubscribeResponse
  - DeviceNotifyUpdateEvent.java                      → DeviceResponseListener.onNotifyUpdate
  - ServerInviteEvent.java                            → DeviceSessionListener.onServerInvite (含 ServerInviteContext)

  保留：
  - DeviceEvent.java                                  ✅ 基类
  + ServerQueryResponseEvent.java                     ✅ 新增外层事件
  + ServerNotifyEvent.java                            ✅
  + ServerLifecycleEvent.java                         ✅
  + ServerSessionEvent.java                           ✅

  新增 payload DTO（api/dto/ 目录）：
  + RegisterContext.java
  + ChallengeContext.java
  + OfflineReason.java
  + InviteProgress.java
  + InviteFailure.java
  + InviteSession.java
  + ServerInviteContext.java

(共计 32 个 Device*Event / Server*Event 类，删除 31 个并下沉为 listener 方法 + 7 个 payload DTO；
 仅保留 `DeviceEvent.java` 作为基类。新增 4 个外层事件 + 7 个 payload DTO)
```

### 5.3 测试删除

```
gb28181-test/
  handler/
  - TestMessageRequestHandler.java        ❌ → 合并到 TestClientImpl   (现 99 行)
  - TestDeviceControlHandler.java         ❌ → 合并到 TestClientImpl   (现 67 行)
  - TestClientEventHandler.java           ❌ → 合并到 TestClientImpl   (现 156 行)
  - TestServerEventHandler.java           ❌ → 合并到 TestServerImpl   (现 61 行)

  + TestClientImpl.java                   ✅ 新增（extends ClientGb28181Adapter）
  + TestServerImpl.java                   ✅ 新增（extends ServerGb28181Adapter）

(4 个测试 handler 删除，2 个统一实现新增)
```

---

## 六、新增清单

### 6.1 client 端新增

```
gb28181-client/
  api/                                               ← 新目录
  + QueryListener.java
  + ControlListener.java
  + ConfigListener.java
  + SubscribeListener.java
  + NotifyListener.java
  + ClientGb28181Adapter.java
  (注：v1.1 删除 SubscribeResult.java —— SubscribeListener 改 fire-and-forget)

  eventbus/event/
  + ClientQueryEvent.java
  + ClientControlEvent.java
  + ClientKeepaliveEvent.java                        ← v1.1 新增（独立于 ControlEvent）
  + ClientConfigEvent.java
  + ClientSubscribeEvent.java
  + ClientNotifyEvent.java
  (保留 ClientInviteEvent / ClientByeEvent / ClientAckEvent / ClientCancelEvent / ClientInfoEvent
   / ClientRegisterChallengeEvent / ClientRegisterFailureEvent / ClientRegisterSuccessEvent
   —— 这些与 SIP method 强绑定，不属于 MESSAGE 体系)

  eventbus/internal/                                 ← 新目录
  + ClientListenerAdapter.java                       (~250 行 instanceof + cmdType switch + Config 类映射表)

  transmit/request/message/handler/notify/           ← 现有目录，仅修改
  ✏️ BroadcastNotifyMessageHandler                    将 messageRequestHandler.broadcastNotify(...)
                                                      替换为 publisher.publishEvent(new ClientNotifyEvent(...))
```

### 6.2 server 端新增

```
gb28181-server/
  api/                                               ← 新目录
  + DeviceResponseListener.java
  + DeviceNotifyListener.java
  + DeviceLifecycleListener.java
  + DeviceSessionListener.java
  + ServerGb28181Adapter.java
  + dto/RegisterContext.java
  + dto/ChallengeContext.java
  + dto/OfflineReason.java
  + dto/InviteProgress.java
  + dto/InviteFailure.java
  + dto/InviteSession.java
  + dto/ServerInviteContext.java

  transmit/event/
  + ServerQueryResponseEvent.java
  + ServerNotifyEvent.java
  + ServerLifecycleEvent.java
  + ServerSessionEvent.java

  transmit/event/internal/
  + ServerListenerAdapter.java
```

---

## 七、Handler 改造范式

### 7.1 简单 query handler（一对一 cmdType）

#### 改造前（当前 v1.4.0）

```java
@Component
public class CatalogQueryMessageClientHandler extends MessageClientHandlerAbstract {

    public CatalogQueryMessageClientHandler(MessageRequestHandler messageRequestHandler) {
        super(messageRequestHandler);  // ❌ 业务接口耦合
    }

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceQuery query = parseXml(DeviceQuery.class);
        DeviceSession session = getDeviceSession(event);

        // ❌ 直接调业务接口同步取数据
        DeviceResponse resp = messageRequestHandler.getDeviceItem(session.getUserId());
        resp.setSn(query.getSn());

        // ❌ 在 handler 里直接发送响应
        ClientCommandSender.sendCatalogCommand(session.getFromDevice(), session.getToDevice(), resp);
    }
}
```

#### 改造后

```java
@Component
@RequiredArgsConstructor
public class CatalogQueryMessageClientHandler extends MessageClientHandlerAbstract {

    private final ApplicationEventPublisher publisher;

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceQuery query = parseXml(DeviceQuery.class);
        DeviceSession session = getDeviceSession(event);

        // ✅ 只发事件，回包由 Adapter 统一处理
        publisher.publishEvent(new ClientQueryEvent(this,
                session.getUserId(), session.getSipId(), query));
    }
}
```

### 7.2 复合 control handler（XML 子标签多态分发）

`DeviceControlMessageHandler` / `DeviceConfigControlMessageHandler` cmdType 单一但 XML 子标签多种。

> ⚠️ v1.3 事实修正：当前 `DeviceControlMessageHandler` **已经在用 HandlerEntry + BiConsumer 模式**做
> XML 子标签 → Java Class 的映射（13 个条目就位），改造工作量比 v1.0/v1.1 暗示的小很多——只需把
> BiConsumer 内部对 `DeviceControlRequestHandler` 各方法的调用换成 `publishEvent(...)`，handler 自身
> 结构不动。

#### 改造前（当前 v1.4.0 实际形态）

```java
@Component
@RequiredArgsConstructor
public class DeviceControlMessageHandler extends MessageClientHandlerAbstract {

    private final DeviceControlRequestHandler controlHandler;  // ❌ 业务接口耦合

    private static final List<HandlerEntry<? extends DeviceControlBase>> HANDLERS = List.of(
            new HandlerEntry<>("PTZCmd",   DeviceControlPtz.class,
                    (h, c) -> h.handlePtzCmd((DeviceControlPtz) c)),
            new HandlerEntry<>("TeleBoot", DeviceControlTeleBoot.class,
                    (h, c) -> h.handleTeleBoot((DeviceControlTeleBoot) c)),
            // ... 13 个映射
    );

    @Override
    public void handForEvt(RequestEvent event) {
        String xmlStr = getXmlStr();
        for (HandlerEntry<?> entry : HANDLERS) {
            if (xmlStr.contains("<" + entry.xmlTag + ">")) {
                DeviceControlBase cmd = (DeviceControlBase) XmlUtils.parseObj(xmlStr, entry.clazz);
                entry.consumer.accept(controlHandler, cmd);  // ❌ 直接打到业务接口
                return;
            }
        }
    }
}
```

#### 改造后

```java
@Component
@RequiredArgsConstructor
public class DeviceControlMessageHandler extends MessageClientHandlerAbstract {

    private final ApplicationEventPublisher publisher;

    /**
     * v1.5.0：HandlerEntry 不再持有 BiConsumer，只剩 xmlTag → Class 映射。
     * Adapter 拿到 typed payload 后用 instanceof 链路由到 ControlListener 方法。
     */
    private static final List<HandlerEntry<? extends DeviceControlBase>> HANDLERS = List.of(
            new HandlerEntry<>("PTZCmd",   DeviceControlPtz.class),
            new HandlerEntry<>("TeleBoot", DeviceControlTeleBoot.class),
            // ... 13 个映射
    );

    @Override
    public void handForEvt(RequestEvent event) {
        String xmlStr = getXmlStr();
        DeviceSession session = getDeviceSession(event);

        for (HandlerEntry<?> entry : HANDLERS) {
            if (xmlStr.contains("<" + entry.xmlTag + ">")) {
                DeviceControlBase cmd = (DeviceControlBase) XmlUtils.parseObj(xmlStr, entry.clazz);
                publisher.publishEvent(new ClientControlEvent(this, session.getUserId(), cmd));
                return;
            }
        }
        log.warn("未识别的 DeviceControl 命令: {}", xmlStr);
    }
}
```

handler 不再依赖 `DeviceControlRequestHandler` 业务接口，分发改在 Adapter 用 `instanceof` 处理 typed 对象。
**实际 diff**：删 `controlHandler` 字段、删 BiConsumer 第三参数、把 `entry.consumer.accept(...)` 改成
`publisher.publishEvent(...)` —— 单文件 ~10 行改动。`DeviceConfigControlMessageHandler` 同形改造。

### 7.3 SUBSCRIBE handler（保留 200 OK 同步性）

```java
@Component
@RequiredArgsConstructor
public class SubscribeAlarmQueryMessageHandler extends SubscribeHandlerAbstract {

    private final ApplicationEventPublisher publisher;

    @Override
    public void handForEvt(RequestEvent event) {
        DeviceSession deviceSession = getDeviceSession(event);
        SIPRequest request = (SIPRequest) event.getRequest();
        SubscribeInfo subscribeInfo = new SubscribeInfo(request, deviceSession.getSipId());
        DeviceAlarmQuery query = parseXml(DeviceAlarmQuery.class);

        // ✅ 协议层维护订阅表（替代原 subscribeRequestHandler.putSubscribe）
        SubscribeRegistry.put(query.getDeviceId(), subscribeInfo);

        // ✅ 同步回 200 OK（毫秒级，必须在 listener 之前）
        ResponseCmd.sendResponse(Response.OK, "OK", contentTypeHeader, event, expiresHeader);

        // ✅ 异步通知业务（listener 慢/异步都不影响 SIP 事务）
        publisher.publishEvent(new ClientSubscribeEvent(this,
                deviceSession.getUserId(), deviceSession.getSipId(), expires, query));
    }
}
```

**handler 简化**：
- 行数：从平均 ~50 行 → ~20 行
- 职责单一：只做 XML 解析 + 发事件（必要时同步回 200 OK）
- 不依赖业务接口

---

## 八、对比总结

| 维度 | 当前 v1.4.0 | 本方案 (v1.1) |
|---|---|---|
| client 端事件类总数 | 18 个（10 个 MESSAGE 系 Client*Event + 8 个 SIP method 类） | **6 个外层 + 8 个 SIP method 类 = 14 个**（含 ClientKeepaliveEvent） |
| server 端事件类总数 | 32 个（含 INVITE 状态 + 注册挑战 + 远端地址变更等） | **4 个外层 + 1 个基类 + 7 个 payload DTO = 12 个** |
| 业务方接入点 | 接口（12 + 13 = 25 方法）+ N 个 @EventListener | **5 个 listener 接口（全 default）+ 一站式 Adapter** |
| 业务方代码量（典型） | 必须实现接口全部方法（~150 行） | **只写关心的（~30 行）** |
| 测试代码量 | ~480 行（3 个 client 端 handler + 1 个 server 端 handler 散落：99 + 67 + 156 + 61 = 383 行 mock + ~100 行 FlowTest 注入） | **~150 行（1 个 TestClientImpl + 1 个 TestServerImpl）** |
| 与 v1.3.0 哲学一致 | ❌ 半途 | **✅ 彻底** |
| 跨切监听 | ❌ 接口被业务占用 | **✅ Layer 1 协议事件保留** |
| 添加新协议改动量 | 加事件类 + 加 listener + 改 handler | **加 listener 一个 hook + 改 Adapter 一行 instanceof** |
| Java 单继承束缚 | 接口式：单 `MessageRequestHandler` impl | **接口多实现：QueryListener + ControlListener + ...** |
| Java 版本要求 | 17 | **17（不依赖 21 sealed switch）** |
| SIP 事务时序安全 | 部分接口同步阻塞 SIP 事务 | **listener 异步，不阻塞 SIP 事务** |

---

## 九、PR 拆分实施计划（v1.1 修订）

### PR-1：Layer 1 协议事件 + Layer 2 接口（client 端，纯新增）
- 新增 `gb28181-client/api/` 5 个 listener 接口 + Adapter 基类
- 新增 `gb28181-client/eventbus/event/` 6 个外层事件（含 ClientKeepaliveEvent）
- 新增 `gb28181-client/eventbus/internal/ClientListenerAdapter`（Java 17 instanceof 形式）
- **风险**：低，纯新增，不破坏现有代码
- **工作量**：4h

### PR-2：handler 改造 + 旧接口删除（client 端，**破坏性单 PR**）

> v1.1 修订：原 PR-2 / PR-3 合并 —— `@ConditionalOnBean(MessageRequestHandler.class)`
> 必须和接口删除同步进行，分两个 PR 中间状态根本跑不起来。

- 13 个 `*MessageClientHandler` 改成只发外层事件
- 修改 `MessageClientHandlerAbstract`：删除 `@ConditionalOnBean` / `messageRequestHandler` 字段 / 构造器入参
- 修改 `SubscribeHandlerAbstract` / `DefaultSubscribeProcessor`：内化 SubscribeRegistry
- 删除 `MessageRequestHandler` / `CustomMessageRequestHandler` / `DeviceControlRequestHandler` / `SubscribeRequestHandler`
- 删除 10 个 `Client*Event`
- 调整自动配置（`Gb28181ClientAutoConfig` 中如有 default impl 的 `@Bean` 需移除）
- **风险**：高（破坏 API），但 v1.5.0 主版本可承担
- **工作量**：5h

### PR-3：测试迁移到新 listener（client 端）
- 删除 `TestMessageRequestHandler` / `TestDeviceControlHandler` / `TestClientEventHandler`
- 新增 `TestClientImpl extends ClientGb28181Adapter`
- 修改受影响的 13 个 `*FlowTest`（替换 handler 注入为新 impl 注入）
- **风险**：低（仅测试代码）
- **工作量**：3h

### PR-4：server 端对称改造（破坏性单 PR）
- 新增 `gb28181-server/api/` 4 个 listener + 7 个 payload DTO
- 新增 4 个外层事件 + Adapter
- handler 改造发外层事件
- 删除 31 个 Device*Event / ServerInviteEvent
- 测试迁移
- **风险**：高（语义合并复杂，特别是 INVITE 状态机）
- **工作量**：8h

### PR-5：sip-gateway / voglander 业务侧迁移
- 由各业务侧仓库认领（不在 sip-proxy 内）
- 提供 LISTENER-MIGRATION-GUIDE.md 作为映射依据
- **工作量**：业务侧自主，sip-proxy 提供文档支持

### PR-6：CHANGELOG / 迁移指南
- 1.5.0 BREAKING CHANGES 说明
- 业务方迁移指引（接口 → listener 映射表）
- **工作量**：2h

**总计**：~22h（sip-proxy 内部），跨 5 个 PR + 业务侧迁移工作。

---

## 十、风险与回滚

### 10.1 已知风险

1. **Java 17 instanceof 链可读性**：方案改用 `if (cmd instanceof Xxx c) ...` 链替代 Java 21 switch pattern，13 个 control 命令的链式判断会有 ~30 行。
   - **缓解**：内部允许重构为 `Map<Class<?>, BiConsumer<L, Object>>`，对外接口不变；`@VisibleForTesting` 暴露分发表用于断言完整性
2. **多 listener 顺序问题（已修复）**：v1.1 改 query 强制单 bean（`getIfUnique`），多实例启动期 fail。Control / Notify 类用 List 全部调用，无顺序依赖
3. **ConfigListener instanceof 顺序陷阱**：所有 cfg 子类必须在 `DeviceConfigControl` 基类之前判断
   - **缓解**：在 ConfigListener javadoc + Adapter 内代码注释双重提示；CI 加单元测试覆盖 5 种 config 类型分发正确性
4. **协议层事件被双重消费**：Adapter 监听 + 业务方自己监听 Layer 1 事件，可能导致响应被发两次
   - **缓解**：文档明确「Layer 1 事件仅供监控、跨切，不要在业务方监听做响应」；`@EventListener(condition = "...")` 用条件表达式区分
5. **测试影响面大**：13 个 FlowTest 全部需要改 handler 注入
   - **缓解**：PR-3 单独验证，所有现有测试断言保持不变
6. **server 端 INVITE 状态合并语义**：trying / ok / failure 三态合并到 `DeviceSessionListener` 后，业务方需要自行维护状态机
   - **缓解**：保留 trying 独立 hook（v1.1）；`InviteProgress` payload 携带原始 SIP 响应码，业务方可直接判断
7. **SUBSCRIBE 不再支持 deny（设计取舍）**：v1.1 砍掉了 listener 返回 SubscribeResult 的能力
   - **缓解**：业务方需要拒绝订阅时，在 Layer 1 事件层用全局拦截器实现；99% 的 GB28181 部署不需要这个能力
8. **`ClientDeviceSupplier.getDevice(sipId)` 强转 ToDevice**（v1.3 新增）：Adapter 在 Query 分发路径强转 supplier 返回值为 `ToDevice` 用于 `ClientCommandSender.sendXxxCommand(from, to, resp)`
   - **风险场景**：业务方覆写 `ClientDeviceSupplier` 返回 `FromDevice` / null / 自定义子类时，运行期 ClassCastException 或 NPE，且只在第一次平台查询到达时才触发
   - **缓解**：(a) 在 `ClientDeviceSupplier.getDevice` javadoc 中明确"client 角色发外呼时此方法必须返回 ToDevice 子类型"；(b) Adapter 内部强转改为带显式失败信息的辅助方法，例如 `requireToDevice(supplier.getDevice(sipId), sipId)`，转换失败时抛 `IllegalStateException` 并提示业务方供应器实现错误
9. **业务侧 listener bean 扫描路径**（v1.3 新增）：Adapter 通过 `ObjectProvider<QueryListener>` 在 Spring 容器内寻找业务方 bean，但 sip-proxy 是 library，业务方 listener bean 必须落在业务侧 `@SpringBootApplication.scanBasePackages` 之下
   - **风险场景**：业务方把 listener 放到 sip-proxy 不感知的子包 / 漏加 `@Component` / 用了 `@ConditionalOnProperty` 但条件为 false，`getIfUnique()` 取到 null，所有 query 静默走默认空响应——表面上注册成功、查询超时，且没有日志告警
   - **缓解**：(a) Adapter 的 `dispatch(ClientQueryEvent)` 在 `l == null` 分支加 `log.warn("收到 ClientQueryEvent 但未注册 QueryListener，所有查询将走默认空响应")`，仅首次告警一次；(b) `LISTENER-MIGRATION-GUIDE.md` 在故障排查章节明确这一陷阱
10. **Adapter 与 supplier 协议的隐式契约**（v1.3 新增）：Adapter 假设 `ClientDeviceSupplier.getClientFromDevice()` 返回的 `FromDevice` 与每次 query 事件的"当前客户端"是同一个 —— 在多 client 共用一个 JVM 的场景（cascading：本地既是 server 也是 client）下，事件携带的 `userId` 与 supplier 默认返回的 FromDevice 可能不一致
    - **缓解**：Adapter 应基于 `event.getUserId()` 反查对应 FromDevice（要求 `ClientDeviceSupplier` 提供 `getClientFromDevice(userId)` 重载），而不是无参取默认值；参考 `DefaultClientDeviceSupplier` 现有签名补齐

### 10.2 回滚策略

每个 PR 独立可回滚（PR-2 / PR-4 是破坏性变更但内部完整）。最坏情况下回到 v1.4.0 状态。建议在 v1.5.0-RC 发布前完成 PR-1~PR-4，并在主分支灰度运行 1 个月后再正式发版。

---

## 十一、配套文档更新

- `CLAUDE.md`（项目级）：更新「业务方接入约定」一节
- `LAYERED-ARCHITECTURE.md`：补充 listener interface 在分层中的位置
- `CHANGELOG.md`：1.5.0 版本变更记录
- `BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md`：标记为「上一阶段，已被本方案替代」
- 新增 `LISTENER-MIGRATION-GUIDE.md`：业务方从 v1.4.0 迁移到 v1.5.0 的 step-by-step（含接口方法 → listener 方法逐项映射表）

---

## 十二、不在本方案范围内

- ✗ 业务方代码侵入式重写（外层只暴露 listener interface，业务方自行决定如何组织实现类）
- ✗ 跨进程 listener 分发（仍然是同进程 Spring 事件）
- ✗ Listener 优先级 / 并发执行（沿用 Spring `@EventListener` 默认行为，需要时业务方加 `@Order` / `@Async`）
- ✗ 兼容期 Adapter（旧接口 → 新 listener 的桥接），v1.5.0 一刀切
- ✗ Reactive 化（如需要 Mono/Flux 返回值，留作 v2.0+ 议题）
- ✗ Query 类继承统一（DeviceRecordQuery 等改 extends DeviceBase），v2.0 议题
- ✗ Java 版本升级到 21（如升级，Adapter 可重构为 sealed switch pattern，对外接口不变）

---

## 十三、决策记录

| 决策 | 选择 | 原因 |
|---|---|---|
| Listener 是接口还是抽象类 | **接口（default 方法）** | Java 单继承限制，接口可多实现 |
| 一站式 vs 分类 listener | **分类 5 个 + 提供 Adapter 便利基类** | 业务方按需选择粒度 |
| Adapter 用 ObjectProvider vs 直接 List 注入 | **ObjectProvider** | 兼容业务方零 listener bean 的场景（启动期不报错） |
| 多 listener 处理 query 的策略 (v1.1 修订) | **getIfUnique，多实例 fail fast** | firstNonNull 顺序静默不确定，多 listener 时容易踩坑 |
| 多 listener 处理 control 的策略 | **全部调用** | 控制类是观察者模式（多个业务可能都要记录） |
| Pattern matching 形式 (v1.1 修订) | **Java 17 instanceof 链 + 字符串 switch** | 项目锁 Java 17，不引入预览特性 |
| Config 类分发形式 (v1.3 新增) | **`Class<?> → Consumer` 显式映射表** | 5 个 config 类当前互为兄弟节点，instanceof 链顺序不敏感；但映射表能在未来父子化重构时免改、并支持单元测试遍历断言完整性 |
| Control 类分发形式 (v1.3 新增) | **暂留 instanceof 链** | 13 个 control 类同样兄弟节点，链路可读性可接受；如未来到 20+ 再重构为映射表 |
| KeepaliveControl 拆分论据 (v1.3 修订) | **L0 cmdType 分流 + 语义差异** | 实际继承链为 KeepaliveControl→ControlBase→DeviceControlBase（间接继承），所以"类型不同"不成立。真正理由是 cmdType=Keepalive 由独立 L0 handler 接收 + 语义是状态上报而非控制 |
| Adapter 强转 ToDevice 的安全策略 (v1.3 新增) | **辅助方法 requireToDevice(...) + 收紧 supplier javadoc** | 强转失败时给明确错误信息而非裸 ClassCastException |
| SUBSCRIBE 决策权 (v1.1 修订) | **fire-and-forget，不暴露 accept/deny** | 避免阻塞 SIP 事务；deny 场景极少 |
| ClientKeepaliveEvent 是否独立 (v1.1 新增 / v1.3 修订论据) | **独立** | KeepaliveControl 间接继承 DeviceControlBase（v1.3 修正），但 cmdType=Keepalive 的 L0 handler 已经独立，且语义不同（状态上报非控制指令），独立事件让 Adapter 不必再做 instanceof 区分 |
| server lifecycle 粒度 (v1.1 修订) | **保留 register/challenge/remoteAddress/offline 独立 hook + payload** | 这些 payload 业务必需，不能简化为 onDeviceOnline(deviceId) |
| INVITE 状态合并 (v1.1 修订) | **trying/ok/failure/ack/bye/byeError 全部独立 hook** | 状态机语义不同，不能合并；payload 也不一样 |
| PR 拆分粒度 (v1.1 修订) | **PR-2/PR-3 合并为破坏性 PR** | @ConditionalOnBean 与接口删除必须原子，否则中间状态不可用 |
| 删除策略 | **一刀切（v1.5.0 主版本）** | 兼容期会让事件总线再陷半途困境 |

---

## 十四、关联协议层迁移

本方案是 GB28181 协议层迁移的最后一公里：

```
v1.0  → v1.3.0：MESSAGE/NOTIFY 接口 → 事件总线（已完成）
v1.3.0 → v1.4.0：补齐 GBT-28181-2022 协议字段（已完成）
v1.4.0 → v1.5.0：Listener 化业务接口分层（本方案）
v1.5.0 → 后续：Reactive 化 / 多语言 SDK / 跨进程分发（v2.0+）
```

完成本方案后，sip-proxy 的业务接入完全脱离 SIP 协议细节，业务方代码与 GB28181 标准一一对应、且可读性好——这才是 v1.3.0 事件总线设计的完整形态。

---

## 十五、sip-gateway / voglander 业务侧迁移影响（v1.1 新增）

sip-proxy 是被 voglander / sip-gateway 等业务侧仓库依赖的 Maven library，v1.5.0 升级会**直接断编译**业务侧。需要双侧协同。

### 15.1 业务侧改动范围（以 voglander 为例）

| 业务侧位置（推测） | 改动 |
|---|---|
| `voglander-integration/.../sip/` 中实现 `MessageRequestHandler` 的类 | 改 `implements QueryListener` (+ 选择性其他 listener) |
| 实现 `DeviceControlRequestHandler` 的类 | 改 `implements ControlListener` |
| 监听 `Client*Event` / `Device*Event` 的 `@EventListener` 方法 | 大部分迁移到 listener 方法；纯监控类继续用 Layer 1 事件 |
| `voglander-test/` 中的 mock | 全部继承 `ClientGb28181Adapter` / `ServerGb28181Adapter` |

### 15.2 接口方法 → listener 方法映射（核心摘录）

| v1.4.0 接口方法 | v1.5.0 listener 方法 |
|---|---|
| `MessageRequestHandler.getDeviceItem(userId)` | `QueryListener.onCatalogQuery(platformId, q)` —— 注意参数从 userId 改为完整 query |
| `MessageRequestHandler.getDeviceInfo(userId)` | `QueryListener.onDeviceInfoQuery(platformId, q)` |
| `MessageRequestHandler.getDeviceStatus(userId)` | `QueryListener.onDeviceStatusQuery(platformId, q)` |
| `MessageRequestHandler.getDeviceRecord(deviceRecordQuery)` | `QueryListener.onRecordInfoQuery(platformId, q)` |
| `MessageRequestHandler.getDeviceAlarmNotify(deviceAlarmQuery)` | `QueryListener.onAlarmQuery(platformId, q)` |
| `MessageRequestHandler.getDeviceConfigResponse(deviceConfigDownload)` | `QueryListener.onConfigDownloadQuery(platformId, q)` |
| `MessageRequestHandler.getDevicePresetQueryResponse(presetQuery)` | `QueryListener.onPresetQuery(platformId, q)` |
| `MessageRequestHandler.getMobilePositionNotify(mobilePositionQuery)` | `QueryListener.onMobilePositionQuery(platformId, q)` |
| `MessageRequestHandler.broadcastNotify(broadcastNotify)` | `NotifyListener.onBroadcastNotify(platformId, n)` |
| `MessageRequestHandler.deviceControl(...)` | 删除（DeviceControlMessageHandler 已用 ControlListener 取代） |
| `DeviceControlRequestHandler.handlePtzCmd(c)` 等 13 个方法 | `ControlListener.onPtzControl(platformId, c)` 等 13 个方法（一一对应） |
| `SubscribeRequestHandler.putSubscribe(userId, info)` | 删除（协议层内化，业务方无需关心） |
| `SubscribeRequestHandler.getDeviceSubscribe(deviceQuery)` | 删除（业务方主动催发 NOTIFY 即可） |
| `@EventListener ClientPtzPositionQueryEvent` | `QueryListener.onPtzPositionQuery(platformId, q)` |
| `@EventListener ClientSnapShotConfigEvent` | `ConfigListener.onSnapShotConfig(platformId, c)` |
| `@EventListener DeviceCatalogEvent` | `DeviceResponseListener.onCatalogResponse(deviceId, catalog)` |
| `@EventListener DeviceAlarmEvent` | `DeviceNotifyListener.onAlarmNotify(deviceId, notify)` |
| `@EventListener DeviceRegisterEvent` | `DeviceLifecycleListener.onDeviceRegister(deviceId, ctx)` |
| `@EventListener DeviceInviteOkEvent` | `DeviceSessionListener.onInviteOk(session)` |

### 15.3 业务侧迁移建议节奏

1. sip-proxy v1.5.0-RC1 发布
2. voglander 在独立分支拉新依赖，**先**让编译通过（删除旧 `MessageRequestHandler` impl，改 `implements QueryListener`，方法重命名）
3. 跑现有集成测试，验证 listener 收到事件
4. 验证完合并到 voglander 主干，sip-proxy 正式发版 v1.5.0
5. 删除 v1.4.0 的兼容代码（如有）

业务侧改动量预估：voglander 大约 **20-30 个类**（接口实现 + EventListener 散点）受影响，集中在 `voglander-integration/sip/` 模块。

### 15.4 业务侧 listener bean 必须在 Spring 扫描路径内（v1.3 新增）

sip-proxy 是 Maven library，`ClientListenerAdapter` 通过 `ObjectProvider<QueryListener>` 在业务侧 Spring 容器内寻找业务方注册的 listener bean。这意味着：

| 必须 | 后果（如违反） |
|---|---|
| 业务方 listener 类标注 `@Component` / `@Service` / `@Bean` | bean 不存在，`getIfUnique()` 返回 null，**所有 query 静默走默认空响应** |
| 业务方 listener 类的包必须落在 `@SpringBootApplication.scanBasePackages` 之下 | 同上：bean 不被扫描 |
| 业务方 listener 类**不要**误加 `@ConditionalOnProperty` 等条件后让条件为 false | 同上：bean 缺失，且无任何编译期错误 |
| 业务方注册 ≥2 个 `QueryListener` 时必须明确 `@Primary` 或留唯一一个 | Spring 启动期 fail fast（`getIfUnique()` 返回 null + 多 bean 警告），但失败信息不直观 |

**自我诊断**：Adapter 在收到 `ClientQueryEvent` 但 `QueryListener` 为 null 时，**首次告警**（不是每次，避免日志洪水）：
```
WARN ClientListenerAdapter — 收到 ClientQueryEvent 但未找到 QueryListener bean。
  请检查业务侧 listener 是否：
    1. 标注 @Component / @Service / @Bean
    2. 落在 @SpringBootApplication.scanBasePackages 路径内
    3. 没有被 @ConditionalOnProperty 等条件过滤
  本次查询将走默认空响应（无回包）。
```

业务侧团队故障排查时优先看这条 warn，可省 90% 沟通成本。

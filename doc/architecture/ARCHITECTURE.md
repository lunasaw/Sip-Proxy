# SIP Proxy 架构说明

> 版本：1.5.6 | Java 17 + Spring Boot 3.3.1
>
> 关联：
> - [LAYERED-ARCHITECTURE.md](LAYERED-ARCHITECTURE.md) — 业务侧 sip-gateway 的部署 / 状态切分 / 横向扩展
> - [LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md) — 三层 listener 接口的设计动机与决策
> - [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md) — 每个 cmdType 的 L0 类 / L1 事件 / L2 listener 方法对应表
> - [PROTOCOL-DECOUPLING-PLAN.md](../plans/1.3.0/PROTOCOL-DECOUPLING-PLAN.md) — sip-common 与 GB28181 解耦边界
> - [HORIZONTAL-SCALING.md](HORIZONTAL-SCALING.md) — 多节点部署、状态本地性、VIP 拓扑
>
> 历史变更：
> - 1.5.6 协议路由修正 + 死代码清理 + 4 个 cmdType 补全（VideoUpload / MobilePosition Notify / ConfigDownload / PresetQuery 应答）
> - 1.5.2 删除 32 个 server 端 typed `Device*Event`，server 真正 listener 化
> - 1.5.0 Listener 化业务接口分层（client 5 接口 + server 4 接口 + 双 Adapter），client 端零兼容包袱
> - 1.4.0 GBT-28181-2022 协议扩展 13 阶段全量落地
> - 1.3.0 全量删除 `*Handler` 接口、统一 Spring Event 总线、INVITE 异步化、sip-common 与 GB28181 解耦

本框架以 **Maven 库**形式嵌入业务方进程（典型为 `sip-gateway`），同 JVM 内可同时启用平台服务端（gb28181-server）与设备客户端（gb28181-client）。

## 模块依赖

```
gb28181-test
    ├── gb28181-client ──┐
    └── gb28181-server ──┤
                         ├── gb28181-common ── sip-common
```

| 模块 | 职责 | 是否含 GB28181 |
|---|---|---|
| [sip-common](#sip-common--通用-sip-协议层) | 通用 SIP 协议栈（JAIN-SIP 封装、事务、缓存、指标、TimeSync） | ❌ 不允许 |
| [gb28181-common](#gb28181-common--gb28181-协议数据模型) | GB28181 数据模型（JAXB XML 实体、SDP 工具、命令策略基类） | ✅ |
| [gb28181-client](#gb28181-client--设备客户端) | 设备客户端：5 个 listener 接口 + 6 个 L1 协议事件 + 8 个 SIP method 事件 | ✅ |
| [gb28181-server](#gb28181-server--平台服务端) | 平台服务端：4 个 listener 接口 + 4 个 L1 聚合事件 | ✅ |
| gb28181-test | 集成测试 + sip-gateway 业务侧参考实现（`gateway/` 包） | ✅ |

> **协议纯净性**：`sip-common` 不允许出现 `gb28181 / GB28181 / gbproxy / Catalog / MobilePosition / GbSession / GbSip / GbUtil`。CI 通过 `scripts/check-sip-common-purity.sh` 强制校验，失败即 build fail。

---

## 三层协议栈模型（Listener 化后）

业务方接入完全脱离 SIP 协议细节,只面对 L2 listener 接口。L0/L1 由框架内部分发,跨切层(metrics/audit/tracing)可监听 L1 事件。

```
┌────────────────────────────────────────────────────────────┐
│  L2  业务方 Listener（业务方 implements，only override）    │
│  Client: QueryListener / ControlListener / ConfigListener   │
│          SubscribeListener / NotifyListener                 │
│          → 一站式基类 ClientGb28181Adapter                  │
│  Server: DeviceResponseListener / DeviceNotifyListener      │
│          DeviceLifecycleListener / DeviceSessionListener    │
│          → 一站式基类 ServerGb28181Adapter                  │
└──────────────────▲─────────────────────────────────────────┘
                   │ ObjectProvider 注入 + 框架内部分发
┌──────────────────┴─────────────────────────────────────────┐
│  L1  协议事件（任何 bean 可 @EventListener,跨切监听）       │
│  Client: ClientQueryEvent / ClientControlEvent              │
│          ClientConfigEvent / ClientSubscribeEvent           │
│          ClientNotifyEvent / ClientKeepaliveEvent           │
│          + 8 个 SIP method 事件（Invite/Bye/Ack/Cancel      │
│            /Info/Register{Success,Failure,Challenge,Redirect})│
│  Server: ServerLifecycleEvent / ServerNotifyEvent           │
│          ServerQueryResponseEvent / ServerSessionEvent      │
└──────────────────▲─────────────────────────────────────────┘
                   │ publishEvent
┌──────────────────┴─────────────────────────────────────────┐
│  L0  SIP 入站处理器（框架内部）                              │
│  AbstractSipListener → XxxRequestProcessor                  │
│    → MESSAGE 子分发器（按 GB28181 cmdType）                  │
│    → 具体 *MessageHandler.parseXml + publishEvent           │
└─────────────────���──────────────────────────────────────────┘
```

每个 cmdType 在三层的具体落点见 [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md)。

---

## sip-common — 通用 SIP 协议层

JAIN-SIP 封装,纯协议栈,不含 GB28181 业务。

| 包 | 关键类 | 职责 |
|---|---|---|
| `layer/` | `SipLayer` | JAIN-SIP `SipProviderImpl` 封装,管理 UDP/TCP 监听点 |
| `transmit/` | `AbstractSipListener` | 统一 SIP 事件分发,`method → List<SipRequestProcessor>` 多播 |
| `transmit/` | `SipSender` | 静态工具,封装 INVITE/BYE/ACK/INFO/MESSAGE/NOTIFY/REGISTER/SUBSCRIBE 八类发送 |
| `transmit/` | `SipMessageTransmitter` | 底层消息出站 |
| `transmit/` | `SipTransactionRegistry` | 事务注册表(`callId → ServerTransaction`),**JVM 本地状态**,跨节点不可序列化 |
| `transmit/` | `SipServerTransactionProvider` | INVITE 异步回包用的事务取回入口 |
| `transmit/` | `AsyncSipListener` / `DefaultSipListener` / `CustomerSipListener` | listener 实现变体 |
| `transmit/request/` | `SipRequestBuilderFactory` + 8 个 `XxxRequestBuilder` | 按 method 路由请求构建 |
| `transmit/strategy/` | `SipRequestStrategyFactory` + 8 个 `XxxRequestStrategy` | 按 method 路由发送策略 |
| `transmit/event/` | `Event` / `EventResult` / `SipMethod` / `SipSubscribe` | 事件原语 |
| `transmit/event/request/` | `SipRequestProcessor` / `SipRequestProcessorAbstract` | 请求处理器 SPI |
| `transmit/event/response/` | `SipResponseProcessor` / `AbstractSipResponseProcessor` | 响应处理器 SPI |
| `transmit/event/message/` | `MessageHandler` / `MessageHandlerAbstract` / `SipMessageRequestProcessorAbstract` | MESSAGE 处理基类 |
| `service/` | `DeviceSupplier` / `ClientDeviceSupplier` / `ServerDeviceSupplier` | 设备信息提供器接口 |
| `service/` | `TimeSyncService` (+ `NtpTimeSyncScheduler`) | GB28181 §9.10 网络校时(SIP Date 头域 + NTP 双模) |
| `cache/` | `CacheService` (Caffeine) | 本地缓存 |
| `metrics/` | `SipMetrics` | Micrometer 指标采集 |
| `context/` | `SipTransactionContext` | TraceId 异步传播上下文 |
| `entity/` | `Device` / `FromDevice` / `ToDevice` / `DeviceSession` / `RemoteAddressInfo` / `SdpSessionDescription` | 通用实体 |
| `conf/` | `SipProxyAutoConfig` / `EnableSipProxy` / `DefaultProperties` | Spring Boot 自动配置入口 |
| `config/` | `SipCommonProperties`(`sip.*`) / `SipCommonContextHolder` / `MetricsConfig` | 配置 |
| `exception/` | `SipException` / `SipConfigurationException` / `SipErrorType` | 异常体系 |

**自动配置**:`SipProxyAutoConfig`(经 `spring.factories`)。

---

## gb28181-common — GB28181 协议数据模型

纯数据模型 + JAXB XML 序列化 + 命令策略基类,无业务逻辑。

| 包 | 内容 |
|---|---|
| `entity/base/` | `DeviceBase` |
| `entity/control/` | 19 个控制实体(`DeviceControlPtz` / `RecordCmd` / `Guard` / `Alarm` / `IFame` / `DragZoom{In,Out}` / `Position` / `PTZPrecise` / `TeleBoot` / `SDCardFormat` / `TargetTrack` / `DeviceUpgradeControl` / `KeepaliveControl` / `DeviceConfigControl` / `SnapShotConfig` / `ControlBase`) |
| `entity/control/cfg/` | 10 个配置实体(`OsdConfig` / `AlarmReportConfig` / `VideoAlarmRecordConfig` / `VideoRecordPlanConfig` / `PictureMaskConfig` / `FrameMirrorConfig` / `SVAC{Encode,Decode}Config` / `VideoParam{Attribute,Opt}Config`) |
| `entity/control/instruction/` | PTZ 指令构建 / 加密 / 序列化(`PTZInstructionBuilder` / `PTZInstructionCrypto` / `PTZInstructionSerializer` / `PTZInstructionManager`) |
| `entity/notify/` | 10 个通知实体(`DeviceAlarmNotify` / `DeviceBroadcastNotify` / `DeviceKeepLiveNotify` / `MobilePositionNotify` / `MediaStatusNotify` / `UpgradeResultNotify` / `UploadSnapShotFinishedNotify` / `VideoUploadNotify` / `DeviceOtherUpdateNotify` / `DeviceUpdateItem`) |
| `entity/query/` | 12 个查询实体(`DeviceQuery` / `DeviceAlarmQuery` / `DeviceConfigDownload` / `DeviceMobileQuery` / `DeviceRecordQuery` / `PresetQuery` / `MobilePositionQuery` / `PTZPositionQuery` / `HomePositionQuery` / `SDCardStatusQuery` / `CruiseTrackQuery` / `CruiseTrackListQuery`) |
| `entity/response/` | 14 个应答实体(`DeviceCatalog` / `DeviceInfo` / `DeviceStatus` / `DeviceRecord` / `DeviceConfigResponse` / `DeviceConfigDownloadResponse` / `PresetQueryResponse` / `MobilePositionResponse`* / `PTZPositionResponse` / `HomePositionResponse` / `SDCardStatusResponse` / `CruiseTrackResponse` / `CruiseTrackListResponse` / `DeviceResponse`) |
| `entity/sdp/` | `GbSessionDescription` |
| `entity/mansrtsp/` | INFO MANSRTSP(回放控制 PLAY/PAUSE/TEARDOWN/SCALE) |
| `entity/enums/` | `CmdTypeEnum` / `DeviceGbType` / `InviteSessionNameEnum` / `ManufacturerEnum` / `StreamModeEnum` / `TransModeEnum` |
| `entity/utils/` | **`GbSdpUtils`**(GB SDP 解析) / **`GbUtil`**(GB28181 编码生成) / `PtzCmdEnum` / `PtzUtils` |
| `entity/xml/` | `XmlBean`(所有 GB28181 实体共同父类) |
| `transmit/cmd/` | `AbstractCommandStrategy` / `CommandContext` / `CommandStrategy` / `CommandStrategyFactory`(client/server 共用基类) |
| `conf/` | `Gb28181CommonAutoConfig` |

**自动配置**:`Gb28181CommonAutoConfig`(经 `@EnableSipClient` / `@EnableSipServer` 注解 import)。

---

## gb28181-client — 设备客户端

模拟 GB28181 设备端行为。**业务方接入面**:5 个 listener 接口 + `ClientGb28181Adapter` 一站式基类。

### L2 业务接口(`api/`)

| 接口 | 方法数 | 语义 | 多 bean 行为 |
|---|---|---|---|
| `QueryListener` | 14(Catalog/DeviceInfo/DeviceStatus/RecordInfo/Alarm/ConfigDownload/Preset/MobilePosition/PTZPosition/HomePosition/CruiseTrack(List)/SDCardStatus + DeviceMobile) | 平台查询设备,**返回非 null 时 Adapter 自动回包** | `ObjectProvider#getIfUnique()` 强制单 bean,多实例 fail fast |
| `ControlListener` | 14(13 个 DeviceControl 子类型 + 1 个 Keepalive) | 平台下发控制指令,fire-and-forget | `List<>` 注入,全部调用(观察者) |
| `ConfigListener` | 5(SnapShot / Osd / AlarmReport / VideoAlarmRecord / DeviceConfigControl) | 平台下发设备配置,fire-and-forget | `List<>` 注入,全部调用 |
| `SubscribeListener` | 4(Catalog / Alarm / MobilePosition / PTZPosition) | 平台订阅设备事件,200 OK 由协议层同步返回,listener **不能拒绝订阅** | `List<>` 注入,全部调用 |
| `NotifyListener` | Broadcast 通知 | 平台下发广播 | `List<>` 注入,全部调用 |

`ClientGb28181Adapter` 同时实现 5 个 listener,业务方继承后只 override 关心的方法即可。

### L1 协议事件(`eventbus/event/`)

外层 6 个(MESSAGE 体系 + Keepalive,跨切监听用):

| 事件 | 触发条件 | payload |
|---|---|---|
| `ClientQueryEvent` | rootType=Query | 19 类 query 实体多态承载 |
| `ClientControlEvent` | rootType=Control & cmdType=DeviceControl | 13 类 DeviceControl 子类 |
| `ClientKeepaliveEvent` | cmdType=Keepalive | `KeepaliveControl`(独立成事件,语义是状态上报) |
| `ClientConfigEvent` | rootType=Control & cmdType=DeviceConfig | 5 类 config 实体 |
| `ClientSubscribeEvent` | method=SUBSCRIBE | 4 类 subscribe 实体 |
| `ClientNotifyEvent` | rootType=Notify | Broadcast 等 |

SIP method 系 8 个(与 SIP method 强绑定,继续作为协议层规范 API):

`ClientInviteEvent` / `ClientByeEvent` / `ClientAckEvent` / `ClientCancelEvent` / `ClientInfoEvent` / `ClientRegister{Success,Failure,Challenge,Redirect}Event`

### L1 → L2 分发器(`eventbus/internal/`)

`ClientListenerAdapter` —— 监听 6 个外层事件,按 payload 类型分发到 listener 接口方法。Query 类自动回包(调对应 `ClientCommandSender` 方法)。

### L0 入站处理器(`transmit/request/`)

| SIP method | 处理器 |
|---|---|
| INVITE | `InviteRequestProcessor` |
| ACK | `ClientAckRequestProcessor` |
| BYE | `ByeRequestProcessorClient` |
| CANCEL | `CancelRequestProcessor` |
| INFO | `InfoRequestProcessor`(支持 MANSRTSP 回放控制) |
| MESSAGE | `ClientMessageRequestProcessor` → `MessageClientHandlerAbstract` 子类(按 cmdType) |
| SUBSCRIBE | `SubscribeRequestProcessor` → `SubscribeHandlerAbstract` 子类 + `SubscribeRegistry`(协议层订��注册表,业务无感知) |

MESSAGE 子处理器(`message/handler/`):
- `query/`:13 个查询(`AlarmQuery` / `Catalog` / `ConfigDownload` / `CruiseTrack(List)` / `DeviceInfo` / `DeviceMobile` / `DeviceStatus` / `HomePosition` / `Preset` / `PtzPosition` / `RecordInfo` / `SdCardStatus`)
- `control/`:`DeviceControl`(13 子类型,内部 `Class<?> → BiConsumer` 显式分发) / `DeviceConfigControl`(5 子类型) / `Keepalive`
- `notify/`:`BroadcastNotify`

SUBSCRIBE 子处理器(`subscribe/`):`alarm/` / `catalog/` / `mobile/` / `ptz/` 各一个 handler。

### L0 出站(`transmit/cmd/`)

`ClientCommandSender`(Spring bean) → `CommandStrategyFactory` → 8 个 `XxxCommandStrategy`(Ack/Bye/Info/Invite/Message/Notify/Register/Subscribe) → `SipSender`。

### 入站响应(`transmit/response/`)

`ClientAbstractSipResponseProcessor` + `ClientRegisterResponseProcessor` / `ClientAckResponseProcessor` / `ByeResponseProcessor` / `CancelResponseProcessor`。

### 配置 / 自动配置

- `@EnableSipClient`(注解化接入)
- `SipClientProperties`(`sip.client.*`)
- `SipProxyClientAutoConfig`
- `DefaultClientDeviceSupplier`(单机演示用,生产应自定义实现 `ClientDeviceSupplier`)

### 必备 bean

| Bean | 实现来源 | 说明 |
|---|---|---|
| `ClientDeviceSupplier` | 业务方提供,默认 `DefaultClientDeviceSupplier` 仅适用单机 | 提供本端 client 设备身份 |
| `DeviceSupplier` | 业务方提供 | 提供目标设备身份(上级平台) |
| `QueryListener` | 业务方提供(可选,缺失时框架默认空响应) | 单 bean 强制 |

---

## gb28181-server — 平台服务端

提供 GB28181 平台级服务。**业务方接入面**:4 个 listener 接口 + `ServerGb28181Adapter` 一站式基类。

### L2 业务接口(`api/`)

| 接口 | 方法数 | 语义 |
|---|---|---|
| `DeviceResponseListener` | 14(Catalog / DeviceInfo / DeviceStatus / RecordInfo / DeviceConfig / **ConfigDownload** / **PresetQuery** / Cruise(List) / PTZPosition / HomePosition / SDCardStatus / NotifyUpdate / SubscribeResponse) | 设备应答查询 |
| `DeviceNotifyListener` | 8(Keepalive / Alarm / MediaStatus / **MobilePosition** / SnapShotFinished / UpgradeResult / **VideoUpload** + Broadcast 应答) | 设备主动通知 |
| `DeviceLifecycleListener` | 5(Register / Offline / RegisterChallenge / Online / RemoteAddressChanged) | 设备生命周期 |
| `DeviceSessionListener` | 7(Invite{Trying,Ok,Failure} / Ack / Bye / Info / Cancel) | INVITE/BYE 状态机 |

`ServerGb28181Adapter` 同时实现 4 个 listener,业务方继承后只 override 关心的方法。

`api/dto/`:`DeviceInfoError` / `DeviceInfoRequest` / `DeviceSubscribeResponse` / `LifecycleType` / `SessionType`(listener 方法参数 DTO)。

### L1 ��议事件(`transmit/event/`)

v1.5.2 起删除了 32 个 typed `Device*Event`,改为 4 个聚合事件:

| 事件 | 承载 | 用途 |
|---|---|---|
| `ServerLifecycleEvent` | `LifecycleType` + payload | 注册 / 离线 / 挑战 / 在线 / 远端地址漂移 |
| `ServerNotifyEvent` | cmdType + 8 类 notify payload | 设备主动 notify |
| `ServerQueryResponseEvent` | cmdType + 14 类 response payload | 设备应答 query |
| `ServerSessionEvent` | `SessionType` + INVITE/BYE/ACK 上下文 | 会话状态机 |

> 跨切监听只需 `@EventListener Server*Event`,根据 `event.getXxxType()` switch 即可。业务监听**强烈建议**走 listener 接口。

### L1 → L2 分发器(`transmit/event/internal/`)

`ServerListenerAdapter` —— 监听 4 个聚合事件,转发到 4 个 listener 接口方法。

### L0 入站处理器(`transmit/request/`)

| SIP method | 处理器 |
|---|---|
| REGISTER | `ServerRegisterRequestProcessor` + `DigestServerAuthenticationHelper`(401 摘要认证) + `RegisterInfo` |
| INVITE | `ServerInviteRequestProcessor`(异步:100 Trying + 事件 + 异步回包) |
| INFO | `ServerInfoRequestProcessor` |
| BYE | `ByeRequestProcessorServer` |
| MESSAGE | `ServerMessageRequestProcessor` → `MessageServerHandlerAbstract` 子类(按 cmdType) |
| NOTIFY | `ServerNotifyRequestProcessor` → `NotifyServerHandlerAbstract` 子类 |

MESSAGE 子处理器(`message/`):
- `notify/`:7 个(`AlarmNotify` / `Keepalive` / `MediaStatus` / `MobilePosition` / `UpgradeResult` / `UploadSnapShotFinished` / `VideoUpload`)
- `response/`:12 个(`ConfigDownloadResponse` / `CruiseTrack(List)` / `DeviceConfig` / `DeviceInfo` / `DeviceStatus` / `HomePosition` / `PresetQueryResponse` / `PtzPosition` / `RecordInfo` / `ResponseCatalog` / `SdCardStatus`)

NOTIFY 子处理器:`notify/catalog/CatalogNotifyHandler`。

### L0 出站(`transmit/cmd/`)

`ServerCommandSender`(Spring bean) → `CommandStrategyFactory` → 7 个 `XxxCommandStrategy`(Ack/Bye/Info/Invite/Message/Register/Subscribe) → `SipSender`。

`DeviceSessionCache` —— 设备会话缓存接口,**生产部署必须用 Redis 等共享存储实现**(默认实现仅适用单机演示)。

### 入站响应(`transmit/response/`)

`ServerAbstractSipResponseProcessor` + `ServerAckResponseProcessor` / `InviteResponseProcessor` / `SubscribeResponseProcessor`。

### 配置 / 自动配置

- `@EnableSipServer`(注解化接入)
- `SipServerProperties`(`sip.server.*`)
- `SipProxyServerAutoConfig`
- `DefaultServerDeviceSupplier`(单机演示用,生产应自定义实现 `ServerDeviceSupplier`)

### 必备 bean

| Bean | 实现来源 | 说明 |
|---|---|---|
| `ServerDeviceSupplier` | 业务方提供,需实现 `authenticate(...)` | 提供本端 server 身份 + 设备鉴权 |
| `DeviceSessionCache` | 业务方提供(多节点必须 Redis) | 设备会话(ip / port / transport)持久化 |
| `DeviceSupplier` | 业务方提供 | 提供已注册设备身份信息 |

---

## 关键流程

### 设备注册(REGISTER)

```
设备 → REGISTER (无 Authorization)
  → ServerRegisterRequestProcessor
  → 401 Unauthorized + WWW-Authenticate(nonce)
设备 → REGISTER (带 Authorization)
  → DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword
  → ServerDeviceSupplier.authenticate(deviceId, password)
  → 200 OK
  → publishEvent(ServerLifecycleEvent{type=REGISTER})
  → ServerListenerAdapter → DeviceLifecycleListener.onDeviceRegister
```

### 实时点播(INVITE,异步化)

```
平台 → ServerCommandSender.deviceInvitePlay(fromDevice, toDevice, sdp)
     → InviteCommandStrategy → SipSender.doInviteRequest → 设备

设备 → 100 Trying
     → InviteResponseProcessor → ServerSessionEvent{type=INVITE_TRYING}
     → DeviceSessionListener.onInviteTrying

设备 → 200 OK (含设备 SDP)
     → InviteResponseProcessor → ServerSessionEvent{type=INVITE_OK, sdp}
     → DeviceSessionListener.onInviteOk
     → 业务方调 ServerCommandSender.deviceAck

设备 → 推流到 SDP 中的媒体地址
```

### 设备控制(MESSAGE/DeviceControl,client 端入站)

```
平台 → MESSAGE (XML: <Control>+<DeviceID>+<PTZCmd>)
设备(client) →
  ClientMessageRequestProcessor
  → DeviceControlMessageHandler.parseXml
  → 按 XML 子标签查 Class<?> → BiConsumer 表
  → publishEvent(ClientControlEvent{payload=DeviceControlPtz})
  → ClientListenerAdapter
  → ControlListener.onPtz(toDevice, fromDevice, ptzCmd)
```

### 查询应答(client 端,自动回包)

```
平台 → MESSAGE (XML: <Query>+<CmdType>Catalog</CmdType>)
设备(client) →
  ClientMessageRequestProcessor → CatalogQueryMessageClientHandler
  → publishEvent(ClientQueryEvent{payload=DeviceQuery})
  → ClientListenerAdapter
  → QueryListener.onCatalogQuery(toDevice, fromDevice, query)
  → 返回 DeviceCatalog
  → Adapter 自动调 ClientCommandSender.deviceCatalogResponse(...)
  → 平台收到设备目录
```

> 业务方写代码时全程不接触 SIP 协议,只看 listener 接口。

---

## 配置命名空间

| 属性类 | 命名空间 | 模块 |
|---|---|---|
| `SipCommonProperties` | `sip` / `sip.common.*` | sip-common |
| `SipClientProperties` | `sip.client` | gb28181-client |
| `SipServerProperties` | `sip.server` | gb28181-server |

NAT 场景:`sip.external-ip` / `sip.external-port` 用于覆盖 `Via` / `Contact` 头域,未设置时回退到 `sip.ip` / `sip.port`。

校时:`sip.common.time-sync.*`(SIP Date 头域 + NTP)。

---

## 启用方式

业务方在 Spring Boot 启动类标注:

```java
@EnableSipClient   // 引入 gb28181-client + gb28181-common + sip-common
@EnableSipServer   // 引入 gb28181-server + gb28181-common + sip-common
@SpringBootApplication
public class GatewayApplication { ... }
```

两个注解可同时使用(级联代理场景:同一进程同时作为下级平台的 server 和上级平台的 client)。

---

## 关键设计决策

1. **三层 listener 模型** —— L0 协议处理器 / L1 Spring Event / L2 业务 listener。业务方只面对 L2,跨切层(metrics / audit / tracing)仍可在 L1 层监听。新增 cmdType 成本固定:listener 加一个 hook + Adapter 加一行分发即可。

2. **client 严格 listener 化、server 加性 listener 化** —— v1.5.0 一刀切删除 client 端 4 个旧业务接口(`MessageRequestHandler` / `CustomMessageRequestHandler` / `DeviceControlRequestHandler` / `SubscribeRequestHandler`)+ 10 个旧 client 事件;server 端因 voglander 等业务侧已大量监听 typed `Device*Event`,改造采取加性策略,直到 v1.5.2 才将 32 个 typed event 收敛为 4 个聚合 `Server*Event`。

3. **静态工具类全删,统一策略模式** —— 旧版 `ClientSendCmd` / `ServerSendCmd` 静态工具已删除,统一为 `XxxCommandSender` Spring bean + `CommandStrategyFactory` 路由 + `XxxCommandStrategy` 实现,便于扩展和测试。

4. **协议层纯净性** —— `sip-common` 通过 CI 脚本 `scripts/check-sip-common-purity.sh` 强制不允许出现 GB28181 关键词。GB28181 SDP / 编码生成等下沉到 `gb28181-common` 的 `GbSdpUtils` / `GbUtil`。

5. **INVITE 异步化** —— 1.3.0 起 `ServerInviteRequestProcessor` 收到 INVITE 立即回 100 Trying 并发布 `ServerSessionEvent`,200 OK 由业务方异步调 `ServerCommandSender.deviceInviteResponseOk` 回包。`SipTransactionRegistry` 持有 JAIN-SIP 事务对象,**JVM 本地状态**,多节点必须配合源 IP hash VIP。

6. **状态本地性硬约束** —— `SipTransactionRegistry` 必须在收到 INVITE 的同一节点处理回包(SIP 协议约束);`DeviceSessionCache` / `ServerDeviceSupplier` 数据必须外置共享存储(Redis),否则跨节点失效转移失败。详见 [HORIZONTAL-SCALING.md](HORIZONTAL-SCALING.md)。

7. **JaCoCo 80% 行覆盖率门禁** —— 测试不达标即 build fail,保证核心协议逻辑测试质量。

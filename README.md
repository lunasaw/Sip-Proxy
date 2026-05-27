# SIP Proxy

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/sip-proxy)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-common)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/gb28181-proxy/master/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Version](https://img.shields.io/badge/version-1.7.0-blue.svg)](CHANGELOG.md)

[项目文档](https://lunasaw.github.io/gb28181-proxy/) | [问题反馈](https://github.com/lunasaw/gb28181-proxy/issues) | [CHANGELOG](CHANGELOG.md)

基于 Java 17 + Spring Boot 3.3.1 实现的 **SIP 协议代理框架**，以 Maven 库的形式集成到业务进程中使用。

**核心定位**：纯协议层框架，屏蔽 SIP 协议细节。业务方实现 [Listener 接口](#二listener-接口业务方主入口)（推荐）或监听 [Layer 1 协议事件](#三layer-1-协议事件跨切层) 接收消息，通过 `ClientCommandSender` / `ServerCommandSender` 发送命令，**不直接接触 JAIN-SIP**。框架内置 GB28181-2016 + GB/T 28181-2022 协议实现，单 JVM 可同时启用平台服务端（`gb28181-server`）和设备客户端（`gb28181-client`），支持级联代理场景。

> **架构主线**：
> - **1.7.0 — 出站 Dialog 维护**：BYE / SUBSCRIBE 续订 / 退订改为 dialog-aware。`ServerCommandSender.deviceBye(deviceId, callId)` → `deviceBye(callId)`，`ClientCommandSender.sendByeCommand(FromDevice, ToDevice)` → `sendByeCommand(callId)`，`SipSender.doByeRequest(FromDevice, ToDevice)` **直接删除**改 `doByeRequest(callId)`。无 dialog 时抛 `DialogNotFoundException` 而非吞 `481`。新增进程内 `DialogRegistry` + `DialogRegistryCleaner`，INVITE / SUBSCRIBE 改走 stateful 发送，自动建立 JAIN-SIP Dialog；新增 `refreshSubscribe(callId, ...)` / `unsubscribe(callId)` API 替代历史"重发 SUBSCRIBE"做法。详见 [doc/OUTBOUND-DIALOG-PLAN.md](doc/OUTBOUND-DIALOG-PLAN.md)。
> - **1.5.x — Listener 化业务接口**：业务接口完全 listener 化（client 5 个 listener + server 4 个 listener，全部默认方法、按需 override）；server 端 32 个 `Device*Event` 已收敛为 4 个 `Server*Event` + 强类型 payload；client 端 4 个旧 `*Handler` 接口与 10 个细粒度 query/config event 已删除；GB/T 28181-2022 命令集（设备升级、抓拍、PTZ 精准、SD 卡、看守位、巡航轨迹、目标跟踪、报警订阅、语音对讲、视频下载等）全量落地。详见 [doc/LISTENER-LAYERED-DESIGN.md](doc/LISTENER-LAYERED-DESIGN.md) 与 [doc/PROTOCOL-LAYERING-MATRIX.md](doc/PROTOCOL-LAYERING-MATRIX.md)。
>
> 完整版本历史见 [CHANGELOG.md](CHANGELOG.md)。

## 模块结构

```
sip-proxy
├── sip-common          # 通用 SIP 协议栈（JAIN-SIP 封装、事务注册表、缓存、指标、TimeSync）；零 GB28181 代码
├── gb28181-common      # GB28181 数据模型 + GB SDP 工具（JAXB XML 实体，无业务逻辑）
├── gb28181-client      # 设备客户端：ClientCommandSender、L0 入站 handler、L1 协议事件、L2 listener API
├── gb28181-server      # 平台服务端：ServerCommandSender、L0 入站 handler、L1 协议事件、L2 listener API
└── gb28181-test        # 集成测试 + sip-gateway 业务侧单机参考实现
```

依赖顺序：`sip-common` ← `gb28181-common` ← `gb28181-client` / `gb28181-server` ← `gb28181-test`

> ⚠️ **`sip-common` 协议纯净性**：不允许出现任何 GB28181 关键词（`gb28181 / GB28181 / gbproxy / Catalog / MobilePosition / GbSession / GbSip / GbUtil`）。CI 通过 [`scripts/check-sip-common-purity.sh`](scripts/check-sip-common-purity.sh) 强制校验。GB28181 相关逻辑下沉至 `gb28181-common`（如 `GbSdpUtils`、`GbUtil`）。详见 [doc/PROTOCOL-DECOUPLING-PLAN.md](doc/PROTOCOL-DECOUPLING-PLAN.md)。

## 整体分层（部署形态）

sip-proxy 不是独立服务，而是嵌入到业务方实现的 **sip-gateway** 网关进程中。三层架构：

```
┌─────────────────────────────────────────────────────────┐
│                     业务服务器                            │
│  设备管理、录像、告警、流媒体调度等业务逻辑                  │
│  调 sip-gateway HTTP/MQ 接口触发 SIP 命令                 │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP / MQ
┌──────────────────────────▼──────────────────────────────┐
│                     sip-gateway（业务方实现）              │
│  Spring Boot 应用，多节点部署，与 sip-proxy 同 JVM         │
│  ├── 实现 DeviceSessionCache  → Redis（共享）             │
│  ├── 实现 ServerDeviceSupplier → Redis（共享）            │
│  ├── 继承 ServerGb28181Adapter / ClientGb28181Adapter    │
│  │   或 implements 单个 listener interface（按需）        │
│  └── 暴露 HTTP API → 接收业务指令调 ServerCommandSender    │
└──────────────────────────┬──────────────────────────────┘
                           │ Maven 依赖
┌──────────────────────────▼──────────────────────────────┐
│                     sip-proxy（本框架）                   │
│  L0 解析 SIP 消息 → L1 发布外层 ApplicationEvent          │
│  → L2 ListenerAdapter 分发到 listener 接口（含自动回包）   │
│  提供 ClientCommandSender / ServerCommandSender           │
└─────────────────────────────────────────────────────────┘
```

**为什么 sip-proxy 必须与 sip-gateway 同 JVM**：`SipTransactionRegistry` 持有的 `ServerTransaction` 是 JAIN-SIP 实现类（`SIPServerTransactionImpl`），不可序列化；`RequestEvent` 内部持有 socket 引用（`SipProvider`、`Dialog`），跨进程后无法回包。完整方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md)。

> **参考实现**：[`gb28181-test/.../gateway/`](gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 提供了 sip-gateway 单机版完整实现（`GatewayProperties` / `InviteContextStore` / `SipEventForwarder` / `SipCommandController` / `BusinessNotifier`），生产部署需将 `InMemoryInviteContextStore` 替换为 Redis 实现、`NoopBusinessNotifier` 替换为实际 HTTP/MQ 推送、`nodeAddressMap` 替换为 K8s/Nacos 动态发现。

## 典型使用场景

| 场景 | 引入模块 | 说明 |
|------|---------|------|
| 平台服务端（接收设备注册） | `gb28181-server` | 业务系统作为 GB28181 平台，管理下级设备 |
| 设备客户端（向平台注册） | `gb28181-client` | 业务系统模拟设备，向上级平台注册 |
| 级联代理（双角色共存） | `gb28181-client` + `gb28181-server` | 同一进程同时作为下级平台的服务端和上级平台的客户端 |
| 自定义 SIP 扩展协议 | `sip-common` + 自定义模块 | 基于 SIP 协议栈实现非 GB28181 的私有协议 |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Redis（多节点部署必需，单机演示可用 `InMemoryInviteContextStore`）

### 引入依赖

```xml
<!-- 平台服务端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>1.7.0</version>
</dependency>

<!-- 设备客户端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>1.7.0</version>
</dependency>
```

### 启用注解

```java
@SpringBootApplication
@EnableSipServer       // 平台服务端
// @EnableSipClient    // 设备客户端（级联场景两个一起加）
public class SipGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SipGatewayApplication.class, args);
    }
}
```

`@EnableSipServer` 自动激活 `SipProxyServerAutoConfig` + `Gb28181CommonAutoConfig` + `SipProxyAutoConfig`；`@EnableSipClient` 同理。

### 基础配置

```yaml
sip:
  server:
    enabled: true
    ip: 0.0.0.0                     # 监听地址（推荐 0.0.0.0）
    port: 5060
    external-ip: 1.2.3.4            # NAT/多节点场景填 VIP 或公网地址
    external-port: 5060             # 不填则 fallback 到 port
    server-id: 34020000002000000001
    domain: 34020000002000000001
    realm: "34020000"
    enable-udp: true
    enable-tcp: false
  common:
    user-agent: sip-proxy           # 默认 sip-proxy
    time-sync:
      enabled: true                 # SIP/NTP 校时（详见 SipCommonProperties.TimeSync）
      mode: SIP                     # SIP / NTP / BOTH
```

`external-ip` / `external-port` 写入出站 SIP 包的 `Via` / `Contact` 头。多节点部署时填 VIP，确保设备后续消息能回到集群。

### 必须实现的 Bean

业务方至少需要实现：

| Bean | 用途 | 实现要求 |
|------|------|---------|
| `DeviceSessionCache` | 设备会话寻址（ip / port / transport） | **多节点部署必须用 Redis 等共享存储**，框架默认实现仅适用单机演示 |
| `ServerDeviceSupplier` | 服务端设备身份 + 注册鉴权 | 启用 `@EnableSipServer` 必须实现；`authenticate(userId, SIPRequest)` 默认调 `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword` 完成摘要校验 |
| `ClientDeviceSupplier` | 客户端设备身份 | 启用 `@EnableSipClient` 必须实现 |

框架提供 `DefaultServerDeviceSupplier` / `DefaultClientDeviceSupplier`（基于配置文件），仅适用于单节点 demo 或测试场景。

## 业务接入：三层架构

sip-proxy 把入站消息处理拆成三层（listener 三层模型自 v1.5.0 起作为主线），业务方主入口在 **L2 Listener 接口**，跨切层走 **L1 Layer 1 协议事件**：

```
┌──────────────────────────────────────────────────────────┐
│  L2 业务接口 + Adapter（gb28181-{client,server}/api）       │
│    Listener 接口（业务方实现，默认方法按需 override）         │
│    ListenerAdapter（框架内部，按 payload 类型分发，自动回包） │
└──────────────────┬─────────────────────────────────────���─┘
                   │ Spring ApplicationEvent
┌──────────────────▼───────────────────────────────────────┐
│  L1 协议层事件（gb28181-{client,server}/eventbus|event）   │
│    Client: ClientQuery/Control/Keepalive/Config/Subscribe/ │
│            Notify/Invite/Bye/Ack/Cancel/Info/Register*Event│
│    Server: ServerLifecycleEvent / ServerSessionEvent /     │
│            ServerNotifyEvent / ServerQueryResponseEvent    │
│    跨切层（metrics / audit / tracing）可同时监听            │
└──────────────────┬───────────────────────────────────────┘
                   │ publishEvent
┌──────────────────▼───────────────────────────────────────┐
│  L0 协议解析（*MessageHandler / *RequestProcessor）        │
│    parseXml → publishEvent(L1 外层事件 + 多态 payload)     │
└──────────────────────────────────────────────────────────┘
```

> 📌 每行 cmdType 在 L0 / L1 / L2 三层的具体落点，见 [doc/PROTOCOL-LAYERING-MATRIX.md](doc/PROTOCOL-LAYERING-MATRIX.md)。新增 cmdType 时**先改矩阵，再改代码**。

### 一、Listener 接口（业务方主入口）

#### Client 端（设备侧）— 5 个 listener

| 接口 | 职责 | 调用语义 | 多 listener 策略 |
|------|------|---------|----------------|
| `QueryListener` | 平台主动查询：13 个查询 hook（Catalog/DeviceInfo/DeviceStatus/RecordInfo/Alarm/ConfigDownload/Preset/MobilePosition/PTZPosition/SDCardStatus/HomePosition/CruiseTrackList/CruiseTrack） | 返回非 null = Adapter 自动 `sendXxxResponse` 回包；返回 null = 不回包 | **强制单 bean**（`ObjectProvider#getIfUnique()`），多实例 fail fast |
| `ControlListener` | 平台控制：13 个 cmdType=DeviceControl 子标签（PTZ / TeleBoot / Record / Guard / AlarmReset / IFrame / DragIn / DragOut / HomePosition / DeviceUpgrade / PtzPrecise / FormatSdCard / TargetTrack）+ Keepalive | fire-and-forget | 全部调用（观察者） |
| `ConfigListener` | 平台配置：12 个 cmdType=DeviceConfig 子标签（BasicParam / VideoParamOpt / SVAC{En,De}code / VideoParamAttribute / VideoRecordPlan / VideoAlarmRecord / PictureMask / FrameMirror / AlarmReport / Osd / SnapShot） | fire-and-forget；用 `Class<?> → Consumer` 显式映射避免 instanceof 顺序陷阱 | 全部调用 |
| `SubscribeListener` | 平台订阅：4 个订阅 hook（Catalog / Alarm / MobilePosition / PTZPosition） | fire-and-forget；200 OK 由协议层同步返回，listener 不能拒绝订阅 | 全部调用 |
| `NotifyListener` | 平台通知：Broadcast 语音广播 + 兜底 | fire-and-forget | 全部调用 |

#### Server 端（平台侧）— 4 个 listener

| 接口 | 职责 | 调用语义 |
|------|------|---------|
| `DeviceLifecycleListener` | 设备生命周期：注册 / 挑战 / 在线 / 离线 / 远端地址变更（NAT 漂移） | fire-and-forget |
| `DeviceSessionListener` | INVITE/BYE/ACK 状态机：onInviteTrying / onInviteOk / onInviteFailure / onAck / onBye / onByeError + **`onServerInvite`**（设备主动 INVITE，含 `transactionContextKey` 用于异步回包） | fire-and-forget；UDP 重传场景需按 callId 自行幂等 |
| `DeviceNotifyListener` | 设备主动通知：Alarm / Keepalive / MediaStatus / MobilePosition / UpgradeResult / SnapShotFinished / VideoUploadNotify | fire-and-forget |
| `DeviceResponseListener` | 设备应答：Catalog / DeviceInfo / DeviceStatus / RecordInfo / PTZPosition / SDCardStatus / HomePosition / CruiseTrack{List,Single} / DeviceConfig / ConfigDownload / PresetQuery / Subscribe / NotifyUpdate（目录变更通知） | fire-and-forget |

#### 一站式 Adapter

```java
// Server 端：业务方继承基类即可获得全部 4 个 listener 的所有 hook
@Component
public class MyServerAdapter extends ServerGb28181Adapter {

    @Override
    public void onDeviceRegister(String deviceId, RegisterInfo info) {
        log.info("设备上线: {} from {}", deviceId, info.getRemoteIp());
    }

    @Override
    public void onAlarmNotify(String deviceId, DeviceAlarmNotify notify) {
        alarmService.dispatch(deviceId, notify);
    }

    @Override
    public void onServerInvite(String callId, String fromUserId, String toUserId,
                               GbSessionDescription sd, String transactionContextKey) {
        inviteContextStore.save(callId, nodeId, transactionContextKey, ttlMs);
        businessNotifier.inviteIncoming(callId, fromUserId, toUserId, sd);
    }
    // 其他 hook 不写一行
}

// Client 端：同理
@Component
public class MyDeviceImpl extends ClientGb28181Adapter {

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery q) {
        return buildCatalogResponse();   // 返回非 null = 框架自动回包
    }

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        ptzExecutor.execute(cmd);
    }
}
```

业务方也可按需 `implements` 单个或几个 listener interface，不必继承 Adapter。

### 二、Layer 1 协议事件（跨切层）

适合 metrics / audit / tracing / 全链路 trace 等横切关注点，与 listener 互不干扰：

#### Client 端 L1 事件

| 事件 | 触发时机 |
|------|---------|
| `ClientQueryEvent` | rootType=Query 的统一外层事件，多态承载 13 类 query payload |
| `ClientControlEvent` | rootType=Control + cmdType=DeviceControl |
| `ClientKeepaliveEvent` | cmdType=Keepalive（独立事件，与控制指令拆开） |
| `ClientConfigEvent` | cmdType=DeviceConfig，多态 payload |
| `ClientSubscribeEvent` | method=SUBSCRIBE |
| `ClientNotifyEvent` | rootType=Notify（含 Broadcast 等） |
| `ClientInviteEvent` / `ClientByeEvent` / `ClientAckEvent` / `ClientCancelEvent` / `ClientInfoEvent` | 上级 INVITE / BYE / ACK / CANCEL / INFO（INFO 含结构化 MANSRTSP） |
| `ClientRegister{Success,Failure,Challenge,Redirect}Event` | 客户端注册响应 |

#### Server 端 L1 事件（v1.5.2 起从 32 个 typed event 收敛为 4 个外层事件）

| 事件 | type/payload | 覆盖语义 |
|------|--------------|---------|
| `ServerLifecycleEvent` | `LifecycleType` ∈ {REGISTER / CHALLENGE / ONLINE / OFFLINE / REMOTE_ADDRESS_CHANGED} | 注册 / 上下线 / NAT 漂移 |
| `ServerSessionEvent` | `SessionType` ∈ {INVITE_TRYING / INVITE_OK / INVITE_FAILURE / ACK / BYE / BYE_ERROR / SERVER_INVITE} | INVITE 三向握手 + BYE + 设备主动 INVITE（语音对讲等场景，携带 `transactionContextKey`） |
| `ServerNotifyEvent` | typed payload：`DeviceAlarmNotify` / `DeviceKeepLiveNotify` / `MediaStatusNotify` / `MobilePositionNotify` / `UpgradeResultNotify` / `UploadSnapShotFinishedNotify` / `VideoUploadNotify` | 设备主动通知 |
| `ServerQueryResponseEvent` | typed payload：`DeviceResponse`(Catalog) / `DeviceInfo` / `DeviceStatus` / `DeviceRecord` / `PTZPositionResponse` / `SDCardStatusResponse` / `HomePositionResponse` / `CruiseTrackListResponse` / `CruiseTrackResponse` / `DeviceConfigResponse` / `DeviceConfigDownloadResponse` / `PresetQueryResponse` / `DeviceOtherUpdateNotify` / 订阅应答元数据 | 设备查询应答 + 错误返回 |

L1 事件携带强类型 payload，业务方既可走 listener 接口，也可 `@EventListener` 直接监听 L1 事件做跨切。

### 三、命令发送

#### `ServerCommandSender`（平台 → 设备，按 `deviceId` 寻址，依赖 `DeviceSessionCache`）

48 个出站方法，覆盖 GB/T 28181-2022 全集：

```java
@Autowired ServerCommandSender serverCommandSender;

// 查询类（GB/T 28181-2022 §A.2.4）
String callId = serverCommandSender.deviceInfoQuery(deviceId);
String callId = serverCommandSender.deviceCatalogQuery(deviceId);
String callId = serverCommandSender.deviceStatusQuery(deviceId);
String callId = serverCommandSender.deviceRecordInfoQuery(deviceId, startMs, endMs);
String callId = serverCommandSender.devicePresetQuery(deviceId);
String callId = serverCommandSender.devicePtzPositionQuery(deviceId);
String callId = serverCommandSender.deviceSdCardStatusQuery(deviceId);
String callId = serverCommandSender.deviceHomePositionQuery(deviceId);
String callId = serverCommandSender.deviceCruiseTrackListQuery(deviceId);
String callId = serverCommandSender.deviceCruiseTrackQuery(deviceId, trackNumber);
String callId = serverCommandSender.deviceConfigDownload(deviceId, "BasicParam");
String callId = serverCommandSender.deviceMobilePositionQuery(deviceId, interval);
String callId = serverCommandSender.deviceAlarmQuery(deviceId, start, end, level, method, type);

// 订阅类（§9.11）
String callId = serverCommandSender.deviceCatalogSubscribe(deviceId, expires, eventType);
String callId = serverCommandSender.deviceMobilePositionSubscribe(deviceId, interval, expires);
String callId = serverCommandSender.deviceAlarmSubscribe(deviceId, expires, eventType, /* AlarmSubscribeInfo */);
String callId = serverCommandSender.devicePtzPositionSubscribe(deviceId, expires);

// 控制类（§A.2.3.1）
String callId = serverCommandSender.deviceControlPtzCmd(deviceId, PtzCmdEnum.UP, 50);
String callId = serverCommandSender.deviceControlReboot(deviceId);
String callId = serverCommandSender.deviceControlPtzPrecise(deviceId, pan, tilt, zoom);
String callId = serverCommandSender.deviceControlIFrame(deviceId);
String callId = serverCommandSender.deviceControlDragZoomIn(deviceId, dragZoom);
String callId = serverCommandSender.deviceControlFormatSDCard(deviceId, sdNumber);
String callId = serverCommandSender.deviceControlHomePosition(deviceId, "true", resetTime, presetIndex);
String callId = serverCommandSender.deviceControlTargetTrack(deviceId, mode, target, /* TargetArea */);
String callId = serverCommandSender.deviceUpgrade(deviceId, firmware, fileURL, manufacturer, sessionId);
String callId = serverCommandSender.deviceSnapShot(deviceId, snapNum, interval, uploadURL, sessionId);

// 配置类（§A.2.3.2）
String callId = serverCommandSender.deviceConfig(deviceId, name, expiration, /* heartBeatInterval */, /* heartBeatCount */);
String callId = serverCommandSender.deviceConfigOsd(deviceId, osdInfo);
String callId = serverCommandSender.deviceConfigVideoAlarmRecord(deviceId, /* config */);
String callId = serverCommandSender.deviceConfigAlarmReport(deviceId, /* config */);

// 媒体会话（§9.2 / §9.7~9 / §9.12）
String callId = serverCommandSender.deviceInvitePlay(deviceId, mediaIp, mediaPort, StreamModeEnum.UDP);
String callId = serverCommandSender.deviceInvitePlayBack(deviceId, mediaIp, mediaPort, startMs, endMs, StreamModeEnum.UDP);
String callId = serverCommandSender.deviceInviteTalk(deviceId, mediaIp, mediaPort, StreamModeEnum.UDP);
String callId = serverCommandSender.deviceInviteDownload(deviceId, mediaIp, mediaPort, startMs, endMs, downloadSpeed, StreamModeEnum.UDP);
String callId = serverCommandSender.deviceInvitePlayBackControl(deviceId, PlayActionEnums.PAUSE);
serverCommandSender.deviceBye(callId);   // 1.7.0+：dialog-aware，必须先 INVITE 200 OK

// SUBSCRIBE 续订 / 退订（1.7.0+，dialog-aware）
serverCommandSender.refreshSubscribe(callId, 3600);              // Catalog/PTZPosition：保留原 SubscribeContent
serverCommandSender.refreshSubscribe(callId, content, 3600);     // MobilePosition/Alarm：附携新 content（如新 Interval）
serverCommandSender.unsubscribe(callId);                          // Expires=0，等价退订

// 语音广播（§9.12.1）
String callId = serverCommandSender.deviceBroadcast(deviceId);
```

> v1.5.1 起 `ServerCommandSender` 31 个 `@Deprecated` 静态门面方法已删除，业务侧需注入 bean 调用同名实例方法。
>
> v1.7.0 起 `deviceBye(deviceId, callId)` 改为 `deviceBye(callId)`、`SipSender.doByeRequest(FromDevice, ToDevice)` 直接删除。详见下文「Dialog-Aware 出站」与 [doc/OUTBOUND-DIALOG-PLAN.md](doc/OUTBOUND-DIALOG-PLAN.md)。

#### `ClientCommandSender`（设备 → 平台）

设备侧主动上报与应答（注册、心跳、Catalog 通知、报警上报、抓拍完成通知、升级结果通知、视频上传通知等）。绝大部分查询应答由 `ClientListenerAdapter` 在 `QueryListener` 返回非 null 时自动调用，业务方一般无需直接接触。

### 四、Dialog-Aware 出站（v1.7.0+）

INVITE 与 SUBSCRIBE 走 **stateful 发送**（`SipMessageTransmitter.transmitStateful` / `transmitStatefulPreRegister`），通过 `ClientTransaction` 让 JAIN-SIP 自动建立 `Dialog`，同时记录到进程内 `DialogRegistry`（按 `callId` 索引，含 `kind=INVITE|SUBSCRIBE` 与 `expiresAtMs`）。

**约束**：

- **BYE 必须有已建立的 dialog**：`deviceBye(callId)` / `sendByeCommand(callId)` / `doByeRequest(callId)`。无 dialog 则抛 `DialogNotFoundException`，**不会被设备 `481` 静默吞掉**。
- **SUBSCRIBE 续订 / 退订必须 dialog-aware**：业务方调 `refreshSubscribe(callId, expires)` / `unsubscribe(callId)`，**禁止历史"重发新 SUBSCRIBE"做法**——会在设备侧产生孤儿订阅持续浪费带宽。
- **INVITE 200 OK 的 ACK** 改用 `dialog.sendAck`（见 `InviteResponseProcessor.sendAck`），与 BYE 路径对称。

**Dialog 清理路径**：

| 场景 | 触发方式 | 路径 |
|------|---------|------|
| INVITE 主���径（BYE 后） | JAIN-SIP `DialogTerminatedEvent` | `AbstractSipListener.processDialogTerminated` → `DialogRegistry.remove` |
| SUBSCRIBE 自然过期 | 兜底定时清理（RFC 6665 §4.4.1 case 3 无 `DialogTerminatedEvent`） | `DialogRegistryCleaner` `@Scheduled` 60s 跑一次 |

**业务侧调用模板**：

```java
try {
    commandSender.deviceBye(callId);
} catch (DialogNotFoundException e) {
    // dialog 已不存在（对端先发 BYE / INVITE 还未 200 OK / callId 错误）
    log.warn("BYE 失败：dialog 不存在 callId={}", callId, e);
}
```

**为什么需要先 deprecated 桥接的形式被否决**：旧 BYE API（`deviceBye(deviceId, callId)`）会构造缺 to-tag 的 `From`/`To`，设备直接返回 `481 Call leg/Transaction does not exist`，长期被静默吞掉。1.7.0 选择**直接删除**让编译期一次性暴露所有调用点，强制业务侧迁移到 dialog-aware 路径。

> ⚠️ **新增任何 in-dialog 出站请求**（re-INVITE、UPDATE、INFO、server-side NOTIFY 等）必须走 stateful path 并从 `DialogRegistry` 取出 dialog，**不要**自己拼 `From`/`To` 头。

### 五、INVITE 异步回包模型（设备主动 INVITE）

设备主动发起的 INVITE（如语音对讲）需要业务方准备 SDP，框架采用**两步异步**模型：

```
设备 → INVITE
  → sip-proxy: ServerInviteRequestProcessor
      1. 立即发 100 Trying（防对端重传）
      2. 存 SipTransactionRegistry（contextKey = callId_fromTag_cseq → RequestEvent，���程内）
      3. 发布 ServerSessionEvent.serverInvite(callId, fromUserId, toUserId, sdp, contextKey)
  → sip-gateway: ServerListenerAdapter → DeviceSessionListener.onServerInvite
      1. 存 Redis: "sip:invite:ctx:{callId}" → "{nodeId}:{contextKey}"（30s TTL）
      2. 推送业务服务器（含 callId、SDP）

业务服务器 → POST /sip/invite/response {callId, sdp}
  → sip-gateway:
      1. 从 Redis 取 "{nodeId}:{contextKey}"
      2. nodeId == 本节点：用 contextKey 取 SipTransactionRegistry 回包
         nodeId != 本节点：通过 nodeAddressMap 转发 HTTP 到对应节点
      3. ResponseCmd.sendResponse(200, sdp, ctx.getOriginalEvent())
```

> ⚠️ **设备 Timer B 限制**：即使框架侧 `extendContext` 续期到 90 秒，设备侧（INVITE 客户端）按 RFC 3261 §17.1.1.2 在 `Timer B = 64*T1 = 32s` 后会放弃事务。业务处理时间应控制在 **30s 内直接回包**；30~60s 需主动发 `180 Ringing` + `extendContext`；> 60s 改为先回 200 OK + 占位 SDP，后续走 re-INVITE。详见 [doc/LAYERED-ARCHITECTURE.md §7](doc/LAYERED-ARCHITECTURE.md)。

### 六、协议层扩展点（进阶）

业务方一般只需实现 `DeviceSessionCache` / `*DeviceSupplier` + listener。少数高级场景才需要扩展协议层：

| 扩展点 | 用途 |
|--------|------|
| `MessageHandler` 接口 | 新增 GB28181 cmdType 处理器，通过 `SipRequestProcessorAbstract.addHandler()` 注册 |
| `SipRequestProcessorAbstract` 子类 | 新增 SIP method 处理器（如自定义非标准方法）；server 端可继承 `ServerAbstractSipRequestProcessor` |
| `ClientCommandStrategy` / `ServerCommandStrategy` | 新增出站命令策略，通过对应的 `CommandStrategyFactory` 注册 |

## SIP 消息处理流水线（内部）

```
SIP Message
  → AbstractSipListener            # 统一事件分发，TraceId 传播
  → XXXRequestProcessor            # 消息类型路由（REGISTER / INVITE / MESSAGE / NOTIFY / BYE / SUBSCRIBE / ACK / INFO …）
  → XXXSubProcessor                # MESSAGE 子类型路由（按 GB28181 cmdType）
  → L0 *MessageHandler             # parseXml + publishEvent(L1)
  → L1 ApplicationEvent            # ClientQueryEvent / ServerSessionEvent / ...
  → L2 ListenerAdapter             # @EventListener 监听 L1，按 payload 分发到 listener
  → 业务方 listener 实现             # QueryListener.onCatalogQuery 等
```

## 水平扩容

完整方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md) 与 [doc/HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md)。

### 状态分层（核心约束）

**本地节点只保留 SIP 事务状态，业务状态必须外化。**

| 状态类型 | 存储位置 | 说明 |
|---------|---------|------|
| `ServerTransaction` / `SipTransactionRegistry` | **进程内**（不可外化） | JAIN-SIP 实现类不可序列化、持有 socket 引用；同设备消息必须打同节点 |
| `DialogRegistry`（出站 dialog 注册表，1.7.0+） | **进程内**（不可外化） | 持有 JAIN-SIP `Dialog` 引用，BYE / SUBSCRIBE refresh 必须打到原 INVITE / SUBSCRIBE 所在节点；与 SIP 事务状态同生命周期 |
| `DeviceSessionCache`（设备注册信息） | **Redis**（共享，需高可用） | 业务方实现，节点间共享，节点故障后新节点可接管 |
| `ServerDeviceSupplier`（设备信息） | **Redis**（共享，需高可用） | 业务方实现，读 Redis |
| 设备订阅状态 | 业务方自管（client 端协议层 `SubscribeRegistry` 内化） | 框架 1.3.0 已删除全局 `SubscribeHolder`；client 端 SUBSCRIBE 200 OK 由 `SubscribeRegistry.put()` 内部维护 |
| INVITE 事务上下文 | **进程内** + Redis 存路由映射（需高可用） | `transactionContextKey` 仅在收到 INVITE 的节点有效；Redis 用 `callId` 作键存 `{nodeId}:{contextKey}` 供跨节点回包路由 |

> ⚠️ **Redis 是新的 SPOF**：跨节点 INVITE 路由、设备会话、注册鉴权全部依赖 Redis。生产环境必须使用 Redis Sentinel 或 Cluster；`InviteContextStore` 实现需把后端故障显式抛 `ResponseStatusException(SERVICE_UNAVAILABLE)`，让 `/sip/invite/response` 返回 503 触发业务侧重试。

### 部署拓扑

```
设备 ──→ VIP 1.2.3.4:5060 ──→ Node-1 (sip-gateway + sip-proxy)
         (keepalived + ipvs) └→ Node-2 (sip-gateway + sip-proxy)
         源 IP 哈希                    │
                                    Redis（共享 DeviceSessionCache + InviteContextStore）
                                    业务服务器
```

- VIP 四层透明转发，按**源 IP 哈希**保证同一设备打到同一节点
- 节点故障时 keepalived 自动摘除，设备重新注册分到存活节点
- 扩容粒度是 **NAT 出口**而非物理设备数。共享 NAT 的设备会全部落到同一节点，规划容量时按 NAT 出口数估算

### 接入要求

业务方多节点部署时必须：

1. 实现 `DeviceSessionCache`，使用 Redis 等共享存储（不得用本地 Map）
2. 实现 `ServerDeviceSupplier`，设备信息从共享存储读取
3. 配置 `external-ip` 为 VIP，确保 SIP 包内 `Via` / `Contact` 头是集群可达地址
4. 实现 `InviteContextStore`（参考 [`InMemoryInviteContextStore`](gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InMemoryInviteContextStore.java) 改 Redis 版），处理 INVITE 异步回包跨节点路由
5. 装配 `nodeAddressMap` Bean（K8s Endpoints / Nacos / Consul），将 `nodeId` 映射到内网地址
6. **保证 BYE / SUBSCRIBE refresh 路由到原节点**：`DialogRegistry` 进程内持有 dialog，VIP 源 IP 哈希已能保证设备主动 BYE 落到原节点；业务方主动调 `deviceBye(callId)` / `refreshSubscribe(callId, ...)` 时，需在 sip-gateway HTTP 入口校验当前节点是否持有该 callId 的 dialog（同 INVITE 路由表，复用 `InviteContextStore` 的 `{nodeId}` 映射），否则转发到目标节点

## 构建与测试

```bash
# 编译
mvn clean compile

# 构建（含单元测试）
mvn clean install

# 集成测试
mvn verify

# 指定模块测试
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest

# 指定测试方法
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest#methodName

# 协议纯净性校验（CI 在 mvn verify 阶段自动调用）
bash scripts/check-sip-common-purity.sh
```

> JaCoCo 强制要求行覆盖率 ≥ 80%。

## 开发规范

- 使用 `jakarta.*` 包，禁止 `javax.*`（Spring Boot 3.x 要求）
- 测试中使用 `@MockitoBean`，`@MockBean` 已废弃
- 访问 JAIN-SIP 实现特定方法时，将 `Request` 强转为 `SIPRequest`
- 异步线程中需显式传播 TraceId（SkyWalking）
- JSON 序列化统一使用 `fastjson2`
- `sip-common` 中禁止出现 GB28181 关键词（CI 校验，详见模块结构小节）
- 新增 cmdType 时**先更新 [PROTOCOL-LAYERING-MATRIX.md](doc/PROTOCOL-LAYERING-MATRIX.md)，再改代码**

## 配置命名空间

- `sip.server.*` — 服务端协议监听设置（`enabled` / `ip` / `port` / `external-ip` / `external-port` / `server-id` / `domain` / `realm` / `enable-udp` / `enable-tcp` 等）
- `sip.client.*` — 客户端协议设置
- `sip.common.*` — 通用框架配置（`user-agent`、`time-sync.{enabled,mode,offset-threshold,ntp-server,ntp-sync-interval,...}`）
- 环境覆盖：`application-{env}.yml`

## 协议覆盖度

| 章节 | 覆盖范围 | 状态 |
|------|---------|------|
| GB/T 28181-2022 §A.2.3.1 设备控制 | 13/13 cmdType | ✅ 全部 listener 化 |
| GB/T 28181-2022 §A.2.3.2 设备配置 | 11/11 cmdType（外加 1 个 GB28181-2016 兼容） | ✅ 全部 listener 化 |
| GB/T 28181-2022 §A.2.4 查询 | 13/13 cmdType | ✅ 全部 listener 化 |
| GB/T 28181-2022 §A.2.5 通知 | 8/8 cmdType | ✅ 全部 listener 化 |
| GB/T 28181-2022 §A.2.6 应答 | 12/15 listener 化 + 3 transport-only | ✅（剩余 3 项无业务语义） |
| GB/T 28181-2022 §9.x 流程级能力 | 注册 / 实时点播 / 历史检索回放下载 / 网络校时 / 订阅通知 / 语音广播 / 语音对讲 / 软件升级 / 图像抓拍 | ✅ |

详细矩阵见 [doc/PROTOCOL-LAYERING-MATRIX.md](doc/PROTOCOL-LAYERING-MATRIX.md)。

## 参考文档

`doc/` 目录下的关键文档：

| 文档 | 内容 |
|------|------|
| [OUTBOUND-DIALOG-PLAN.md](doc/OUTBOUND-DIALOG-PLAN.md) | 出站 Dialog 维护方案（v1.7.0 主干，BYE / SUBSCRIBE refresh dialog-aware） |
| [LISTENER-LAYERED-DESIGN.md](doc/LISTENER-LAYERED-DESIGN.md) | Listener 化业务接口分层设计（v1.5.0 主干） |
| [LISTENER-MIGRATION-GUIDE.md](doc/LISTENER-MIGRATION-GUIDE.md) | v1.4.0 → v1.5.0 业务侧迁移指南 |
| [PROTOCOL-LAYERING-MATRIX.md](doc/PROTOCOL-LAYERING-MATRIX.md) | L0/L1/L2 三层协议栈逐 cmdType 落地矩阵（v1.5.6 基于代码事实） |
| [LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md) | sip-proxy ↔ sip-gateway ↔ 业务服务器分层架构 |
| [HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md) | 多节点部署、状态分层、VIP 拓扑、NAT 处理 |
| [PROTOCOL-DECOUPLING-PLAN.md](doc/PROTOCOL-DECOUPLING-PLAN.md) | sip-common / gb28181-common 边界规则（1.3.0） |
| [BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md](doc/BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md) | 1.3.0 全量删除 `*Handler` 接口、统一 Spring Event |
| [INVITE-REFACTOR-PLAN.md](doc/INVITE-REFACTOR-PLAN.md) | INVITE 异步化重构（1.3.0） |
| [GB28181-2016.md](doc/GB28181-2016.md) / [GBT-28181-2022.md](doc/GBT-28181-2022.md) | 协议参考 |
| [CHANGELOG.md](CHANGELOG.md) | 各版本对外可见变更 |

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>如果这个项目对您有帮助，请给我们一个 ⭐️ Star！</p>
  <p>Made with ❤️ by <a href="https://github.com/lunasaw">@lunasaw</a></p>
</div>

# Listener 迁移指南：v1.4.0 → v1.5.0

> 版本：1.0 | 日期：2026-05-24
>
> 本文档面向 sip-proxy 的业务侧（如 voglander、sip-gateway）开发者，提供从 v1.4.0
> 旧的 `MessageRequestHandler` / `DeviceControlRequestHandler` / `SubscribeRequestHandler`
> 接口形态迁移到 v1.5.0 listener 接口形态的逐项映射与示例。
>
> 设计依据：[LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md)
> 历史背景：[REFACTOR-HANDLER-TO-SPRING-EVENT.md](../plans/1.3.0/REFACTOR-HANDLER-TO-SPRING-EVENT.md)

---

## 一、Client 端迁移（设备侧）

### 1.1 新业务接入方式

业务方有两种选择：

```java
// 选项 A：一站式继承基类（推荐）
@Component
public class MyClientImpl extends ClientGb28181Adapter {

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery q) {
        // 返回非 null = 框架自动 sendCatalogCommand 回包
        return buildCatalogResponse(...);
    }

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        // fire-and-forget
        ptzQueue.enqueue(cmd.getDeviceId(), cmd.getPtzCmd());
    }
    // 不关心的方法不写一行
}

// 选项 B：按需 implements 单个或几个 listener interface
@Component
public class MyQueryHandler implements QueryListener, ControlListener {
    @Override public DeviceResponse onCatalogQuery(...) { ... }
    @Override public void onPtzControl(...) { ... }
}
```

### 1.2 接口方法 → listener 方法映射表

#### `MessageRequestHandler`（已删除）→ `QueryListener` / `NotifyListener`

| v1.4.0 | v1.5.0 | 备注 |
|---|---|---|
| `getDeviceItem(String userId)` | `QueryListener.onCatalogQuery(platformId, DeviceQuery q)` | 参数从 userId 升级为完��� query |
| `getDeviceInfo(String userId)` | `QueryListener.onDeviceInfoQuery(platformId, DeviceQuery q)` | 同上 |
| `getDeviceStatus(String userId)` | `QueryListener.onDeviceStatusQuery(platformId, DeviceQuery q)` | 同上 |
| `getDeviceRecord(DeviceRecordQuery q)` | `QueryListener.onRecordInfoQuery(platformId, DeviceRecordQuery q)` | 一一对应 |
| `getDeviceAlarmNotify(DeviceAlarmQuery q)` | `QueryListener.onAlarmQuery(platformId, DeviceAlarmQuery q)` | 一一对应 |
| `getDeviceConfigResponse(DeviceConfigDownload q)` | `QueryListener.onConfigDownloadQuery(platformId, DeviceConfigDownload q)` | 一一对应 |
| `getConfigDownloadResponse(String userId, String configType)` | `QueryListener.onConfigDownloadQueryV2(platformId, ConfigDownloadQuery q)` | 参数升级为完整 query |
| `getDevicePresetQueryResponse(PresetQuery q)` | `QueryListener.onPresetQuery(platformId, PresetQuery q)` | 一一对应 |
| `getPresetQueryResponse(String userId)` | 删除（被 `onPresetQuery` 取代） | — |
| `getMobilePositionNotify(MobilePositionQuery q)` | `QueryListener.onMobilePositionQuery(platformId, MobilePositionQuery q)` | 一一对应 |
| `broadcastNotify(DeviceBroadcastNotify n)` | `NotifyListener.onBroadcastNotify(platformId, DeviceBroadcastNotify n)` | 一一对应 |
| `<T> void deviceControl(T cmd)` | 删除（由 `ControlListener` 13 个 typed 方法取代） | — |

#### `DeviceControlRequestHandler`（已删除）→ `ControlListener`

| v1.4.0 | v1.5.0 |
|---|---|
| `handlePtzCmd(DeviceControlPtz)` | `onPtzControl(platformId, DeviceControlPtz)` |
| `handleTeleBoot(DeviceControlTeleBoot)` | `onTeleBoot(platformId, DeviceControlTeleBoot)` |
| `handleRecordCmd(DeviceControlRecordCmd)` | `onRecord(platformId, DeviceControlRecordCmd)` |
| `handleGuardCmd(DeviceControlGuard)` | `onGuard(platformId, DeviceControlGuard)` |
| `handleAlarmCmd(DeviceControlAlarm)` | `onAlarmReset(platformId, DeviceControlAlarm)` |
| `handleIFameCmd(DeviceControlIFame)` | `onIFrame(platformId, DeviceControlIFame)` |
| `handleDragZoomIn(DeviceControlDragIn)` | `onDragIn(platformId, DeviceControlDragIn)` |
| `handleDragZoomOut(DeviceControlDragOut)` | `onDragOut(platformId, DeviceControlDragOut)` |
| `handleHomePosition(DeviceControlPosition)` | `onHomePositionControl(platformId, DeviceControlPosition)` |
| `handleDeviceUpgrade(DeviceUpgradeControl)` | `onDeviceUpgrade(platformId, DeviceUpgradeControl)` |
| `handlePtzPreciseCtrl(DeviceControlPTZPrecise)` | `onPtzPrecise(platformId, DeviceControlPTZPrecise)` |
| `handleFormatSDCard(DeviceControlSDCardFormat)` | `onFormatSdCard(platformId, DeviceControlSDCardFormat)` |
| `handleTargetTrack(DeviceControlTargetTrack)` | `onTargetTrack(platformId, DeviceControlTargetTrack)` |

新增 `onKeepalive(platformId, KeepaliveControl)`（cmdType=Keepalive 独立 hook，原 v1.4.0 在 `KeepaliveMessageClientHandler` 内联消化）。

#### `SubscribeRequestHandler`（已删除）→ `SubscribeListener` 或框架内化

| v1.4.0 | v1.5.0 |
|---|---|
| `putSubscribe(userId, SubscribeInfo)` | **删除**：协议层内化为 `SubscribeRegistry.put()`，业务方无感知 |
| `getDeviceSubscribe(DeviceQuery)` | **删除**：业务方主动催发 NOTIFY 即可（直接调 `ClientCommandSender.sendCatalogCommand` 等） |
| 新增 catalog 订阅 hook | `SubscribeListener.onCatalogSubscribe(platformId, expires, DeviceQuery)` |
| 新增 alarm 订阅 hook | `SubscribeListener.onAlarmSubscribe(platformId, expires, DeviceAlarmQuery)` |
| 新增移动位置订阅 hook | `SubscribeListener.onMobilePositionSubscribe(platformId, expires, DeviceMobileQuery)` |

#### 旧 client 事件（已删除）→ listener 方法

| v1.4.0 `@EventListener` 方法 | v1.5.0 listener 方法 |
|---|---|
| `ClientPtzPositionQueryEvent` | `QueryListener.onPtzPositionQuery(platformId, PTZPositionQuery)` |
| `ClientSdCardStatusQueryEvent` | `QueryListener.onSdCardStatusQuery(platformId, SDCardStatusQuery)` |
| `ClientHomePositionQueryEvent` | `QueryListener.onHomePositionQuery(platformId, HomePositionQuery)` |
| `ClientCruiseTrackListQueryEvent` | `QueryListener.onCruiseTrackListQuery(platformId, CruiseTrackListQuery)` |
| `ClientCruiseTrackQueryEvent` | `QueryListener.onCruiseTrackQuery(platformId, CruiseTrackQuery)` |
| `ClientSnapShotConfigEvent` | `ConfigListener.onSnapShotConfig(platformId, SnapShotConfig)` |
| `ClientOsdConfigEvent` | `ConfigListener.onOsdConfig(platformId, OsdConfig)` |
| `ClientAlarmReportConfigEvent` | `ConfigListener.onAlarmReportConfig(platformId, AlarmReportConfig)` |
| `ClientVideoAlarmRecordConfigEvent` | `ConfigListener.onVideoAlarmRecordConfig(platformId, VideoAlarmRecordConfig)` |
| `ClientAlarmSubscribeEvent` | `SubscribeListener.onAlarmSubscribe(platformId, expires, DeviceAlarmQuery)` |

### 1.3 跨切监听（不打扰业务）

业务接口和协议事件**正交**，跨切层（metrics / audit / tracing）继续监听 L1 协议事件：

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

---

## 二、Server 端迁移（平台侧）

### 2.1 加性策略

server 端 32 个 `Device*Event` 全部**保留**，原有 `@EventListener public void onXxx(DeviceXxxEvent)`
代码继续工作。新增的 4 个 listener 接口是**可选**的一站式入口。

业务方可二选一：

```java
// 选项 A：继续用 typed event（旧代码无需改动）
@EventListener
public void onCatalog(DeviceCatalogEvent e) {
    String deviceId = e.getDeviceId();
    DeviceResponse catalog = e.getCatalog();
    ...
}

// 选项 B：用新 listener 接口（与 client 端形态对齐）
@Component
public class MyServerImpl extends ServerGb28181Adapter {
    @Override
    public void onCatalogResponse(String deviceId, String sn, DeviceResponse catalog) {
        ...
    }
}
```

### 2.2 server 端 listener 接口对照表

| 原 typed event | 对应 listener 方法 |
|---|---|
| `DeviceCatalogEvent` | `DeviceResponseListener.onCatalogResponse(deviceId, sn, catalog)` |
| `DeviceInfoEvent` | `DeviceResponseListener.onDeviceInfoResponse(deviceId, sn, info)` |
| `DeviceInfoErrorEvent` | `DeviceResponseListener.onDeviceInfoError(deviceId, reason)` |
| `DeviceInfoRequestEvent` | `DeviceResponseListener.onDeviceInfoRequest(deviceId, content)` |
| `DeviceStatusEvent` | `DeviceResponseListener.onDeviceStatusResponse(deviceId, sn, status)` |
| `DeviceRecordEvent` | `DeviceResponseListener.onRecordInfoResponse(deviceId, sn, record)` |
| `DevicePtzPositionEvent` | `DeviceResponseListener.onPtzPositionResponse(deviceId, response)` |
| `DeviceSdCardStatusEvent` | `DeviceResponseListener.onSdCardStatusResponse(deviceId, response)` |
| `DeviceHomePositionEvent` | `DeviceResponseListener.onHomePositionResponse(deviceId, response)` |
| `DeviceCruiseTrackEvent` (LIST) | `DeviceResponseListener.onCruiseTrackListResponse(deviceId, response)` |
| `DeviceCruiseTrackEvent` (SINGLE) | `DeviceResponseListener.onCruiseTrackResponse(deviceId, response)` |
| `DeviceConfigEvent` | `DeviceResponseListener.onConfigResponse(deviceId, sn, response)` |
| `DeviceSubscribeResponseEvent` | `DeviceResponseListener.onSubscribeResponse(deviceId, callId, statusCode)` |
| `DeviceNotifyUpdateEvent` | `DeviceResponseListener.onNotifyUpdate(deviceId, notify)` |
| `DeviceAlarmEvent` | `DeviceNotifyListener.onAlarmNotify(deviceId, notify)` |
| `DeviceKeepaliveEvent` | `DeviceNotifyListener.onKeepalive(deviceId, notify)` |
| `DeviceMediaStatusEvent` | `DeviceNotifyListener.onMediaStatus(deviceId, notify)` |
| `DeviceMobilePositionEvent` | `DeviceNotifyListener.onMobilePositionNotify(deviceId, notify)` |
| `DeviceUpgradeResultEvent` | `DeviceNotifyListener.onUpgradeResult(deviceId, notify)` |
| `DeviceSnapShotFinishedEvent` | `DeviceNotifyListener.onSnapShotFinished(deviceId, notify)` |
| `DeviceRegisterEvent` | `DeviceLifecycleListener.onDeviceRegister(deviceId, registerInfo)` |
| `DeviceRegisterChallengeEvent` | `DeviceLifecycleListener.onRegisterChallenge(deviceId)` |
| `DeviceOnlineEvent` | `DeviceLifecycleListener.onDeviceOnline(deviceId, sipTransaction)` |
| `DeviceOfflineEvent` | `DeviceLifecycleListener.onDeviceOffline(deviceId, registerInfo, sipTransaction)` |
| `DeviceRemoteAddressEvent` | `DeviceLifecycleListener.onRemoteAddressChanged(deviceId, remoteAddressInfo)` |
| `DeviceInviteTryingEvent` | `DeviceSessionListener.onInviteTrying(deviceId, callId)` |
| `DeviceInviteOkEvent` | `DeviceSessionListener.onInviteOk(deviceId, callId)` |
| `DeviceInviteFailureEvent` | `DeviceSessionListener.onInviteFailure(deviceId, callId, statusCode)` |
| `DeviceAckEvent` | `DeviceSessionListener.onAck(deviceId, callId, statusCode)` |
| `DeviceByeEvent` | `DeviceSessionListener.onBye(deviceId)` |
| `DeviceByeErrorEvent` | `DeviceSessionListener.onByeError(deviceId, errorMessage)` |
| `ServerInviteEvent` | `DeviceSessionListener.onServerInvite(callId, fromUserId, toUserId, sdp, transactionContextKey)` |

---

## 三、故障排查清单

### 3.1 业务方注册了 QueryListener 但所有 query 走默认空响应

**症状**：日志中看到一次性 WARN：
```
WARN ClientListenerAdapter — 收到 ClientQueryEvent 但未找到唯一的 QueryListener bean
```

**原因检查**：
1. 业务方 listener 类是否标注 `@Component` / `@Service` / `@Bean`？
2. 业务方 listener 类的包是否落在 `@SpringBootApplication.scanBasePackages` 路径之下？
3. 业务方 listener 类是否被 `@ConditionalOnProperty` 等条件过滤掉？
4. 注册数量 ≥2 个 `QueryListener` 时，Spring 启动期 `getIfUnique()` 返回 null（多实例 fail-fast 设计）—— 用 `@Primary` 或合并到单 bean

### 3.2 Adapter 强转 ToDevice 失败

**症状**：日志中看到：
```
WARN ClientListenerAdapter — ClientDeviceSupplier.getDevice(xxx) 返回 null，无法回包
```

**原因**：业务方覆写了 `ClientDeviceSupplier.getDevice(sipId)` 但返回了 null 或非 `ToDevice` 类型。

**修复**：确认 supplier 的 `getDevice(sipId)` 返回 `ToDevice` 子类型，且 sipId 在缓存中存在。

### 3.3 Subscribe listener 被调多次

**原因**：UDP 重传 + 协议层不去重，业务方需自行幂等（按 callId 或 deviceId+expires）。

---

## 四、迁移建议节奏

1. sip-proxy v1.5.0-RC1 发布
2. 业务侧（如 voglander）独立分支拉新依赖，**先**让编译通过：
   - 删除旧 `MessageRequestHandler` impl，改 `implements QueryListener`
   - 删除旧 `DeviceControlRequestHandler` impl，改 `implements ControlListener`
   - 删除 `subscribeRequestHandler.putSubscribe` 调用（协议层已内化）
   - 把 10 个 `@EventListener Client*Event` 散点统一到 listener 接口方法
3. 跑现有集成测试，验证 listener 收到事件
4. 验证完合并到业务侧主干，sip-proxy 正式发版 v1.5.0
5. 删除 v1.4.0 的兼容代码（如有）

业务侧改动量预估：voglander 大约 **20-30 个类**受影响，集中在 `voglander-integration/sip/` 模块。

---

## 五、不在本次迁移范围内

- ✗ Reactive 化（Mono/Flux 返回值），留作 v2.0+
- ✗ 跨进程 listener 分发（仍是同进程 Spring 事件）
- ✗ Listener 优先级 / 并发执行（沿用 Spring `@EventListener` 默认行为，需要时业务方加 `@Order` / `@Async`）
- ✗ Query 类继承统一（DeviceRecordQuery 等改 extends DeviceBase），v2.0 议题
- ✗ Java 版本升级到 21（如升级，Adapter 可重构为 sealed switch pattern，对外接口不变）
- ✗ Server 端 32 个 typed event 删除（保护现有业务侧兼容性，与设计文档 §3.4 偏差，记录在 CHANGELOG）

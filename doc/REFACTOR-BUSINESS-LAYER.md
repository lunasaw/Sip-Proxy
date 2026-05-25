# 业务接入层重构方案

> 目标版本：1.4.0 | 状态：待实施

## 背景与动机

当前业务接入层存在四个核心问题：

1. **`ServerMessageProcessorHandler` 是大接口**：业务方必须实现 10 个方法（5 个强制），大多数是空实现，违反 ISP 原则，随协议功能增加持续膨胀。`ServerNotifyProcessorHandler` 存在同样问题。
2. **回调缺少关联上下文**：`updateDeviceRecord(String userId, DeviceRecord)` 等回调不携带 `SN`，业务层无法将异步响应与之前发出的查询请求对应，需自己维护状态机。
3. **`ServerCommandSender` 是伪 Bean**：虽标注 `@Component`，但通过 `ApplicationContextAware` + 静态 `INSTANCE` 暴露静态 API，无法 Mock，业务层单元测试困难；且调用方需自己构造 `FromDevice`/`ToDevice`，暴露 SIP 协议细节。
4. **`ToDevice` 是万能包**：将所有消息类型的上下文（寻址、对话状态、媒体协商、订阅参数）全部塞入一个对象，业务方构造时不知道该填哪些字段，不同命令所需字段完全不同却共用同一模型。

---

## 设计决策

### 1. 用 Spring 事件替代大接口

不拆成多个小接口，而是改用 Spring `ApplicationEventPublisher`。

**理由**：拆成 7 个监听器接口，业务方要注册 7 个 Bean，接入代码反而更多。Spring 事件是框架内置机制，业务方只需 `@EventListener` 按需监听，零强制实现，且天然支持多个监听者。

框架内部将原来的 `handler.keepLiveDevice(notify)` 改为 `publishEvent(new DeviceKeepaliveEvent(...))`，`ServerMessageProcessorHandler` 和 `ServerNotifyProcessorHandler` 整体废弃。

### 2. 事件对象携带 SN

所有异步响应事件对象直接包含 `sn` 字段，业务层通过事件对象获取，无需自维护状态机。

### 3. `ServerCommandSender` 真正去静态化，引入 `DeviceSessionCache`

去掉静态方法和 `INSTANCE` hack，改为纯实例 Bean。

`ToDevice` 的寻址信息（ip/port/transport）来自设备注册缓存。框架不知道业务方用什么缓存（Redis/内存/数据库），因此定义 `DeviceSessionCache` 接口由业务方实现，框架注入后内部构造 `ToDevice`，业务方只传 `deviceId`。

### 4. `ToDevice` 瘦身，命令参数显式化

**现状**：`ToDevice` 混合了三类不同生命周期的数据：

| 字段 | 类型 | 实际用途 |
|---|---|---|
| `userId` / `ip` / `port` / `transport` | 寻址信息 | 所有命令必须，来自注册缓存 |
| `toTag` / `callId` | 对话状态 | 仅 BYE/ACK 续接对话时必须 |
| `streamMode` / `subject` | 媒体协商 | 仅 INVITE 必须 |
| `expires` / `eventType` / `eventId` | 订阅参数 | 仅 SUBSCRIBE 必须 |
| `localIp` | 无实际使用 | 删除 |

**重构后**：`ToDevice` 只保留寻址信息（`userId/ip/port/transport/toTag`），其余字段从 `ToDevice` 移出，改为各命令方法的显式参数。`DeviceSessionCache.getToDevice()` 返回的就是这个精简模型，业务方实现时只需填入注册时缓存的 ip/port。

```java
// 重构前：业务方不知道 toDevice 里该填什么
ToDevice to = new ToDevice();
to.setUserId(deviceId);
to.setIp(ip); to.setPort(port);
to.setStreamMode("TCP_PASSIVE");  // 为什么要填这个？
ServerCommandSender.deviceInvitePlay(from, to, sdpIp, mediaPort);

// 重构后：所有参数显式，意图清晰
commandSender.deviceInvitePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.TCP_PASSIVE);
```

---

## 新接口层次结构

```
gb28181-server
└── transmit/
    ├── event/                          新增：事件类包
    │   ├── DeviceKeepaliveEvent        心跳事件
    │   ├── DeviceAlarmEvent            报警事件
    │   ├── DeviceCatalogEvent          目录查询响应（含 sn）
    │   ├── DeviceRecordEvent           录像检索响应（含 sn）
    │   ├── DeviceInfoEvent             设备信息响应（含 sn）
    │   ├── DeviceStatusEvent           设备状态响应（含 sn）
    │   ├── DeviceConfigEvent           配置下载响应（含 sn）
    │   ├── DeviceMediaStatusEvent      媒体状态通知
    │   ├── DeviceMobilePositionEvent   移动位置通知
    │   ├── DeviceRemoteAddressEvent    设备地址更新
    │   └── DeviceNotifyUpdateEvent     其他 NOTIFY 通知
    └── cmd/
        ├── ServerCommandSender         去掉静态方法，纯 @Component
        └── DeviceSessionCache          新增：设备会话缓存接口（业务方实现）
```

旧接口 `ServerMessageProcessorHandler`、`ServerNotifyProcessorHandler` 标记 `@Deprecated`，`DefaultServerMessageProcessorHandler`、`DefaultServerNotifyProcessorHandler` 内部改为 `publishEvent`，保证向后兼容过渡期。

---

## 接口与事件定义

### 事件基类

```java
public abstract class DeviceEvent extends ApplicationEvent {
    private final String deviceId;
    protected DeviceEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
    }
    public String getDeviceId() { return deviceId; }
}
```

### 事件类示例

```java
// 心跳（无 sn，推送型）
public class DeviceKeepaliveEvent extends DeviceEvent {
    private final DeviceKeepLiveNotify notify;
}

// 目录响应（有 sn，请求-响应型）
public class DeviceCatalogEvent extends DeviceEvent {
    private final int sn;
    private final DeviceResponse catalog;
}

// 录像响应（有 sn）
public class DeviceRecordEvent extends DeviceEvent {
    private final int sn;
    private final DeviceRecord record;
}

// 设备信息响应（有 sn）
public class DeviceInfoEvent extends DeviceEvent {
    private final int sn;
    private final DeviceInfo info;
}
```

### 框架内部分发（替换 DefaultServerMessageProcessorHandler）

```java
@Component
@ConditionalOnMissingBean(ServerMessageProcessorHandler.class)
public class EventPublishingMessageHandler implements ServerMessageProcessorHandler {

    @Autowired private ApplicationEventPublisher publisher;

    @Override
    public void keepLiveDevice(DeviceKeepLiveNotify notify) {
        publisher.publishEvent(new DeviceKeepaliveEvent(this, notify.getDeviceID(), notify));
    }

    @Override
    public void updateDeviceResponse(String userId, DeviceResponse response) {
        publisher.publishEvent(new DeviceCatalogEvent(this, userId, response.getSn(), response));
    }

    @Override
    public void updateDeviceRecord(String userId, DeviceRecord record) {
        publisher.publishEvent(new DeviceRecordEvent(this, userId, record.getSn(), record));
    }

    // ... 其余方法同理
}
```

### DeviceSessionCache 接口

```java
// gb28181-server 定义，业务方实现
public interface DeviceSessionCache {
    /**
     * 根据设备 ID 获取会话信息（ip、port、transport 等）
     * 通常从设备注册时缓存的数据中取
     */
    ToDevice getToDevice(String deviceId);
}
```

### ServerCommandSender 改造

```java
// 改造前（静态调用，业务方需自己构造 FromDevice/ToDevice，且不清楚 ToDevice 该填哪些字段）
ServerCommandSender.deviceCatalogQuery(fromDevice, toDevice);
ServerCommandSender.deviceInvitePlay(fromDevice, toDevice, sdpIp, mediaPort); // toDevice 里还藏着 streamMode

// 改造后（注入调用，参数全部显式）
@Autowired private ServerCommandSender commandSender;
commandSender.deviceCatalogQuery(deviceId);
commandSender.deviceInvitePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.TCP_PASSIVE);
```

内部实现：

```java
@Component
public class ServerCommandSender {

    @Autowired private CommandStrategyFactory factory;
    @Autowired private ServerDeviceSupplier deviceSupplier;
    @Autowired private DeviceSessionCache sessionCache;

    // MESSAGE 类命令：只需 deviceId
    public String deviceCatalogQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.CATALOG.getType(), sn(), deviceId));
    }

    public String deviceInfoQuery(String deviceId) {
        return send("MESSAGE", deviceId,
            new DeviceQuery(CmdTypeEnum.DEVICE_INFO.getType(), sn(), deviceId));
    }

    // INVITE：媒体协商参数显式传入，不再藏在 ToDevice 里
    public String deviceInvitePlay(String deviceId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        ToDevice to = sessionCache.getToDevice(deviceId);
        to.setStreamMode(streamMode.name());  // 仅在此处设置，不由业务方构造
        InviteRequest req = new InviteRequest(deviceId, streamMode, sdpIp, mediaPort);
        FromDevice from = deviceSupplier.getServerFromDevice();
        return factory.getStrategy("server", "INVITE")
            .execute(CommandContext.builder()
                .role("server").commandType("INVITE")
                .fromDevice(from).toDevice(to).content(req.getContent()).build());
    }

    // SUBSCRIBE：订阅参数显式传入
    public String deviceCatalogSubscribe(String deviceId, Integer expires, String eventType) {
        ToDevice to = sessionCache.getToDevice(deviceId);
        to.setExpires(expires);
        to.setEventType(eventType);
        // ...
    }

    // BYE/ACK：对话状态参数显式传入
    public String deviceBye(String deviceId, String callId) {
        ToDevice to = sessionCache.getToDevice(deviceId);
        to.setCallId(callId);
        // ...
    }

    private String send(String commandType, String deviceId, Object body) {
        FromDevice from = deviceSupplier.getServerFromDevice();
        ToDevice to = sessionCache.getToDevice(deviceId);
        return factory.getStrategy("server", commandType)
            .execute(CommandContext.builder()
                .role("server").commandType(commandType)
                .fromDevice(from).toDevice(to).body(body).build());
    }

    private static String sn() { return RandomStrUtil.getValidationCode(); }

    // 旧静态方法标记 @Deprecated，过渡期保留
    @Deprecated
    public static String deviceCatalogQuery(FromDevice from, ToDevice to) { ... }
}
```

---

## 数据模型变更

### ToDevice 瘦身（sip-common）

删除 `localIp` 字段（无实际使用）。`streamMode`、`subject`、`expires`、`eventType`、`eventId` 保留在 `ToDevice` 中作为框架内部传递载体，但**不再由业务方设置**，改由 `ServerCommandSender` 各方法内部按需填充。

`DeviceSessionCache.getToDevice()` 的契约：只需返回含 `userId/ip/port/transport` 的最小集合，`toTag` 在对话续接场景由框架从 `SipTransactionContext` 自动填充。

### 响应实体 sn 字段（gb28181-common）

`DeviceRecord`、`DeviceInfo`、`DeviceStatus`、`DeviceResponse`、`DeviceConfigResponse` 确认 `sn` 字段已从 XML 正确映射：

```java
// 若缺失，补充：
@JacksonXmlProperty(localName = "SN")
private Integer sn;
```

---

## 迁移路径

| 阶段 | 内容 | 影响范围 |
|---|---|---|
| 1 | 新增所有事件类（纯新增） | 无破坏 |
| 2 | 新增 `DeviceSessionCache` 接口 | 无破坏 |
| 3 | 新增 `EventPublishingMessageHandler`，替换 `DefaultServerMessageProcessorHandler` 的 `@ConditionalOnMissingBean` | 无破坏，旧实现仍可覆盖 |
| 4 | 检查并补全实体 `sn` 字段 JAXB 映射 | `gb28181-common` |
| 5 | `ToDevice` 删除 `localIp`；`ServerCommandSender` 增加实例方法，各命令参数显式化，旧静态方法标记 `@Deprecated` | `sip-common` 小改，向后兼容 |
| 6 | 更新 `gb28181-test` 示例，演示新接入方式 | 仅测试模块 |
| 7 | 下一个大版本移除 `@Deprecated` 方法和旧大接口 | Breaking change，提前公告 |

---

## 业务方接入示例（改造后）

```java
@Component
public class SipEventHandler {

    // 只写关心的事件，其余不写，无需空实现
    @EventListener
    public void onKeepalive(DeviceKeepaliveEvent event) {
        deviceService.updateOnlineTime(event.getDeviceId());
    }

    @EventListener
    public void onCatalog(DeviceCatalogEvent event) {
        // sn 直接从事件取，可关联之前发出的查询请求
        channelService.syncChannels(event.getDeviceId(), event.getSn(), event.getCatalog());
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent event) {
        alarmService.handleAlarm(event.getDeviceId(), event.getNotify());
    }
}

// 实现设备会话缓存（框架要求，业务方按自己的缓存实现）
@Component
public class RedisDeviceSessionCache implements DeviceSessionCache {
    @Override
    public ToDevice getToDevice(String deviceId) {
        // 从 Redis / 内存 / 数据库取注册时缓存的设备地址信息
        DeviceInfo info = redisTemplate.opsForValue().get("device:" + deviceId);
        return ToDevice.getInstance(deviceId, info.getIp(), info.getPort());
    }
}
```

对比改造前，业务方需实现 `ServerMessageProcessorHandler` 的全部 10 个方法（5 个强制空实现）。

---

## 不在本次范围内

- `ServerRegisterProcessorHandler`：注册逻辑有状态（鉴权、401 challenge），不适合事件模型，保持现有接口。
- `ServerInviteRequestHandler`：媒体会话逻辑独立，不变。
- `ServerDeviceSupplier`：职责清晰，不变。
- 命令策略层（`CommandStrategyFactory` + `AbstractServerCommandStrategy`）：已是策略模式，设计合理，不变。
- 多响应消息聚合（附录 N）：独立问题，另立方案。
- 媒体流保活（附录 M）：独立问题，另立方案。

# 大版本重构：移除 Handler 接口层，Handler 直接发布 Spring 事件

> 目标版本：2.0.0 | 状态：待实施

## 目标

彻底移除 `ServerMessageProcessorHandler` 和 `ServerNotifyProcessorHandler` 接口，让各 Handler 子类直接注入 `ApplicationEventPublisher` 发布事件，消除中间层。

---

## 当前调用链

```
SIP Message
  → ServerMessageRequestProcessor          注入 ServerMessageProcessorHandler
  → doMessageHandForEvt()
  → XxxMessageHandler.handForEvt()         注入 ServerMessageProcessorHandler，调用其方法
  → EventPublishingMessageHandler           实现接口，将调用转为 publishEvent()
  → Spring 事件
```

**问题**：`EventPublishingMessageHandler` 是一个纯转发层，存在的唯一原因是兼容旧接口。移除后调用链变为：

```
SIP Message
  → ServerMessageRequestProcessor
  → doMessageHandForEvt()
  → XxxMessageHandler.handForEvt()         直接 publishEvent()
  → Spring 事件
```

---

## 需要修改的文件

### 删除（共 4 个文件）

| 文件 | 原因 |
|---|---|
| `ServerMessageProcessorHandler.java` | 接口本身 |
| `ServerNotifyProcessorHandler.java` | 接口本身 |
| `EventPublishingMessageHandler.java` | 纯转发层，不再需要 |
| `EventPublishingNotifyHandler.java` | 纯转发层，不再需要 |

### 修改 MessageServerHandlerAbstract

移除 `serverMessageProcessorHandler` 字段，改为注入 `ApplicationEventPublisher`：

```java
// 修改前
@Data
@Component
public abstract class MessageServerHandlerAbstract extends MessageHandlerAbstract implements InitializingBean {
    public ServerMessageProcessorHandler serverMessageProcessorHandler;
    public ServerDeviceSupplier serverDeviceSupplier;

    public MessageServerHandlerAbstract(@Lazy ServerMessageProcessorHandler serverMessageProcessorHandler,
                                         ServerDeviceSupplier serverDeviceSupplier) {
        this.serverMessageProcessorHandler = serverMessageProcessorHandler;
        this.serverDeviceSupplier = serverDeviceSupplier;
    }
}

// 修改后
@Data
@Component
public abstract class MessageServerHandlerAbstract extends MessageHandlerAbstract implements InitializingBean {
    public ApplicationEventPublisher publisher;
    public ServerDeviceSupplier serverDeviceSupplier;

    public MessageServerHandlerAbstract(ApplicationEventPublisher publisher,
                                         ServerDeviceSupplier serverDeviceSupplier) {
        this.publisher = publisher;
        this.serverDeviceSupplier = serverDeviceSupplier;
    }
}
```

### 修改 NotifyServerHandlerAbstract

同上，移除 `serverNotifyProcessorHandler`，改为 `ApplicationEventPublisher`：

```java
// 修改后
public abstract class NotifyServerHandlerAbstract extends ... {
    public ApplicationEventPublisher publisher;
    public ServerDeviceSupplier serverDeviceSupplier;

    public NotifyServerHandlerAbstract(ApplicationEventPublisher publisher,
                                        ServerDeviceSupplier serverDeviceSupplier) {
        this.publisher = publisher;
        this.serverDeviceSupplier = serverDeviceSupplier;
    }
}
```

### 修改 ServerMessageRequestProcessor

当前 `process()` 通过接口做了三件事：权限校验、错误回调、获取 fromDevice。删除接口后需要逐一替代：

| 当前调用 | 替代方案 |
|---|---|
| `serverMessageProcessorHandler.validateDevicePermission(evt)` | `serverDeviceSupplier.checkDevice(evt)` — 已有等价实现 |
| `serverMessageProcessorHandler.handleMessageError(evt, msg)` | 直接 `log.warn()` 即可，错误不需要回调给业务方 |
| `serverDeviceSupplier.getServerFromDevice()` | 不变 |

```java
// 修改后的 process()
@Override
public void process(RequestEvent evt, ServerTransaction serverTransaction) {
    try {
        if (!serverDeviceSupplier.checkDevice(evt)) {
            log.warn("MESSAGE 请求权限验证失败");
            return;
        }
        FromDevice fromDevice = serverDeviceSupplier.getServerFromDevice();
        if (fromDevice == null) {
            log.warn("MESSAGE 请求无法获取发送设备信息");
            return;
        }
        doMessageHandForEvt(evt, fromDevice, serverTransaction);
    } catch (Exception e) {
        log.error("处理 MESSAGE 请求异常", e);
    }
}
```

### 修改 ServerNotifyRequestProcessor

同上，`validateDevicePermission` → `deviceSupplier.checkDevice(evt)`，`handleNotifyError` → `log.warn()`。

### 修改各 Handler 子类（共 9 个）

每个子类的构造函数参数从 `ServerMessageProcessorHandler` 改为 `ApplicationEventPublisher`，`handForEvt()` 中的调用改为直接 `publisher.publishEvent()`。

| 子类 | 当前调用 | 改为发布事件 |
|---|---|---|
| `KeepaliveNotifyMessageHandler` | `serverMessageProcessorHandler.keepLiveDevice(notify)` | `publisher.publishEvent(new DeviceKeepaliveEvent(...))` |
| | `serverMessageProcessorHandler.updateRemoteAddress(userId, info)` | `publisher.publishEvent(new DeviceRemoteAddressEvent(...))` |
| `AlarmNotifyMessageHandler` | `serverMessageProcessorHandler.updateDeviceAlarm(notify)` | `publisher.publishEvent(new DeviceAlarmEvent(...))` |
| `MediaStatusNotifyMessageHandler` | `serverMessageProcessorHandler.updateMediaStatus(notify)` | `publisher.publishEvent(new DeviceMediaStatusEvent(...))` |
| `ResponseCatalogMessageHandler` | `serverMessageProcessorHandler.updateDeviceResponse(userId, response)` | `publisher.publishEvent(new DeviceCatalogEvent(...))` |
| `DeviceInfoMessageServerHandler` | `serverMessageProcessorHandler.updateDeviceInfo(userId, info)` | `publisher.publishEvent(new DeviceInfoEvent(...))` |
| `DeviceStatusMessageServerHandler` | `serverMessageProcessorHandler.updateDeviceStatus(userId, status)` | `publisher.publishEvent(new DeviceStatusEvent(...))` |
| `RecordInfoMessageHandler` | `serverMessageProcessorHandler.updateDeviceRecord(userId, record)` | `publisher.publishEvent(new DeviceRecordEvent(...))` |
| `DeviceConfigMessageServerHandler` | `serverMessageProcessorHandler.updateDeviceConfig(userId, config)` | `publisher.publishEvent(new DeviceConfigEvent(...))` |
| `CatalogNotifyHandler` | `serverNotifyProcessorHandler.deviceNotifyUpdate(userId, notify)` | `publisher.publishEvent(new DeviceNotifyUpdateEvent(...))` |

---

## 实施顺序

1. 修改 `MessageServerHandlerAbstract`，字段从接口改为 `ApplicationEventPublisher`
2. 修改 `NotifyServerHandlerAbstract`，同上
3. 修改 9 个 Handler 子类的构造函数和 `handForEvt()` 方法
4. 修改 `ServerMessageRequestProcessor`，移除接口字段
5. 修改 `ServerNotifyRequestProcessor`，移除接口字段
6. 删除 4 个文件：两个接口 + 两个 EventPublishing 实现
7. 编译验证
8. 运行 `EventPublishingMessageHandlerTest`（此时已无意义，一并删除）并补充新的 Handler 子类测试

---

## 对业务方的影响

**零影响**。业务方已经在用 `@EventListener` 监听事件，事件类（`DeviceKeepaliveEvent` 等）不变，`DeviceSessionCache` 接口不变，`ServerCommandSender` 不变。

唯一的 Breaking change 是：业务方如果之前自己实现了 `ServerMessageProcessorHandler` 接口（覆盖默认行为），该实现将失效，需要改为 `@EventListener`。

---

## 不在本次范围内

- `ServerRegisterProcessorHandler`：注册逻辑有状态（鉴权、401 challenge），保持现有接口
- `ServerCommandSender` 静态方法的最终清理：另立方案
- `gb28181-client` 侧的对称重构：另立方案

# Handler 全量重构：统一为 Spring Event 模式

> 状态：待实施 | 优先级：高 | 关联：BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md

## 问题

当前系统存在两套并行的业务扩展机制，造成架构不一致：

| 模式 | 使用场景 | 业务方接入方式 |
|---|---|---|
| Spring Event | MESSAGE、NOTIFY 系列 | `@EventListener` |
| 接口回调（ProcessorHandler） | REGISTER、BYE、INVITE、ACK、CANCEL | 实现接口 + `@Primary` |

`BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md` 已完成 MESSAGE/NOTIFY 的重构。本文覆盖**剩余所有接口回调**的统一化。

---

## 现状盘点

### Server 侧——仍在用接口回调的 Handler（7个接口）

| 接口 | 触发时机 | 关键方法 | 是否有状态/协议逻辑 |
|---|---|---|---|
| `ServerRegisterProcessorHandler` | 收到 REGISTER | `validatePassword`、`handleDeviceOnline`、`handleDeviceOffline`、`handleRegisterInfoUpdate` | **有**：`validatePassword` 在协议层被同步调用，结果决定返回 200/403 |
| `ServerInviteRequestHandler` | 收到 INVITE | `inviteSession`、`getInviteResponse` | **有**：`getInviteResponse` 返回值直接写入 SDP 响应 |
| `ServerByeProcessorHandler` | 收到 BYE | `handleByeRequest`、`validateDevicePermission` | 有：`validateDevicePermission` 决定是否处理 |
| `ServerInfoProcessorHandler` | 收到 INFO | `handleInfoRequest` | 无 |
| `ServerAckProcessorHandler` | 收到 ACK 响应 | `handleAckResponse` | 无 |
| `InviteResponseProcessorHandler` | 收到 INVITE 响应 | `handleOkResponse`、`handleFailureResponse`、`processOkResponse` | 有：`processOkResponse` 含协议层 ACK 发送 |
| `SubscribeResponseProcessorHandler` | 收到 SUBSCRIBE 响应 | `handleSubscribeResponse` | 无 |

### Client 侧——仍在用接口回调的 Handler（6个接口）

| 接口 | 触发时机 | 关键方法 | 是否有状态/协议逻辑 |
|---|---|---|---|
| `RegisterProcessorHandler` | 收到 REGISTER 响应 | `registerSuccess`、`handleUnauthorized`、`handleRegisterFailure`、`getExpire` | **有**：`getExpire` 在协议层被同步调用；`handleUnauthorized` 后框架自动重发 |
| `ClientByeProcessorHandler` | 收到 BYE 响应 | `handleByeResponse`、`closeStream` | 无 |
| `ClientAckProcessorHandler` | 收到 ACK 响应 | `handleAckResponse` | 无 |
| `CancelProcessorHandler` | 收到 CANCEL 响应 | `handleCancelResponse` | 无 |
| `InviteRequestHandler` | 收到 INVITE 请求 | `inviteSession`、`getInviteResponse` | **有**：`getInviteResponse` 返回值写入 SDP |
| `MessageRequestHandler` | 收到 MESSAGE 请求 | 20+ 方法（查询/控制/通知） | 有：多个方法有返回值，用于构造响应 |

---

## 重构原则

**能改为 Event 的全部改；不能改的保留接口但简化。**

判断标准：
- 方法有**返回值**且返回值被协议层使用 → 保留接口（同步调用无法替换为异步事件）
- 方法是**纯通知**（void，无副作用影响协议流程） → 改为 publishEvent

---

## 分类处理方案

### 类型 A：全部改为 Spring Event（接口可删除）

这些接口的所有方法都是 void 纯通知，不影响协议流程。

#### Server 侧

**`ServerAckProcessorHandler`** → 删除接口，`ServerAckResponseProcessor` 直接 publishEvent

```java
DeviceAckEvent(source, callId, statusCode)
```

**`SubscribeResponseProcessorHandler`** → 删除接口，`SubscribeResponseProcessor` 直接 publishEvent

```java
DeviceSubscribeResponseEvent(source, callId, statusCode)
```

#### Client 侧

**`ClientAckProcessorHandler`** → 删除接口，`ClientAckResponseProcessor` 直接 publishEvent

```java
ClientAckEvent(source, callId, statusCode)
```

**`CancelProcessorHandler`** → 删除接口，`CancelResponseProcessor` 直接 publishEvent

```java
ClientCancelEvent(source, callId, statusCode)
```

---

### 类型 B：拆分——纯通知方法改 Event，有返回值方法保留接口

#### `ServerByeProcessorHandler`（Server 侧）

> **修正**：原方案误将此接口归入类型A。`validateDevicePermission()` 有返回值且被 Processor 用于 if 判断，必须保留。

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `validateDevicePermission(userId, sipId, evt)` | 有返回值，协议层同步调用 | **保留在接口** |
| `handleByeRequest(userId, evt)` | void | 改为 `publishEvent(new DeviceByeEvent(...))` |
| `handleByeError(userId, errorMessage, evt)` | void | 改为 `publishEvent(new DeviceByeErrorEvent(...))` |

重构后接口精简为：

```java
public interface ServerByeProcessorHandler {
    default boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) { return true; }
}
```

#### `ServerInfoProcessorHandler`（Server 侧）

> **修正**：原方案误将此接口归入类型A。`validateDevicePermission()` 有返回值且被 Processor 用于 if 判断，必须保留。

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `validateDevicePermission(userId, sipId, evt)` | 有返回值，协议层同步调用 | **保留在接口** |
| `handleInfoRequest(userId, content, evt)` | void | 改为 `publishEvent(new DeviceInfoRequestEvent(...))` |
| `handleInfoError(userId, errorMessage, evt)` | void | 改为 `publishEvent(new DeviceInfoErrorEvent(...))` |

重构后接口精简为：

```java
public interface ServerInfoProcessorHandler {
    default boolean validateDevicePermission(String userId, String sipId, RequestEvent evt) { return true; }
}
```

#### `ClientByeProcessorHandler`（Client 侧）

> **修正**：`closeStream(callId)` 是非 default 方法（强制实现），删除接口会破坏约束。整个接口删除，两个方法均改为 Event。

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `handleByeResponse(callId, statusCode, evt)` | void default | 改为 `publishEvent(new ClientByeEvent(...))` |
| `closeStream(callId)` | 非 default void | 改为 `publishEvent(new ClientByeEvent(...))` 携带 callId，业务方在 `@EventListener` 中关流 |

删除接口，`ByeResponseProcessor` 直接 publishEvent `ClientByeEvent`。

#### `ServerRegisterProcessorHandler`（Server 侧）

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `validatePassword(userId, password, evt)` | 有返回值，协议层同步调用 | **保留在接口** |
| `getDeviceTransaction(userId)` | 有返回值，用于续订判断 | **保留在接口** |
| `getDeviceExpire(userId)` | 有返回值 | **保留在接口** |
| `handleUnauthorized(userId, evt)` | void | 改为 `publishEvent(new DeviceRegisterChallengeEvent(...))` |
| `handleRegisterInfoUpdate(userId, info, evt)` | void | 改为 `publishEvent(new DeviceRegisterEvent(...))` |
| `handleDeviceOnline(userId, transaction, evt)` | void | 改为 `publishEvent(new DeviceOnlineEvent(...))` |
| `handleDeviceOffline(userId, info, transaction, evt)` | void | 改为 `publishEvent(new DeviceOfflineEvent(...))` |

重构后接口精简为：

```java
public interface ServerRegisterProcessorHandler {
    boolean validatePassword(String userId, String password, RequestEvent evt);
    SipTransaction getDeviceTransaction(String userId);
    default Integer getDeviceExpire(String userId) { return 3600; }
}
```

`DefaultServerRegisterProcessorHandler` 保留，只实现这 3 个方法。

#### `RegisterProcessorHandler`（Client 侧）

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `getExpire(userId)` | 有返回值，协议层同步调用 | **保留在接口** |
| `registerSuccess(toUserId)` | 非 default void（原强制实现） | 改为 `publishEvent(new ClientRegisterSuccessEvent(...))` |
| `handleUnauthorized(evt, toUserId, callId)` | void | 改为 `publishEvent(new ClientRegisterChallengeEvent(...))` |
| `handleRegisterFailure(toUserId, statusCode)` | void | 改为 `publishEvent(new ClientRegisterFailureEvent(...))` |

重构后接口精简为：

```java
public interface RegisterProcessorHandler {
    default Integer getExpire(String userId) { return 3600; }
}
```

> **注意**：`registerSuccess` 原为非 default 方法，`TestClientRegisterHandler` 通过实现它触发 CountDownLatch。
> 改为 Event 后，测试类改为 `@EventListener(ClientRegisterSuccessEvent.class)` 监听，CountDownLatch 逻辑不变。

#### `ServerInviteRequestHandler` / `InviteRequestHandler`（Server + Client 侧）

`getInviteResponse` 有返回值且直接写入 SDP，**整个接口保留**，无法改为 Event。

但可以在调用完接口后额外 publishEvent 通知业务方：

```java
// ServerInviteRequestProcessor 处理完后追加：
publisher.publishEvent(new DeviceInviteEvent(this, callId, sessionDescription));
```

#### `InviteResponseProcessorHandler`（Server 侧）

| 方法 | 类型 | 处理方式 |
|---|---|---|
| `processOkResponse(evt, callId)` | 含协议层 ACK 发送 | **保留在接口** |
| `handleTryingResponse(evt, callId)` | void | 改为 Event |
| `handleOkResponse(evt, callId)` | void | 改为 Event |
| `handleFailureResponse(evt, callId, statusCode)` | void | 改为 Event |

#### `MessageRequestHandler`（Client 侧）

所有方法都有返回值（用于构造 SIP 响应），**整个接口保留**，无法改为 Event。

这是客户端作为"被查询方"时的同步响应接口，本质上是 RPC 而非事件通知。

---

### 类型 C：保持不变

| 接口 | 原因 |
|---|---|
| `ServerInviteRequestHandler` | `getInviteResponse` 返回值写入 SDP |
| `InviteRequestHandler` | 同上 |
| `MessageRequestHandler` | 全部方法有返回值，用于构造响应 |

---

## 新增 Spring Event 类汇总

### Server 侧新增（10个）

```
DeviceRegisterEvent          - 设备注册成功（含 RegisterInfo）
DeviceOnlineEvent            - 设备上线（含 SipTransaction）
DeviceOfflineEvent           - 设备下线（含 RegisterInfo、SipTransaction）
DeviceRegisterChallengeEvent - 收到注册请求但尚未认证（401 挑战前）
DeviceByeEvent               - 收到 BYE 请求（void 通知）
DeviceByeErrorEvent          - BYE 请求处理错误
DeviceInfoRequestEvent       - 收到 INFO 请求（void 通知）
DeviceInfoErrorEvent         - INFO 请求处理错误
DeviceAckEvent               - 收到 ACK 响应
DeviceSubscribeResponseEvent - 收到 SUBSCRIBE 响应
```

### Client 侧新增（5个）

```
ClientRegisterSuccessEvent   - 注册成功
ClientRegisterFailureEvent   - 注册失败
ClientRegisterChallengeEvent - 收到 401
ClientByeEvent               - 收到 BYE 响应（含 callId，业务方在此关流）
ClientCancelEvent            - 收到 CANCEL 响应
```

---

## 实施顺序

1. **新增 Event 类**（无破坏性，可先合入）
2. **Server 类型 A**：删除 `ServerAckProcessorHandler`、`SubscribeResponseProcessorHandler`，对应 Processor 改为 publishEvent
3. **Server 类型 B BYE/INFO**：精简 `ServerByeProcessorHandler`、`ServerInfoProcessorHandler`，Processor void 回调改为 publishEvent
4. **Server 类型 B REGISTER**：精简 `ServerRegisterProcessorHandler`，`DefaultServerRegisterProcessorHandler` 同步精简，`ServerRegisterRequestProcessor` void 回调改为 publishEvent
5. **Client 类型 A**：删除 `ClientAckProcessorHandler`、`CancelProcessorHandler`，对应 Processor 改为 publishEvent
6. **Client 类型 B BYE**：删除 `ClientByeProcessorHandler`，`ByeResponseProcessor` 改为 publishEvent
7. **Client 类型 B REGISTER**：精简 `RegisterProcessorHandler`，`ClientRegisterResponseProcessor` void 回调改为 publishEvent
8. **更新测试类**：`TestClientRegisterHandler` / `TestServerRegisterHandler` 改为 `@EventListener`，删除接口实现
9. **验证 `RegistrationFlowTest`**：确认新的 Event 流程通过

---

## 对业务方的影响

| 变更 | 影响 |
|---|---|
| 删除 `ServerAckProcessorHandler`、`SubscribeResponseProcessorHandler` | 业务方如果实现了这些接口，需改为 `@EventListener` |
| 删除 `ClientAckProcessorHandler`、`CancelProcessorHandler`、`ClientByeProcessorHandler` | 同上 |
| `ServerByeProcessorHandler` 精简（仅保留 `validateDevicePermission`） | 业务方如果覆盖了 void 方法，需改为 `@EventListener` |
| `ServerInfoProcessorHandler` 精简（仅保留 `validateDevicePermission`） | 同上 |
| `ServerRegisterProcessorHandler` 精简 | 业务方如果覆盖了 void 方法，需改为 `@EventListener` |
| `RegisterProcessorHandler` 精简（仅保留 `getExpire`） | 同上 |
| 新增 Event 类 | 无破坏性 |
| `MessageRequestHandler` 不变 | 无影响 |
| `ServerInviteRequestHandler` / `InviteRequestHandler` 不变 | 无影响 |

**最小化迁移路径**：业务方只需将原来实现接口的 void 方法，改写为对应 Event 的 `@EventListener` 方法，签名从 `(String userId, RequestEvent evt)` 变为 `(DeviceXxxEvent event)`。

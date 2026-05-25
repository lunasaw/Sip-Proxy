# INVITE 处理重构技术方案

> 版本：1.2 | 日期：2026-05-24
>
> ✅ **实施状态**：v1.2 已合入。本方案描述的协议层改造（INVITE 异步化、INFO 事件化、BYE 200 OK 修复、REGISTER 鉴权下沉到 `ServerDeviceSupplier.authenticate`、`SipTransactionRegistry.extendContext`、`*Handler` 接口全量删除）均已落地，集成测试 `RegistrationFlowTest` / `InvitePlayFlowTest` / `AlarmFlowTest` 通过。
>
> v1.1 → v1.2 变更：补充摘要鉴权下沉、`extendContext` 续期接口、INVITE 重传幂等约束、BYE 200 OK 协议合规修复、`ServerDeviceSupplier` 方法改名为 `authenticate` 避免与现有 `checkDevice(RequestEvent)` 语义冲突；新增 `ClientDeviceSupplier.checkDevice` 用于同 JVM 同时启用 client/server 时按 To-Header 隔离两侧 INVITE 处理。

---

## 一、背景与目标

### 当前问题

客户端 `InviteRequestProcessor` 和服务端 `ServerInviteRequestProcessor` 采用**同步回包**模式：

```
收到 INVITE → 调用 InviteRequestHandler.getInviteResponse() → 立即发 200 OK
```

这导致：
1. 业务方必须实现 `InviteRequestHandler` 接口，框架与业务强耦合
2. 无法在回包前做异步操作（如查询媒体服务器地址、申请流端口）
3. 与其他处理器（BYE、INFO、MESSAGE）的**事件总线模式**不一致

同时，`ByeRequestProcessorServer`、`ServerInfoRequestProcessor`、`ServerRegisterRequestProcessor` 仍依赖 `XxxProcessorHandler` 接口，也需要清理。

### 目标

统一所有入站请求处理器为**事件总线模式**：

```
收到请求 → 发 100 Trying（INVITE）或 200 OK（其他）→ 发布 Spring Event → 业务方 @EventListener 异步处理
```

删除所有 `XxxRequestHandler` / `XxxProcessorHandler` 接口及其默认实现。

---

## 二、现状分析

### 2.1 需要改造的处理器

| 模块 | 处理器 | 当前依赖接口 | 改造内容 |
|------|--------|------------|---------|
| client | `InviteRequestProcessor` | `InviteRequestHandler` | 改为发 100 Trying + `ClientInviteEvent` |
| client | `InfoRequestProcessor` | `InfoRequestHandler` | 改为发 200 OK + `ClientInfoEvent` |
| server | `ServerInviteRequestProcessor` | `ServerInviteRequestHandler` | 改为发 100 Trying + `ServerInviteEvent` |
| server | `ByeRequestProcessorServer` | `ServerByeProcessorHandler` | 删除 handler 依赖，直接发事件 |
| server | `ServerInfoRequestProcessor` | `ServerInfoProcessorHandler` | 删除 handler 依赖，直接发事件 |
| server | `ServerRegisterRequestProcessor` | `ServerRegisterProcessorHandler` | 删除 handler 依赖，直接发事件 |

### 2.2 已完成事件化的处理器（参考模式）

- `ByeRequestProcessorClient` → 发 200 OK + `ClientByeEvent` ✅
- `ServerMessageRequestProcessor` → 发 200 OK + 各类 `DeviceXxxEvent` ✅

### 2.3 INVITE 的特殊性

INVITE 需要**异步回包**（业务方需要时间准备 SDP），因此：
1. 收到 INVITE 后立即发 **100 Trying**（协议要求，防止对端重传）
2. 将 `RequestEvent` 存入 `SipTransactionRegistry`（按 callId 索引）
3. 发布事件，携带 `callId` 和 `transactionContextKey`
4. 业务方监听事件，准备好 SDP 后，通过 `callId` 取回 `RequestEvent`，调用 `ResponseCmd.sendResponse(200, sdp, evt)` 回包

---

## 三、详细设计

### 3.1 客户端 INVITE 改造

#### 3.1.1 修改 `ClientInviteEvent`

新增 `transactionContextKey` 字段，供业务方取回 `RequestEvent`：

```java
// 已修改：ClientInviteEvent 新增 transactionContextKey 字段
public class ClientInviteEvent extends ApplicationEvent {
    private final String callId;
    private final String userId;
    private final SdpSessionDescription sessionDescription;
    private final String transactionContextKey;  // 新增
}
```

#### 3.1.2 改造 `InviteRequestProcessor`

```java
@Override
public void process(RequestEvent evt) {
    SIPRequest request = (SIPRequest) evt.getRequest();
    String userId = SipUtils.getUserIdFromToHeader(request);
    String callId = SipUtils.getCallId(request);

    // 1. 立即发 100 Trying，防止对端重传
    ResponseCmd.sendResponse(Response.TRYING, evt);

    // 2. 存入事务注册表，供业务方异��回包
    ServerTransaction serverTransaction = evt.getServerTransaction();
    SipTransactionRegistry.TransactionContextInfo ctx =
        SipTransactionRegistry.createContext(evt, serverTransaction);

    // 3. 解析 SDP，发布事件
    GbSessionDescription sdp = (GbSessionDescription) SipUtils.parseSdp(new String(request.getRawContent()));
    publisher.publishEvent(new ClientInviteEvent(this, callId, userId, sdp, ctx.getContextKey()));
}
```

> **重传幂等约束**：UDP 下设备未收到 200 OK 时会按 SIP T1 指数退避重传 INVITE。框架不做去重——`SipTransactionRegistry` 按相同 contextKey 覆盖写入是安全的，但 `ClientInviteEvent` 会被重复 publish。业务方监听器**必须**按 `callId` 自行幂等（如已发 200 OK 则忽略后续事件，或对正在异步处理的 callId 加锁）。

#### 3.1.3 删除文件

- `InviteRequestHandler.java`
- `DefaultInviteRequestHandler.java`

#### 3.1.4 业务方使用方式

```java
@EventListener
public void onInvite(ClientInviteEvent event) {
    // 异步准备 SDP
    String sdp = buildSdpResponse(event.getSessionDescription());

    // 取回 RequestEvent，发 200 OK
    SipTransactionRegistry.TransactionContextInfo ctx =
        SipTransactionRegistry.getContext(event.getTransactionContextKey());
    if (ctx != null) {
        ContentTypeHeader ct = ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader();
        ResponseCmd.sendResponse(Response.OK, sdp, ct, ctx.getOriginalEvent());
    }
}
```

---

### 3.2 客户端 INFO 改造

#### 3.2.1 新增 `ClientInfoEvent`

```java
public class ClientInfoEvent extends ApplicationEvent {
    private final String userId;
    private final String content;
}
```

#### 3.2.2 改造 `InfoRequestProcessor`

```java
@Override
public void process(RequestEvent evt) {
    SIPRequest request = (SIPRequest) evt.getRequest();
    String userId = SipUtils.getUserIdFromToHeader(request);
    String content = new String(request.getRawContent());

    ResponseCmd.sendResponse(Response.OK, evt);  // INFO 同步回 200 OK
    publisher.publishEvent(new ClientInfoEvent(this, userId, content));
}
```

#### 3.2.3 删除文件

- `InfoRequestHandler.java`
- `DefaultClientInfoRequestHandler.java`

---

### 3.3 服务端 INVITE 改造

与客户端 INVITE 完全对称，新增 `ServerInviteEvent`（携带 `transactionContextKey`），改造 `ServerInviteRequestProcessor`，删除 `ServerInviteRequestHandler` 和 `DefaultServerInviteRequestHandler`。

---

### 3.4 服务端 BYE 改造

`ByeRequestProcessorServer` 当前已发事件，但仍依赖 `ServerByeProcessorHandler.validateDevicePermission()`。

该接口的默认实现永远返回 `true`，无实际价值。**同时存在协议合规 bug**：当前实现**根本没发 200 OK**（参见 [ByeRequestProcessorServer.java:37-58](../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/bye/ByeRequestProcessorServer.java#L37-L58)），违反 RFC 3261 §15.1.2。本次顺手修复。

改造方案：

```java
// 删除 serverByeProcessorHandler 依赖，先回 200 OK 再发事件
@Override
public void process(RequestEvent evt) {
    SIPRequest request = (SIPRequest) evt.getRequest();
    String userId = SipUtils.getUserIdFromFromHeader(request);
    ResponseCmd.sendResponse(Response.OK, evt);  // ⚠️ 修复：原代码漏发 200 OK
    publisher.publishEvent(new DeviceByeEvent(this, userId));
}
```

删除 `ServerByeProcessorHandler.java`。

---

### 3.5 服务端 INFO 改造

`ServerInfoRequestProcessor` 同上，删除 `ServerInfoProcessorHandler` 依赖：

```java
@Override
public void process(RequestEvent evt) {
    SIPRequest request = (SIPRequest) evt.getRequest();
    String userId = SipUtils.getUserIdFromFromHeader(request);
    String content = evt.getRequest().getRawContent() != null
        ? new String(evt.getRequest().getRawContent()) : "";
    ResponseCmd.sendResponse(Response.OK, evt);
    publisher.publishEvent(new DeviceInfoRequestEvent(this, userId, content));
}
```

删除 `ServerInfoProcessorHandler.java`。

---

### 3.6 服务端 REGISTER 改造

`ServerRegisterRequestProcessor` 依赖 `ServerRegisterProcessorHandler` 的两个方法：

| 方法 | 当前用途 | 改造方案 |
|------|---------|---------|
| `validatePassword(userId, password, evt)` | 密码验证 | 移入 `ServerDeviceSupplier.authenticate(userId, request)`，由框架完成摘要计算，业务方只提供明文密码 |
| `getDeviceTransaction(userId)` | 查询已有事务（续订判断） | 改为通过 `ServerDeviceSupplier.getDevice(userId)` 间接获取（业务方在 `Device` 上扩展 lastTransaction 字段，或干脆放弃续订优化每次走完整流程） |

#### 3.6.1 摘要鉴权落地（关键 bug 修复）

当前 [`ServerRegisterRequestProcessor.java:155-159`](../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/register/ServerRegisterRequestProcessor.java#L155-L159) 的 `extractPassword` 直接 `return ""`——这意味着**即使业务方实现了 `validatePassword`，收到的 password 永远是空串，鉴权形同虚设**。本次必须修复。

GB28181 设备使用 HTTP Digest 鉴权，请求头 `Authorization` 中携带的不是密码而是 `response = MD5(HA1:nonce[:nc:cnonce:qop]:HA2)`。框架已有 [`DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, plainPassword)`](../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/register/DigestServerAuthenticationHelper.java) 完成全部摘要计算和比对，业务方只需提供明文密码即可。

#### 3.6.2 改造方案

`ServerRegisterRequestProcessor` 改造后：

```java
// 1. 校验 ServerDeviceSupplier 是否注入（必备依赖）
// 2. 鉴权：业务方决定如何取密码、如何比对
if (!serverDeviceSupplier.authenticate(userId, request)) {
    log.warn("REGISTER鉴权失败: userId={}", userId);
    ResponseCmd.sendResponse(Response.FORBIDDEN, "Forbidden", evt);
    return;
}
```

完全删除 `extractPassword` 私有方法、删除 `ServerRegisterProcessorHandler` 字段。

#### 3.6.3 业务方默认接入示例

```java
@Component
public class GatewayServerDeviceSupplier implements ServerDeviceSupplier {
    @Autowired private RedisTemplate<String, Device> redis;

    @Override
    public boolean authenticate(String userId, SIPRequest request) {
        Device device = redis.opsForValue().get("sip:device:" + userId);
        if (device == null || device.getPassword() == null) {
            return false;
        }
        return DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, device.getPassword());
    }
}
```

`ServerRegisterProcessorHandler` 接口删除，`DefaultServerRegisterProcessorHandler` 删除。

#### 3.6.4 续订判断方案

原 `getDeviceTransaction(userId)` 用于跳过认证挑战的续订路径。改造后两种处理方式：

- **方案 A（推荐，简化）**：删除续订快路径，所有 REGISTER 都走 401→Auth→200 OK 流程。设备 SIP 栈自动缓存 `WWW-Authenticate` 信息，重发的成本是一次额外往返，但简化了状态管理。
- **方案 B（保留续订）**：在 `Device` DO 上加 `String lastCallId` 字段，由业务方在 `DeviceRegisterEvent` 监听器里自行维护。`ServerRegisterRequestProcessor` 通过 `serverDeviceSupplier.getDevice(userId).getLastCallId()` 比对。

本次改造采用**方案 A**，避免继续在框架里维护设备事务状态。

---

## 四、文件变更清单

### 删除文件

| 文件 | 模块 |
|------|------|
| `client/.../invite/InviteRequestHandler.java` | gb28181-client |
| `client/.../invite/DefaultInviteRequestHandler.java` | gb28181-client |
| `client/.../info/InfoRequestHandler.java` | gb28181-client |
| `client/.../info/DefaultClientInfoRequestHandler.java` | gb28181-client |
| `server/.../invite/ServerInviteRequestHandler.java` | gb28181-server |
| `server/.../invite/DefaultServerInviteRequestHandler.java` | gb28181-server |
| `server/.../bye/ServerByeProcessorHandler.java` | gb28181-server |
| `server/.../info/ServerInfoProcessorHandler.java` | gb28181-server |
| `server/.../register/ServerRegisterProcessorHandler.java` | gb28181-server |
| `server/.../register/DefaultServerRegisterProcessorHandler.java` | gb28181-server |
| `test/.../handler/TestInviteRequestHandler.java` | gb28181-test |

### 新增文件

| 文件 | 模块 | 说明 |
|------|------|------|
| `client/.../event/ClientInfoEvent.java` | gb28181-client | INFO 事件 |
| `server/.../event/ServerInviteEvent.java` | gb28181-server | 服务端收到 INVITE 事件 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `ClientInviteEvent.java` | 新增 `transactionContextKey` 字段（已完成） |
| `InviteRequestProcessor.java` | 改为 100 Trying + 发事件，删除 handler 依赖 |
| `InfoRequestProcessor.java` | 改为 200 OK + 发事件，删除 handler 依赖 |
| `ServerInviteRequestProcessor.java` | 改为 100 Trying + 发事件，删除 handler 依赖 |
| `ByeRequestProcessorServer.java` | **修复 200 OK 漏发**，删除 handler 依赖，直接发事件 |
| `ServerInfoRequestProcessor.java` | 删除 handler 依赖，直接发事件 |
| `ServerRegisterRequestProcessor.java` | 调 `serverDeviceSupplier.authenticate`，删除 `extractPassword`，删除 handler 依赖，移除续订快路径 |
| `ServerDeviceSupplier.java` | 新增 `authenticate(String, SIPRequest)` default 方法 |
| `SipTransactionRegistry.java` | 新增 `extendContext(String, long)` 静态方法 + `TransactionContextInfo.validUntilOverride` 字段 + `extendValidity(long)` 方法 |
| `TestServerEventHandler.java` | 新增 `ServerInviteEvent` / `ClientInfoEvent` 监听 |
| `TestServerRegisterHandler.java` | 改为实现 `ServerDeviceSupplier.authenticate` |
| `InvitePlayFlowTest.java` | 改为通过 `@EventListener` 回包，验证重传幂等 |

---

## 五、ServerDeviceSupplier 接口扩展

`ServerRegisterRequestProcessor` 改造后，密码验证下沉到 `ServerDeviceSupplier`。

### 5.1 命名冲突说明

现有 [`ServerDeviceSupplier.java:45`](../sip-common/src/main/java/io/github/lunasaw/sip/common/service/ServerDeviceSupplier.java#L45) 已有方法 `checkDevice(RequestEvent evt)`，语义是**判断 To-Header userId 是否等于本机 serverFromDevice.userId**（即"这条消息是否发给我"），与"密码鉴权"完全不同。**不能复用 `checkDevice` 名称做重载**，否则业务方实现时极易混淆。

### 5.2 新增 `authenticate` 方法

```java
public interface ServerDeviceSupplier extends DeviceSupplier {
    FromDevice getServerFromDevice();
    void setServerFromDevice(FromDevice fromDevice);

    /** 已有：判断消息是否发给本服务，与本次新增方法语义无关 */
    default boolean checkDevice(RequestEvent evt) { /* 既有实现保持不变 */ }

    /**
     * 设备注册鉴权（HTTP Digest）。
     * 业务方典型实现：取出 userId 对应明文密码，调
     * {@code DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, password)} 比对。
     * 默认放行，便于不需要鉴权的内网/测试环境快速接入。
     *
     * @param userId  设备 ID（来自 From-Header）
     * @param request 完整 SIP 请求，含 AuthorizationHeader
     * @return true 通过，false 拒绝注册（返回 403）
     */
    default boolean authenticate(String userId, SIPRequest request) {
        return true;
    }
}
```

`DefaultServerDeviceSupplier` 不需要覆盖（继承默认实现即可），向后兼容。生产接入方必须显式覆盖此���法，否则等于关闭鉴权。

> **为何把 `SIPRequest` 而非 `String password` 传给业务方**：避免框架猜测密码格式（明文 / 哈希 / 双重哈希）。业务方可以根据自身存储格式选择 `doAuthenticatePlainTextPassword` 或 `doAuthenticateHashedPassword`，也可以做更复杂的策略（如基于 IP 白名单跳过鉴权）。

---

## 六、SipTransactionRegistry 续期接口

### 6.1 现状问题

[`SipTransactionRegistry.TransactionContextInfo.checkAndUpdateValidity()`](../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/SipTransactionRegistry.java) 写死 `age > 32000` 即标记无效，且 `createTime` 是 `final` 字段，业务方无法续期。

异步 INVITE 场景下，业务方查媒体服务器、申请流端口等耗时常见 5~20s，但偶发抖动可能逼近或超过 30s。一旦触发硬超时，`getContext(callId)` 返回 null，回包失败、业务必须重新发起 INVITE，用户体验差。

### 6.2 改造方案

在 `TransactionContextInfo` 新增 `validUntilOverride` 字段（非 final，volatile），`checkAndUpdateValidity` 优先使用该字段：

```java
@Data
public static class TransactionContextInfo {
    private final long createTime;
    private volatile long validUntilOverride = -1L;  // 新增：业务方续期截止时间

    public boolean checkAndUpdateValidity() {
        // ... 既有 ServerTransaction state 判定保持不变 ...

        long now = System.currentTimeMillis();
        long deadline = (validUntilOverride > 0) ? validUntilOverride : (createTime + 32000);
        if (now > deadline) {
            this.isValid = false;
            log.debug("事务超时: contextKey={}, deadline={}", contextKey, deadline);
            return false;
        }
        return isValid;
    }

    /** 续期到指定毫秒数之后失效（绝对时间，业务方调 System.currentTimeMillis() + ttlMs） */
    public void extendValidity(long ttlMs) {
        this.validUntilOverride = System.currentTimeMillis() + ttlMs;
    }
}
```

在 `SipTransactionRegistry` 顶层暴露便捷方法：

```java
/** 续期事务上下文有效期。返回 false 表示上下文已不存在或已被 SIP 栈终结，无法续期。 */
public static boolean extendContext(String contextKey, long ttlMs) {
    TransactionContextInfo ctx = TRANSACTION_CONTEXTS.get(contextKey);
    if (ctx == null || !ctx.checkAndUpdateValidity()) {
        return false;
    }
    ctx.extendValidity(ttlMs);
    return true;
}
```

### 6.3 业务方使用方式

```java
@EventListener
public void onServerInvite(ServerInviteEvent event) {
    // 预计业务处理需要 60s，提前续期到 90s 留余量
    SipTransactionRegistry.extendContext(event.getTransactionContextKey(), 90_000);

    asyncExecutor.submit(() -> {
        String sdp = mediaService.allocateAndBuildSdp(event);  // 可能 30~60s
        // 回包...
    });
}
```

> **30s 是 SIP T1 派生超时的隐式约束**：JAIN-SIP 服务端事务在 `Proceeding` 状态本身没有 `Timer C` 强制超时，但客户端（设备）会按 RFC 3261 §13.3.1.4 在 `Timer B`（默认 64*T1 = 32s）后放弃等待。即使续期 `TransactionContextInfo` 到 90s，设备也已超时不再接受响应。**续期超过 30s 的实际意义是允许业务方做"180 Ringing 占位 → 后续 200 OK 携带真实 SDP"分两步处理**，单次回包仍受设备侧约束。文档需在 LAYERED-ARCHITECTURE.md §七同步说明此限制。

---

## 七、实施顺序

```
1. 新增 ClientInfoEvent、ServerInviteEvent
2. 改造 InviteRequestProcessor（client）→ 100 Trying + 发事件，加重传幂等注释
3. 改造 InfoRequestProcessor（client）
4. 改造 ServerInviteRequestProcessor → 100 Trying + 发事件
5. 改造 ByeRequestProcessorServer → 修复 200 OK 漏发 bug + 发事件（删 handler）
6. 改造 ServerInfoRequestProcessor → 删 handler 依赖
7. SipTransactionRegistry 新增 extendContext + validUntilOverride 字段
8. ServerDeviceSupplier 新增 authenticate(userId, SIPRequest) 默认方法
9. 改造 ServerRegisterRequestProcessor → 调 authenticate + 删 extractPassword + 删 handler 依赖
10. 删除所有 XxxHandler/XxxProcessorHandler 接口及默认实现
11. 更新 InvitePlayFlowTest（改为事件监听回包）
12. 更新 TestServerRegisterHandler / TestInviteRequestHandler（改为 authenticate + 事件监听器）
13. mvn clean compile && mvn test 验证
```

**单元测试覆盖要点**：
- `extendContext` 续期后 `getContext` 仍返回有效上下文
- `extendContext` 对已过期上下文返回 false
- `authenticate` 默认实现返回 true，覆盖后用错误密码返回 false
- `ServerInviteRequestProcessor` 重传场景下 contextKey 一致、事件被多次 publish

---

## 八、对接入方的影响

| 场景 | 旧接入方式 | 新接入方式 |
|------|----------|----------|
| 处理设备 INVITE | 实现 `InviteRequestHandler` | `@EventListener ClientInviteEvent`，异步调用 `ResponseCmd.sendResponse(200, sdp, ctx.getOriginalEvent())`；监听器需按 `callId` 幂等 |
| 处理 INFO | 实现 `InfoRequestHandler` | `@EventListener ClientInfoEvent` |
| 处理服务端 INVITE | ��现 `ServerInviteRequestHandler` | `@EventListener ServerInviteEvent`；超过 30s 业务调 `SipTransactionRegistry.extendContext(callId, ttl)` 续期 |
| 设备密码验证 | 实现 `ServerRegisterProcessorHandler.validatePassword`（**实际上无效，因 extractPassword 返回空串**） | 实现 `ServerDeviceSupplier.authenticate(userId, request)`，调用 `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, password)` 完成摘要校验 |
| BYE 协议合规 | 框架不发 200 OK（bug） | 框架自动发 200 OK，设备不再重传 |
| 续订路径优化 | `ServerRegisterProcessorHandler.getDeviceTransaction` | 移除（所有 REGISTER 走完整 401→Auth→200 OK 流程） |

**破坏性变更**：删除了 `InviteRequestHandler`、`InfoRequestHandler`、`ServerInviteRequestHandler`、`ServerByeProcessorHandler`、`ServerInfoProcessorHandler`、`ServerRegisterProcessorHandler` 接口。接入方需按上表迁移。

**非破坏性变更**：
- `ServerDeviceSupplier.authenticate` 是 `default` 方法，未覆盖的接入方编译期通过，运行期默认放行（与原 `validatePassword` 默认行为一致）。但**生产环境必须显式覆盖**，否则等于关闭鉴权。
- `SipTransactionRegistry.extendContext` 是新增静态方法，不调用即不影响既有 32s 超时行为。

**默认放行的安全风险提示**：原 `validatePassword` 默认 true 也是放行，但因 `extractPassword` 返回空串，业务方实现的 `validatePassword(userId, "", evt)` 在严格判等时会拒绝——形成"看似有鉴权实则放行"的偶然安全性。改造后默认放行是**显式且明确**的，必须在文档和代码注释中强调"生产必须覆盖 `authenticate`"。

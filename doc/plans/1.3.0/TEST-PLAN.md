# 测试方案

> 版本：1.3.0 | 对应重构：REFACTOR-COMMAND-LAYER.md

---

## 一、测试分层原则

| 层次 | 模块 | 范围 | 外部依赖处理 |
|------|------|------|-------------|
| 单元测试 | `gb28181-client` | 命令发送层、策略路由、消息处理层 | Mock `SipSender`、`CommandStrategyFactory`、`MessageRequestHandler`、`ClientDeviceSupplier` |
| 单元测试 | `gb28181-server` | 命令发送层、消息处理器 | Mock `SipSender`、`ServerMessageProcessorHandler` |
| 集成测试 | `gb28181-test` | client ↔ server 完整消息流 | 真实 Spring 容器，Mock `SipLayer`（网络层） |

---

## 二、gb28181-client 单元测试

### 2.1 CommandStrategyFactory 路由测试

**文件**：`gb28181-client/src/test/java/.../cmd/CommandStrategyFactoryTest.java`

```
测试类：CommandStrategyFactoryTest
依赖：纯 Java，无 Spring

TC-01 正常路由
  Given: factory 注入 [client:MESSAGE, client:REGISTER, server:MESSAGE]
  When:  getStrategy("client", "MESSAGE")
  Then:  返回 client 侧 MessageCommandStrategy

TC-02 role 隔离
  Given: 同上
  When:  getStrategy("server", "MESSAGE")
  Then:  返回 server 侧 MessageCommandStrategy（不同实例）

TC-03 未知策略 fail-fast
  Given: 同上
  When:  getStrategy("client", "UNKNOWN")
  Then:  抛出 IllegalArgumentException，消息含 "client:UNKNOWN"

TC-04 重复策略注册
  Given: 两个 getRole()+getCommandType() 相同的策略
  When:  构造 CommandStrategyFactory
  Then:  抛出 IllegalStateException
```

### 2.2 RegisterCommandStrategy 线程安全测试

**文件**：`gb28181-server/src/test/java/.../strategy/impl/RegisterCommandStrategyTest.java`

```
测试类：RegisterCommandStrategyTest
Mock：SipSender（静态方法，用 MockedStatic）

TC-05 expires 从 CommandContext.extras 正确读取
  Given: CommandContext.forRegister("client", from, to, 7200)
  When:  strategy.execute(ctx)
  Then:  SipSender.doRegisterRequest 被调用，expires=7200

TC-06 expires 为 null 时使用默认值 3600
  Given: CommandContext 中 extras 不含 "expires"
  When:  strategy.execute(ctx)
  Then:  SipSender.doRegisterRequest 被调用，expires=3600

TC-07 并发执行不互相干扰（验证无实例变量）
  Given: 10 个线程同时调用，expires 各不相同（100~1000）
  When:  并发 execute
  Then:  每次 SipSender 收到的 expires 与对应 CommandContext 一致
```

### 2.3 ClientCommandSender 静态委托测试

**文件**：`gb28181-client/src/test/java/.../cmd/ClientCommandSenderTest.java`

```
测试类：ClientCommandSenderTest（@SpringBootTest 最小上下文）
Mock：CommandStrategyFactory（@MockitoBean）

TC-08 sendRegisterCommand 构造正确的 CommandContext
  Given: expires=3600
  When:  ClientCommandSender.sendRegisterCommand(from, to, 3600)
  Then:  factory.getStrategy("client","REGISTER").execute(ctx) 被调用
         ctx.getExtra("expires", Integer.class) == 3600

TC-09 sendAlarmCommand 使用 MESSAGE 策略
  When:  ClientCommandSender.sendAlarmCommand(from, to, alarm)
  Then:  factory.getStrategy("client","MESSAGE") 被调用
         ctx.getBody() == alarm

TC-10 INSTANCE 未初始化时抛出明确错误
  Given: 反射将 INSTANCE 置为 null
  When:  ClientCommandSender.sendRegisterCommand(...)
  Then:  抛出 AssertionError，消息含 "尚未初始化"
```

### 2.4 SubscribeCommandStrategy 调用测试

**文件**：`gb28181-server/src/test/java/.../strategy/impl/SubscribeCommandStrategyTest.java`

> **注意**：当前实现无 subscribeInfo 分支，直接调用单一重载。TC-11/TC-12 调整为验证实际行为；
> 若后续需要 subscribeInfo 分支，先补充策略代码再恢复原用例。

```
TC-11 正常订阅调用 doSubscribeRequest
  Given: CommandContext.forSubscribe("server", from, to, subscribeInfo, 3600)
  When:  strategy.execute(ctx)
  Then:  SipSender.doSubscribeRequest 被调用一次，参数与 ctx 一致

TC-12 subscribeInfo 为 null 时调用不抛异常
  Given: extras 中无 subscribeInfo
  When:  strategy.execute(ctx)
  Then:  SipSender.doSubscribeRequest 被调用一次，无异常抛出
```

### 2.5 ClientMessageRequestProcessor 过滤逻辑测试

**文件**：`gb28181-client/src/test/java/.../request/message/ClientMessageRequestProcessorTest.java`

```
Mock：ClientDeviceSupplier（返回固定 FromDevice）、SIPRequest（构造 from/to header）

TC-20 toUserId 匹配时转发给消息处理框架
  Given: SIPRequest.toHeader.userId == clientFromDevice.userId
  When:  processor.process(evt)
  Then:  doMessageHandForEvt 被调用一次

TC-21 toUserId 不匹配时静默丢弃
  Given: SIPRequest.toHeader.userId != clientFromDevice.userId
  When:  processor.process(evt)
  Then:  doMessageHandForEvt 不被调用，无异常抛出
```

### 2.6 消息 Handler 链路测试（协议解析 → 业务调用 → 发送响应）

**测试原则**：每个 handler 覆盖两个用例——正常链路 + 业务接口异常时不向上传播。

Mock：`MessageRequestHandler`（@MockitoBean）、`ClientDeviceSupplier`（@MockitoBean）、`ClientCommandSender`（MockedStatic）

| 测试文件 | Handler | 业务接口调用 | 响应发送方法 |
|---------|---------|------------|------------|
| `DeviceInfoQueryMessageClientHandlerTest` | `DeviceInfoQueryMessageClientHandler` | `getDeviceInfo(userId)` | `sendDeviceInfoCommand` |
| `CatalogQueryMessageClientHandlerTest` | `CatalogQueryMessageClientHandler` | `getDeviceItem(userId)` | `sendCatalogCommand` |
| `AlarmQueryMessageClientHandlerTest` | `AlarmQueryMessageClientHandler` | `getDeviceAlarmNotify(query)` | `sendAlarmCommand` |
| `DeviceStatusQueryMessageClientHandlerTest` | `DeviceStatusQueryMessageClientHandler` | `getDeviceStatus(userId)` | `sendDeviceStatusCommand` |
| `RecordInfoQueryMessageClientHandlerTest` | `RecordInfoQueryMessageClientHandler` | `getDeviceRecord(query)` | `sendDeviceRecordCommand` |
| `ConfigDownloadQueryMessageClientHandlerTest` | `ConfigDownloadQueryMessageClientHandler` | `getConfigDownloadResponse(userId, type)` | `sendConfigDownloadResponse` |
| `PresetQueryMessageClientHandlerTest` | `PresetQueryMessageClientHandler` | `getPresetQueryResponse(userId)` | `sendPresetQueryResponse` |
| `DeviceMobileQueryMessageClientHandlerTest` | `DeviceMobileQueryMessageClientHandler` | `getMobilePositionNotify(query)` | `sendMobilePositionNotify` |
| `BroadcastNotifyMessageHandlerTest` | `BroadcastNotifyMessageHandler` | `broadcastNotify(notify)` | 无（仅通知，无响应） |

**每个测试文件的标准用例**（以 `DeviceInfoQueryMessageClientHandler` 为例，其余同理）：

```
TC-22 正常链路：XML 解析 → 业务调用 → 响应发送
  Given: XML body 含合法 CmdType/SN，messageRequestHandler.getDeviceInfo 返回 DeviceInfo
  When:  handler.handForEvt(event)
  Then:  ClientCommandSender.sendDeviceInfoCommand 被调用一次
         传入的 DeviceInfo.sn == XML 中的 SN 值

TC-23 业务接口抛异常时不向上传播
  Given: messageRequestHandler.getDeviceInfo 抛 RuntimeException
  When:  handler.handForEvt(event)
  Then:  方法正常返回，ClientCommandSender 不被调用
```

---

## 三、gb28181-server 单元测试

### 3.1 ServerCommandSender 命令构造测试

**文件**：`gb28181-server/src/test/java/.../cmd/ServerCommandSenderTest.java`

```
Mock：CommandStrategyFactory（@MockitoBean）

TC-13 deviceInfoQuery 构造 MESSAGE + DeviceQuery(DEVICE_INFO)
TC-14 deviceInvitePlayBackControl 构造 INFO + controlBody 在 extras 中
TC-15 deviceAck(from, to, callId) 构造 ACK + callId 在 extras 中
TC-16 deviceCatalogSubscribe 构造 SUBSCRIBE + subscribeInfo 在 extras 中
```

### 3.2 DefaultServerMessageProcessorHandler 消息处理测试

**文件**：`gb28181-server/src/test/java/.../request/message/DefaultServerMessageProcessorHandlerTest.java`

```
Mock：无（测试默认空实现不抛异常）

TC-17 keepLiveDevice 默认实现不抛异常
TC-18 validateDevicePermission 默认返回 true
TC-19 handleMessageError 默认实现不抛异常
```

---

## 四、gb28181-test 集成测试

### 4.1 测试环境设计

```
测试 Spring 容器同时加载 client + server 配置
Mock 网络层：用 MockSipLayer 替换真实 SipLayer，拦截发出的 SIP 消息
             并可手动注入入站消息触发处理器

关键 Mock 组件：
  MockSipLayer        — 捕获 transmitRequest 调用，记录发出的消息
  TestDeviceSupplier  — 提供固定的 FromDevice/ToDevice 测试数据
  TestMessageHandler  — 实现 MessageRequestHandler，记录收到的业务对象
  TestServerHandler   — 实现 ServerMessageProcessorHandler，记录收到的业务对象
```

### 4.2 集成测试场景

**文件**：`gb28181-test/src/test/java/.../integration/`

#### IT-01 设备注册流程

```
场景：client 发起注册 → server 收到并处理

步骤：
  1. ClientCommandSender.sendRegisterCommand(clientDevice, serverDevice, 3600)
  2. MockSipLayer 捕获发出的 REGISTER 消息
  3. 将消息注入 ServerMessageRequestProcessor
  4. 断言 TestServerHandler.keepLiveDevice() 被调用
  5. 断言 REGISTER 消息头 Expires=3600
```

#### IT-02 设备目录查询流程

```
场景：server 查询 → client 收到并回复

步骤：
  1. ServerCommandSender.deviceCatalogQuery(serverDevice, clientDevice)
  2. MockSipLayer 捕获发出的 MESSAGE(Catalog)
  3. 将消息注入 ClientMessageRequestProcessor
  4. 断言 TestMessageHandler.getDeviceItem() 被调用（或对应方法）
  5. ClientCommandSender.sendCatalogCommand(clientDevice, serverDevice, catalogResponse)
  6. 断言 MockSipLayer 捕获到 MESSAGE(CatalogResponse)
```

#### IT-03 实时点播流程

```
场景：server 发起 INVITE → client 收到

步骤：
  1. ServerCommandSender.deviceInvitePlay(serverDevice, clientDevice, sdpIp, port)
  2. MockSipLayer 捕获 INVITE 消息，验证 SDP 内容格式
  3. 将 INVITE 注入 ClientInviteRequestProcessor
  4. 断言 client 侧触发对应 handler
```

#### IT-04 心跳保活流程

```
场景：client 发送心跳 → server 更新设备状态

步骤：
  1. ClientCommandSender.sendKeepaliveCommand(clientDevice, serverDevice, "OK")
  2. MockSipLayer 捕获 MESSAGE(Keepalive)
  3. 注入 ServerMessageRequestProcessor
  4. 断言 TestServerHandler.keepLiveDevice() 被调用
  5. 断言 keepalive 消息中 DeviceID 与 clientDevice.getUserId() 一致
```

#### IT-05 云台控制流程

```
场景：server 发送 PTZ 控制命令

步骤：
  1. ServerCommandSender.deviceControlPtzCmd(serverDevice, clientDevice, PtzCmdEnum.UP, 50)
  2. MockSipLayer 捕获 MESSAGE(DeviceControl)
  3. 断言消息体包含 PTZ 指令字段
```

#### IT-06 BYE 会话结束流程

```
场景：server 发送 BYE → client 收到

步骤：
  1. ServerCommandSender.deviceBye(serverDevice, clientDevice)
  2. MockSipLayer 捕获 BYE 消息
  3. 注入 ClientByeRequestProcessor
  4. 断言 client 侧 handler 被调用
```

---

## 五、测试数据 Fixture

```java
// 所有测试共用的设备数据
FromDevice CLIENT_DEVICE = FromDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
ToDevice   SERVER_DEVICE = ToDevice.getInstance("34020000002000000001", "192.168.1.200", 5060);

// SERVER 视角（角色互换）
FromDevice SERVER_FROM   = FromDevice.getInstance("34020000002000000001", "192.168.1.200", 5060);
ToDevice   CLIENT_TO     = ToDevice.getInstance("34020000001320000001", "192.168.1.100", 5060);
```

---

## 六、执行命令

```bash
# 各模块单元测试
mvn test -pl gb28181-client
mvn test -pl gb28181-server

# 集成测试
mvn verify -pl gb28181-test --also-make

# 全量
mvn test -pl gb28181-client,gb28181-server && mvn verify -pl gb28181-test --also-make
```

---

## 七、需补充的 pom.xml 依赖

**gb28181-client / gb28181-server**（各自 pom.xml 添加）：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**gb28181-test**：awaitility 依赖已就绪，无需修改。

**gb28181-client / gb28181-server**：`spring-boot-starter-test` 已在根 pom 中定义，子模块继承后可直接使用。执行前用以下命令验证：
```bash
mvn test -pl gb28181-client -q
```

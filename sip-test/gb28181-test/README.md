# SIP代理测试重构说明

## 测试架构概述

本项目采用**集成测试架构**，通过客户端发送SIP请求来测试服务端处理器的执行情况。测试流程如下：

```
客户端测试类 → 发送SIP请求 → 服务端处理器 → 验证处理结果
```

### 测试架构特点

1. **端到端测试**：通过真实的SIP消息交互验证功能
2. **处理器验证**：重点验证服务端处理器的业务逻辑
3. **消息流测试**：验证完整的SIP消息处理流程
4. **设备配置分离**：严格区分客户端和服务端设备配置

## 测试结构重组

由于客户端和服务端功能混合，现将测试类拆分成独立的客户端和服务端测试，每个测试类专注于测试特定的命令交互。

## 目录结构

```
gb28181-test/src/test/java/io/github/lunasaw/gbproxy/test/
├── client/                          # 客户端测试类
│   ├── ClientRegisterTest.java      # 设备注册测试
│   ├── ClientHeartbeatTest.java     # 设备心跳测试
│   ├── ClientCatalogTest.java       # 设备目录查询测试
│   ├── ClientControlTest.java       # 设备控制响应测试
│   └── ClientInviteTest.java        # 设备点播响应测试
├── server/                          # 服务端测试类
│   ├── ServerRegisterTest.java      # 设备注册管理测试
│   ├── ServerHeartbeatTest.java     # 设备心跳监控测试
│   ├── ServerCatalogTest.java       # 设备目录查询测试
│   ├── ServerControlTest.java       # 设备控制命令测试
│   └── ServerInviteTest.java        # 设备点播命令测试
├── config/                          # 测试配置
├── register/                        # 原有注册测试（保留）
├── invite/                          # 原有点播测试（保留）
├── subscribe/                       # 原有订阅测试（保留）
└── README.md                        # 本说明文件
```

## 测试规则详解

### 1. 设备配置分离规范（强制性要求）

所有单元测试中，涉及SIP消息模拟时，必须严格区分client和server的设备配置：

- **客户端发起的消息**：from/to必须分别使用`clientFromDevice`/`clientToDevice`
- **服务端发起的消息**：from/to必须分别使用`serverFromDevice`/`serverToDevice`
- **不允许混用或写反**
- 所有后续单测都要遵循此规范，client和server的设备配置、消息模拟、断言等必须分开处理

#### 设备配置获取方法

```java
// 客户端设备配置
FromDevice clientFromDevice = testDeviceSupplier.getClientFromDevice();
ToDevice clientToDevice = testDeviceSupplier.getClientToDevice();

// 服务端设备配置
FromDevice serverFromDevice = testDeviceSupplier.getServerFromDevice();
ToDevice serverToDevice = testDeviceSupplier.getServerToDevice();
```

#### 设备配置使用规范（强制性要求）

**重要：SIP请求创建时必须正确使用FromDevice和ToDevice**

- **客户端发送请求**：使用`clientFromDevice`作为From，`clientToDevice`作为To
- **服务端发送请求**：使用`serverFromDevice`作为From，`serverToDevice`作为To
- **禁止错误使用**：不能将两个FromDevice或两个ToDevice传递给请求创建方法

**正确示例：**

```java
// 客户端发送注册请求
Request registerRequest = SipRequestProvider.createRegisterRequest(
                clientFromDevice,  // From: 客户端设备
                clientToDevice,    // To: 服务端设备
                3600,
                callId);

// 服务端发送查询请求
Request infoRequest = SipRequestProvider.createInfoRequest(
        serverFromDevice,  // From: 服务端设备
        serverToDevice,    // To: 客户端设备
        callId,
        content);
```

**错误示例：**

```java
// ❌ 错误：使用两个FromDevice
Request registerRequest = SipRequestProvider.createRegisterRequest(
                clientFromDevice,  // 错误：两个FromDevice
                serverFromDevice,  // 错误：两个FromDevice
                3600,
                callId);

// ❌ 错误：使用两个ToDevice
Request registerRequest = SipRequestProvider.createRegisterRequest(
        clientToDevice,    // 错误：两个ToDevice
        serverToDevice,    // 错误：两个ToDevice
        3600,
        callId);
```

### 2. 处理器测试架构

#### 2.1 服务端处理器测试流程

以测试`ServerRegisterRequestProcessor`为例：

1. **客户端发送注册请求**：通过`ClientRegisterTest.testClientDeviceRegistration()`
2. **服务端接收处理**：`ServerRegisterRequestProcessor.process()`被调用
3. **验证处理结果**：检查注册信息更新、响应发送等

#### 2.2 测试代码示例

```java

@Test
@DisplayName("测试服务端注册请求处理器")
public void testServerRegisterRequestProcessor() throws Exception {
    // 1. 获取设备配置
    FromDevice clientFromDevice = testDeviceSupplier.getClientFromDevice();
    ToDevice clientToDevice = testDeviceSupplier.getClientToDevice();

    // 2. 创建注册请求（客户端发送到服务端）
    String callId = SipRequestUtils.getNewCallId();
    Request registerRequest = SipRequestProvider.createRegisterRequest(
            clientFromDevice,  // From: 客户端设备
            clientToDevice,    // To: 服务端设备
            3600,
            callId);

    // 3. 发送请求（触发服务端处理器）
    SipSender.transmitRequestSuccess(clientFromDevice.getIp(), registerRequest, event -> {
        // 4. 验证响应
        Assertions.assertEquals(200, event.getStatusCode(), "注册应该成功");
    });

    // 5. 验证服务端处理器执行结果
    // 检查设备注册信息是否更新
    // 检查事务信息是否正确
}
```

### 3. 单独测试处理器的方法

#### 3.1 直接测试处理器类

可以创建专门的处理器测试类，直接实例化处理器进行测试：

```java

@SpringBootTest
public class ServerRegisterRequestProcessorTest {

    @Autowired
    private ServerRegisterRequestProcessor processor;

    @Test
    @DisplayName("直接测试注册请求处理器")
    public void testProcessorDirectly() {
        // 1. 创建模拟的RequestEvent
        RequestEvent mockEvent = createMockRegisterRequestEvent();

        // 2. 直接调用处理器
        processor.process(mockEvent);

        // 3. 验证处理结果
        // 检查注册信息、事务更新等
    }

    private RequestEvent createMockRegisterRequestEvent() {
        // 创建模拟的SIP注册请求事件
        // 包含必要的请求头、设备信息等
    }
}
```

#### 3.2 使用Mock框架测试

```java

@Test
@DisplayName("使用Mock测试注册处理器")
public void testProcessorWithMock() {
    // 1. Mock依赖组件
    RegisterProcessorServer mockProcessor = Mockito.mock(RegisterProcessorServer.class);
    SipUserGenerateServer mockUserGenerate = Mockito.mock(SipUserGenerateServer.class);

    // 2. 注入Mock对象
    processor.setRegisterProcessorServer(mockProcessor);
    processor.setSipUserGenerate(mockUserGenerate);

    // 3. 设置Mock行为
    when(mockUserGenerate.getFromDevice()).thenReturn(mockFromDevice);

    // 4. 执行测试
    processor.process(mockRequestEvent);

    // 5. 验证Mock调用
    verify(mockProcessor).updateRegisterInfo(anyString(), any(RegisterInfo.class));
}
```

### 4. 测试分类原则

#### 4.1 客户端测试特点

- **模拟设备端行为**：模拟GB28181设备的行为
- **被动响应服务端命令**：处理服务端下发的控制命令
- **主动发送注册、心跳等消息**：主动向服务端发送状态信息
- **验证响应处理逻辑**：验证客户端对服务端响应的处理

#### 4.2 服务端测试特点

- **模拟平台端行为**：模拟GB28181平台的行为
- **主动发送控制命令**：向设备发送控制、查询等命令
- **被动接收设备响应**：处理设备上报的状态信息
- **验证设备管理逻辑**：验证设备注册、状态管理等逻辑

### 5. 测试执行顺序

#### 5.1 客户端测试

1. `ClientRegisterTest` - 设备注册功能
2. `ClientHeartbeatTest` - 设备心跳功能
3. `ClientCatalogTest` - 设备目录查询功能
4. `ClientControlTest` - 设备控制响应功能
5. `ClientInviteTest` - 设备点播响应功能

#### 5.2 服务端测试

1. `ServerRegisterTest` - 设备注册管理功能
2. `ServerHeartbeatTest` - 设备心跳监控功能
3. `ServerCatalogTest` - 设备目录查询功能
4. `ServerControlTest` - 设备控制命令功能
5. `ServerInviteTest` - 设备点播命令功能

### 6. 测试配置说明

#### 6.1 客户端测试配置

```java
@TestPropertySource(properties = {
        "sip.gb28181.client.keepAliveInterval=1m",
        "sip.gb28181.client.maxRetries=3",
        "sip.gb28181.client.retryDelay=5s",
        "sip.gb28181.client.registerExpires=3600",
        "sip.gb28181.client.clientId=34020000001320000001",
        "sip.gb28181.client.clientName=GB28181-Client",
        "sip.gb28181.client.username=admin",
        "sip.gb28181.client.password=123456"
})
```

#### 6.2 服务端测试配置

```java
@TestPropertySource(properties = {
        "sip.gb28181.server.port=8118",
        "sip.gb28181.server.realm=3402000000",
        "sip.gb28181.server.password=123456",
        "sip.gb28181.server.serverId=34020000002000000001"
})
```

### 7. 测试最佳实践

#### 7.1 测试方法命名规范

- 使用`@DisplayName`提供清晰的测试描述
- 测试方法名使用`test`前缀
- 方法名描述测试的具体场景

#### 7.2 测试数据管理

- 使用`@BeforeEach`进行测试初始化
- 使用`@AfterEach`进行测试清理
- 测试数据独立，避免测试间相互影响

#### 7.3 异常处理测试

```java

@Test
@DisplayName("测试注册失败场景")
public void testRegisterFailure() {
    // 测试密码错误
    // 测试设备ID不存在
    // 测试网络异常
}
```

#### 7.4 性能测试

```java

@Test
@DisplayName("测试并发注册性能")
public void testConcurrentRegistration() {
    // 模拟多个设备同时注册
    // 验证系统并发处理能力
}
```

### 8. 注意事项

1. **每个测试类都是独立的**，可以单独运行
2. **测试类之间没有依赖关系**，避免测试顺序依赖
3. **使用`@TestMethodOrder`**确保测试方法按顺序执行
4. **每个测试类都有完整的初始化和清理逻辑**
5. **测试结果独立验证**，便于问题定位
6. **严格遵循设备配置分离规范**，确保测试的准确性
7. **使用真实的SIP消息交互**，验证端到端功能
8. **重点验证处理器业务逻辑**，确保功能正确性
9. **正确使用FromDevice和ToDevice**，避免传递错误的设备类型

### 9. 设备配置使用检查清单

在编写测试代码时，请务必检查以下几点：

#### 9.1 设备获取检查

- [ ] 是否正确获取了客户端设备：`clientFromDevice`和`clientToDevice`
- [ ] 是否正确获取了服务端设备：`serverFromDevice`和`serverToDevice`
- [ ] 设备配置是否完整（IP、端口、用户ID等）

#### 9.2 请求创建检查

- [ ] 客户端发送请求时，是否使用`clientFromDevice`作为From，`clientToDevice`作为To
- [ ] 服务端发送请求时，是否使用`serverFromDevice`作为From，`serverToDevice`作为To
- [ ] 是否避免了传递两个FromDevice或两个ToDevice的错误

#### 9.3 常见错误检查

- [ ] 是否错误地使用了`clientFromDevice`和`serverFromDevice`作为请求参数
- [ ] 是否错误地使用了`clientToDevice`和`serverToDevice`作为请求参数
- [ ] 是否在注释中明确标注了From和To的含义

#### 9.4 代码审查要点

```java
// ✅ 正确：客户端发送注册请求
Request registerRequest = SipRequestProvider.createRegisterRequest(
                clientFromDevice,  // From: 客户端设备
                clientToDevice,    // To: 服务端设备
                3600,
                callId);

// ❌ 错误：使用两个FromDevice
Request registerRequest = SipRequestProvider.createRegisterRequest(
        clientFromDevice,  // 错误：两个FromDevice
        serverFromDevice,  // 错误：两个FromDevice
        3600,
        callId);
```

### 10. 常见问题解决

#### 10.1 测试超时问题

```java
// 设置合理的超时时间
boolean received = responseLatch.await(10, TimeUnit.SECONDS);
```

#### 10.2 设备配置问题

```java
// 确保设备配置正确
Assertions.assertNotNull(clientFromDevice, "客户端设备应该被正确配置");
Assertions.

assertNotNull(clientToDevice, "服务端设备应该被正确配置");
```

#### 10.3 消息处理验证

```java
// 验证消息处理结果
Assertions.assertTrue(
        lastEventResult.getStatusCode() >=200&&lastEventResult.

getStatusCode() < 300,
        "注册应该成功，状态码应该在200-299之间");
```

## 总结

本项目的测试架构通过客户端发送请求来测试服务端处理器的执行情况，既保证了测试的真实性，又能够有效验证处理器的业务逻辑。通过严格遵循设备配置分离规范，确保测试的准确性和可维护性。
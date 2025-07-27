# INVITE请求测试架构重构说明

## 重构背景

原有的 `InviteRequestProcessor` 中直接包含了测试钩子代码，违反了分层架构原则，导致：
1. 主业务代码依赖测试代码
2. 编译错误：`io.github.lunasaw.gbproxy.test cannot be resolved`
3. 代码职责不清晰

## 重构方案

### 1. 架构分离

**重构前**：
```
InviteRequestProcessor (主业务代码)
    ↓ 直接调用
TestClientMessageProcessorHandler.updateInvitePlay() (测试钩子)
```

**重构后**：
```
InviteRequestProcessor (主业务代码)
    ↓ 依赖注入
InviteRequestHandler (接口)
    ↓ 实现
TestInviteRequestHandler (测试实现)
    ↓ 内部调用
测试钩子方法
```

### 2. 文件结构

```
gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/transmit/request/invite/
├── InviteRequestProcessor.java          # 主业务处理器（已清理测试代码）
├── InviteRequestHandler.java            # 业务处理器接口
└── DefaultInviteRequestHandler.java     # 默认业务实现

gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/
├── handler/
│   └── TestInviteRequestHandler.java    # 测试专用处理器
├── config/
│   └── TestInviteConfiguration.java     # 测试配置
└── test/java/io/github/lunasaw/gbproxy/test/
    └── InviteRequestProcessorTest.java  # 示例测试用例
```

## 核心组件说明

### 1. InviteRequestProcessor (主业务代码)

**职责**：只负责SIP协议层面的消息处理
- 解析SIP消息
- 调用业务处理器接口
- 构建响应

**特点**：
- 不包含任何测试相关代码
- 只依赖 `InviteRequestHandler` 接口
- 符合单一职责原则

### 2. TestInviteRequestHandler (测试实现)

**职责**：
- 实现 `InviteRequestHandler` 接口
- 提供测试钩子功能
- 记录测试状态

**核心方法**：
```java
// 业务方法
void inviteSession(String callId, SdpSessionDescription sessionDescription)
String getInviteResponse(String userId, SdpSessionDescription sessionDescription)

// 测试钩子方法
static boolean waitForInvitePlay(long timeout, TimeUnit unit)
static boolean hasReceivedInvitePlay()
static String getReceivedInvitePlayCallId()
static void resetTestState()
```

### 3. TestInviteConfiguration (测试配置)

**职责**：
- 配置测试环境使用 `TestInviteRequestHandler`
- 使用 `@Primary` 注解确保测试优先级

## 使用方法

### 1. 编写测试用例

```java
@SpringBootTest
@ActiveProfiles("test")
public class InviteRequestProcessorTest {

    @BeforeEach
    void setUp() {
        // 重置测试状态
        TestInviteRequestHandler.resetTestState();
    }

    @Test
    void testInvitePlayRequest() throws InterruptedException {
        // 发送INVITE请求
        String callId = SipSender.doInviteRequest(fromDevice, toDevice, sdpContent, subject);

        // 等待测试钩子触发
        boolean received = TestInviteRequestHandler.waitForInvitePlay(5, TimeUnit.SECONDS);
        assertTrue(received, "应该收到INVITE实时点播请求");

        // 验证测试钩子状态
        assertTrue(TestInviteRequestHandler.hasReceivedInvitePlay());
        assertEquals(callId, TestInviteRequestHandler.getReceivedInvitePlayCallId());
    }
}
```

### 2. 测试钩子使用

```java
// 等待实时点播
TestInviteRequestHandler.waitForInvitePlay(timeout, unit);

// 等待回放点播
TestInviteRequestHandler.waitForInvitePlayBack(timeout, unit);

// 检查是否收到
TestInviteRequestHandler.hasReceivedInvitePlay();
TestInviteRequestHandler.hasReceivedInvitePlayBack();

// 获取接收到的数据
TestInviteRequestHandler.getReceivedInvitePlayCallId();
TestInviteRequestHandler.getReceivedInvitePlaySdp();
```

## 架构优势

### 1. 职责分离
- **主业务代码**：只负责协议处理，不包含测试逻辑
- **测试代码**：专门处理测试钩子和验证逻辑

### 2. 依赖倒置
- 主业务代码依赖接口，不依赖具体实现
- 测试代码通过实现接口提供测试功能

### 3. 可维护性
- 协议变更不影响测试代码
- 测试逻辑变更不影响主业务代码
- 便于独立测试和维护

### 4. 扩展性
- 可以轻松添加新的测试场景
- 可以复用测试钩子机制
- 支持多种测试策略

## 注意事项

### 1. 测试环境配置
确保测试类使用 `@ActiveProfiles("test")` 注解，这样会自动加载 `TestInviteConfiguration`。

### 2. 测试状态重置
每个测试方法开始前都要调用 `TestInviteRequestHandler.resetTestState()` 重置测试状态。

### 3. 超时设置
测试钩子等待方法需要设置合理的超时时间，避免测试无限等待。

### 4. 异常处理
测试钩子方法内部包含异常处理，确保测试稳定性。

## 后续扩展

### 1. 添加更多测试场景
可以在 `TestInviteRequestHandler` 中添加更多测试钩子，如：
- 错误响应测试
- 超时测试
- 并发测试

### 2. 集成其他测试框架
可以集成 Mockito、TestContainers 等测试框架，提供更丰富的测试能力。

### 3. 性能测试
可以基于此架构添加性能测试钩子，监控INVITE请求的处理性能。
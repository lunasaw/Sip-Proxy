# SIP Proxy

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/sip-proxy)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-common)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/gb28181-proxy/master/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

[项目文档](https://lunasaw.github.io/gb28181-proxy/) | [在线演示](http://www.isluna.ml) | [问题反馈](https://github.com/lunasaw/gb28181-proxy/issues)

## 📖 项目介绍

基于Java 17 + Spring Boot 3.3.1 +
SIP协议栈实现的GB28181通信框架，采用多模块架构设计，提供完整的SIP协议通信能力。项目支持客户端和服务端双向通信，专为构建视频监控、安防系统等GB28181协议应用而设计。

### 🎯 设计目标

- **高性能**：异步消息处理、连接池管理、缓存优化
- **易扩展**：模块化架构、插件化设计、统一接口
- **标准化**：严格遵循GB28181-2016协议规范
- **生产级**：完整的监控、日志、异常处理机制

## 🏗️ 项目架构

### 模块架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      SIP Proxy 项目架构                      │
├─────────────────────┬───────────────────────────────────────┤
│   gb28181-test      │            测试和示例模块              │
│   ├─ 集成测试       │            性能测试                   │
│   └─ 示例代码       │            配置示例                   │
├─────────────────────┼───────────────────────────────────────┤
│  gb28181-client     │         gb28181-server              │
│  ├─ 设备注册        │         ├─ 设备管理                  │
│  ├─ 心跳检测        │         ├─ 设备控制                  │
│  ├─ 告警上报        │         ├─ 云台控制                  │
│  ├─ 实时点播响应    │         ├─ 实时点播                  │
│  └─ 回放控制响应    │         └─ 视频回放                  │
├─────────────────────┼───────────────────────────────────────┤
│           gb28181-common (GB28181协议模型)                 │
│           ├─ 设备模型    ├─ 控制命令    ├─ 告警模型         │
│           └─ 查询模型    └─ 响应模型    └─ 通知模型         │
├─────────────────────────────────────────────────────────────┤
│                    sip-common (SIP基础包)                  │
│    ├─ SIP协议栈封装        ├─ 异步消息处理                 │
│    ├─ 连接池管理           ├─ 缓存服务                     │
│    ├─ 配置管理             ├─ 性能监控                     │
│    └─ 设备管理             └─ 事件总线                     │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

| 技术          | 版本         | 说明     |
|-------------|------------|--------|
| Java        | 17         | 编程语言   |
| Spring Boot | 3.3.1      | 应用框架   |
| JAIN-SIP    | 1.3.0-91   | SIP协议栈 |
| Caffeine    | 3.1.8      | 高性能缓存  |
| Micrometer  | 1.12.0     | 性能监控   |
| Guava       | 32.1.3-jre | 工具库    |
| Dom4j       | 2.1.4      | XML处理  |
| Maven       | 3.8+       | 构建工具   |

## ✨ 功能特性

### 🔧 SIP通用能力

- [x] **SIP协议栈封装**：基于JAIN-SIP的高性能封装
- [x] **异步消息处理**：高并发异步处理机制
- [x] **连接池管理**：SIP连接池管理和监控
- [x] **缓存服务**：基于Caffeine的多级缓存
- [x] **配置管理**：外部化配置和动态配置
- [x] **性能监控**：基于Micrometer的指标收集
- [x] **Spring Boot Starter**：开箱即用的自动配置

### 📱 GB28181客户端

- [x] **设备注册**：完整的设备注册认证流程
- [x] **心跳检测**：设备保活和状态监控
- [x] **告警上报**：设备告警信息推送
- [x] **事件推送**：设备状态变更通知
- [x] **实时点播响应**：实时视频流媒体响应
- [x] **回放控制响应**：历史视频回放控制
- [x] **设备控制响应**：云台控制、录像控制等
- [x] **设备信息查询响应**：设备状态、目录信息查询
- [x] **语音广播处理**：语音广播消息处理

### 🖥️ GB28181服务端

- [x] **设备管理**：设备注册、认证、会话管理
- [x] **设备控制**：云台控制、录像控制、复位等
- [x] **实时点播**：实时视频流请求和控制
- [x] **视频回放**：历史视频回放和控制
- [x] **告警处理**：告警接收、处理、转发
- [x] **设备查询**：设备信息、状态、目录查询
- [x] **订阅管理**：设备状态、告警、目录订阅
- [x] **级联支持**：上下级平台级联通信

### 🧪 测试和示例

- [x] **集成测试**：完整的客户端服务端测试
- [x] **性能测试**：压力测试和性能基准
- [x] **示例代码**：详细的使用示例和配置
- [x] **测试工具**：SIP消息构建和验证工具

## 🚀 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Spring Boot 3.3.1+

### 安装依赖

#### 全量包引入

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-proxy</artifactId>
    <version>${last.version}</version>
</dependency>
```

#### 按需引入

```xml
<!-- SIP基础包 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-common</artifactId>
    <version>${last.version}</version>
</dependency>

<!-- GB28181协议模型 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-common</artifactId>
    <version>${last.version}</version>
</dependency>

<!-- GB28181客户端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>${last.version}</version>
</dependency>

<!-- GB28181服务端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>${last.version}</version>
</dependency>
```

### 基础配置

创建 `application.yml` 配置文件：

```yaml
# SIP基础配置
sip:
  # 本地配置
  local:
    ip: 127.0.0.1
    port: 5060
    transport: UDP
    charset: UTF-8

  # 性能配置
  performance:
    enable-async: true
    thread-pool-size: 200
    cache-enable: true
    cache-size: 10000

  # 监控配置
  monitor:
    enable: true
    metrics-enable: true

# GB28181协议配置
gb28181:
  # 设备配置
  device:
    domain: 4405010000
    device-id: 44050100001327000001
    device-name: "测试设备"
    manufacturer: "测试厂商"
    model: "测试型号"
    firmware: "V1.0.0"

  # 认证配置
  auth:
    enable: true
    realm: "4405010000"
    username: "admin"
    password: "12345678"

  # 超时配置
  timeout:
    register: 60
    heartbeat: 30
    invite: 30
```

### 快速示例

#### 启动GB28181服务端

```java
@SpringBootApplication
@EnableSipProxy
public class Gb28181ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(Gb28181ServerApplication.class, args);
    }

    @Autowired
    private ServerSendCmd serverSendCmd;

    @PostConstruct
    public void init() {
        // 服务端启动后自动监听设备注册
        log.info("GB28181服务端启动成功");
    }
}
```

#### 启动GB28181客户端

```java
@SpringBootApplication
@EnableSipProxy
public class Gb28181ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(Gb28181ClientApplication.class, args);
    }

    @Autowired
    private ClientSendCmd clientSendCmd;

    @PostConstruct
    public void registerDevice() {
        // 客户端启动后自动注册设备
        clientSendCmd.register();
        log.info("设备注册请求已发送");
    }
}
```

## 📚 模块说明

### sip-common（SIP基础包）

SIP协议的基础封装和通用功能模块，提供SIP通信的核心能力。

**核心组件：**

- `SipLayer`：SIP协议层封装
- `AsyncSipMessageProcessor`：异步消息处理器
- `CacheService`：缓存服务
- `SipConnectionPool`：连接池管理
- `SipSender`：SIP消息发送器
- `SipConfigurationManager`：配置管理器

**主要功能：**

- SIP协议栈封装和配置管理
- 异步消息处理和线程池管理
- 高性能缓存系统
- 连接池管理和监控
- 性能指标收集

### gb28181-common（GB28181协议模型）

GB28181协议的数据模型和实体定义模块。

**主要包含：**

- 设备控制命令：云台控制、录像控制等
- 设备信息模型：设备目录、状态信息等
- 告警模型：设备告警、告警通知等
- 查询模型：各种查询和响应类

### gb28181-client（GB28181客户端）

GB28181协议的客户端实现，模拟设备端行为。

**核心服务：**

- `ClientSendCmd`：主动服务接口，提供各种SIP命令发送
- 被动消息处理器：各种RequestProcessor和ResponseProcessor
- 设备配置和用户管理

**使用场景：**

- 模拟GB28181设备进行测试
- 构建设备端应用
- 协议兼容性验证

### gb28181-server（GB28181服务端）

GB28181协议的服务端实现，提供平台级服务。

**核心服务：**

- `ServerSendCmd`：主动服务接口，提供各种SIP命令发送
- 被动消息处理器：各种RequestProcessor和ResponseProcessor
- 设备会话管理

**使用场景：**

- 构建GB28181平台服务
- 设备管理和控制
- 视频监控系统集成

### gb28181-test（测试和示例）

集成测试和示例代码模块。

**主要内容：**

- 客户端和服务端集成测试
- 协议功能验证
- 性能测试和压力测试
- 示例代码和配置

## ⚙️ 配置指南

### 基础配置

#### SIP协议配置

```yaml
sip:
  local:
    ip: 127.0.0.1              # 本地IP地址
    port: 5060                 # 本地端口
    transport: UDP             # 传输协议 UDP/TCP
    charset: UTF-8             # 字符编码

  performance:
    enable-async: true         # 启用异步处理
    thread-pool-size: 200      # 线程池大小
    cache-enable: true         # 启用缓存
    cache-size: 10000          # 缓存大小

  monitor:
    enable: true               # 启用监控
    metrics-enable: true       # 启用指标收集
```

#### GB28181协议配置

```yaml
gb28181:
  device:
    domain: 4405010000                    # SIP域
    device-id: 44050100001327000001       # 设备ID
    device-name: "测试设备"                # 设备名称
    manufacturer: "测试厂商"               # 厂商
    model: "测试型号"                     # 型号
    firmware: "V1.0.0"                   # 固件版本

  auth:
    enable: true                         # 启用认证
    realm: "4405010000"                  # 认证域
    username: "admin"                    # 用户名
    password: "12345678"                 # 密码

  timeout:
    register: 60                         # 注册超时(秒)
    heartbeat: 30                        # 心跳超时(秒)
    invite: 30                           # 邀请超时(秒)
```

### 高级配置

#### 性能优化配置

```yaml
sip:
  performance:
    # 异步处理配置
    async:
      core-pool-size: 50               # 核心线程数
      max-pool-size: 200               # 最大线程数
      queue-capacity: 1000             # 队列容量
      keep-alive: 60                   # 线程保活时间

    # 缓存配置
    cache:
      initial-capacity: 100            # 初始容量
      maximum-size: 10000              # 最大容量
      expire-after-write: 3600         # 写入后过期时间(秒)
      expire-after-access: 1800        # 访问后过期时间(秒)

    # 连接池配置
    pool:
      max-connections: 100             # 最大连接数
      min-connections: 10              # 最小连接数
      connection-timeout: 30           # 连接超时(秒)
      idle-timeout: 300                # 空闲超时(秒)
```

#### 监控配置

```yaml
sip:
  monitor:
    metrics:
      enable: true                     # 启用指标收集
      step: 60                         # 收集间隔(秒)

    logging:
      level: INFO                      # 日志级别
      pattern: "[%d{HH:mm:ss}] [%thread] %-5level %logger{36} - %msg%n"

    health:
      enable: true                     # 启用健康检查
      interval: 30                     # 检查间隔(秒)
```

## 💻 使用示例

### 设备注册示例

```java
@Component
public class DeviceRegistrationExample {

    @Autowired
    private ClientSendCmd clientSendCmd;

    /**
     * 设备注册
     */
    public void registerDevice() {
        try {
            // 发送注册请求
            ResultDTO result = clientSendCmd.register();

            if (result.isSuccess()) {
                log.info("设备注册成功");
            } else {
                log.error("设备注册失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("设备注册异常", e);
        }
    }

    /**
     * 设备心跳
     */
    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        try {
            clientSendCmd.keepalive();
            log.debug("发送心跳成功");
        } catch (Exception e) {
            log.error("发送心跳失败", e);
        }
    }
}
```

### 设备控制示例

```java
@Component
public class DeviceControlExample {

    @Autowired
    private ServerSendCmd serverSendCmd;

    /**
     * 云台控制
     */
    public void ptzControl(String deviceId, int command) {
        try {
            DeviceControlPtz ptzCmd = new DeviceControlPtz();
            ptzCmd.setDeviceId(deviceId);
            ptzCmd.setPtzCmd(command);

            ResultDTO result = serverSendCmd.deviceControl(ptzCmd);

            if (result.isSuccess()) {
                log.info("云台控制成功");
            } else {
                log.error("云台控制失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("云台控制异常", e);
        }
    }

    /**
     * 设备查询
     */
    public void queryDevice(String deviceId) {
        try {
            DeviceInfoQuery query = new DeviceInfoQuery();
            query.setDeviceId(deviceId);

            ResultDTO result = serverSendCmd.deviceInfoQuery(query);

            if (result.isSuccess()) {
                log.info("设备查询成功");
            } else {
                log.error("设备查询失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("设备查询异常", e);
        }
    }
}
```

### 实时点播示例

```java
@Component
public class VideoPlayExample {

    @Autowired
    private ServerSendCmd serverSendCmd;

    /**
     * 实时点播
     */
    public void invitePlay(String deviceId, String channelId) {
        try {
            InviteRequest inviteRequest = new InviteRequest();
            inviteRequest.setDeviceId(deviceId);
            inviteRequest.setChannelId(channelId);
            inviteRequest.setMediaServerId("zlm001");

            ResultDTO<InviteEntity> result = serverSendCmd.invitePlay(inviteRequest);

            if (result.isSuccess()) {
                InviteEntity invite = result.getData();
                log.info("点播成功, 流地址: {}", invite.getStreamUrl());
            } else {
                log.error("点播失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("点播异常", e);
        }
    }

    /**
     * 停止点播
     */
    public void stopPlay(String deviceId, String channelId) {
        try {
            ResultDTO result = serverSendCmd.byePlay(deviceId, channelId);

            if (result.isSuccess()) {
                log.info("停止点播成功");
            } else {
                log.error("停止点播失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("停止点播异常", e);
        }
    }
}
```

### 告警处理示例

```java
@Component
public class AlarmHandlingExample {

    @Autowired
    private ClientSendCmd clientSendCmd;

    /**
     * 发送告警
     */
    public void sendAlarm(String deviceId, String alarmType) {
        try {
            DeviceAlarm alarm = new DeviceAlarm();
            alarm.setDeviceId(deviceId);
            alarm.setAlarmType(alarmType);
            alarm.setAlarmTime(System.currentTimeMillis());
            alarm.setAlarmDescription("设备告警");

            ResultDTO result = clientSendCmd.alarm(alarm);

            if (result.isSuccess()) {
                log.info("告警发送成功");
            } else {
                log.error("告警发送失败: {}", result.getMsg());
            }
        } catch (Exception e) {
            log.error("告警发送异常", e);
        }
    }
}
```

## 🔧 开发指南

### 代码规范

#### Java代码规范

1. **命名约定**
    - 类名使用 PascalCase
    - 方法名和变量名使用 camelCase
    - 常量使用 UPPER_SNAKE_CASE
    - 包名使用小写，以 `io.github.lunasaw` 为根包

2. **Lombok使用**
    - 统一使用 `@Slf4j` 处理日志
    - 统一使用 `@Getter` 和 `@Setter` 处理访问器
    - 统一使用 `@AllArgsConstructor` 和 `@NoArgsConstructor` 处理构造方法
    - 统一使用 `@ToString` 处理字符串转换

3. **依赖注入**
    - 强制使用 `jakarta` 包而不是 `javax` 包
    - 使用 `@Autowired` 进行依赖注入
    - 使用 `@Component`、`@Service`、`@Repository` 等注解

4. **异常处理**
    - 使用 `ServiceException` 进行业务异常处理
    - 使用 `GlobalExceptionHandler` 进行全局异常处理
    - 自定义异常枚举 `ServiceExceptionEnum`

#### 时间处理规范

1. **DO/DTO层**：统一使用 `LocalDateTime` 类型
2. **VO层**：统一返回unix时间戳（毫秒级）
3. **转换方法**：提供 `fieldNameToEpochMilli()` 方法

#### JSON序列化规范

- 项目内统一使用 `fastjson2` 进行JSON序列化
- 禁止使用 `Jackson`、`Gson` 等其他JSON库

#### 外部接口规范

- 所有外部接口调用必须通过 `Wrapper` 类封装
- 统一返回 `ResultDTO` 格式
- 包含完整的异常处理和日志记录

### 扩展开发

#### 自定义消息处理器

```java
@Component
public class CustomMessageProcessor extends AbstractSipRequestProcessor {

    @Override
    public String getMethod() {
        return Request.MESSAGE;
    }

    @Override
    protected void processInternal(RequestEvent requestEvent) {
        // 自定义消息处理逻辑
        SIPRequest request = (SIPRequest) requestEvent.getRequest();

        // 解析消息体
        String body = new String(request.getRawContent());

        // 业务处理
        handleCustomMessage(body);

        // 发送响应
        sendResponse(requestEvent, Response.OK);
    }

    private void handleCustomMessage(String body) {
        // 实现自定义业务逻辑
        log.info("处理自定义消息: {}", body);
    }
}
```

#### 自定义设备提供器

```java
@Component
public class CustomDeviceSupplier implements DeviceSupplier {

    @Override
    public Device queryDevice(String deviceId) {
        // 从数据库或外部接口查询设备信息
        return deviceRepository.findByDeviceId(deviceId);
    }

    @Override
    public List<Device> queryDeviceList(String parentId) {
        // 查询设备列表
        return deviceRepository.findByParentId(parentId);
    }

    @Override
    public void updateDevice(Device device) {
        // 更新设备信息
        deviceRepository.save(device);
    }
}
```

#### 自定义缓存策略

```java
@Configuration
public class CustomCacheConfig {

    @Bean
    public CacheManager customCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats());
        return cacheManager;
    }
}
```

### 测试开发

#### 单元测试规范

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class DeviceControllerTest {

    @MockitoBean
    private DeviceService deviceService;

    @Autowired
    private DeviceController deviceController;

    @Test
    void testRegisterDevice() {
        // 准备测试数据
        Device device = new Device();
        device.setDeviceId("44050100001327000001");

        // Mock服务行为
        when(deviceService.register(any(Device.class)))
            .thenReturn(ResultDTOUtils.success());

        // 执行测试
        ResultDTO result = deviceController.register(device);

        // 验证结果
        assertTrue(result.isSuccess());
        verify(deviceService).register(device);
    }
}
```

#### 集成测试示例

```java
@SpringBootTest
@Testcontainers
class Gb28181IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private Gb28181Client client;

    @Autowired
    private Gb28181Server server;

    @Test
    void testDeviceRegistration() {
        // 启动服务端
        server.start();

        // 客户端注册
        ResultDTO result = client.register();

        // 验证注册成功
        assertTrue(result.isSuccess());

        // 验证设备状态
        assertTrue(client.isOnline());
    }
}
```

## 📊 性能优化

### 异步处理优化

```yaml
sip:
  performance:
    async:
      core-pool-size: 50
      max-pool-size: 200
      queue-capacity: 1000
      keep-alive: 60
```

### 缓存优化

```yaml
sip:
  performance:
    cache:
      initial-capacity: 100
      maximum-size: 10000
      expire-after-write: 3600
      expire-after-access: 1800
```

### 连接池优化

```yaml
sip:
  performance:
    pool:
      max-connections: 100
      min-connections: 10
      connection-timeout: 30
      idle-timeout: 300
```

## 🔍 监控和诊断

### 性能指标

项目集成了Micrometer，提供以下监控指标：

- **SIP消息处理指标**：请求数量、处理时间、成功率
- **设备状态指标**：在线设备数、注册成功率、心跳成功率
- **系统资源指标**：CPU使用率、内存使用率、线程池状态
- **缓存指标**：命中率、缓存大小、过期统计

### 健康检查

```java
@Component
public class SipHealthIndicator implements HealthIndicator {

    @Autowired
    private SipLayer sipLayer;

    @Override
    public Health health() {
        if (sipLayer.isRunning()) {
            return Health.up()
                .withDetail("sip-stack", "running")
                .withDetail("listening-points", sipLayer.getListeningPoints().size())
                .build();
        } else {
            return Health.down()
                .withDetail("sip-stack", "stopped")
                .build();
        }
    }
}
```

### 日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%d{HH:mm:ss}] [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/sip-proxy.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/sip-proxy.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>[%d{yyyy-MM-dd HH:mm:ss}] [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="io.github.lunasaw" level="INFO"/>
    <logger name="gov.nist.javax.sip" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

## 🤝 贡献指南

### 贡献流程

1. **Fork项目**：Fork本项目到您的GitHub账号
2. **创建分支**：创建功能分支 `feature/your-feature`
3. **提交更改**：提交您的更改到分支
4. **创建PR**：创建Pull Request到主分支
5. **代码审查**：等待代码审查和合并

### 代码提交规范

提交信息格式：`<type>(<scope>): <description>`

**类型说明：**

- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例：**

```
feat(client): 添加设备注册功能
fix(server): 修复心跳检测bug
docs(readme): 更新使用说明
```

### 代码质量要求

- 遵循项目代码规范
- 添加必要的单元测试
- 通过所有测试用例
- 添加适当的文档说明
- 使用统一的代码格式化工具

## 📋 更新日志

### v1.2.0 (2024-01-15)

**新增功能：**

- 支持Spring Boot 3.3.1
- 添加异步消息处理机制
- 集成Micrometer监控
- 添加缓存服务支持

**功能改进：**

- 优化SIP连接池管理
- 提升消息处理性能
- 完善错误处理机制
- 增强配置管理能力

**Bug修复：**

- 修复设备注册超时问题
- 解决心跳检测异常
- 修复内存泄漏问题

### v1.1.0 (2023-12-01)

**新增功能：**

- 支持GB28181-2016协议
- 添加设备控制功能
- 实现云台控制
- 支持视频回放

**功能改进：**

- 优化消息处理性能
- 完善异常处理机制
- 增强日志记录能力

## 📞 技术支持

### 问题反馈

- **GitHub Issues**: [提交问题](https://github.com/lunasaw/gb28181-proxy/issues)
- **讨论区**: [GitHub Discussions](https://github.com/lunasaw/gb28181-proxy/discussions)
- **邮件支持**: lunasaw@qq.com

### 技术交流

- **技术博客**: [www.isluna.ml](http://www.isluna.ml)
- **项目文档**: [在线文档](https://lunasaw.github.io/gb28181-proxy/)
- **API文档**: [API Reference](https://lunasaw.github.io/gb28181-proxy/api/)

## 🏆 致谢

感谢所有为本项目做出贡献的开发者！

### 核心贡献者

- [@lunasaw](https://github.com/lunasaw) - 项目创建者和维护者

### 特别感谢

- [JAIN-SIP](https://github.com/usnistgov/jsip) - 优秀的SIP协议栈实现
- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的应用框架
- [Caffeine](https://github.com/ben-manes/caffeine) - 高性能缓存库

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

---

<div align="center">
  <p>如果这个项目对您有帮助，请给我们一个 ⭐️ Star！</p>
  <p>Made with ❤️ by <a href="https://github.com/lunasaw">@lunasaw</a></p>
</div>

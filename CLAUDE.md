# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在处理此代码仓库时提供指导。

## 项目概述

SIP Proxy 是一个基于 Java 17 和 Spring Boot 3.3.1 构建的 GB28181-2016 通信框架。它是一个多模块 Maven
项目，为构建视频监控和安防系统提供完整的 SIP 协议通信能力。

## 构建和开发命令

### 构建命令

- `mvn clean compile` - 清理并编译所有模块
- `mvn clean package` - 将所有模块构建为 JAR 文件
- `mvn clean install` - 将模块安装到本地仓库

### 测试命令

- `mvn test` - 运行单元测试
- `mvn verify` - 运行集成测试 (failsafe 插件)
- `mvn test -Dspring.profiles.active=test` - 使用测试配置文件运行测试

### 运行命令

- `mvn spring-boot:run` - 运行 Spring Boot 应用程序
- 测试模块主类：`io.github.lunasaw.gbproxy.test.TestFrameworkVerifyApplication`

### 模块构建

- 可以使用以下命令构建单个模块：`mvn clean install -pl <module-name>`
- 构建特定模块：`mvn clean install -pl gb28181-test`

## 架构概述

### 模块结构

```
sip-proxy (父项目)
├── sip-common          - 核心 SIP 协议栈和工具
├── gb28181-common      - GB28181 协议数据模型  
├── gb28181-client      - GB28181 设备客户端实现
├── gb28181-server      - GB28181 平台服务器实现
└── gb28181-test        - 集成测试和示例
```

### 依赖层次结构

- `gb28181-client` 和 `gb28181-server` 依赖于 `sip-common` 和 `gb28181-common`
- `gb28181-test` 依赖于所有其他模块
- `sip-common` 是基础模块，没有内部依赖

### 核心技术栈

- **Java 17** (使用 Jakarta EE 包，不是 javax)
- **Spring Boot 3.3.1**
- **JAIN-SIP 1.3.0-91** - SIP 协议栈
- **Caffeine 3.1.8** - 高性能缓存
- **Micrometer 1.12.0** - 指标和监控
- **Maven** - 构建工具

## 关键架构模式

### SIP 消息处理流程

```
SIP Message → AbstractSipListener → XXXRequestProcessor → XXXRequestHandler → Business Logic
```

### 主动与被动服务

- **主动服务**：`ClientSendCmd`、`ServerSendCmd` - 发送出站 SIP 命令
- **被动处理**：`XXXRequestProcessor`、`XXXResponseProcessor` - 处理传入的 SIP 消息

### 消息处理层

1. **SIP 监听器层**：`AbstractSipListener` - 统一的 SIP 事件分发
2. **协议层**：`SipRequest` 处理 - 协议级处理
3. **消息类型层**：`XXXRequestProcessor` - 处理不同的 SIP 消息类型
4. **业务子类型层**：`XXXRequestSubProcessor` - 处理 MESSAGE 内的 cmdType 路由
5. **业务逻辑层**：`XXXRequestHandler` - 实际的业务实现

## 重要开发约定

### Java/Spring 要求

- **强制**：使用 `jakarta` 包而不是 `javax` (Spring Boot 3.x 要求)
- **强制**：在测试中使用 `@MockitoBean` 而不是已弃用的 `@MockBean`
- 使用 `@Slf4j` 进行日志记录，使用 Lombok 处理样板代码
- 遵循既定的处理器/处理程序分离模式

### SIP 协议特性

- 客户端和服务器模块在测试中有单独的设备配置
- SIP 请求处理需要强类型：在访问实现特定方法时将 `Request` 转换为 `SIPRequest`
- 异步 SIP 监听器必须在执行器线程中正确处理 TraceId 传播

### 测试模式

- 在消息发送测试之前设置 SIP 监听点：`sipLayer.setSipListener()` 和
  `sipLayer.addListeningPoint()`
- 在测试中使用单独的客户端/服务器设备配置以避免混淆
- 集成测试使用 TestContainers，包括单元测试和集成测试插件

## 关键接口和扩展点

### 设备管理

- `DeviceSupplier` - 提供设备信息的接口
- `ClientDeviceSupplier`、`ServerDeviceSupplier` - 模块特定的实现

### 消息处理扩展

- 为新的 SIP 消息类型扩展 `AbstractSipRequestProcessor`
- 为业务逻辑实现 `XXXProcessorHandler` 接口
- 服务器处理器扩展 `ServerAbstractSipResponseProcessor`
- 客户端处理器扩展 `ClientAbstractSipResponseProcessor`

### 配置

- 主配置：`application.yml`
- SIP 协议配置在 `sip:` 命名空间下
- GB28181 协议配置在 `gb28181:` 命名空间下
- 环境特定配置：`application-{env}.yml`

## 常见开发任务

### 添加新的 SIP 消息处理器

1. 创建扩展适当基类的 `XXXRequestProcessor`
2. 为业务逻辑创建 `XXXRequestHandler` 接口
3. 在处理程序中实现业务逻辑
4. 向 SIP 监听器注册处理器

### 测试 SIP 功能

1. 在 `@BeforeEach` 中设置测试监听点
2. 使用单独的客户端/服务器设备配置
3. 为实现特定方法将 `Request` 转换为 `SIPRequest`
4. 验证异步操作中的 TraceId 处理

### 性能优化

- 利用 Caffeine 缓存处理频繁访问的数据
- 使用具有适当 TraceId 管理的异步处理模式
- 使用 Micrometer 指标集成进行监控
# SIP Proxy

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/sip-proxy)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-common)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/gb28181-proxy/master/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Version](https://img.shields.io/badge/version-1.2.5-blue.svg)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-proxy)

[项目文档](https://lunasaw.github.io/gb28181-proxy/) | [问题反馈](https://github.com/lunasaw/gb28181-proxy/issues)

基于 Java 17 + Spring Boot 3.3.1 实现的 **SIP 协议代理框架**，以库的形式集成到业务代码中使用。

核心定位：**协议代理层**，屏蔽底层 SIP 协议细节，让业务代码专注于业务逻辑。框架同时支持 SIP 标准协议及其扩展协议（当前内置 GB28181-2016），业务系统可按需同时接入 `gb28181-client` 和 `gb28181-server`——既可作为平台服务端接收设备注册，也可作为客户端中转向上级平台注册，两种角色可在同一进程中共存。

## 典型使用场景

| 场景 | 引入模块 | 说明 |
|------|---------|------|
| 纯平台服务端（接收设备注册） | `gb28181-server` | 业务系统作为 GB28181 平台，管理下级设备 |
| 纯设备客户端（向平台注册） | `gb28181-client` | 业务系统模拟设备，向上级平台注册 |
| 级联代理（双角色共存） | `gb28181-client` + `gb28181-server` | 同时作为下级平台的服务端和上级平台的客户端，实现协议中转 |
| 自定义 SIP 扩展协议 | `sip-common` + 自定义模块 | 基于 SIP 协议栈实现非 GB28181 的私有扩展协议 |

## 模块结构

```
sip-proxy
├── sip-common          # SIP 协议栈封装（JAIN-SIP、监听器、缓存、指标）
├── gb28181-common      # GB28181 数据模型（JAXB XML 实体，无业务逻辑）
├── gb28181-client      # 设备客户端（ClientSendCmd、请求/响应处理器）
├── gb28181-server      # 平台服务端（ServerSendCmd、请求/响应处理器）
└── gb28181-test        # 集成测试和示例
```

依赖顺序：`sip-common` ← `gb28181-common` ← `gb28181-client` / `gb28181-server` ← `gb28181-test`

## 技术栈

| 技术          | 版本           |
|-------------|--------------|
| Java        | 17           |
| Spring Boot | 3.3.1        |
| JAIN-SIP    | 1.3.0-91     |
| Caffeine    | 3.1.8        |
| Micrometer  | 1.12.0       |
| Guava       | 32.1.3-jre   |
| Dom4j       | 2.1.4        |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+

### 引入依赖

```xml
<!-- SIP 基础包 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-common</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- GB28181 协议模型 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-common</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- GB28181 客户端（设备端） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- GB28181 服务端（平台端） -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>1.2.5</version>
</dependency>
```

### 基础配置

```yaml
sip:
  local:
    ip: 127.0.0.1
    port: 5060
    transport: UDP
    charset: UTF-8

gb28181:
  device:
    domain: 4405010000
    device-id: 44050100001327000001
    device-name: "测试设备"
  auth:
    enable: true
    realm: "4405010000"
    username: "admin"
    password: "12345678"
  timeout:
    register: 60
    heartbeat: 30
    invite: 30
```

## 架构说明

### SIP 消息处理流水线

```
SIP Message
  → AbstractSipListener          # 统一事件分发，TraceId 传播
  → XXXRequestProcessor          # 消息类型路由（REGISTER / INVITE / MESSAGE / NOTIFY / BYE …）
  → XXXRequestSubProcessor       # MESSAGE 子类型路由（按 GB28181 cmdType）
  → XXXRequestHandler            # 业务逻辑实现
```

### 主要扩展点

| 接口 / 基类 | 用途 |
|---|---|
| `ClientDeviceSupplier` | 客户端设备信息提供，继承 `DeviceSupplier` |
| `ServerDeviceSupplier` | 服务端设备信息提供，继承 `DeviceSupplier` |
| `AbstractSipRequestProcessor` | 新增入站消息类型处理器 |
| `ServerAbstractSipRequestProcessor` | 服务端请求处理器基类 |
| `ClientSendCmd` | 客户端主动命令发送（静态工具类） |
| `ServerSendCmd` | 服务端主动命令发送（静态工具类） |

### 命令发送示例

```java
// 服务端查询设备信息
String callId = ServerSendCmd.deviceInfo(fromDevice, toDevice);

// 服务端云台控制
String callId = ServerSendCmd.deviceControl(fromDevice, toDevice, ptzCmd);

// 客户端告警上报
String callId = ClientSendCmd.deviceAlarmNotify(fromDevice, toDevice, alarmNotify);

// 客户端心跳
String callId = ClientSendCmd.keepalive(fromDevice, toDevice, keepaliveNotify);
```

### 自定义设备提供器

```java
@Component
public class MyDeviceSupplier implements ClientDeviceSupplier {

    @Override
    public FromDevice getFromDevice(String userId) {
        // 返回本地设备信息
    }

    @Override
    public ToDevice getToDevice(String userId) {
        // 返回目标设备信息
    }
}
```

### 自定义消息处理器

```java
@Component
public class CustomMessageProcessor extends AbstractSipRequestProcessor {

    @Override
    public String getMethod() {
        return Request.MESSAGE;
    }

    @Override
    protected void processInternal(RequestEvent requestEvent) {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();
        String body = new String(request.getRawContent());
        // 业务处理 ...
        sendResponse(requestEvent, Response.OK);
    }
}
```

## 水平扩容

> 详细方案见 [doc/HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md)

### 设计约束

**本地节点只保留 SIP 事务状态，业务状态必须外化。**

| 状态类型 | 存储位置 | 说明 |
|---|---|---|
| `ServerTransaction` / `SipTransactionRegistry` | 进程内（不可外化） | SIP 协议约束，同一设备消息必须打同一节点 |
| `DeviceSessionCache` | 业务方实现（建议 Redis） | 设备注册信息，节点间共享 |
| `SipSubscribe` / `SubscribeHolder` | 进程内（建议改为 Redis） | 订阅状态，节点故障后需恢复 |

**违反此约束会导致节点故障时业务状态丢失且无法恢复。**

### 接入要求

业务方接入时必须：

1. **实现 `DeviceSessionCache` 接口**，使用 Redis 等共享存储，不得使用本地 Map
2. **实现 `ServerDeviceSupplier` / `ClientDeviceSupplier`**，设备信息从共享存储读取
3. **配置 `external-ip`**（NAT/虚拟机场景），确保 `Via`/`Contact` 头填对外可达地址

```yaml
sip:
  server:
    ip: 0.0.0.0           # 监听地址
    port: 5060
    external-ip: 1.2.3.4  # VIP 或公网地址，填入 Via/Contact（NAT 场景必填）
    external-port: 5060
```

### 部署拓扑

```
设备 ──→ VIP (keepalived + ipvs, 源IP哈希) ──→ Node-1
                                             └─→ Node-2
                                                  │
                                                Redis（共享 DeviceSessionCache）
```

## 构建与测试

```bash
# 编译
mvn clean compile

# 构建（含单元测试）
mvn clean install

# 集成测试
mvn verify

# 指定模块测试
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest

# 运行测试应用
# 主类：io.github.lunasaw.gbproxy.test.TestFrameworkVerifyApplication
```

> JaCoCo 强制要求行覆盖率 ≥ 80%。

## 开发规范

- 使用 `jakarta.*` 包，禁止 `javax.*`（Spring Boot 3.x 要求）
- 测试中使用 `@MockitoBean`，`@MockBean` 已废弃
- 访问 JAIN-SIP 实现特定方法时，将 `Request` 强转为 `SIPRequest`
- 异步线程中需显式传播 TraceId（SkyWalking）
- JSON 序列化统一使用 `fastjson2`

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>如果这个项目对您有帮助，请给我们一个 ⭐️ Star！</p>
  <p>Made with ❤️ by <a href="https://github.com/lunasaw">@lunasaw</a></p>
</div>

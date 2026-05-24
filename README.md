# SIP Proxy

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/sip-proxy)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-common)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/gb28181-proxy/master/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Version](https://img.shields.io/badge/version-1.2.5-blue.svg)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-proxy)

[项目文档](https://lunasaw.github.io/gb28181-proxy/) | [问题反馈](https://github.com/lunasaw/gb28181-proxy/issues)

基于 Java 17 + Spring Boot 3.3.1 实现的 **SIP 协议代理框架**，以 Maven 库的形式集成到业务进程中使用。

**核心定位**：协议层框架，屏蔽 SIP 协议细节，业务方通过 Spring Event 接收消息、通过 `CommandSender` 发送命令。框架内置 GB28181-2016 协议实现，业务系统可在同一进程中同时启用平台服务端（`gb28181-server`）和设备客户端（`gb28181-client`），支持级联代理场景。

## 模块结构

```
sip-proxy
├── sip-common          # SIP 协议栈封装（JAIN-SIP、监听器、事务注册表、缓存、指标）
├── gb28181-common      # GB28181 数据模型（JAXB XML 实体，无业务逻辑）
├── gb28181-client      # 设备客户端（ClientCommandSender、请求/响应处理器、客户端事件）
├── gb28181-server      # 平台服务端（ServerCommandSender、请求/响应处理器、服务端事件）
└── gb28181-test        # 集成测试和示例
```

依赖顺序：`sip-common` ← `gb28181-common` ← `gb28181-client` / `gb28181-server` ← `gb28181-test`

## 典型使用场景

| 场景 | 引入模块 | 说明 |
|------|---------|------|
| 平台服务端（接收设备注册） | `gb28181-server` | 业务系统作为 GB28181 平台，管理下级设备 |
| 设备客户端（向平台注册） | `gb28181-client` | 业务系统模拟设备，向上级平台注册 |
| 级联代理（双角色共存） | `gb28181-client` + `gb28181-server` | 同一进程同时作为下级平台的服务端和上级平台的客户端 |
| 自定义 SIP 扩展协议 | `sip-common` + 自定义模块 | 基于 SIP 协议栈实现非 GB28181 的私有协议 |

## 推荐接入架构

框架本身只做协议层。业务方在 sip-proxy 之上构建 **sip-gateway**（业务方实现的网关服务），sip-gateway 作为 SIP 协议层与业务服务器之间的桥梁：

```
┌──────────────────────────────────────────┐
│              业务服务器                     │
│  设备管理、录像、告警、流媒体调度等业务逻辑    │
│  调 sip-gateway HTTP API 触发 SIP 命令     │
└──────────────────┬───────────────────────┘
                   │ HTTP / MQ
┌──────────────────▼───────────────────────┐
│           sip-gateway（业务方实现）         │
│  引入 sip-proxy，与 sip-proxy 同一 JVM     │
│  ├── 实现 DeviceSessionCache → Redis      │
│  ├── 实现 ServerDeviceSupplier → Redis    │
│  ├── @EventListener → 推送业务服务器        │
│  └── 暴露 HTTP API → 接收业务指令           │
└──────────────────┬───────────────────────┘
                   │ Maven 依赖
┌──────────────────▼───────────────────────┐
│           sip-proxy（本框架）               │
│  解析 SIP 消息 → 发布 Spring Event          │
│  提供 ClientCommandSender / ServerCommandSender │
└──────────────────────────────────────────┘
```

详细方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md)。

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+

### 引入依赖

```xml
<!-- 平台服务端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>1.2.5</version>
</dependency>

<!-- 设备客户端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>1.2.5</version>
</dependency>
```

### 启用注解

```java
@SpringBootApplication
@EnableSipServer       // 平台服务端
// @EnableSipClient    // 设备客户端（级联场景两个一起加）
public class SipGatewayApplication { ... }
```

`@EnableSipServer` 自动激活 `SipProxyServerAutoConfig` + `Gb28181CommonAutoConfig` + `SipProxyAutoConfig`。

### 基础配置

```yaml
sip:
  server:
    ip: 0.0.0.0              # 监听地址（推荐 0.0.0.0）
    port: 5060
    external-ip: 1.2.3.4     # NAT/多节点场景填 VIP 或公网地址
    external-port: 5060      # 不填则 fallback 到 port
    serverId: 34020000002000000001
    realm: "34020000"
```

`external-ip` / `external-port` 会写入出站 SIP 包的 `Via` / `Contact` 头。多节点部署时填 VIP，确保设备后续消息能回到集群。不配置时 fallback 到 `ip` / `port`。

### 必须实现的接口

| 接口 | 用途 | 实现要求 |
|------|------|---------|
| `DeviceSessionCache` | 设备会话缓存（ip/port/transport） | **必须实现**，多节点部署必须用 Redis 等共享存储 |
| `ServerDeviceSupplier` | 服务端设备信息提供 | 启用 `@EnableSipServer` 时必须提供，建议读 Redis |
| `ClientDeviceSupplier` | 客户端设备信息提供 | 启用 `@EnableSipClient` 时必须提供 |

框架提供了 `DefaultServerDeviceSupplier` / `DefaultClientDeviceSupplier` 默认实现（基于配置文件），仅适用于单节点 demo 或测试场景，生产环境请自行实现。

## 架构说明

### SIP 消息处理流水线

```
SIP Message
  → AbstractSipListener          # 统一事件分发，TraceId 传播
  → XXXRequestProcessor          # 消息类型路由（REGISTER / INVITE / MESSAGE / NOTIFY / BYE …）
  → XXXSubProcessor              # MESSAGE 子类型路由（按 GB28181 cmdType）
  → Spring Event 发布             # 业务方通过 @EventListener 接收
```

业务方**只通过 Spring Event 接收消息**，无需关心协议解析。

### 命令发送

服务端用 `ServerCommandSender`，客户端用 `ClientCommandSender`，按 `deviceId` 寻址（依赖 `DeviceSessionCache`）：

```java
@Autowired ServerCommandSender serverCommandSender;

// 查询类
String callId = serverCommandSender.deviceInfoQuery(deviceId);
String callId = serverCommandSender.deviceCatalogQuery(deviceId);
String callId = serverCommandSender.deviceRecordInfoQuery(deviceId, startMs, endMs);

// 控制类
String callId = serverCommandSender.deviceControlPtzCmd(deviceId, PtzCmdEnum.UP, 50);
String callId = serverCommandSender.deviceControlReboot(deviceId);

// 点播 / 回放
String callId = serverCommandSender.deviceInvitePlay(deviceId, mediaIp, mediaPort, StreamModeEnum.UDP);
String callId = serverCommandSender.deviceInvitePlayBack(deviceId, mediaIp, mediaPort, startMs, endMs, StreamModeEnum.UDP);

// 终止会话
String callId = serverCommandSender.deviceBye(deviceId, originalCallId);
```

### 事件监听

业务逻辑通过 Spring `@EventListener` 实现。常用事件：

**服务端事件**（`io.github.lunasaw.gbproxy.server.transmit.event`）

| 事件 | 触发时机 |
|------|---------|
| `DeviceRegisterEvent` | 设备注册（含鉴权前） |
| `DeviceOnlineEvent` / `DeviceOfflineEvent` | 设备上线/下线 |
| `DeviceKeepaliveEvent` | 设备心跳 |
| `DeviceInfoEvent` / `DeviceStatusEvent` / `DeviceCatalogEvent` | 设备查询结果 |
| `DeviceAlarmEvent` | 设备告警 |
| `DeviceInviteTryingEvent` / `DeviceInviteOkEvent` / `DeviceInviteFailureEvent` | 点播响应 |
| `DeviceMobilePositionEvent` | 设备位置上报 |
| `DeviceByeEvent` | 设备主动结束会话 |

**客户端事件**（`io.github.lunasaw.gbproxy.client.eventbus.event`）

| 事件 | 触发时机 |
|------|---------|
| `ClientRegisterSuccessEvent` / `ClientRegisterFailureEvent` / `ClientRegisterChallengeEvent` | 客户端注册响应 |
| `ClientInviteEvent` | 收到上级 INVITE（含 `transactionContextKey`，业务方异步回包用） |
| `ClientAckEvent` / `ClientByeEvent` / `ClientCancelEvent` | 上级 ACK/BYE/CANCEL |

```java
@Component
@Slf4j
public class SipEventHandler {

    @EventListener
    public void onRegister(DeviceRegisterEvent e) {
        log.info("设备注册: {}", e.getDeviceId());
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent e) {
        log.info("设备告警: {} type={}", e.getDeviceId(), e.getAlarmType());
    }

    @EventListener
    public void onInviteOk(DeviceInviteOkEvent e) {
        // 平台主动 INVITE 收到 200 OK，含设备 SDP
        log.info("点播建立: callId={}", e.getCallId());
    }
}
```

### 扩展点（进阶）

业务方一般只需实现 `DeviceSessionCache` / `DeviceSupplier` 接口。少数高级场景才需要扩展协议层：

| 扩展点 | 用途 |
|--------|------|
| `MessageHandler` 接口 | 新增 GB28181 cmdType 处理器，通过 `SipRequestProcessorAbstract.addHandler()` 注册 |
| `SipRequestProcessorAbstract` | 新增 SIP method 处理器（如自定义非标准方法），子类需声明 `private String method = "..."` 字段；server 端可继承 `ServerAbstractSipRequestProcessor` |
| `ClientCommandStrategy` / `ServerCommandStrategy` | 新增出站命令策略，通过对应的 `CommandStrategyFactory` 注册 |

## 水平扩容

完整方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md) 与 [doc/HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md)。

### 状态分层

**本地节点只保留 SIP 事务状态，业务状态必须外化。**

| 状态类型 | 存储位置 | 说明 |
|---------|---------|------|
| `ServerTransaction` / `SipTransactionRegistry` | 进程内（不可外化） | SIP 协议约束，同一设备消息必须打到同一节点 |
| `DeviceSessionCache`（设备注册信息） | Redis（共享） | 业务方实现，节点间共享，节点故障后新节点可接管 |
| `ServerDeviceSupplier`（设备信息） | Redis（共享） | 业务方实现，读 Redis |
| `SipSubscribe` / `SubscribeHolder`（订阅状态） | 进程内（建议改为 Redis） | 节点故障后需恢复 |
| INVITE 事务上下文 key | 进程内 + Redis 存节点映射 | 异步回包跨节点路由用 |

违反此约束会导致节点故障时业务状态丢失且无法恢复。

### 部署拓扑

```
设备 ──→ VIP 1.2.3.4:5060 ──→ Node-1 (sip-gateway + sip-proxy)
         (keepalived + ipvs) └→ Node-2 (sip-gateway + sip-proxy)
         源 IP 哈希                    │
                                    Redis（共享 DeviceSessionCache）
                                    业务服务器
```

- VIP 四层透明转发，按**源 IP 哈希**保证同一设备打到同一节点
- 节点故障时 keepalived 自动摘除，设备重新注册分到存活节点
- 扩容粒度是**设备**，单设备并发瓶颈不会随扩容缓解（GB28181 场景足够）

### 接入要求

业务方多节点部署时必须：

1. 实现 `DeviceSessionCache`，使用 Redis 等共享存储（不得用本地 Map）
2. 实现 `ServerDeviceSupplier`，设备信息从共享存储读取
3. 配置 `external-ip` 为 VIP，确保 SIP 包内 `Via` / `Contact` 头是集群可达地址
4. 处理 INVITE 异步回包跨节点路由（见 [LAYERED-ARCHITECTURE.md §5.3](doc/LAYERED-ARCHITECTURE.md)）

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

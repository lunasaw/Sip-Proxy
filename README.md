# SIP Proxy

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/sip-proxy)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-common)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/gb28181-proxy/master/LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Version](https://img.shields.io/badge/version-1.3.0-blue.svg)](https://mvnrepository.com/artifact/io.github.lunasaw/sip-proxy)

[项目文档](https://lunasaw.github.io/gb28181-proxy/) | [问题反馈](https://github.com/lunasaw/gb28181-proxy/issues) | [CHANGELOG](CHANGELOG.md)

基于 Java 17 + Spring Boot 3.3.1 实现的 **SIP 协议代理框架**，以 Maven 库的形式集成到业务进程中使用。

**核心定位**：纯协议层框架，屏蔽 SIP 协议细节。业务方通过 Spring `@EventListener` 接收消息、通过 `CommandSender` 发送命令，**不直接接触 JAIN-SIP**。框架内置 GB28181-2016 协议实现，单 JVM 可同时启用平台服务端（`gb28181-server`）和设备客户端（`gb28181-client`），支持级联代理场景。

> **1.3.0 重大变更**：全量删除 `*Handler` 接口，统一为 Spring Event 总线；INVITE 改为异步处理（100 Trying + 事件 + 异步回包）；`sip-common` 与 GB28181 协议解耦。详见 [CHANGELOG.md](CHANGELOG.md) 和 [doc/BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md](doc/BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md)。

## 模块结构

```
sip-proxy
├── sip-common          # 通用 SIP 协议栈��JAIN-SIP 封装、事务注册表、缓存、指标）；不含任何 GB28181 代码
├── gb28181-common      # GB28181 数据模型 + GB SDP 工具（JAXB XML 实体，无业务逻辑）
├── gb28181-client      # 设备客户端：ClientCommandSender、入站请求/响应处理器、Client*Event
├── gb28181-server      # 平台服务端：ServerCommandSender、入站请求/响应处理器、Device*Event / ServerInviteEvent
└── gb28181-test        # 集成测试 + sip-gateway 业务侧单机参考实现
```

依赖顺序：`sip-common` ← `gb28181-common` ← `gb28181-client` / `gb28181-server` ← `gb28181-test`

> ⚠️ **`sip-common` 协议纯净性**：不允许出现任何 GB28181 关键词（`gb28181 / GB28181 / gbproxy / Catalog / MobilePosition / GbSession / GbSip / GbUtil`）。CI 通过 [`scripts/check-sip-common-purity.sh`](scripts/check-sip-common-purity.sh) 强制校验。GB28181 相关逻辑请下沉至 `gb28181-common`（如 `GbSdpUtils`、`GbUtil`）。详见 [doc/PROTOCOL-DECOUPLING-PLAN.md](doc/PROTOCOL-DECOUPLING-PLAN.md)。

## 整体分层

sip-proxy 不是独立服务，而是嵌入到业务方实现的 **sip-gateway** 网关进程中。三层架构如下：

```
┌─────────────────────────────────────────────────────────┐
│                     业务服务器                            │
│  设备管理、录像、告警、流媒体调度等业务逻辑                  │
│  调 sip-gateway HTTP/MQ 接口触发 SIP 命令                 │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP / MQ
┌──────────────────────────▼──────────────────────────────┐
│                     sip-gateway（业务方实现）              │
│  Spring Boot 应用，多节点部署，与 sip-proxy 同 JVM         │
│  ├── 实现 DeviceSessionCache  → Redis（共享）             │
│  ├── 实现 ServerDeviceSupplier → Redis（共享）            │
│  ├── @EventListener → 推送业务服务器（HTTP / MQ）          │
│  └── 暴露 HTTP API → 接收业务指令调 ServerCommandSender    │
└──────────────────────────┬──────────────────────────────┘
                           │ Maven 依赖
┌──────────────────────────▼──────────────────────────────┐
│                     sip-proxy（本框架）                   │
│  解析 SIP 消息 → 发布 Spring Event                        │
│  提供 ClientCommandSender / ServerCommandSender           │
└─────────────────────────────────────────────────────────┘
```

**为什么 sip-proxy 必须与 sip-gateway 同 JVM**：`SipTransactionRegistry` 持有的 `ServerTransaction` 是 JAIN-SIP 实现类（`SIPServerTransactionImpl`），不可序列化；`RequestEvent` 内部持有 socket 引用（`SipProvider`、`Dialog`），跨进程后无法回包。完整方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md)。

> **参考实现**：[`gb28181-test/.../gateway/`](gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 提供了 sip-gateway 单机版完整实现（`GatewayProperties` / `InviteContextStore` / `SipEventForwarder` / `SipCommandController` / `BusinessNotifier`），生产部署需将 `InMemoryInviteContextStore` 替换为 Redis 实现、`NoopBusinessNotifier` 替换为实际 HTTP/MQ 推送、`nodeAddressMap` 替换为 K8s/Nacos 动态发现。

## 典型使用场景

| 场景 | 引入模块 | 说明 |
|------|---------|------|
| 平台服务端（接收设备注册） | `gb28181-server` | 业务系统作为 GB28181 平台，管理下级设备 |
| 设备客户端（向平台注册） | `gb28181-client` | 业务系统模拟设备，向上级平台注册 |
| 级联代理（双角色共存） | `gb28181-client` + `gb28181-server` | 同一进程同时作为下级平台的服务端和上级平台的客户端 |
| 自定义 SIP 扩展协议 | `sip-common` + 自定义模块 | 基于 SIP 协议栈实现非 GB28181 的私有协议 |

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- Redis（多节点部署必需，单机演示可用 `InMemoryInviteContextStore`）

### 引入依赖

```xml
<!-- 平台服务端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-server</artifactId>
    <version>1.3.0</version>
</dependency>

<!-- 设备客户端 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-client</artifactId>
    <version>1.3.0</version>
</dependency>
```

### 启用注解

```java
@SpringBootApplication
@EnableSipServer       // 平台服务端
// @EnableSipClient    // 设备客户端（级联场景两个一起加）
public class SipGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SipGatewayApplication.class, args);
    }
}
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
  common:
    user-agent: sip-proxy    # 1.3.0 默认值（旧值 LunaSaw-GB28181-Proxy 需显式配置保留）
    time-sync:
      enabled: false         # 1.3.0 配置 key 由 sip.gb28181.time-sync.* 改名
```

`external-ip` / `external-port` 会写入出站 SIP 包的 `Via` / `Contact` 头。多节点部署时填 VIP，确保设备后续消息能回到集群。

### 必须实现的接口

业务方（sip-gateway）至少要实现以下三个接口，框架不提供生产可用的默认实现：

| 接口 | 用途 | 实现要求 |
|------|------|---------|
| `DeviceSessionCache` | 设备会话寻址（ip / port / transport） | **多节点部署必须用 Redis 等共享存储**，框架默认实现仅适用单机演示 |
| `ServerDeviceSupplier` | 服务端设备身份 + 注册鉴权 | 启用 `@EnableSipServer` 必须实现；`authenticate(userId, SIPRequest)` 默认调 `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword` 完成摘要校验 |
| `ClientDeviceSupplier` | 客户端设备身份 | 启用 `@EnableSipClient` 必须实现 |

框架提供 `DefaultServerDeviceSupplier` / `DefaultClientDeviceSupplier`（基于配置文件），仅适用于单节点 demo 或测试场景。

## 架构说明

### SIP 消息处理流水线

```
SIP Message
  → AbstractSipListener            # 统一事件分发，TraceId 传播
  → XXXRequestProcessor            # 消息类型路由（REGISTER / INVITE / MESSAGE / NOTIFY / BYE …）
  → XXXSubProcessor                # MESSAGE 子类型路由（按 GB28181 cmdType）
  → Spring ApplicationEvent 发布   # 业务方通过 @EventListener 接收
```

业务方**只通过 Spring Event 接收消息**，无需关心协议解析。1.3.0 起所有 `*Handler` 接口已删除，统一事件总线模式。

### 命令发送

服务端用 `ServerCommandSender`，客户端用 `ClientCommandSender`。`ServerCommandSender` 按 `deviceId` 寻址，依赖 `DeviceSessionCache`：

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

业务逻辑通过 Spring `@EventListener` 实现。

**服务端事件**（`io.github.lunasaw.gbproxy.server.transmit.event`）：

| 事件 | 触发时机 |
|------|---------|
| `DeviceRegisterEvent` / `DeviceRegisterChallengeEvent` | 设备注册（含鉴权挑战） |
| `DeviceOnlineEvent` / `DeviceOfflineEvent` | 设备上线 / 下线 |
| `DeviceKeepaliveEvent` | 设备心跳 |
| `DeviceInfoEvent` / `DeviceStatusEvent` / `DeviceCatalogEvent` / `DeviceConfigEvent` / `DeviceRecordEvent` | 设备查询结果 |
| `DeviceAlarmEvent` / `DeviceMobilePositionEvent` / `DeviceNotifyUpdateEvent` | 设备主动上报 |
| `DeviceInviteTryingEvent` / `DeviceInviteOkEvent` / `DeviceInviteFailureEvent` | 平台发起 INVITE 的响应 |
| **`ServerInviteEvent`** | **设备主动 INVITE（含 `transactionContextKey`，用于异步回包）** |
| `DeviceMediaStatusEvent` / `DeviceByeEvent` / `DeviceAckEvent` | 媒体会话状态变更 |
| `DeviceInfoRequestEvent` / `DeviceSubscribeResponseEvent` | 设备 INFO / 订阅响应 |

**客户端事件**（`io.github.lunasaw.gbproxy.client.eventbus.event`）：

| 事件 | 触发时机 |
|------|---------|
| `ClientRegisterSuccessEvent` / `ClientRegisterFailureEvent` / `ClientRegisterChallengeEvent` | 客户端注册响应 |
| `ClientInviteEvent` | 收到上级 INVITE（含 `transactionContextKey`，用于异步回包） |
| `ClientAckEvent` / `ClientByeEvent` / `ClientCancelEvent` / `ClientInfoEvent` | 上级 ACK / BYE / CANCEL / INFO |

```java
@Component
@Slf4j
public class SipEventForwarder {

    @EventListener
    public void onRegister(DeviceRegisterEvent e) {
        log.info("设备注册: {}", e.getDeviceId());
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent e) {
        log.info("设备告警: {} type={}", e.getDeviceId(), e.getAlarmType());
    }

    @EventListener
    public void onServerInvite(ServerInviteEvent e) {
        // 设备主动 INVITE（语音对讲等场景）：异步回包，详见下文
        log.info("设备 INVITE: callId={} ctxKey={}", e.getCallId(), e.getTransactionContextKey());
    }
}
```

### INVITE 异步回包模型（1.3.0 关键变更）

设备主动发起的 INVITE（如语音对讲）需要业务方准备 SDP，框架已重构为**两步异步**：

```
设备 → INVITE
  → sip-proxy: ServerInviteRequestProcessor
      1. 立即发 100 Trying（防对端重传）
      2. 存 SipTransactionRegistry（contextKey = callId_fromTag_cseq → RequestEvent，进程内）
      3. 发布 ServerInviteEvent（含 callId、contextKey、SDP）
  → sip-gateway: @EventListener
      1. 存 Redis: "sip:invite:ctx:{callId}" → "{nodeId}:{contextKey}"（30s TTL）
      2. 推送业务服务器（含 callId、SDP）

业务服务器 → POST /sip/invite/response {callId, sdp}
  → sip-gateway:
      1. 从 Redis 取 "{nodeId}:{contextKey}"
      2. nodeId == 本节点：用 contextKey 取 SipTransactionRegistry 回包
         nodeId != 本节点：通过 nodeAddressMap 转发 HTTP 到对应节点
      3. ResponseCmd.sendResponse(200, sdp, ctx.getOriginalEvent())
```

> ⚠️ **设备 Timer B 限制**：即使框架侧 `extendContext` 续期到 90 秒，设备侧（INVITE 客户端）按 RFC 3261 §17.1.1.2 在 `Timer B = 64*T1 = 32s` 后会放弃事务。业务处理时间应控制在 **30s 内直接回包**；30~60s 需主动发 `180 Ringing` + `extendContext`；> 60s 改为先回 200 OK + 占位 SDP，后续走 re-INVITE。详见 [doc/LAYERED-ARCHITECTURE.md §7](doc/LAYERED-ARCHITECTURE.md)。

### 扩展点（进阶）

业务方一般只需实现 `DeviceSessionCache` / `*DeviceSupplier` 接口。少数高级场景才需要扩展协议层：

| 扩展点 | 用途 |
|--------|------|
| `MessageHandler` 接口 | 新增 GB28181 cmdType 处理器，通过 `SipRequestProcessorAbstract.addHandler()` 注册 |
| `SipRequestProcessorAbstract` 子类 | 新增 SIP method 处理器（如自定义非标准方法）；server 端可继承 `ServerAbstractSipRequestProcessor` |
| `ClientCommandStrategy` / `ServerCommandStrategy` | 新增出站命令策略，通过对应的 `CommandStrategyFactory` 注册 |

## 水平扩容

完整方案见 [doc/LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md) 与 [doc/HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md)。

### 状态分层（核心约束）

**本地节点只保留 SIP 事务状态，业务状态必须外化。**

| 状态类型 | 存储位置 | 说明 |
|---------|---------|------|
| `ServerTransaction` / `SipTransactionRegistry` | **进程内**（不可外化） | JAIN-SIP 实现类不可序列化、持有 socket 引用；同设备消息必须打同节点 |
| `DeviceSessionCache`（设备注册信息） | **Redis**（共享，需高可用） | 业务方实现，节点间共享，节点故障后新节点可接管 |
| `ServerDeviceSupplier`（设备信息） | **Redis**（共享，需高可用） | 业务方实现，读 Redis |
| 设备订阅状态 | 业务方自管 | 框架 1.3.0 已删除 `SubscribeHolder`，由业务方按需自管 |
| INVITE 事务上下文 | **进程内** + Redis 存路由映射（需高可用） | `transactionContextKey` 仅在收到 INVITE 的节点有效；Redis 用 `callId` 作键存 `{nodeId}:{contextKey}` 供跨节点回包路由 |

> ⚠️ **Redis 是新的 SPOF**：跨节点 INVITE 路由、设备会话、注册鉴权全部依赖 Redis。生产环境必须使用 Redis Sentinel 或 Cluster；`InviteContextStore` 实现需把后端故障显式抛 `ResponseStatusException(SERVICE_UNAVAILABLE)`，让 `/sip/invite/response` 返回 503 触发业务侧重试。

### 部署拓扑

```
设备 ──→ VIP 1.2.3.4:5060 ──→ Node-1 (sip-gateway + sip-proxy)
         (keepalived + ipvs) └→ Node-2 (sip-gateway + sip-proxy)
         源 IP 哈希                    │
                                    Redis（共享 DeviceSessionCache + InviteContextStore）
                                    业务服务器
```

- VIP 四层透明转发，按**源 IP 哈希**保证同一设备打到同一节点
- 节点故障时 keepalived 自动摘除，设备重新注册分到存活节点
- 扩容粒度是 **NAT 出口**而非物理设备数。共享 NAT 的设备会全部落到同一节点，规划容量时按 NAT 出口数估算

### 接入要求

业务方多节点部署时必须：

1. 实现 `DeviceSessionCache`，使用 Redis 等共享存储（不得用本地 Map）
2. 实现 `ServerDeviceSupplier`，设备信息从共享存储读取
3. 配置 `external-ip` 为 VIP，确保 SIP 包内 `Via` / `Contact` 头是集群可达地址
4. 实现 `InviteContextStore`（参考 [`InMemoryInviteContextStore`](gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InMemoryInviteContextStore.java) 改 Redis 版），处理 INVITE 异步回包跨节点路由
5. 装配 `nodeAddressMap` Bean（K8s Endpoints / Nacos / Consul），将 `nodeId` 映射到内网地址

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

# 指定测试方法
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest#methodName

# 协议纯净性校验（CI 在 mvn verify 阶段自动调用）
bash scripts/check-sip-common-purity.sh
```

> JaCoCo 强制要求行覆盖率 ≥ 80%。

## 开发规范

- 使用 `jakarta.*` 包，禁止 `javax.*`（Spring Boot 3.x 要求）
- 测试中使用 `@MockitoBean`，`@MockBean` 已废弃
- 访问 JAIN-SIP 实现特定方法时，将 `Request` 强转为 `SIPRequest`
- 异步线程中需显式传播 TraceId（SkyWalking）
- JSON 序列化统一使用 `fastjson2`
- `sip-common` 中禁止出现 GB28181 关键词（CI 校验，详见模块结构小节）

## 配置命名空间

- `sip.server.*` — SIP 协议监听设置（`ip` / `port` / `external-ip` / `external-port` / `serverId` / `realm`）
- `sip.common.*` — 通用框架配置（`user-agent`、`time-sync.*`）
- `gb28181:` — GB28181 协议设置
- 环境覆盖：`application-{env}.yml`

> **1.3.0 配置迁移**：`sip.gb28181.time-sync.*` → `sip.common.time-sync.*`；默认 `User-Agent` 由 `LunaSaw-GB28181-Proxy` 改为 `sip-proxy`。

## 参考文档

`doc/` 目录下的关键文档：

| 文档 | 内容 |
|------|------|
| [LAYERED-ARCHITECTURE.md](doc/LAYERED-ARCHITECTURE.md) | sip-proxy ↔ sip-gateway ↔ 业务服务器分层架构（v2.5） |
| [HORIZONTAL-SCALING.md](doc/HORIZONTAL-SCALING.md) | 多节点部署、状态分层、VIP 拓扑、NAT 处理 |
| [PROTOCOL-DECOUPLING-PLAN.md](doc/PROTOCOL-DECOUPLING-PLAN.md) | sip-common / gb28181-common 边界规则（1.3.0） |
| [BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md](doc/BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md) | 1.3.0 全量删除 `*Handler` 接口、统一 Spring Event |
| [INVITE-REFACTOR-PLAN.md](doc/INVITE-REFACTOR-PLAN.md) | INVITE 异步化重构（1.3.0） |
| [GB28181-2016.md](doc/GB28181-2016.md) / [GBT-28181-2022.md](doc/GBT-28181-2022.md) | 协议参考 |
| [CHANGELOG.md](CHANGELOG.md) | 各版本对外可见变更 |

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>如果这个项目对您有帮助，请给我们一个 ⭐️ Star！</p>
  <p>Made with ❤️ by <a href="https://github.com/lunasaw">@lunasaw</a></p>
</div>

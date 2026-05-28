# gateway-gb28181 模块化方案（1.8.0 执行手册）

> 版本：2.0 草案 | 日期：2026-05-28 | 关联：[SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md)（**主纲领**）、[UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md)、[LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) v2.5、[OUTBOUND-DIALOG-PLAN.md](../1.7.0/OUTBOUND-DIALOG-PLAN.md) v1.2、[HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)
>
> **本文档承担 1.8.0 代码迁移的执行手册**：把现已落在 [gb28181-test/.../gateway/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 的"业务方网关"参考实现，按 [SIP-GATEWAY-AGGREGATION-PLAN](SIP-GATEWAY-AGGREGATION-PLAN.md) 决定的父聚合形态升级为正式 Maven 子模块 `gateway-gb28181`，并配合 envelope 协议化（[UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md)）一并落地。
>
> **v1.2 → v2.0 变更（与父聚合方案对齐）**：
> - 模块名从单一的 `gb28181-gateway` 改为父聚合 `sip-gateway/` 下的子模块 `gateway-gb28181`，业务方接入入口为 `sip-gateway-spring-boot-starter`
> - 包名从 `io.github.lunasaw.gbproxy.gateway.*` 改为 `io.github.lunasaw.sipgateway.gb28181.*`
> - 配置前缀从 `gateway.*` / `gb28181.gateway.*` 改为 `gateway.gb28181.*`（核心节点配置 `gateway.*` 由 `gateway-core` 统一）
> - HTTP 路径从 `/sip/*` 改为 `/gateway/gb28181/*`（协议特殊端点） + `/gateway/command`（gateway-core 协议中立分发，envelope 形态见 [UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md)）
> - `BusinessNotifier` 接口从 3 方法改为单方法 `notify(GatewayEvent)`，envelope 化
> - `InviteContextStore` 改为 `TransactionContextStore<String, InviteContext>` 的具体化
> - 新增 `Gb28181Module implements ProtocolModule` 自报命名空间
>
> **v1.2 历史变更（保留）**：拆分为两步走 —— 先发布 **1.7.3**（`ServerSessionEvent` / `DeviceSessionListener` 增 `rawSdp` 字段，协议层小升级），再发布 **1.8.0**（gateway 父聚合 + envelope 协议化 + GB28181 适配器上线），降低单次破坏性变更范围。

---

## 一、可行性评估

### 1.1 协议层前置条件

LAYERED-ARCHITECTURE.md §8 描述的框架层改造已在 v1.3.0 ~ v1.7.0 大部分完成，gateway 模块站在这些能力之上。**唯一遗留的小协议层改动**是在 1.7.3 补齐 `rawSdp` 透传，避免业务方拿到 INVITE 后无法把原始 SDP 转给 ZLM/SRS 推流：

| 协议层能力 | 状态 | 模块 | 备注 |
|-----------|------|------|------|
| `@EnableSipServer` / `@EnableSipClient` | ✅ 1.3.0 | gb28181-server / gb28181-client | gateway 通过 `@EnableSipServer` 嵌入即可 |
| `SipTransactionRegistry` + `extendContext` | ✅ 1.3.0 | sip-common | INVITE 异步回包基础设施 |
| `ServerInviteEvent`（含 `transactionContextKey`） | ✅ 1.3.0 | gb28181-server | 异步回包入口 |
| `ServerDeviceSupplier.authenticate(userId, SIPRequest)` | ✅ 1.3.0 | sip-common | 注册鉴权下沉 |
| Listener 接口分层（`DeviceLifecycleListener` 等 4 个） | ✅ 1.5.0 | gb28181-server | gateway forwarder 直接 implements |
| `DialogRegistry` + `deviceBye(callId)` | ✅ 1.7.0 | sip-common / gb28181-server | gateway HTTP API 已对齐 |
| `BYE 200 OK` 协议合规 | ✅ 1.3.0 | gb28181-server | RFC 3261 §15.1.2 |
| **`ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)`** | 🚧 **1.7.3 待发** | gb28181-server | 见 §3.3.1，业务侧透传 SDP 给流媒体节点的必备字段 |

**结论**：协议层只剩一个小字段补齐(1.7.3)，随后 gateway 模块化是纯包装/打包工作(1.8.0)，不再引入新协议层概念。

### 1.2 现有参考实现盘点

[gb28181-test/.../gateway/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 已包含 9 个顶层 Java 文件 + 5 个 DTO（共 14 个）：

```
gateway/
├── GatewayProperties.java          # @ConfigurationProperties("gateway")
├── GatewayConfig.java              # @Configuration + 3 个 @ConditionalOnMissingBean
├── InviteContextStore.java         # 跨节点 INVITE 上下文存储接口
├── InMemoryInviteContextStore.java # Caffeine 单机版默认实现
├── BusinessNotifier.java           # 业务侧推送抽象
├── NoopBusinessNotifier.java       # 默认日志实现
├── SipEventForwarder.java          # 实现 3 个 listener，转发到 BusinessNotifier
├── SipCommandController.java       # @RestController 暴露 6 个 HTTP API
└── dto/
    ├── ByeRequest.java
    ├── CatalogQueryRequest.java
    ├── InviteResponseRequest.java
    ├── InviteStartRequest.java
    └── PtzRequest.java
```

行数 541（实测 `wc -l`），全部已包含中文 javadoc、错误语义约定（503/410/502）、UDP INVITE 重传幂等、跨节点路由兜底。代码质量等同正式模块，**只是寄居错位**。

### 1.3 模块化的收益

| 维度 | 现状（参考实现） | 模块化后 |
|------|-----------------|---------|
| 接入成本 | 业务方 clone test 模块 → 复制 10 个文件 → 改包名 → 修依赖 | `<dependency>gb28181-gateway</dependency>` + 配置 3 个属性 |
| 升级路径 | 框架更新后业务方手工同步代码 | Maven 版本升级一行 |
| 测试覆盖 | 在 gb28181-test 内做单元测试 | 独立模块独立测试，可加集成测试套 |
| API 稳定性 | 测试代码语义"参考"，业务方不敢深度依赖 | 正式 API + SemVer + CHANGELOG |
| 默认实现替换 | 业务方需理解整个流程才能扩展 | `@ConditionalOnMissingBean` + Spring Boot autoconfig，明确扩展点 |
| 多节点支持 | 内存版 store + 单机示例 | 模块内置内存版，扩展模块按需提供 Redis/MQ 实现 |

### 1.4 风险与边界

**已识别的风险点**：

1. **Spring Web 强依赖** — `SipCommandController` 是 `@RestController`，模块默认引入 `spring-boot-starter-web` 会污染只想用事件总线的业务方。**对策**：用 `@ConditionalOnClass(RestController.class)` + `@ConditionalOnWebApplication` 守门，HTTP API 单独成 `WebMvcConfiguration`，用户不引 web starter 自动跳过。
2. **Caffeine 依赖** — `SipEventForwarder` 用 Caffeine 做 INVITE 重传幂等。Caffeine 已是父 pom `dependencyManagement` 成员，模块直接引入风险可控。
3. **业务侧推送实现选择** — `BusinessNotifier` 接口约束了"必须异步推送"，但默认提供 Noop 实现。HTTP/MQ 实现各家差异大，**不内置具体实现**，仅在文档给出 RestTemplate / Spring Cloud Stream 模板代码。
4. **多节点 store 实现** — 生产环境必须 Redis 实现 `InviteContextStore`。**主模块只提供内存版**；Redis 实现作为可选扩展模块（见 §3.5）或文档示例，避免主模块强依赖 lettuce/jedis。
5. **`nodeAddressMap` 装配** — 主模块只提供 `GatewayProperties.nodes` 静态配置版本，K8s/Nacos 动态发现由业务方自行装配同名 Bean 覆盖。
6. **`DeviceSessionCache` 默认实现** — 现 `SipBusinessConfig` 是测试代码，写在业务包内。模块化后**不提供默认实现**，强制业务方提供——因为 NAT IP 切换的脏数据策略、TTL、Redis key 结构都强业务相关，给默认实现反而误导。文档明确这是"必须实现的接口"。

**模块边界（不做什么）**：

- 不内置 Redis 客户端、不内置 RestTemplate/WebClient 配置（业务方 starter 提供）
- 不实现具体的 MQ 推送（Kafka / RocketMQ / RabbitMQ 各家差异大）
- 不解决"业务服务器"侧的代码（业务服务器是另一个进程，与本模块无关）
- 不替代 sip-gateway 的"网关进程"形态——业务方仍可选择把 gateway 模块嵌到自己的应用里，不强制独立进程

### 1.5 可行性结论

**强可行**。9 个顶层 Java 文件 + 5 个 DTO（共 14 个，541 行）已是产品级，迁移工作主要是：

1. **先升 1.7.3**：协议层为 `ServerSessionEvent` / `DeviceSessionListener.onServerInvite` 增 `rawSdp` 字段（破坏性签名变更，CHANGELOG 强提示）
2. 创建 sip-gateway 父聚合 + 4 子模块骨架（gateway-core / gateway-gb28181 / sip-gateway-bom / sip-gateway-spring-boot-starter）
3. 迁移参考实现到 `gateway-gb28181`，包名重定向 `gbproxy.test.gateway` → `sipgateway.gb28181`
4. 拆分 envelope/SPI/Registry/web 到 `gateway-core`（协议中立），命令表/Forwarder/Store/Web 留在 `gateway-gb28181`（GB28181 专属）
5. type 全部改三段式 `gb28181.<Group>.<Name>`，HTTP 路径改 `/gateway/*`，配置前缀改 `gateway.gb28181.*`
6. 添加 Spring Boot 3 的 `META-INF/spring/AutoConfiguration.imports`（拆 core / gb28181 两条）
7. 编写自动装配条件（`@ConditionalOnClass(RestController.class)` / `@ConditionalOnBean(ServerCommandSender.class)` 双重守门）
8. 配置 namespace 同步 `additional-spring-configuration-metadata.json` deprecation 元数据
9. 测试用例从 gb28181-test 迁出，作为模块自身单测；父 pom 补 jacoco `check` goal 让 80% 阈值真正生效
10. CHANGELOG + 用户接入文档（含 type 老→新对照表 94 条）

预估工作量：

- 1.7.3 协议层小升级：**0.5 天**（含 5 处链路改动 + 测试同步）
- 1.8.0 sip-gateway 父聚合 + envelope 协议化：**4 个工作日**（详见 [SIP-GATEWAY-AGGREGATION-PLAN §九](SIP-GATEWAY-AGGREGATION-PLAN.md#L460)）
- gateway-gb28181-redis 扩展（v1.9.0 候选）：另行 1 天

---

## 二、模块定位与职责

### 2.1 在分层架构中的位置

```
┌──────────────────────────────────────────────────────────────┐
│  业务服务器（业务方实现，进程外）                                │
│  接收 BusinessNotifier 推送 / 调 sip-gateway HTTP API           │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / MQ
┌──────────────────────────────▼──────────────────────────────┐
│  sip-gateway（父聚合，业务方应用 = sip-proxy 提供的 starter）   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ gateway-core（协议中立内核）                              ││
│  │  ├── envelope / SPI / Registry / web                    ││
│  │  ├── POST /gateway/command（协议中立分发）                ││
│  │  └── ProtocolModule SPI（自报命名空间）                   ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │ gateway-gb28181（本模块） ★                              ││
│  │  ├── Gb28181Module / Gb28181CommandSpecs                ││
│  │  ├── Gb28181WhitelistHandlers / Gb28181EventForwarder   ││
│  │  ├── InviteContextStore（跨节点 INVITE 路由）             ││
│  │  └── POST /gateway/gb28181/invite/response（事务回包）    ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │ sip-gateway-bom + sip-gateway-spring-boot-starter        ││
│  │  业务方一键接入入口                                        ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────┬──────────────────────────────┘
                               │ Maven 依赖（同 JVM）
┌──────────────────────────────▼──────────────────────────────┐
│  gb28181-server  +  gb28181-client（可选，级联场景）           │
│  ServerCommandSender / DeviceLifecycleListener / ...          │
└──────────────────────────────────────────────────────────────┘
```

**关键约束**（来自 [LAYERED-ARCHITECTURE.md §2.2](../../architecture/LAYERED-ARCHITECTURE.md#L56)，不可违反）：

- **必须与 sip-proxy 同 JVM**：`SipTransactionRegistry`、`Dialog` 都是进程内对象，跨进程后无法回包
- **不持有可外化业务状态**：`DeviceSessionCache` / `ServerDeviceSupplier` 由业务方实现并落 Redis，gateway 只是协调者
- **stateless service 视角**：除 `processedInvites` 幂等缓存外不持有可变状态，重启后通过 Redis（业务方实现）恢复
- **gateway-core 协议中立**：CI 强制纯度检查（`scripts/check-gateway-core-purity.sh`），禁止 `gateway-core/src/main/java` 内出现 `gb28181 / GB28181 / sip / jain / onvif / gt1078` 等协议 token。详见 [SIP-GATEWAY-AGGREGATION-PLAN §3.2](SIP-GATEWAY-AGGREGATION-PLAN.md#L155)

### 2.2 与现有模块的关系

| 模块 | gateway-gb28181 关系 | 依赖方向 |
|------|--------------------|---------|
| sip-common | 间接依赖（通过 server） | gateway-gb28181 → gb28181-server → sip-common |
| gb28181-common | 间接依赖（通过 server） | gateway-gb28181 → gb28181-server → gb28181-common |
| gb28181-server | **强依赖**（`ServerCommandSender` / 4 个 listener） | gateway-gb28181 → gb28181-server |
| gb28181-client | **不依赖**（默认） | 业务方需要级联时自行同时引入 client |
| **gateway-core** | **强依赖**（envelope / SPI / Registry） | gateway-gb28181 → gateway-core |
| sip-gateway-bom | gateway-gb28181 ∈ bom 管理坐标 | bom → gateway-gb28181（依赖管理向） |
| sip-gateway-spring-boot-starter | starter 默认引入 gateway-gb28181 | starter → gateway-gb28181（依赖向） |
| gb28181-test | 迁移后 test 引用 starter 做集成测试 | test → starter → gateway-gb28181 |

**不引入循环依赖**：gateway-gb28181 永远只依赖 gateway-core 与 gb28181-server，不被它们任何一个反向依赖。CI 通过 `scripts/check-gateway-core-purity.sh` 拦截。

---

## 三、技术方案

### 3.1 模块骨架

> **完整父聚合拓扑见 [SIP-GATEWAY-AGGREGATION-PLAN §二](SIP-GATEWAY-AGGREGATION-PLAN.md#L75)**。本节仅展开 gateway-gb28181 子模块的目录结构。

```
sip-proxy/                                        (parent pom)
├── sip-common/
├── gb28181-common/
├── gb28181-client/
├── gb28181-server/
├── sip-gateway/                                  ★ 父聚合（packaging=pom）
│   ├── pom.xml                                   aggregator
│   ├── gateway-core/                             见 SIP-GATEWAY-AGGREGATION-PLAN §3.2
│   ├── gateway-gb28181/                          ★ 本模块
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/io/github/lunasaw/sipgateway/gb28181/
│   │       │   ├── handler/
│   │       │   │   ├── Gb28181Module.java                    # implements ProtocolModule
│   │       │   │   ├── Gb28181CommandSpecs.java              # 39 行表条目（type 全带 gb28181. 前缀）
│   │       │   │   └── Gb28181WhitelistHandlers.java         # ~20 个 @CommandMapping 方法
│   │       │   ├── forwarder/
│   │       │   │   └── Gb28181EventForwarder.java            # 4 listener × 35 emit
│   │       │   ├── store/
│   │       │   │   ├── InviteContextStore.java               # extends TransactionContextStore<String, InviteContext>
│   │       │   │   ├── InviteContext.java                    # record(nodeId, ctxKey)
│   │       │   │   └── InMemoryInviteContextStore.java
│   │       │   ├── web/
│   │       │   │   └── Gb28181InviteResponseController.java  # POST /gateway/gb28181/invite/response
│   │       │   ├── config/
│   │       │   │   ├── Gb28181GatewayProperties.java         # gateway.gb28181.*
│   │       │   │   └── Gb28181GatewayAutoConfiguration.java
│   │       │   └── dto/
│   │       │       └── InviteResponseRequest.java            # 仅保留事务回包 DTO
│   │       └── resources/META-INF/
│   │           └── additional-spring-configuration-metadata.json   # 配置 deprecation
│   ├── sip-gateway-bom/                          见 SIP-GATEWAY-AGGREGATION-PLAN §3.4
│   └── sip-gateway-spring-boot-starter/          见 SIP-GATEWAY-AGGREGATION-PLAN §3.5
└── gb28181-test/
```

**包名约定**：`io.github.lunasaw.sipgateway.gb28181.*`（去掉 test 命名 + 品牌统一为 sipgateway）。AutoConfig 注入 `META-INF/spring/AutoConfiguration.imports`：

```
io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayAutoConfiguration
```

由 `sip-gateway-spring-boot-starter` 集中暴露给业务方。

### 3.2 Maven 配置

#### 仓库根 pom 变更

```xml
<modules>
    <module>sip-common</module>
    <module>gb28181-common</module>
    <module>gb28181-client</module>
    <module>gb28181-server</module>
    <module>sip-gateway</module>      <!-- ★ 新增父聚合（packaging=pom），下含 4 子模块 -->
    <module>gb28181-test</module>     <!-- 仍保留，迁移后引用 starter 做集成测试 -->
</modules>
```

`<dependencyManagement>` 增加 4 个新坐标：

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gateway-core</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gateway-gb28181</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-bom</artifactId>
    <version>${gb28181-proxy.version}</version>
    <type>pom</type>
</dependency>
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
```

#### sip-gateway 父聚合 pom

```xml
<artifactId>sip-gateway</artifactId>
<packaging>pom</packaging>

<modules>
    <module>gateway-core</module>
    <module>gateway-gb28181</module>
    <module>sip-gateway-bom</module>
    <module>sip-gateway-spring-boot-starter</module>
</modules>
```

只负责聚合构建，不产 jar。详见 [SIP-GATEWAY-AGGREGATION-PLAN §3.1](SIP-GATEWAY-AGGREGATION-PLAN.md#L141)。

#### gateway-gb28181/pom.xml

```xml
<dependencies>
    <!-- 强依赖：协议中立内核（envelope / SPI / Registry） -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gateway-core</artifactId>
    </dependency>

    <!-- 强依赖：gb28181-server 的 ServerCommandSender + 4 个 listener -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gb28181-server</artifactId>
    </dependency>

    <!-- INVITE 重传幂等缓存（已在父 pom dependencyManagement 中） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- spring-web：HTTP API 用，optional 不传递；业务方需要 web 时引 spring-boot-starter-web -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**`<optional>true</optional>` 的语义**：业务方直接引入 gateway-gb28181 时不会传递 spring-web，必须自己引入 `spring-boot-starter-web`（或排除 HTTP API 部分）。这是有意设计——纯事件转发用法可以不要 web。**业务方走 `sip-gateway-spring-boot-starter` 一键接入时由 starter 引 spring-boot-starter-web**，无需关心。

#### sip-gateway-bom/pom.xml 与 starter pom 见 [SIP-GATEWAY-AGGREGATION-PLAN §3.4 / §3.5](SIP-GATEWAY-AGGREGATION-PLAN.md#L207)

### 3.3 核心扩展点设计

#### 3.3.1 BusinessNotifier（必须由业务方提供生产实现）

> **注意**：1.8.0 起 `BusinessNotifier` 接口签名简化为单方法 `notify(GatewayEvent)`，envelope 化（详见 [UNIFIED-ENVELOPE-PLAN §2.2](UNIFIED-ENVELOPE-PLAN.md#L70)）。本节描述 v1.7.x 老接口的 INVITE rawSdp 透传约束如何映射到新 envelope 中。

新接口（落 `gateway-core/api/BusinessNotifier.java`）：

```java
package io.github.lunasaw.sipgateway.core.api;

public interface BusinessNotifier {
    /**
     * 业务方实现：把 event 推到 HTTP/MQ/Webhook。
     * <p><strong>必须异步</strong>，否则会阻塞 SIP 事件线程导致设备超时重传。
     */
    void notify(GatewayEvent event);
}
```

GB28181 设备主动 INVITE 时，`Gb28181EventForwarder.onServerInvite` 会发出 `gb28181.Session.ServerInvite` 事件，payload 含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `fromUserId` | String | 发起方设备 ID |
| `toUserId` | String | 目标方设备 ID |
| `rawSdp` | String | **原始 SDP 文本**（UTF-8 解码自 INVITE body），转给 ZLM/SRS 推流时用 |
| `sdp` | GbSessionDescription | 已解析的 SDP 模型，业务侧抠 ssrc / m-line 端口做流匹配时用 |
| `ctxKey` | String | 跨节点回包用的 transactionContextKey |

**rawSdp 上溯改动**（隶属 1.7.3，先于 gateway 模块化落地）：

| 文件 | 改动 |
|------|------|
| [ServerInviteRequestProcessor.java:75-80](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/invite/ServerInviteRequestProcessor.java#L75-L80) | 把 `new String(rawContent, UTF_8)` 提为局部变量 `rawSdp`，传给 `ServerSessionEvent.serverInvite(...)` |
| [ServerSessionEvent.java](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/event/ServerSessionEvent.java) | 加 `private final String rawSdp` 字段；`serverInvite(...)` 工厂增参 |
| [DeviceSessionListener.java:38-39](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/api/DeviceSessionListener.java#L38-L39) | `onServerInvite` 增 `String rawSdp` 参数 |
| [ServerListenerAdapter.java:169-170](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/event/internal/ServerListenerAdapter.java#L169-L170) | `e.getRawSdp()` 透传 |
| `Gb28181EventForwarder` | 透传到 `gb28181.Session.ServerInvite` 事件的 payload |

**实现策略**（v1.0 落地）：

- gateway-core 仅提供 `NoopBusinessNotifier`（日志输出，单机演示用）+ `AbstractProtocolBusinessNotifier`（按 protocol 分发的可选基类）
- 文档给出 `RestTemplateBusinessNotifier` 模板（30 行示例）
- **不内置具体 HTTP/MQ 实现**——避免引入 RestTemplate/WebClient/Spring Cloud Stream 依赖污染

**异步约束**：javadoc 明确"实现必须异步"，否则会阻塞 SIP 事件线程导致设备超时重传。给出 `@Async` 标准用法。

#### 3.3.2 InviteContextStore（多节点必须替换为 Redis）

`InviteContextStore` 是 `TransactionContextStore<String, InviteContext>` 的具体化（详见 [SIP-GATEWAY-AGGREGATION-PLAN §4.5](SIP-GATEWAY-AGGREGATION-PLAN.md#L335)）。

接口定义（落 `gateway-gb28181/store/`）：

```java
package io.github.lunasaw.sipgateway.gb28181.store;

import io.github.lunasaw.sipgateway.core.api.TransactionContextStore;

public interface InviteContextStore extends TransactionContextStore<String, InviteContext> {
}

public record InviteContext(String nodeId, String ctxKey) { }
```

`SipCommandController` 跨节点路由逻辑：

```
find(callId) → InviteContext{nodeId, ctxKey}
  → nodeId == 本节点? 直接处理 : 转发到 gateway.nodes[nodeId]
```

**实现策略**：

- gateway-gb28181 提供 `InMemoryInviteContextStore`（Caffeine，单机/单测，启动期 WARN 提示生产换 Redis）
- **不内置 Redis 实现**（避免依赖 lettuce / jedis）
- 文档给出 `RedisTemplateInviteContextStore` 模板，强调错误映射约定：

```java
@Override
public InviteContext find(String callId) {
    try {
        String value = redisTemplate.opsForValue().get("sip:invite:ctx:" + callId);
        if (value == null) return null;
        // value 格式："{nodeId}:{ctxKey}"
        int sep = value.indexOf(':');
        return new InviteContext(value.substring(0, sep), value.substring(sep + 1));
    } catch (RedisConnectionFailureException | RedisCommandTimeoutException e) {
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "redis unavailable", e);
    }
}
```

可选演进：单独建子模块 `gateway-gb28181-redis`（依赖 spring-data-redis）。本期暂缓，看用户需求（[§六](#六未来演进) 1.9.0 候选）。

#### 3.3.3 Gb28181EventForwarder（成品转发器）

代码细节见 [UNIFIED-ENVELOPE-PLAN §4.1](UNIFIED-ENVELOPE-PLAN.md#L584)。关键变化：

- 类名从 `SipEventForwarder` 改为 `Gb28181EventForwarder`，体现协议归属
- 实现接口从 3 个 listener 扩展为 4 个 listener × 35 个方法
- 移除对 `SipBusinessConfig`（测试模块的 `DeviceSessionCache` 实现）的硬依赖，改为构造函数注入 `Optional<DeviceSessionCache>`：业务方未提供时退化为"只推送不缓存设备会话"，由业务方自行决定是否在 `BusinessNotifier` 里写库
- `processedInvites` 缓存继续按 callId 幂等
- 加上 `@ConditionalOnBean(BusinessNotifier.class)` 守门——理论上 NoopBusinessNotifier 会作为兜底，永远成立
- type 全部带 `gb28181.` 前缀（详见 [UNIFIED-ENVELOPE-PLAN §4.1](UNIFIED-ENVELOPE-PLAN.md#L584) 35 个 emit 方法）

#### 3.3.4 GatewayDispatchController + Gb28181InviteResponseController（HTTP API）

HTTP 路径全部改 `/gateway/*`：

| 端点 | 模块 | 路径 |
|------|------|------|
| 协议中立分发 | gateway-core | `POST /gateway/command` |
| 节点身份 | gateway-core | `GET /gateway/whoami` |
| GB28181 INVITE 异步回包 | gateway-gb28181 | `POST /gateway/gb28181/invite/response` |

错误码契约与 [LAYERED-ARCHITECTURE.md §6.4](../../architecture/LAYERED-ARCHITECTURE.md#L99) 严格一致（410 / 502 / 503）。用 `@ConditionalOnWebApplication(type = SERVLET)` 守门。

老 6 个 HTTP 路径全部删除（`/sip/invite/start` / `/sip/invite/bye` 等）；新 cmdType 通过 envelope 进入 `POST /gateway/command`，详见 [UNIFIED-ENVELOPE-PLAN §七](UNIFIED-ENVELOPE-PLAN.md#L883)。

**新增建议（v1.1 候选）**：

- `POST /gateway/gb28181/invite/refresh-ringing` — 业务处理 25s 边界主动发 180 Ringing + extendContext
- `GET /gateway/gb28181/dialogs/{callId}` — 调试用，回看 DialogRegistry 状态
- `POST /gateway/gb28181/subscribe/refresh` / `POST /gateway/gb28181/subscribe/unsubscribe` — 对接 1.7.0 新增的 SUBSCRIBE 续订 API

v1.0 仅做现有 6 个端点（已 envelope 化为 1 个 dispatch + 1 个 invite/response + whoami），新端点等用户反馈再加。

### 3.4 Spring Boot 自动装配

详见 [SIP-GATEWAY-AGGREGATION-PLAN §八](SIP-GATEWAY-AGGREGATION-PLAN.md#L460)。本节摘 GB28181 适配器的 AutoConfig：

```java
@AutoConfiguration(after = GatewayCoreAutoConfiguration.class)
@EnableConfigurationProperties(Gb28181GatewayProperties.class)
@ConditionalOnClass(name = "io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender")
@ConditionalOnBean(ServerCommandSender.class)
public class Gb28181GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InviteContextStore inviteContextStore(Gb28181GatewayProperties props) {
        return new InMemoryInviteContextStore(props.getInviteContextTtlMs());
    }

    @Bean
    public Gb28181Module gb28181Module(ServerCommandSender sender) {
        return new Gb28181Module(sender);
    }

    @Bean
    public Gb28181WhitelistHandlers gb28181WhitelistHandlers(ServerCommandSender sender) {
        return new Gb28181WhitelistHandlers(sender);
    }

    @Bean
    public Gb28181EventForwarder gb28181EventForwarder(
            BusinessNotifier notifier,
            InviteContextStore store,
            GatewayProperties coreProps,
            Gb28181GatewayProperties gb28181Props) {
        return new Gb28181EventForwarder(notifier, store, coreProps, gb28181Props);
    }

    @AutoConfiguration(after = Gb28181GatewayAutoConfiguration.class)
    @ConditionalOnWebApplication(type = SERVLET)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public Gb28181InviteResponseController gb28181InviteResponseController(
                GatewayProperties coreProps,
                ServerCommandSender sender,
                InviteContextStore store,
                @Qualifier("gatewayForwardRestTemplate") RestTemplate forward) {
            return new Gb28181InviteResponseController(coreProps, sender, store, forward);
        }
    }
}
```

**关键守门条件**：
1. `@ConditionalOnClass(ServerCommandSender)` — 业务方未引 gb28181-server 时整个 GB28181 模块自动跳过
2. `@ConditionalOnBean(ServerCommandSender)` — 类存在但业务方未启用 `@EnableSipServer` 时跳过
3. `@AutoConfiguration(after = GatewayCoreAutoConfiguration.class)` — 保证 `CommandHandlerRegistry` 装配时 `Gb28181Module` 已注册

**`AutoConfiguration.imports`**：

```
# gateway-gb28181/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayAutoConfiguration
```

**配置 namespace**：`gateway.gb28181.*`（核心节点配置 `gateway.*` 由 gateway-core 统一）。提供一版兼容期 deprecation：

```yaml
# 老前缀（v1.7.x 参考实现），1.9.x 后移除
gateway:
  invite-context-ttl-ms: 30000

# 新前缀（v1.8.0 起）
gateway:
  gb28181:
    invite-context-ttl-ms: 30000
```

`Gb28181GatewayProperties` 通过 `@DeprecatedConfigurationProperty` + `additional-spring-configuration-metadata.json` 提示迁移：

```json
{
  "properties": [
    {
      "name": "gateway.invite-context-ttl-ms",
      "type": "java.lang.Long",
      "deprecation": {
        "replacement": "gateway.gb28181.invite-context-ttl-ms",
        "level": "warning",
        "reason": "v1.8.0 起 GB28181 专属配置迁入 gateway.gb28181.* 子前缀，便于多协议共存"
      }
    }
  ]
}
```

**JaCoCo 80% 阈值真正生效**：父 pom 当前只跑 `prepare-agent` + `report`，没有 `check` goal。本期顺手补上 aggregate 检查，全模块合计 ≥ 80% 才通过：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>jacoco-aggregate-check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <haltOnFailure>true</haltOnFailure>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3.5 多节点扩展模块（可选，v1.9+）

> 1.8.0 不做，先看用户对 in-memory + 自行接 Redis 模板的反馈。

#### 3.5.1 gateway-gb28181-redis（候选）

```
sip-gateway/
└── gateway-gb28181-redis/
    └── src/main/java/io/github/lunasaw/sipgateway/gb28181/redis/
        ├── RedisInviteContextStore.java         # 实现 InviteContextStore
        └── RedisGb28181GatewayAutoConfiguration.java
```

依赖：`gateway-gb28181` + `spring-boot-starter-data-redis`。`@ConditionalOnClass(RedisTemplate.class)` 守门。`sip-gateway-bom` 加坐标，`sip-gateway-spring-boot-starter` 不默认引（业务方按需选择）。

#### 3.5.2 gateway-discovery（候选）

`gateway.nodes` 的 K8s Endpoints / Nacos / Consul 实现。各装配方式差异大，更适合做单独子模块（按需选择），主模块永远只支持静态配置。**注意**：discovery 是协议中立能力，应放在 `gateway-core` 同级或独立 `gateway-discovery-{kind}` 子模块，不绑定 GB28181。

### 3.6 错误语义契约

完全继承 [LAYERED-ARCHITECTURE.md §6.4](../../architecture/LAYERED-ARCHITECTURE.md#L99) 的契约（v1.0 不变）：

| HTTP Status | 触发条件 | 业务侧行为 |
|------------|---------|----------|
| 200 OK | 正常处理 | 正常流程 |
| 410 Gone | 事务已超时 / `find()` 返回 null | **禁止重试**，重新发起原始命令 |
| 502 Bad Gateway | `gateway.nodes` 不含目标节点 | 200ms × 3 短重试，覆盖刷新窗口 |
| 503 Service Unavailable | 跨节点转发失败 / store 后端不可达 | 200ms × 3 短重试 |
| 500 Internal Server Error | 未受控异常（不应出现） | 视作 bug，告警 + 不重试 |

`GatewayDispatchController` 与 `Gb28181InviteResponseController` 共享相同的 503 兜底（`RuntimeException` → 503），保持不动。

### 3.7 测试策略

#### 3.7.1 单元测试（gateway-core / gateway-gb28181 自己负责）

**gateway-core**：

- `GatewayPropertiesTest` — `gateway.*` 配置解析 + 默认值
- `CommandHandlerRegistryTest` — type 重复 fail-fast、ProtocolModule 命名空间不一致 fail-fast、`@CommandMapping` 覆盖时 WARN/`overrideTable=true` 静默
- `GatewayDispatchControllerTest` — `MockMvc`：未知 type → 404、payload 缺失 → 400、合法分发 → handler 被调用、1.8.0 兼容 shim 命中时 WARN

**gateway-gb28181**：

- `Gb28181GatewayPropertiesTest` — `gateway.gb28181.*` 配置解析 + 默认值 + deprecation alias
- `InMemoryInviteContextStoreTest` — TTL / find / remove 边界
- `Gb28181EventForwarderTest` — 用 Mockito 注入 fake `BusinessNotifier`，验证：
  - 注册事件触发 `gb28181.Lifecycle.Online`（含 NAT IP 切换覆盖语义）
  - INVITE 重传同 callId 在幂等窗口内只推送一次
  - 告警事件直推 `gb28181.Notify.Alarm`
  - 35 个 listener 方法全覆盖（type 三段式正确、correlationId / payload 字段对位）
- `Gb28181CommandSpecsTest` — 39 条 spec 各 1 用例（mock `ServerCommandSender`，断言反射调用参数顺序、类型转换、缺省值生效）
- `Gb28181WhitelistHandlersTest` — 20 个 `@CommandMapping` 方法各 1 用例
- `Gb28181InviteResponseControllerTest` — `MockMvc`：
  - 本节点处理 → 触发 `ResponseCmd.sendResponse`
  - 跨节点转发 → 验证 `RestTemplate.postForObject` 调用
  - store 故障 → 503
  - 事务超时 → 410
  - `gateway.nodes` miss → 502

#### 3.7.2 集成测试（gb28181-test 模块覆盖）

- 全链路 `gb28181.Invite.Play`：业务调 `POST /gateway/command` → 模拟设备回 200 OK → DeviceInviteOkEvent 触发 → BusinessNotifier 收 `gb28181.Session.InviteOk` 推送
- 设备主动 INVITE：模拟设备发 INVITE → `ServerInviteEvent` → BusinessNotifier 收 `gb28181.Session.ServerInvite` 推送 → 业务调 `POST /gateway/gb28181/invite/response` 完成回包
- 多节点 INVITE 路由：用 `MockRestServiceServer` mock 跨节点 HTTP 调用，验证 `Gb28181InviteResponseController` 在 `find(callId).nodeId != localNodeId` 时转发到正确地址；不需要真正起两个 ApplicationContext（同 JVM 内 `SipTransactionRegistry` 无法真正隔离）
- starter 接入测：空白 spring-boot 项目引 `sip-gateway-spring-boot-starter` 启动正常；exclusions 排除 `gateway-gb28181` 后 `gb28181.*` type 返回 404

#### 3.7.3 覆盖率要求

继承父 pom JaCoCo aggregate 80% line coverage 阈值（详见 §3.4）。Web 部分用 `@WebMvcTest` 切片测试。

### 3.8 文档与发布

#### 3.8.1 模块自带 README

`sip-gateway/README.md`（父聚合主文档）：

- 5 分钟快速开始（引 `sip-gateway-spring-boot-starter` + `application.yml` + 实现 `BusinessNotifier`）
- 单机部署示例
- 多节点部署示例（含 Redis store 模板代码 + `gateway.nodes`），**`sip.server.external-ip: <VIP>` 标注为必填项**——多节点下节点 IP 不可达，必须填 VIP 地址，否则设备回包绕过 VIP 导致源 IP 哈希失效（详见 [HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)）
- 错误码契约
- FAQ：与 sip-proxy 必须同 JVM 的原因、为什么不内置 RestTemplate/Redis、如何添加自定义 cmdType（@CommandMapping）、如何加新协议（实现 ProtocolModule）

`sip-gateway/gateway-gb28181/README.md`：

- GB28181 协议适配器特化说明
- 全 59 出站命令 + 35 入站事件的 type 列表（指向 [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#L680)）

#### 3.8.2 LAYERED-ARCHITECTURE.md 更新

§6 整节从"参考实现散落在 gb28181-test"改为"引入 sip-gateway-spring-boot-starter 即可"，§9 对照表增加：

```
| sip-gateway 父聚合 | sip-gateway/ | ✅ 1.8.0 就绪 |
| gateway-core      | sip-gateway/gateway-core/    | ✅ 1.8.0 就绪 |
| gateway-gb28181   | sip-gateway/gateway-gb28181/ | ✅ 1.8.0 就绪 |
```

#### 3.8.3 PROTOCOL-LAYERING-MATRIX.md 更新

在 §三 / §四 末尾追加"L3 网关 envelope 表"小节，cmdType 表格再扩一列指向 [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#L680) 对应行（type 列改三段式 `gb28181.<Group>.<Name>`）。

#### 3.8.4 CHANGELOG

```markdown
## [1.8.0]

### Added
- 新增 `sip-gateway` 父聚合，下含 `gateway-core` / `gateway-gb28181` / `sip-gateway-bom` / `sip-gateway-spring-boot-starter` 4 个子模块。
- 业务方一键接入：`<dependency>sip-gateway-spring-boot-starter</dependency>` 即用全套 GB28181 网关能力。
- 协议无关 envelope：`POST /gateway/command` 单端点承载所有 cmdType；`BusinessNotifier#notify(GatewayEvent)` 单方法承载所有事件。
- type 三段式命名：`<protocol>.<Group>.<Name>`，预留多协议扩展（ONVIF/GT1078/...）零破坏接入。
- `ProtocolModule` SPI：协议适配器自报命名空间，gateway-core 跨协议聚合。
- `TransactionContextStore<K,V>` 泛型基类，每协议自定义具体接口。
- HTTP API 错误码契约与 §6.4 一致（410 / 502 / 503）。
- 父 pom JaCoCo aggregate `check` 阻断 < 80% LINE 覆盖率构建。

### Breaking Changes
- `BusinessNotifier` 接口从 3 方法变 1 方法（envelope 化）。
- 老 6 个 HTTP 路径删除（`/sip/invite/start` 等），全部改 `/gateway/*`。
- type 字符串改三段式（兼容 shim 在 1.10.0 移除）。
- 配置前缀 `gateway.*` → `gateway.gb28181.*`（保留一版 deprecation 别名）。
- 包名 `io.github.lunasaw.gbproxy.gateway.*` → `io.github.lunasaw.sipgateway.gb28181.*`。

### Migration
- 旧用户从 gb28181-test 复制 gateway 包的，删除复制代码，引入 `<dependency>sip-gateway-spring-boot-starter</dependency>`。
- 配置 namespace 从 `gateway.*` 改为 `gateway.gb28181.*`，旧 namespace 兼容一个版本后移除。
- HTTP path 与 type 老→新对照表见 [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#L680) + [SIP-GATEWAY-AGGREGATION-PLAN §九](SIP-GATEWAY-AGGREGATION-PLAN.md#L460)。
```

---

## 四、迁移步骤（从 gb28181-test 抽离 + envelope 化 + 父聚合）

> **本节细化 [SIP-GATEWAY-AGGREGATION-PLAN §九 Stage 0~6](SIP-GATEWAY-AGGREGATION-PLAN.md#L460) 中与 GB28181 适配器代码迁移相关的部分**。envelope schema 与命令表的代码细节见 [UNIFIED-ENVELOPE-PLAN §九](UNIFIED-ENVELOPE-PLAN.md#L1068)。

### Stage 0（1.7.3 协议层小升级，0.5 天）— gateway 模块化前置

- `ServerSessionEvent` 增 `private final String rawSdp` 字段，`serverInvite(...)` 工厂方法增参
- `DeviceSessionListener.onServerInvite` 接口签名增 `String rawSdp` 参数（**破坏性变更**，CHANGELOG 强提示）
- `ServerInviteRequestProcessor` 把已经 decode 过的 SDP 文本透传到事件
- `ServerListenerAdapter` 透传新字段到 listener
- 同步更新 `ServerGb28181Adapter`、单测、`gb28181-test` 现有 `SipEventForwarder` 实现签名
- 发版 1.7.3，让老用户先适配 listener 接口；**不动 gateway/ 包目录**
- 验收：`mvn verify` 全绿；`scripts/check-sip-common-purity.sh` 通过；listener 单测覆盖 rawSdp 路径

### Stage 1：建 sip-gateway 父聚合骨架（0.5 天）

- 仓库根 pom 新增 `<module>sip-gateway</module>`
- `sip-gateway/pom.xml`（packaging=pom）声明 4 个子模块（gateway-core / gateway-gb28181 / sip-gateway-bom / sip-gateway-spring-boot-starter）
- 4 个子模块各建空 pom + 标准 src 目录
- 父 pom `<dependencyManagement>` 加 4 个新坐标
- 新增 `scripts/check-gateway-core-purity.sh` 并挂到 verify 阶段
- 验收：`mvn verify` 全绿、纯度脚本对空 src 通过

### Stage 2：实现 gateway-core（0.5 天）

- 包名 `io.github.lunasaw.sipgateway.core.*`
- 落 envelope 三件套（GatewayCommand / GatewayCommandResult / GatewayEvent）
- 落 SPI：CommandHandler（单参数）/ CommandMapping（含 overrideTable）/ CommandSpec（含 senderClass）/ ParamBinding / ProtocolModule / TransactionContextStore
- 落核心：CommandHandlerRegistry（跨协议聚合 + fail-fast）/ ReflectiveCommandHandler / MethodInvokerHandler / PayloadCodec
- 落 web：GatewayDispatchController（POST /gateway/command + GET /gateway/whoami + 1.8.0 兼容 shim）
- 落 notifier：NoopBusinessNotifier（启动 warn）+ AbstractProtocolBusinessNotifier
- 落 config：GatewayProperties（gateway.*）+ GatewayCoreAutoConfiguration
- 验收：单测覆盖 envelope record / Registry fail-fast / Controller 错误码兜底

### Stage 3：实现 gateway-gb28181（1 天）

- 包名 `io.github.lunasaw.sipgateway.gb28181.*`
- 把 [gb28181-test/.../gateway/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 14 个文件迁过来，按 §3.1 重组目录
- type 全部加 `gb28181.` 前缀（39 行 CommandSpecs + 35 个 emit 调用，详见 [UNIFIED-ENVELOPE-PLAN §3.4 / §4.1](UNIFIED-ENVELOPE-PLAN.md#L225)）
- 落 `Gb28181Module implements ProtocolModule`（自报 `"gb28181"` + 暴露 commandSpecs）
- 落 `Gb28181WhitelistHandlers`（约 20 个 @CommandMapping 方法，签名 `(GatewayCommand) → String`）
- 落 `Gb28181EventForwarder`（替代 SipEventForwarder，4 listener × 35 方法）
- 落 `InMemoryInviteContextStore`（启动 warn "replace with Redis before multi-node"）
- 落 `Gb28181InviteResponseController`（POST /gateway/gb28181/invite/response）
- 落 `Gb28181GatewayProperties`（gateway.gb28181.*） + `Gb28181GatewayAutoConfiguration`（@ConditionalOnClass / @ConditionalOnBean 双重守门）
- 写 `META-INF/spring/AutoConfiguration.imports` + `META-INF/additional-spring-configuration-metadata.json` deprecation 元数据
- 验收：39 spec 单测 + 20 whitelist 单测 + 35 forwarder 单测 + MockMvc 集成测全绿

### Stage 4：BOM + Starter（0.5 天）

- `sip-gateway-bom`：`<dependencyManagement>` 列全部协议子模块（含未来占位注释）
- `sip-gateway-spring-boot-starter`：`<dependencies>` 引 `gateway-core` + `gateway-gb28181`（默认带）+ `spring-boot-starter` + `spring-boot-starter-web`(optional)
- starter `META-INF/spring/AutoConfiguration.imports` 写 2 行（core + gb28181）
- 验收：空白 spring-boot 项目引 starter，能注入 GatewayDispatchController + Gb28181EventForwarder + Gb28181InviteResponseController

### Stage 5：测试 & 强化覆盖率门禁（0.5 天）

- 跨协议 type 重复 fail-fast 测：mock 两个 ProtocolModule 声明同 type → 启动期抛异常
- starter exclusion 测：排除 gateway-gb28181 后只剩 core，启动正常但 `/gateway/command` type=`gb28181.*` 返回 404
- @ConditionalOnBean(ServerCommandSender) 退化路径：业务方无 gb28181-server 时 starter 自动跳过 GB28181 模块
- 跨节点路由测：`MockRestServiceServer` mock 跨节点 HTTP，验证 `nodeId != local` 时正确转发
- 父 pom JaCoCo 改 aggregate report，全模块合计 ≥ 80%
- 跑 `mvn verify` 通过

### Stage 6：gb28181-test 改用 starter & 文档（0.5 天）

- 删除 `gb28181-test/src/main/java/.../test/gateway/` 旧代码
- `gb28181-test/pom.xml` 加 `<dependency>sip-gateway-spring-boot-starter</dependency>`
- 测试模块只保留 `SipBusinessConfig`（DeviceSessionCache 测试实现）+ `TestServerDeviceSupplier` 等业务方抽象
- 更新 [LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) §6 / §9
- 更新 [PROTOCOL-LAYERING-MATRIX.md](../../architecture/PROTOCOL-LAYERING-MATRIX.md) 加 L3 网关 envelope 列
- 写 `sip-gateway/README.md` + `sip-gateway/gateway-gb28181/README.md`
- CHANGELOG 加 1.7.3（rawSdp 破坏性变更）+ 1.8.0（sip-gateway 父聚合 + envelope 化）两条版本条目

**总计：0.5 天 (Stage 0) + 4 天 (Stage 1-6) = 4.5 个工作日**（含 envelope 化 + 父聚合）。

---

## 五、风险与备选

### 5.1 风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| spring-web 列为 `<optional>` 后业务方忘记自己引入 starter | 启动时 NoSuchBeanDefinitionException | starter 已默认引 `spring-boot-starter-web`(optional)；纯 forwarder 用法在 README 强提示 |
| 业务方误用 `NoopBusinessNotifier` 上线 | 设备事件丢失，业务无感知 | NoopBusinessNotifier 启动时 `log.warn` 提示"NoopBusinessNotifier active, replace before production" |
| 业务方误用 `InMemoryInviteContextStore` 多节点部署 | 跨节点 INVITE 回包路由失败 | `InMemoryInviteContextStore` 启动时**无条件** `log.warn("InMemoryInviteContextStore active — replace with Redis implementation before multi-node deployment")`，不依赖 `gateway.nodes` 配置判断（nodes 为空不代表单节点）；同时在 README 多节点部署章节标注为必替换项 |
| 配置 namespace 改名（`gateway.*` → `gateway.gb28181.*`）破坏老用户 | 升级后配置不生效 | 旧 namespace 兼容一个版本，启动 warn |
| 包名搬家（`gbproxy.gateway` → `sipgateway.gb28181`）业务方 import 全失败 | 升级直接编译失败 | CHANGELOG 列对照；业务方走 starter 接入时仅依赖类型，少受影响；裸引子模块的用户必须改 import |
| `gateway-core` 不慎依赖 `gb28181-*` | 多协议梦想破产 | CI 纯度脚本 `check-gateway-core-purity.sh` 在 verify 阶段拦截 |
| ProtocolModule 自报 protocol 与 spec.type 前缀不一致 | 启动期 type 路由错乱 | Registry 启动期断言每个 spec.type 必须以 `module.protocol() + "."` 开头，否则抛 `IllegalStateException` |
| starter 默认带 gb28181，纯 ONVIF 项目被动引入 gb28181-server jar | 包体积 +3MB | 文档明示 `<exclusions>` 用法；如反馈强烈，1.10.0 拆 `sip-gateway-spring-boot-starter-gb28181` 子 starter（加法，非破坏） |

### 5.2 备选方案

**备选 A：不模块化，只整理参考实现**
- 在 doc/ 下产出"标准 sip-gateway 模板"目录，业务方 clone 即用
- 优点：零依赖污染
- 缺点：无法 Maven 升级，与现状无本质区别 → **拒绝**

**备选 B：模块化但不暴露 HTTP API**
- gateway-gb28181 只提供 forwarder + store，HTTP API 让业务方自己写
- 优点：彻底无 spring-web 依赖
- 缺点：业务方还是要复制 controller 100 行跨节点路由逻辑——这是最容易写错的部分 → **拒绝**

**备选 C：模块化但不拆 gateway-core，单一 gb28181-gateway 模块**
- 1.7.x 老方案
- 优点：短期工作量少
- 缺点：未来加 ONVIF/GT1078 时被迫破坏性重构（包名搬家、抽 core）→ **拒绝**

**备选 D：父聚合 + 协议中立 core + GB28181 适配器（本方案）**
- 主模块 web optional + Redis 文档示例 + 单独子模块按需
- 业务方一键接入 starter，未来加协议是纯加法 → **采纳**

---

## 六、未来演进

| 版本 | 计划项 |
|------|--------|
| **1.7.3** ✅ | **协议层小升级前置**：`ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)`（破坏性签名变更） |
| **1.8.0** | **本方案落地**：sip-gateway 父聚合 + 4 子模块（gateway-core / gateway-gb28181 / bom / starter） + envelope 协议化（详见 [UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md)） |
| 1.9.0 | gateway-gb28181-redis 扩展模块（Redis InviteContextStore + DeviceSessionCache 参考实现） |
| 1.10.0 | **gateway-onvif** 子模块（ONVIF SOAP Discovery + Imaging + PTZ）；同时移除 1.8.0 兼容 shim |
| 1.11.0 | **gateway-gt1078** 子模块（GT1078 私有 TCP 长连接 + 流水号） |
| 1.12.0 | gateway-rtsp 子模块（RTSP 直连） |
| 待定 | gateway-discovery（K8s Endpoints / Nacos `gateway.nodes` 动态发现，协议中立） |
| 待定 | sip-gateway-webhook（HttpWebhookBusinessNotifier，HMAC-SHA256 签名）作为可选 starter |
| 待定 | 客户端侧网关（gateway-gb28181-client）—— 当前需求未明确，v1.0 不规划 |
| 待定 | OpenAPI/Swagger 自动生成 HTTP 文档 |
| 待定 | gateway 内置 metrics（按 protocol 维度聚合 INVITE 成功率 / 跨节点转发耗时 / store 命中率） |

---

## 七、决策清单（已确认）

| # | 问题 | 决策 |
|---|------|------|
| 1 | `BusinessNotifier` 是否传 raw SDP | ✅ 必传，envelope `gb28181.Session.ServerInvite` 的 payload 同时含 `rawSdp` + `sdp`（解析后） |
| 2 | 模块拓扑 | sip-gateway 父聚合 + gateway-core + gateway-gb28181 + BOM + starter |
| 3 | 业务方接入入口 | sip-gateway-spring-boot-starter（一键），辅以 BOM 锁版本 |
| 4 | 包名根 | `io.github.lunasaw.sipgateway.{core,gb28181,...}`（品牌统一） |
| 5 | 配置 namespace | `gateway.*`（核心） + `gateway.gb28181.*`（协议子前缀） |
| 6 | type 命名 | `<protocol>.<Group>.<Name>` 三段式（详见 [SIP-GATEWAY-AGGREGATION-PLAN §五](SIP-GATEWAY-AGGREGATION-PLAN.md#L390)） |
| 7 | HTTP path | `/gateway/command`（核心） + `/gateway/{protocol}/...`（协议特殊端点） |
| 8 | spring-web 依赖 | optional + starter 默认引 `spring-boot-starter-web`(optional)，纯事件转发场景免引入 web |
| 9 | RestTemplate Notifier 模板 | 不提供主模块代码，文档里给示例；主模块只 Noop + AbstractProtocolBusinessNotifier |
| 10 | 首版本号 | 1.7.3（rawSdp 协议层小升级）+ 1.8.0（sip-gateway 父聚合 + envelope 化） |
| 11 | gateway-gb28181-redis / gateway-discovery | 1.8.0 出主模块，1.9.0 出 Redis，看用户反馈再决定 discovery |
| 12 | JaCoCo 80% 阈值 | 父 pom 加 aggregate `check` goal，全模块合计 ≥ 80% 才构建通过（详见 §3.4） |

---

## 八、附录：当前参考实现关键代码片段索引

供迁移时直接对照（**1.8.0 起包名 `gbproxy.test.gateway` → `sipgateway.gb28181`**，envelope record 与 SPI 落 gateway-core）：

| 功能 | 现位置 | 1.8.0 迁移后 | 模块 |
|------|--------|------------|------|
| 配置类（核心） | — | `io.github.lunasaw.sipgateway.core.config.GatewayProperties`（gateway.*） | gateway-core |
| 配置类（协议） | [gateway/GatewayProperties.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/GatewayProperties.java) | `io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayProperties`（gateway.gb28181.*） | gateway-gb28181 |
| 装配类（核心） | — | `io.github.lunasaw.sipgateway.core.config.GatewayCoreAutoConfiguration` | gateway-core |
| 装配类（协议） | [gateway/GatewayConfig.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/GatewayConfig.java) | `io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayAutoConfiguration`（拆分 + 双重守门） | gateway-gb28181 |
| envelope 三件套 | — | `io.github.lunasaw.sipgateway.core.api.envelope.{GatewayCommand, GatewayCommandResult, GatewayEvent}` | gateway-core |
| ProtocolModule SPI | — | `io.github.lunasaw.sipgateway.core.api.ProtocolModule` | gateway-core |
| TransactionContextStore | — | `io.github.lunasaw.sipgateway.core.api.TransactionContextStore<K,V>` | gateway-core |
| store 接口（GB28181） | [gateway/InviteContextStore.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InviteContextStore.java) | `io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore`（extends TransactionContextStore） | gateway-gb28181 |
| store 默认实现 | [gateway/InMemoryInviteContextStore.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InMemoryInviteContextStore.java) | `io.github.lunasaw.sipgateway.gb28181.store.InMemoryInviteContextStore` | gateway-gb28181 |
| 业务推送接口 | [gateway/BusinessNotifier.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/BusinessNotifier.java) | `io.github.lunasaw.sipgateway.core.api.BusinessNotifier`（envelope 化，单方法 notify） | gateway-core |
| 默认推送实现 | [gateway/NoopBusinessNotifier.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/NoopBusinessNotifier.java) | `io.github.lunasaw.sipgateway.core.notifier.NoopBusinessNotifier` + `AbstractProtocolBusinessNotifier` | gateway-core |
| 事件转发器 | [gateway/SipEventForwarder.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/SipEventForwarder.java) | `io.github.lunasaw.sipgateway.gb28181.forwarder.Gb28181EventForwarder`（35 emit，全带 gb28181. 前缀） | gateway-gb28181 |
| ProtocolModule 实现 | — | `io.github.lunasaw.sipgateway.gb28181.handler.Gb28181Module` | gateway-gb28181 |
| 命令表 | — | `io.github.lunasaw.sipgateway.gb28181.handler.Gb28181CommandSpecs`（39 行） | gateway-gb28181 |
| 注解白名单 | — | `io.github.lunasaw.sipgateway.gb28181.handler.Gb28181WhitelistHandlers`（~20 个 @CommandMapping） | gateway-gb28181 |
| HTTP API（核心分发） | [gateway/SipCommandController.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/SipCommandController.java)（部分） | `io.github.lunasaw.sipgateway.core.web.GatewayDispatchController`（POST /gateway/command） | gateway-core |
| HTTP API（INVITE 回包） | [gateway/SipCommandController.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/SipCommandController.java)（部分） | `io.github.lunasaw.sipgateway.gb28181.web.Gb28181InviteResponseController`（POST /gateway/gb28181/invite/response） | gateway-gb28181 |
| 5 个 DTO | [gateway/dto/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/dto/) | 仅保留 `io.github.lunasaw.sipgateway.gb28181.dto.InviteResponseRequest`，其余 4 个删除（envelope 化） | gateway-gb28181 |
| Starter | — | `io.github.lunasaw:sip-gateway-spring-boot-starter`（无代码，仅 pom + AutoConfiguration.imports） | sip-gateway-spring-boot-starter |
| BOM | — | `io.github.lunasaw:sip-gateway-bom`（pom only） | sip-gateway-bom |

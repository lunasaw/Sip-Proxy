# gb28181-gateway 模块化方案

> 版本：1.2 草案 | 日期：2026-05-27 | 关联：[LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) v2.5、[OUTBOUND-DIALOG-PLAN.md](../1.7.0/OUTBOUND-DIALOG-PLAN.md) v1.2、[HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)
>
> 目标：把现已落在 [gb28181-test/.../gateway/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 的"业务方网关"参考实现，从测试代码升级为正式模块 `gb28181-gateway`，让业务方以 Maven 依赖一键接入分层架构方案，避免当前必须复制粘贴 10+ 文件的接入门槛。
>
> **v1.2 变更**：拆分为两步走 —— 先发布 **1.7.3**（`ServerSessionEvent` / `DeviceSessionListener` 增 `rawSdp` 字段，协议层小升级），再发布 **1.8.0**（gateway 模块上线），降低单次破坏性变更范围。

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
2. 创建 Maven 模块骨架
3. 包名重定向 `gbproxy.test.gateway` → `gbproxy.gateway`
4. 添加 Spring Boot 3 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
5. 编写自动装配条件（避免污染只用 SIP 协议的业务方）
6. 配置 namespace 从 `gateway.*` 迁到 `gb28181.gateway.*`，附 `additional-spring-configuration-metadata.json` deprecation 元数据
7. 测试用例从 gb28181-test 迁出，作为模块自身单测；父 pom 补 jacoco `check` goal 让 80% 阈值真正生效
8. CHANGELOG + 用户接入文档

预估工作量：

- 1.7.3 协议层小升级：**0.5 天**（含 5 处链路改动 + 测试同步）
- 1.8.0 gateway 主模块：**3 个工作日**
- gb28181-gateway-redis 扩展（v1.9.0 候选）：另行 1 天

---

## 二、模块定位与职责

### 2.1 在分层架构中的位置

```
┌──────────────────────────────────────────────────────────┐
│  业务服务器（业务方实现，进程外）                              │
│  接收 BusinessNotifier 推送 / 调 sip-gateway HTTP API       │
└──────────────────────────┬───────────────────────────────┘
                           │ HTTP / MQ
┌──────────────────────────▼───────────────────────────────┐
│  gb28181-gateway（本模块） ★                              │
│  ├── HTTP API：SipCommandController（→ ServerCommandSender）│
│  ├── 事件转发：SipEventForwarder（listener → BusinessNotifier）│
│  ├── 跨节点路由：InviteContextStore + nodeAddressMap        │
│  └── Spring Boot Autoconfig：@ConditionalOnXxx 守门        │
└──────────────────────────┬───────────────────────────────┘
                           │ Maven 依赖（同 JVM）
┌──────────────────────────▼───────────────────────────────┐
│  gb28181-server  +  gb28181-client（可选，级联场景）        │
│  ServerCommandSender / DeviceLifecycleListener / ...      │
└───────────────────────────────────────────────────────────┘
```

**关键约束**（来自 LAYERED-ARCHITECTURE.md §2.2，不可违反）：

- **必须与 sip-proxy 同 JVM**：`SipTransactionRegistry`、`Dialog` 都是进程内对象，跨进程后无法回包
- **不持有可外化业务状态**：`DeviceSessionCache` / `ServerDeviceSupplier` 由业务方实现并落 Redis，gateway 只是协调者
- **stateless service 视角**：除 `processedInvites` 幂等缓存外不持有可变状态，重启后通过 Redis（业务方实现）恢复

### 2.2 与现有模块的关系

| 模块 | gateway 关系 | 依赖方向 |
|------|------------|---------|
| sip-common | 间接依赖（通过 server） | gateway → server → sip-common |
| gb28181-common | 间接依赖（通过 server） | gateway → server → gb28181-common |
| gb28181-server | **强依赖**（`ServerCommandSender` / 4 个 listener） | gateway → server |
| gb28181-client | **不依赖**（默认） | 业务方需要级联时自行同时引入 client |
| gb28181-test | gb28181-test → gateway（迁移后 test 引用 gateway 做集成测试） | test → gateway |

**不引入循环依赖**：gateway 永远只依赖 server，不被 server / client / common 任何模块依赖。

---

## 三、技术方案

### 3.1 模块骨架

```
sip-proxy/
├── gb28181-gateway/                          # 新增
│   ├── pom.xml
│   └── src/main/
│       ├── java/io/github/lunasaw/gbproxy/gateway/
│       │   ├── api/
│       │   │   ├── BusinessNotifier.java       # 业务推送接口
│       │   │   └── InviteContextStore.java     # 跨节点上下文存储接口
│       │   ├── config/
│       │   │   ├── GatewayProperties.java      # @ConfigurationProperties("gb28181.gateway")
│       │   │   ├── GatewayAutoConfiguration.java
│       │   │   └── GatewayWebMvcConfiguration.java
│       │   ├── dto/
│       │   │   ├── ByeRequest.java
│       │   │   ├── CatalogQueryRequest.java
│       │   │   ├── InviteResponseRequest.java
│       │   │   ├── InviteStartRequest.java
│       │   │   └── PtzRequest.java
│       │   ├── notifier/
│       │   │   └── NoopBusinessNotifier.java   # 默认实现
│       │   ├── store/
│       │   │   └── InMemoryInviteContextStore.java
│       │   ├── web/
│       │   │   └── SipCommandController.java   # @RestController
│       │   └── forwarder/
│       │       └── SipEventForwarder.java      # 实现 3 个 listener
│       └── resources/META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── pom.xml                                    # <module> 增加 gb28181-gateway
```

**包名约定**：`io.github.lunasaw.gbproxy.gateway.*`（去掉 test 命名）。

### 3.2 Maven 配置

#### 父 pom 变更

```xml
<modules>
    <module>sip-common</module>
    <module>gb28181-common</module>
    <module>gb28181-client</module>
    <module>gb28181-server</module>
    <module>gb28181-gateway</module>   <!-- 新增 -->
    <module>gb28181-test</module>      <!-- 仍保留，迁移后引用 gateway 做集成测试 -->
</modules>
```

`<dependencyManagement>` 增加：

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gb28181-gateway</artifactId>
    <version>${gb28181-proxy.version}</version>
</dependency>
```

#### gb28181-gateway/pom.xml

```xml
<dependencies>
    <!-- 强依赖：server 的 ServerCommandSender、4 个 listener 接口 -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gb28181-server</artifactId>
    </dependency>

    <!-- INVITE 重传幂等缓存（已在父 pom dependencyManagement 中） -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- spring-web：HTTP API 用，必填，业务方不需要 web 时
         可通过 <exclusions> 排除（GatewayAutoConfiguration 用 ConditionalOnClass 守门） -->
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

**`<optional>true</optional>` 的语义**：业务方引入 gb28181-gateway 时不会传递 spring-web，必须自己引入 `spring-boot-starter-web`（或排除 HTTP API 部分）。这是有意设计——纯 forwarder 用法可以不要 web。

### 3.3 核心扩展点设计

#### 3.3.1 BusinessNotifier（必须由业务方提供生产实现）

`inviteIncoming` **同时透传 rawSdp 与解析后的 `GbSessionDescription`**，业务方按需取用：

```java
package io.github.lunasaw.gbproxy.gateway.api;

public interface BusinessNotifier {
    void deviceOnline(String deviceId, RegisterInfo registerInfo);

    /**
     * 设备主动 INVITE（语音对讲、级联拉流等）。业务方异步处理后通过
     * {@code transactionContextKey} 取回 RequestEvent 完成回包。
     *
     * @param rawSdp 原始 SDP 文本（UTF-8 解码自 INVITE body），**直接转给 ZLM/SRS 推流时用此参数**，
     *               避免 GbSessionDescription 反向序列化丢字段（自定义 a= 行、y=ssrc、f= 视频参数等）
     * @param parsed 已解析的 SDP 模型，**业务侧抠 ssrc / m-line 端口做流匹配时用此参数**，
     *               省去重复 SDP 解析；可能为 null（INVITE 无 body 或解析失败）
     */
    void inviteIncoming(String callId, String fromUserId, String toUserId,
                        String rawSdp,
                        GbSessionDescription parsed,
                        String transactionContextKey);

    void alarm(String deviceId, DeviceAlarmNotify notify);
}
```

**API 暴露 `GbSessionDescription` 的合理性**：业务方本来就要 `<dependency>gb28181-common</dependency>` 才能用本框架的语义类型，gateway → server → gb28181-common 这条传递依赖天然存在；让业务方自己重做一份 SDP 解析反而是过度净化。

**rawSdp 上溯改动**（隶属 1.7.3，先于 gateway 模块化落地）：

| 文件 | 改动 |
|------|------|
| [ServerInviteRequestProcessor.java:75-80](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/invite/ServerInviteRequestProcessor.java#L75-L80) | 把 `new String(rawContent, UTF_8)` 提为局部变量 `rawSdp`，传给 `ServerSessionEvent.serverInvite(...)` |
| [ServerSessionEvent.java](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/event/ServerSessionEvent.java) | 加 `private final String rawSdp` 字段；`serverInvite(...)` 工厂增参 |
| [DeviceSessionListener.java:38-39](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/api/DeviceSessionListener.java#L38-L39) | `onServerInvite` 增 `String rawSdp` 参数 |
| [ServerListenerAdapter.java:169-170](../../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/event/internal/ServerListenerAdapter.java#L169-L170) | `e.getRawSdp()` 透传 |
| `SipEventForwarder` + `BusinessNotifier.inviteIncoming` | 透传到业务层 |

**实现策略**（v1.0 落地）：

- 主模块仅提供 `NoopBusinessNotifier`（日志输出，单机演示用）
- 文档给出 `RestTemplateBusinessNotifier` 模板（30 行示例）
- **不内置具体 HTTP/MQ 实现**——避免引入 RestTemplate/WebClient/Spring Cloud Stream 依赖污染

**异步约束**：javadoc 明确"实现必须异步"，否则会阻塞 SIP 事件线程导致设备超时重传。给出 `@Async` 标准用法。

#### 3.3.2 InviteContextStore（多节点必须替换为 Redis）

`find` 返回值语义：`{nodeId, ctxKey}` 二元组。`SipCommandController` 逻辑为：

```
find(callId) → InviteContext{nodeId, ctxKey}
  → nodeId == 本节点? 直接处理 : 转发到 nodeAddressMap[nodeId]
```

接口定义：

```java
package io.github.lunasaw.gbproxy.gateway.api;

public interface InviteContextStore {
    void save(String callId, String nodeId, String ctxKey, long ttlMs);
    /** 返回 null 表示不存在（→ 410）；抛 ResponseStatusException(503) 表示后端故障 */
    InviteContext find(String callId);
    void remove(String callId);

    record InviteContext(String nodeId, String ctxKey) {}
}
```

**实现策略**：

- 主模块提供 `InMemoryInviteContextStore`（Caffeine，单机/单测）
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

可选演进：单独建子模块 `gb28181-gateway-redis`（依赖 spring-data-redis）。本期暂缓，看用户需求。

#### 3.3.3 SipEventForwarder（成品转发器）

直接复用现有实现，关键变化：

- 实现接口移除 `SipBusinessConfig`（测试模块的 `DeviceSessionCache` 实现），改为构造函数注入 `Optional<DeviceSessionCache>`：业务方未提供时退化为"只推送不缓存设备会话"，由业务方自行决定是否在 BusinessNotifier 里写库
- `processedInvites` 缓存继续按 callId 幂等
- 加上 `@ConditionalOnBean(BusinessNotifier.class)` 守门——理论上 NoopBusinessNotifier 会作为兜底，永远成立

#### 3.3.4 SipCommandController（HTTP API）

直接复用现有实现，零功能变化：

- `POST /sip/invite/start` / `/sip/invite/bye` / `/sip/invite/response` / `/sip/control/ptz` / `/sip/query/catalog` / `/sip/whoami`
- 错误码契约与 LAYERED-ARCHITECTURE.md §6.4 严格一致（410 / 502 / 503）
- 用 `@ConditionalOnWebApplication(type = SERVLET)` 守门

**新增建议（v1.1 候选）**：

- `POST /sip/invite/refresh-ringing` — 业务处理 25s 边界主动发 180 Ringing + extendContext
- `GET /sip/dialogs/{callId}` — 调试用，回看 DialogRegistry 状态
- `POST /sip/subscribe/refresh` / `/sip/subscribe/unsubscribe` — 对接 1.7.0 新增的 SUBSCRIBE 续订 API

v1.0 仅做现有 6 个端点，新端点等用户反馈再加。

### 3.4 Spring Boot 自动装配

```java
@AutoConfiguration
@EnableConfigurationProperties(GatewayProperties.class)
@ConditionalOnClass({ServerCommandSender.class})
public class GatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InviteContextStore inviteContextStore(GatewayProperties props) {
        return new InMemoryInviteContextStore(props.getInviteContextTtlMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessNotifier businessNotifier() {
        return new NoopBusinessNotifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public SipEventForwarder sipEventForwarder(GatewayProperties props,
                                               InviteContextStore store,
                                               BusinessNotifier notifier,
                                               ObjectProvider<DeviceSessionCache> sessionCache) {
        return new SipEventForwarder(props, store, sessionCache.getIfAvailable(), notifier);
    }

    /** Web 子配置：仅当 servlet 环境下启用 */
    @AutoConfiguration(after = GatewayAutoConfiguration.class)
    @ConditionalOnWebApplication(type = SERVLET)
    @ConditionalOnClass(RestController.class)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public SipCommandController sipCommandController(
                GatewayProperties props,
                ServerCommandSender sender,
                InviteContextStore store,
                @Qualifier("gatewayForwardRestTemplate") RestTemplate forwardRestTemplate) {
            return new SipCommandController(props, sender, store, forwardRestTemplate);
        }

        /**
         * 专用于跨节点转发的 RestTemplate，命名限定避免与业务方 RestTemplate 冲突。
         * 不加 @ConditionalOnProperty(nodes)：单节点也可能需要 RestTemplate 推送业务服务器。
         */
        @Bean("gatewayForwardRestTemplate")
        @ConditionalOnMissingBean(name = "gatewayForwardRestTemplate")
        public RestTemplate gatewayForwardRestTemplate() {
            return new RestTemplate();
        }
    }
}
```

**`@Qualifier` 强约束**：`SipCommandController` 构造函数显式 `@Qualifier("gatewayForwardRestTemplate")`，避免业务方容器内已有其他 `RestTemplate` Bean 时的注入歧义。`ObjectProvider<RestTemplate>` 不能解决重名问题，必须用 qualifier。

**`AutoConfiguration.imports`**：

```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.github.lunasaw.gbproxy.gateway.config.GatewayAutoConfiguration
```

**配置 namespace 重定向**：从 `gateway.*` 改为 `gb28181.gateway.*`，与 `sip:` / `gb28181:` 命名风格一致。提供 1 个版本的 deprecation alias（`gateway.*` → `gb28181.gateway.*` 仍可识别但 warn）。

**Deprecation 元数据**：写入 `src/main/resources/META-INF/additional-spring-configuration-metadata.json`，让 IDE 在用户配置 `gateway.*` 时直接弹出迁移提示：

```json
{
  "properties": [
    {
      "name": "gateway.node-id",
      "type": "java.lang.String",
      "deprecation": {
        "replacement": "gb28181.gateway.node-id",
        "level": "warning",
        "reason": "v1.8.0 起统一使用 gb28181.gateway.* 前缀，与 sip:/gb28181: 风格对齐"
      }
    }
  ]
}
```

`GatewayProperties` 内同步加 `@DeprecatedConfigurationProperty` 注解（spring-boot 3.x 原生支持），运行时 warn 一次。

**JaCoCo 80% 阈值真正生效**：父 pom 当前只跑 `prepare-agent` + `report`，没有 `check` goal——所谓"80% line coverage"是文档约定，不是 maven 阻断。本期顺手补上：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>jacoco-check</id>
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

### 3.5 多节点扩展模块（可选，v1.1+）

> v1.0 不做，先看用户对 in-memory + 自行接 Redis 模板的反馈。

#### 3.5.1 gb28181-gateway-redis（候选）

```
gb28181-gateway-redis/
└── src/main/java/.../gateway/redis/
    ├── RedisInviteContextStore.java   # 实现 InviteContextStore
    └── RedisGatewayAutoConfiguration.java
```

依赖：`spring-boot-starter-data-redis`。`@ConditionalOnClass(RedisTemplate.class)` 守门。

#### 3.5.2 gb28181-gateway-discovery（候选）

`nodeAddressMap` 的 K8s Endpoints / Nacos / Consul 实现。各装配方式差异大，更适合做单独子模块（按需选择），主模块永远只支持静态配置。

### 3.6 错误语义契约

完全继承 LAYERED-ARCHITECTURE.md §6.4 的契约（v1.0 不变）：

| HTTP Status | 触发条件 | 业务侧行为 |
|------------|---------|----------|
| 200 OK | 正常处理 | 正常流程 |
| 410 Gone | 事务已超时 / `find()` 返回 null | **禁止重试**，重新发起 INVITE |
| 502 Bad Gateway | `nodeAddressMap` 不含目标节点 | 200ms × 3 短重试，覆盖刷新窗口 |
| 503 Service Unavailable | 跨节点转发失败 / store 后端不可达 | 200ms × 3 短重试 |
| 500 Internal Server Error | 未受控异常（不应出现） | 视作 bug，告警 + 不重试 |

`SipCommandController.findContextOrTranslate` 已实现 503 兜底（`RuntimeException` → 503），保持不动。

### 3.7 测试策略

#### 3.7.1 单元测试（gb28181-gateway 自己负责）

- `GatewayPropertiesTest` — 配置解析 + 默认值
- `InMemoryInviteContextStoreTest` — TTL / find / remove 边界
- `SipEventForwarderTest` — 用 Mockito 注入 fake `BusinessNotifier`，验证：
  - 注册事件触发 `deviceOnline`（含 NAT IP 切换覆盖语义）
  - INVITE 重传同 callId 在幂等窗口内只推送一次
  - 告警事件直推
- `SipCommandControllerTest` — `MockMvc` 测试：
  - 本节点处理 → 触发 `ResponseCmd.sendResponse`
  - 跨节点转发 → 验证 `RestTemplate.postForObject` 调用
  - store 故障 → 503
  - 事务超时 → 410
  - nodeAddressMap miss → 502

#### 3.7.2 集成测试（gb28181-test 模块覆盖）

- 全链路 INVITE Play：业务调 `/sip/invite/start` → 模拟设备回 200 OK → DeviceInviteOkEvent 触发 → BusinessNotifier 收推送
- 设备主动 INVITE：模拟设备发 INVITE → `ServerInviteEvent` → BusinessNotifier 收推送 → 业务调 `/sip/invite/response` 完成回包
- 多节点 INVITE 路由：用 `MockRestServiceServer` mock 跨节点 HTTP 调用，验证 `SipCommandController` 在 `find(callId).nodeId != localNodeId` 时转发到正确地址；不需要真正起两个 ApplicationContext（同 JVM 内 `SipTransactionRegistry` 无法真正隔离）

#### 3.7.3 覆盖率要求

继承父 pom JaCoCo 80% line coverage 阈值。Web 部分用 `@WebMvcTest` 切片测试。

### 3.8 文档与发布

#### 3.8.1 模块自带 README

`gb28181-gateway/README.md`：

- 5 分钟快速开始（Maven 依赖 + `application.yml` + 实现 `BusinessNotifier`）
- 单机部署示例
- 多节点部署示例（含 Redis store 模板代码 + nodeAddressMap），**`sip.server.external-ip: <VIP>` 标注为必填项**——多节点下节点 IP 不可达，必须填 VIP 地址，否则设备回包绕过 VIP 导致源 IP 哈希失效（详见 [HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)）
- 错误码契约
- FAQ：与 sip-proxy 必须同 JVM 的原因、为什么不内置 RestTemplate/Redis

#### 3.8.2 LAYERED-ARCHITECTURE.md 更新

§6 整节从"参考实现散落在 gb28181-test"改为"引入 gb28181-gateway 即可"，§9 对照表增加：

```
| gb28181-gateway 模块 | gb28181-gateway/ | ✅ 1.x.0 就绪 |
```

#### 3.8.3 CHANGELOG

```markdown
## [1.x.0]

### Added
- 新增 gb28181-gateway 模块，把 LAYERED-ARCHITECTURE §6 描述的业务方网关参考实现升级为正式 Maven 依赖。
- 提供 `@ConditionalOnMissingBean` 自动装配：InviteContextStore / BusinessNotifier / SipEventForwarder / SipCommandController。
- HTTP API 错误码契约与 §6.4 一致（410 / 502 / 503）。

### Migration
- 旧用户从 gb28181-test 复制 gateway 包的，删除复制代码，引入 `<dependency>gb28181-gateway</dependency>`。
- 配置 namespace 从 `gateway.*` 改为 `gb28181.gateway.*`，旧 namespace 兼容一个版本后移除。
```

---

## 四、迁移步骤（从 gb28181-test 抽离）

### Stage 0（1.7.3 协议层小升级，0.5 天）— gateway 模块化前置
- `ServerSessionEvent` 增 `private final String rawSdp` 字段，`serverInvite(...)` 工厂方法增参
- `DeviceSessionListener.onServerInvite` 接口签名增 `String rawSdp` 参数（**破坏性变更**，CHANGELOG 强提示）
- `ServerInviteRequestProcessor` 把已经 decode 过的 SDP 文本透传到事件
- `ServerListenerAdapter` 透传新字段到 listener
- 同步更新 `ServerGb28181Adapter`、单测、`gb28181-test` 现有 `SipEventForwarder` 实现签名
- 发版 1.7.3，让老用户先适配 listener 接口；**不动 gateway/ 包目录**
- 验收：`mvn verify` 全绿；`scripts/check-sip-common-purity.sh` 通过；listener 单测覆盖 rawSdp 路径

### Stage 1：建模块骨架（0.5 天）
- 新增 `gb28181-gateway/pom.xml`
- 父 pom 加 `<module>` 与 `dependencyManagement` 条目
- 新建包 `io.github.lunasaw.gbproxy.gateway.{api,config,dto,notifier,store,web,forwarder}`

### Stage 2：迁移代码（0.5 天）
- 把 [gb28181-test/.../gateway/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) 14 个文件复制过去，重定向包名
- `GatewayProperties` 配置前缀改 `gb28181.gateway`，加 `@DeprecatedConfigurationProperty` 兼容旧前缀
- `SipEventForwarder` 解除对 `SipBusinessConfig` 的硬依赖，改为 `Optional<DeviceSessionCache>`
- `BusinessNotifier.inviteIncoming` 签名改为双参数版本（rawSdp + parsed），透传 1.7.3 引入的 rawSdp
- 写 `META-INF/additional-spring-configuration-metadata.json` deprecation 元数据

### Stage 3：自动装配（0.5 天）
- 新建 `GatewayAutoConfiguration` + 内嵌 `WebConfig`
- 添加 `META-INF/spring/AutoConfiguration.imports`
- `SipCommandController` 构造函数显式 `@Qualifier("gatewayForwardRestTemplate")`
- 验证：在空白 Spring Boot 应用引入依赖，能注入 `SipEventForwarder` 和 `SipCommandController`

### Stage 4：迁移测试 & 强化覆盖率门禁（0.5 天）
- gb28181-test 中 gateway 相关单测迁过来
- 新增 `MockMvc` web 切片测
- 父 pom 补 jacoco `check` execution（`<haltOnFailure>true` + 80% LINE/COVEREDRATIO），让"80% 覆盖率"从口头约定升级为构建阻断
- 跑 `mvn verify` 通过

### Stage 5：gb28181-test 改用模块（0.5 天）
- 删除 `test/gateway/` 旧代码
- gb28181-test/pom.xml 加 `<dependency>gb28181-gateway</dependency>`
- 测试模块只保留 `SipBusinessConfig`（DeviceSessionCache 测试实现）+ `TestServerDeviceSupplier` 等业务方抽象

### Stage 6：文档与版本（0.5 天）
- 更新 LAYERED-ARCHITECTURE.md §6/§9
- 写 gb28181-gateway/README.md
- CHANGELOG 加 1.7.3（rawSdp 破坏性变更）+ 1.8.0（gateway 模块上线）两条版本条目

**总计：0.5 天 (Stage 0) + 3 天 (Stage 1-6) = 3.5 个工作日**。

---

## 五、风险与备选

### 5.1 风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| spring-web 列为 `<optional>` 后业务方忘记自己引入 starter | 启动时 NoSuchBeanDefinitionException | `@ConditionalOnClass(RestController.class)` 守门，不报错只是 Web 端点不暴露 + README 强提示 |
| 业务方误用 `NoopBusinessNotifier` 上线 | 设备事件丢失，业务无感知 | NoopBusinessNotifier 启动时 `log.warn` 提示"NoopBusinessNotifier active, replace before production" |
| 业务方误用 `InMemoryInviteContextStore` 多节点部署 | 跨节点 INVITE 回包路由失败 | `InMemoryInviteContextStore` 启动时**无条件** `log.warn("InMemoryInviteContextStore active — replace with Redis implementation before multi-node deployment")`，不依赖 `nodes` 配置判断（`nodes` 为空不代表单节点）；同时在 README 多节点部署章节标注为必替换项 |
| 配置 namespace 改名 (`gateway.*` → `gb28181.gateway.*`) 破坏老用户 | 升级后配置不生效 | 旧 namespace 兼容一个版本，启动 warn |

### 5.2 备选方案

**备选 A：不模块化，只整理参考实现**
- 在 doc/ 下产出"标准 sip-gateway 模板"目录，业务方 clone 即用
- 优点：零依赖污染
- 缺点：无法 Maven 升级，与现状无本质区别 → **拒绝**

**备选 B：模块化但不暴露 HTTP API**
- gb28181-gateway 只提供 forwarder + store，HTTP API 让业务方自己写
- 优点：彻底无 spring-web 依赖
- 缺点：业务方还是要复制 `SipCommandController` 那 100 行跨节点路由逻辑——这是最容易写错的部分 → **拒绝**

**备选 C：模块化 + HTTP API + 内置 Redis 实现**
- 主模块包含 `RedisInviteContextStore`，依赖 spring-data-redis
- 优点：开箱即用
- 缺点：所有用户被迫引入 lettuce 客户端，单机演示场景下变重 → **本期拒绝，作为可选子模块 gb28181-gateway-redis 在 v1.1 提供**

**最终选择：§3 描述的方案**——主模块 web optional + Redis 文档示例 + 单独子模块按需。

---

## 六、未来演进

| 版本 | 计划项 |
|------|--------|
| **1.7.3** | **协议层小升级前置**：`ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)`（破坏性签名变更，让老用户先适配 listener） |
| **1.8.0** | **本方案落地**：gb28181-gateway 模块化主体（基于 1.7.3 已就绪的 rawSdp 透传链路） |
| 1.9.0 | gb28181-gateway-redis 扩展模块（Redis InviteContextStore + DeviceSessionCache 参考实现） |
| 1.10.0 | gb28181-gateway-discovery（K8s Endpoints / Nacos 适配 nodeAddressMap） |
| 待定 | 客户端侧网关（gb28181-client-gateway）—— 当前需求未明确，v1.0 不规划 |
| 待定 | OpenAPI/Swagger 自动生成 HTTP 文档 |
| 待定 | gateway 内置 metrics（INVITE 成功率 / 跨节点转发耗时 / store 命中率） |

---

## 七、决策清单

需用户/团队决策的设计选项：

1. **`BusinessNotifier.inviteIncoming` 是否传 raw SDP**：✅ **已确认必传**
   - 原因：业务侧需透传 SDP 给流媒体节点（ZLM/SRS）做拉流/推流。`GbSessionDescription` 反向序列化存在丢字段风险（自定义 a= 行、y=ssrc、f= 视频参数、厂商方言）
   - 决议：双参数（rawSdp + 解析后的 GbSessionDescription），见 §3.3.1
   - 联动改动：1.7.3 先发 `ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)`

2. **配置 namespace**：`gateway.*` 还是 `gb28181.gateway.*`？  
   推荐后者，与 `sip:` / `gb28181:` 风格一致。需团队确认是否接受 namespace 变动；提供 `@DeprecatedConfigurationProperty` + `additional-spring-configuration-metadata.json` 一个版本平滑迁移。

3. **spring-web 依赖**：`<optional>true</optional>` 还是直接依赖？  
   推荐 optional，纯事件转发场景免引入 web。需确认接入复杂度可接受。

4. **是否提供 RestTemplate Notifier 模板代码到主模块**？  
   推荐不提供，文档里给示例；主模块只 Noop。需确认。

5. **首版本号**：建议拆两步——1.7.3（rawSdp 协议层小升级）+ 1.8.0（gateway 模块上线）。

6. **gb28181-gateway-redis / gb28181-gateway-discovery**：本期是否一起做？  
   推荐分批：1.8.0 出主模块，1.9.0 出 Redis，看用户反馈再决定 discovery。

7. **JaCoCo 80% 阈值**：是否本期顺手把父 pom 的 `check` goal 加上，让目前只是文档约定的覆盖率门禁真正阻断构建？  
   推荐加。详见 §3.4。

---

## 八、附录：当前参考实现关键代码片段索引

供迁移时直接对照：

| 功能 | 现位置 | 迁移后 |
|------|--------|--------|
| 配置类 | [gateway/GatewayProperties.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/GatewayProperties.java) | `gb28181-gateway/.../config/GatewayProperties.java` |
| 装配类 | [gateway/GatewayConfig.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/GatewayConfig.java) | `gb28181-gateway/.../config/GatewayAutoConfiguration.java`（拆分 + 加守门条件） |
| store 接口 | [gateway/InviteContextStore.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InviteContextStore.java) | `gb28181-gateway/.../api/InviteContextStore.java` |
| store 默认实现 | [gateway/InMemoryInviteContextStore.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/InMemoryInviteContextStore.java) | `gb28181-gateway/.../store/InMemoryInviteContextStore.java` |
| 业务推送接口 | [gateway/BusinessNotifier.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/BusinessNotifier.java) | `gb28181-gateway/.../api/BusinessNotifier.java` |
| 默认推送实现 | [gateway/NoopBusinessNotifier.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/NoopBusinessNotifier.java) | `gb28181-gateway/.../notifier/NoopBusinessNotifier.java` |
| 事件转发器 | [gateway/SipEventForwarder.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/SipEventForwarder.java) | `gb28181-gateway/.../forwarder/SipEventForwarder.java`（去除对 SipBusinessConfig 硬依赖） |
| HTTP API | [gateway/SipCommandController.java](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/SipCommandController.java) | `gb28181-gateway/.../web/SipCommandController.java` |
| 5 个 DTO | [gateway/dto/](../../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/dto/) | `gb28181-gateway/.../dto/`（包名重定向） |

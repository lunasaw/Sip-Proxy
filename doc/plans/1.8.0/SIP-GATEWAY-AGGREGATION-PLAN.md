# sip-gateway 父聚合演化方案（1.8.0 主纲领）

> 版本：1.0 草案 | 日期：2026-05-28 | 关联：[UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md)、[GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md)、[LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) v2.5、[PROTOCOL-LAYERING-MATRIX.md](../../architecture/PROTOCOL-LAYERING-MATRIX.md)

> **本方案是 1.8.0 三件套主纲领**：负责拍板"sip-gateway 长成什么样、未来加协议是不是纯加法"。具体的 envelope 字段语义见 [UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md)，具体的代码迁移步骤见 [GB28181-GATEWAY-MODULE-PLAN](GB28181-GATEWAY-MODULE-PLAN.md)，两个文档须与本方案保持一致。

> **核心命题**：sip-gateway 是一个 Maven 父聚合，内部按协议拆子模块（gateway-core / gateway-gb28181 / 未来的 gateway-onvif / gateway-gt1078 ...），业务方引一个 starter 就拿到全套设备网关能力，引 BOM 就锁定一组协议子模块版本。1.8.0 一次锁好所有跨协议契约（type 命名空间、HTTP path、配置前缀、SPI 形态），未来加协议=纯加法、零破坏。

---

## 一、设计目标

| 维度 | 现状（1.7.x） | 目标（1.8.0） |
|------|--------------|---------------|
| 业务方接入 | 复制 [gb28181-test/.../gateway/](../../../gb28181-test/src/test/java/io/github/lunasaw/gbproxy/test/gateway/) 14 个文件 | `<dependency>sip-gateway-spring-boot-starter</dependency>` 即用 |
| 协议扩展 | 加协议 = 改 HTTP controller / DTO / BusinessNotifier 接口 | 加协议 = 新建 `gateway-{proto}` 子模块、`POM` 加一行、starter 自动带上 |
| 模块拓扑 | 业务方网关参考实现寄居 gb28181-test | sip-gateway 父聚合 + gateway-core + gateway-gb28181 + BOM + starter |
| type 命名 | 6 个固定 HTTP path 各自携带语义 | `<protocol>.<Group>.<Name>` 三段式，按协议命名空间隔离 |
| 配置前缀 | `gateway.*`（散落） | `gateway.*`（核心） + `gateway.{protocol}.*`（协议子前缀） |
| 跨节点路由 | 仅 SIP INVITE | `TransactionContextStore<K,V>` 泛化，每协议自管事务存储 |

**两个不变量**：
1. **核心壳协议中立**：`gateway-core` 不依赖任何具体协议模块（CI 强制 + 纯度检查脚本）
2. **协议适配器自治**：`gateway-{proto}` 通过 `ProtocolModule` SPI 向核心注册自己的命令/事件/事务存储/特殊端点

---

## 二、模块拓扑

### 2.1 仓库内子树（最终形态，含未来占位）

```
sip-proxy/                                    (parent pom，仓库根)
├── sip-common/                               (协议栈)
├── gb28181-common/
├── gb28181-client/
├── gb28181-server/
├── sip-gateway/                              ★ 新增父聚合（packaging=pom）
│   ├── pom.xml                               aggregator + dependencyManagement
│   ├── gateway-core/                         ★ 协议中立内核（envelope/registry/web/notifier）
│   ├── gateway-gb28181/                      ★ GB28181 协议适配器
│   ├── sip-gateway-bom/                      ★ BOM（pom only）
│   ├── sip-gateway-spring-boot-starter/      ★ 一键接入 starter
│   ├── gateway-onvif/                        🔜 1.10.0 新增（占位，1.8.0 不建空模块）
│   └── gateway-gt1078/                       🔜 1.11.0 新增
└── gb28181-test/
```

**1.8.0 实际落地的子模块为 4 个**：`gateway-core` + `gateway-gb28181` + `sip-gateway-bom` + `sip-gateway-spring-boot-starter`。其余协议模块以"占位文档"形式登记，不建空模块（避免空 pom 污染）。

### 2.2 为什么是仓库内子树而非独立仓库

| 维度 | 同仓库（推荐） | 独立仓库 |
|------|---------------|---------|
| 版本同步 | 与 sip-common / gb28181-server 共版本，CHANGELOG 一份 | 跨仓库版本号要手工对齐 |
| 同 JVM 约束 | gateway-gb28181 强依赖 gb28181-server（[LAYERED-ARCHITECTURE §2.2](../../architecture/LAYERED-ARCHITECTURE.md#L56)），分仓库依赖管理复杂 | 必须通过 Maven 制品引入 |
| 重构跨边界 | 一次 PR 即可（如调整 listener 签名 + gateway forwarder 同步） | 跨仓库 PR 链 |
| CI | 单 pipeline 全量构建 | 独立 pipeline，需触发链 |
| 参考案例 | spring-boot 与 spring-boot-starters / quarkus 与 quarkus-extensions | — |

**结论**：保持单仓库。`sip-gateway/` 作为父聚合存放在仓库根。

### 2.3 依赖方向（CI 强制单向）

```
sip-gateway-spring-boot-starter ──▶ gateway-core
                                ──▶ gateway-gb28181 ──▶ gateway-core
                                                    ──▶ gb28181-server ──▶ gb28181-common ──▶ sip-common

sip-gateway-bom (dependencyManagement only)
```

**禁止**：任何 `gateway-core → gateway-gb28181`、`gateway-onvif → gateway-gb28181`、`sip-common → gateway-*` 反向依赖。CI 在 `mvn verify` 中通过纯度脚本（参考 [check-sip-common-purity.sh](../../../scripts/check-sip-common-purity.sh)）拦截。

---

## 三、各子模块职责

### 3.1 `sip-gateway/`（父聚合，packaging=pom）

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

只负责聚合构建。**不产 jar**，业务方不直接引这个 artifactId。

### 3.2 `gateway-core`（协议中立内核）

```
gateway-core/src/main/java/io/github/lunasaw/sipgateway/core/
├── api/
│   ├── envelope/
│   │   ├── GatewayCommand.java                  # 入参信封
│   │   ├── GatewayCommandResult.java            # 出参信封
│   │   └── GatewayEvent.java                    # 回调信封
│   ├── BusinessNotifier.java                    # 单方法 notify(GatewayEvent)
│   ├── CommandHandler.java                      # SPI 接口（无 sender 形参）
│   ├── CommandMapping.java                      # 注解
│   ├── CommandSpec.java                         # 表条目（含 senderClass 字段）
│   ├── ParamBinding.java                        # 参数绑定 DSL
│   ├── ProtocolModule.java                      # ★ 协议适配器注册标记 SPI
│   └── TransactionContextStore.java             # 泛型事务存储基类
├── core/
│   ├── CommandHandlerRegistry.java              # 跨协议聚合，启动期 fail-fast
│   ├── ReflectiveCommandHandler.java            # 表条目运行期适配
│   ├── MethodInvokerHandler.java                # 注解方法运行期适配
│   └── PayloadCodec.java                        # fastjson2 二次反序列化封装
├── notifier/
│   ├── NoopBusinessNotifier.java                # 默认实现（仅日志，启动 warn）
│   └── AbstractProtocolBusinessNotifier.java    # 可选基类，按 protocol 拆 dispatch
├── web/
│   └── GatewayDispatchController.java           # POST /gateway/command + /gateway/whoami
└── config/
    ├── GatewayProperties.java                   # gateway.*（协议中立：nodeId / nodes / ...）
    └── GatewayCoreAutoConfiguration.java
```

**依赖**：`spring-context` + `spring-web`(optional) + `fastjson2` + `caffeine`。**绝不依赖**：`sip-common` / `gb28181-*` / `jain-sip-*` / 任何具体协议模块。

**纯度约束**（CI 强制）：

```bash
# scripts/check-gateway-core-purity.sh
SRC="sip-gateway/gateway-core/src/main/java"
FORBIDDEN='gb28181|GB28181|Gb28181|gbproxy|jain|sip\.common|sip\.message|onvif|Onvif|gt1078|rtsp'
grep -rEn "$FORBIDDEN" "$SRC" && { echo "❌ gateway-core must remain protocol-neutral"; exit 1; }
echo "✅ gateway-core purity OK"
```

挂在父 pom 的 `verify` 阶段，与 [check-sip-common-purity.sh](../../../scripts/check-sip-common-purity.sh) 并列。

### 3.3 `gateway-gb28181`（GB28181 协议适配器）

```
gateway-gb28181/src/main/java/io/github/lunasaw/sipgateway/gb28181/
├── handler/
│   ├── Gb28181CommandSpecs.java                 # 39 行表条目（type 全部带 gb28181. 前缀）
│   ├── Gb28181Module.java                       # ★ implements ProtocolModule
│   └── Gb28181WhitelistHandlers.java            # ~20 个 @CommandMapping 方法
├── forwarder/
│   └── Gb28181EventForwarder.java               # 4 listener × 35 方法 → BusinessNotifier
├── store/
│   ├── InviteContextStore.java                  # extends TransactionContextStore<String, InviteContext>
│   ├── InMemoryInviteContextStore.java          # Caffeine 默认实现
│   └── InviteContext.java                       # record(nodeId, ctxKey)
├── web/
│   └── Gb28181InviteResponseController.java     # POST /gateway/gb28181/invite/response
└── config/
    ├── Gb28181GatewayProperties.java            # gateway.gb28181.*
    └── Gb28181GatewayAutoConfiguration.java     # @ConditionalOnClass(ServerCommandSender.class)
```

**依赖**：`gateway-core` + `gb28181-server`。type 命名空间：`gb28181.<Group>.<Name>`（详见 §五）。

### 3.4 `sip-gateway-bom`（pom only，依赖管理入口）

```xml
<artifactId>sip-gateway-bom</artifactId>
<packaging>pom</packaging>
<dependencyManagement>
    <dependencies>
        <!-- 1.8.0 已发布 -->
        <dependency>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>gateway-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>gateway-gb28181</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>sip-gateway-spring-boot-starter</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- 占位：未来协议子模块在此追加 -->
        <!--
        <dependency><artifactId>gateway-onvif</artifactId>...</dependency>     1.10.0
        <dependency><artifactId>gateway-gt1078</artifactId>...</dependency>    1.11.0
        -->
    </dependencies>
</dependencyManagement>
```

业务方通过 `<scope>import</scope>` 引入 BOM 后，所有 sip-gateway 子模块免写版本号。

### 3.5 `sip-gateway-spring-boot-starter`（一键接入入口）

```xml
<artifactId>sip-gateway-spring-boot-starter</artifactId>
<dependencies>
    <!-- 核心，必带 -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gateway-core</artifactId>
    </dependency>

    <!-- 已发布的协议适配器，默认启用，业务方可 exclusions 排除 -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gateway-gb28181</artifactId>
        <optional>false</optional>
    </dependency>

    <!-- 未来协议（v1.10+）也走 default-on + @ConditionalOnClass 守门：
         业务方未引设备 SDK 时 AutoConfiguration 自动跳过，零运行代价 -->

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**仅 pom，无代码**。配套 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
io.github.lunasaw.sipgateway.core.config.GatewayCoreAutoConfiguration
io.github.lunasaw.sipgateway.gb28181.config.Gb28181GatewayAutoConfiguration
```

未来加协议时此文件追一行即可，业务方零改动。

---

## 四、ProtocolModule SPI 与跨协议契约

### 4.1 `ProtocolModule` 接口（gateway-core）

每个协议适配器必须提供一个 `ProtocolModule` 实现，向核心声明命名空间和命令清单：

```java
package io.github.lunasaw.sipgateway.core.api;

public interface ProtocolModule {
    /**
     * 协议命名空间，必须与 commandSpecs 的 type 第一段一致。
     * 例如 "gb28181"、"onvif"、"gt1078"。
     */
    String protocol();

    /**
     * 该协议的全部静态命令表。注解白名单方法独立扫描。
     */
    Collection<CommandSpec> commandSpecs();

    /**
     * 启动期注册顺序，一般保持 0；若需为 fail-fast 调整覆盖优先级再使用。
     */
    default int order() { return 0; }
}
```

### 4.2 `Gb28181Module` 实现（gateway-gb28181）

```java
@Component
@RequiredArgsConstructor
public class Gb28181Module implements ProtocolModule {

    private final ServerCommandSender sender;   // gateway-gb28181 内部注入

    @Override public String protocol() { return "gb28181"; }

    @Override public Collection<CommandSpec> commandSpecs() {
        return Gb28181CommandSpecs.declare();   // 39 行表条目
    }
}
```

`Gb28181CommandSpecs.declare()` 内部所有 type 字符串均以 `"gb28181."` 开头，详见 [UNIFIED-ENVELOPE-PLAN §3.4](UNIFIED-ENVELOPE-PLAN.md#L190)（修订后版本）。

### 4.3 `CommandHandlerRegistry` 跨协议聚合

```java
@Component
public class CommandHandlerRegistry {

    private final Map<String, CommandHandler> handlers;

    public CommandHandlerRegistry(ApplicationContext ctx,
                                  List<ProtocolModule> modules) {
        Map<String, CommandHandler> all = new HashMap<>();
        Map<String, String> typeOwner = new HashMap<>();   // type → protocol，便于诊断

        // 1) ProtocolModule 注册的静态表
        for (ProtocolModule m : modules.stream()
                .sorted(Comparator.comparingInt(ProtocolModule::order)).toList()) {
            for (CommandSpec spec : m.commandSpecs()) {
                if (!spec.type().startsWith(m.protocol() + ".")) {
                    throw new IllegalStateException(
                        "ProtocolModule '" + m.protocol() + "' declared spec '"
                        + spec.type() + "' not under its namespace");
                }
                if (typeOwner.containsKey(spec.type())) {
                    throw new IllegalStateException(
                        "Duplicate type '" + spec.type() + "': "
                        + typeOwner.get(spec.type()) + " vs " + m.protocol());
                }
                Object sender = ctx.getBean(spec.senderClass());
                all.put(spec.type(), new ReflectiveCommandHandler(spec, sender));
                typeOwner.put(spec.type(), m.protocol());
            }
        }

        // 2) 扫所有 Spring bean 的 @CommandMapping 方法（含业务方自定义 type）
        for (Object bean : ctx.getBeansWithAnnotation(Component.class).values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                CommandMapping ann = method.getAnnotation(CommandMapping.class);
                if (ann == null) continue;
                if (typeOwner.containsKey(ann.value()) && !ann.overrideTable()) {
                    log.warn("@CommandMapping('{}') silently overrides table entry "
                            + "from protocol '{}'. Set overrideTable=true to declare intent.",
                            ann.value(), typeOwner.get(ann.value()));
                }
                all.put(ann.value(), new MethodInvokerHandler(ann.value(), bean, method));
                typeOwner.put(ann.value(), "annotation:" + bean.getClass().getSimpleName());
            }
        }

        this.handlers = Map.copyOf(all);
        log.info("CommandHandlerRegistry ready: {} types from {} modules",
                handlers.size(), modules.size());
    }

    public CommandHandler require(String type) {
        CommandHandler h = handlers.get(type);
        if (h == null) throw new ResponseStatusException(NOT_FOUND, "unknown command type: " + type);
        return h;
    }
}
```

**fail-fast 检查**：
1. ProtocolModule 自报的 `protocol()` 必须与其 `commandSpecs()` 的 type 前缀一致
2. type 重复时启动期抛异常（含跨协议）
3. `@CommandMapping` 静默覆盖 ProtocolModule 注册的 type → 启动 warn，业务方需显式 `overrideTable=true`

### 4.4 `CommandHandler` 接口（去掉 sender 形参）

```java
public interface CommandHandler {
    String type();
    GatewayCommandResult handle(GatewayCommand cmd);
}
```

`ReflectiveCommandHandler` 在构造期持有 `Object sender`（实际类型由 `CommandSpec.senderClass()` 决定）；`MethodInvokerHandler` 调用 `method.invoke(bean, cmd)`，bean 的 `ServerCommandSender` 字段通过 Spring 注入。**对外 SPI 完全不感知 GB28181**。

注解方法签名约束：

```java
@CommandMapping("gb28181.Control.Ptz")
public String ptz(GatewayCommand cmd) {                // ✅ 单参数
    Map<String, Object> p = cmd.payload();
    if (p.containsKey("hex")) return sender.deviceControlPtzCmd(cmd.deviceId(), (String) p.get("hex"));
    return sender.deviceControlPtzCmd(cmd.deviceId(),
            JSON.to(PTZControlEnum.class, p.get("cmd")),
            ((Number) p.getOrDefault("speed", 128)).intValue());
}
```

### 4.5 `TransactionContextStore<K,V>` 泛型化

```java
// gateway-core
public interface TransactionContextStore<K, V> {
    void save(K key, V value, long ttlMs);
    /** 返回 null 表示不存在（→ 410）；后端故障必须抛 ResponseStatusException(503) */
    V find(K key);
    void remove(K key);
}

// gateway-gb28181
public interface InviteContextStore extends TransactionContextStore<String, InviteContext> { }
public record InviteContext(String nodeId, String ctxKey) { }

@Component
public class InMemoryInviteContextStore implements InviteContextStore {
    // 内部 Caffeine cache，启动 warn "replace before multi-node deployment"
}
```

未来 ONVIF 适配器自定义：

```java
public interface OnvifSubscriptionStore extends TransactionContextStore<String, SubscriptionRef> { }
public record SubscriptionRef(String nodeId, String referenceUri, long expiresAt) { }
```

跨节点路由的 `nodeAddressMap` 表逻辑在 `gateway-core` 的 `GatewayDispatchController`，按需通过 `protocol` 维度区分各协议的特殊端点（详见 §六）。

### 4.6 `BusinessNotifier` 与可选基类

```java
// gateway-core
public interface BusinessNotifier {
    /** 实现必须异步，否则会阻塞协议事件线程导致设备超时重传。 */
    void notify(GatewayEvent event);
}
```

业务方应对多协议场景的便利基类：

```java
public abstract class AbstractProtocolBusinessNotifier implements BusinessNotifier {

    @Override
    public final void notify(GatewayEvent event) {
        String type = event.type();
        int firstDot = type.indexOf('.');
        String protocol = firstDot > 0 ? type.substring(0, firstDot) : "unknown";
        onProtocolEvent(protocol, event);
    }

    /** 子类按 protocol 分支或拆方法。 */
    protected abstract void onProtocolEvent(String protocol, GatewayEvent event);
}
```

业务方典型用法：

```java
@Component
public class MyNotifier extends AbstractProtocolBusinessNotifier {
    @Override
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        switch (protocol) {
            case "gb28181" -> handleGb28181(event);
            case "onvif"   -> handleOnvif(event);
            default        -> log.warn("unknown protocol: {}", protocol);
        }
    }
}
```

---

## 五、type 命名空间（三段式契约）

### 5.1 命名规则

```
type ::= <protocol>.<Group>.<Name>

protocol  ∈ { gb28181 | onvif | gt1078 | rtsp | ... }   小写、与 ProtocolModule#protocol() 一致
Group     由各协议自定义，建议首字母大写驼峰
Name      由各协议自定义，与协议规��命令字一致
```

### 5.2 GB28181 命名空间（1.8.0 全表）

| 老 type | 新 type |
|---------|---------|
| `Query.Catalog` | `gb28181.Query.Catalog` |
| `Control.Ptz` | `gb28181.Control.Ptz` |
| `Invite.Play` | `gb28181.Invite.Play` |
| `Lifecycle.Online` | `gb28181.Lifecycle.Online` |
| `Notify.Alarm` | `gb28181.Notify.Alarm` |
| `Session.ServerInvite` | `gb28181.Session.ServerInvite` |
| `Response.Catalog` | `gb28181.Response.Catalog` |

> 全量 59 出站 + 35 入站 type 对照表见 [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#L571)（修订后）。

GB28181 自身的 Group 维度（`Query|Subscribe|Control|Config|Invite|Device|Lifecycle|Notify|Response|Session`）保持不变，仅在前面加 `gb28181.` 前缀。

### 5.3 1.8.0 兼容期 shim

`GatewayDispatchController` 收到无协议前缀的 type 时打印 warn 并默认补 `gb28181.` 前缀：

```java
@PostMapping("/gateway/command")
public GatewayCommandResult dispatch(@RequestBody GatewayCommand cmd) {
    String type = cmd.type();
    boolean hasProtocolPrefix = type.indexOf('.') > 0
            && knownProtocols.contains(type.substring(0, type.indexOf('.')));

    if (!hasProtocolPrefix) {
        log.warn("type '{}' missing protocol prefix; falling back to 'gb28181.{}'. "
               + "This compat shim will be removed in 1.10.0.", type, type);
        type = "gb28181." + type;
        cmd = cmd.withType(type);
    }
    return registry.require(type).handle(cmd);
}
```

**1.10.0 移除 shim**。CHANGELOG 提前一版预告。

### 5.4 反例：禁止扁平命名

❌ `gb28181_Query_Catalog`：阻断分组分析
❌ `query.gb28181.catalog`：协议位漂移到中间，不利于路由
❌ `Catalog`（无前缀）：与 ONVIF/GT1078 同名命令冲突
✅ `gb28181.Query.Catalog`：协议位置首、分组明确

---

## 六、HTTP API 与配置 namespace

### 6.1 HTTP path 统一前缀

| 端点 | 模块 | 路径 | 说明 |
|------|------|------|------|
| 协议中立分发 | gateway-core | `POST /gateway/command` | 所有 type 入口；按 type 路由到对应 handler |
| 节点身份 | gateway-core | `GET /gateway/whoami` | 返回 `{nodeId}`，调试与跨节点路由探测 |
| GB28181 INVITE 回包 | gateway-gb28181 | `POST /gateway/gb28181/invite/response` | 事务回包基础设施，依赖 `SipTransactionRegistry` + 跨节点路由 |
| ONVIF 订阅续订（占位） | gateway-onvif | `POST /gateway/onvif/subscription/renew` | 1.10.0 |
| GT1078 通道控制（占位） | gateway-gt1078 | `POST /gateway/gt1078/channel/<op>` | 1.11.0 |

**老路径全部删除**（`/sip/command` / `/sip/invite/start` 等）。一次性切换，CHANGELOG 提供老→新对照表。

### 6.2 配置 namespace 分层

```yaml
# 协议中立（gateway-core）
gateway:
  node-id: node-1
  nodes:
    node-1: http://10.0.0.1:8080
    node-2: http://10.0.0.2:8080
  forward-timeout-ms: 3000

  # 协议子前缀（每协议自管）
  gb28181:
    invite-context-ttl-ms: 30000
    invite-idempotency-window-ms: 5000
  onvif:                                  # 1.10+ 占位
    discovery-interval-ms: 60000

# sip-proxy 协议层配置不变（gb28181-server 自管）
sip:
  server:
    ip: 0.0.0.0
    external-ip: 1.2.3.4
gb28181:
  # 现有 gb28181-server 配置不动
```

`@ConfigurationProperties` 拆为：

```java
// gateway-core
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private String nodeId;
    private Map<String, String> nodes;
    private long forwardTimeoutMs = 3000;
}

// gateway-gb28181
@ConfigurationProperties(prefix = "gateway.gb28181")
public class Gb28181GatewayProperties {
    private long inviteContextTtlMs = 30_000;
    private long inviteIdempotencyWindowMs = 5_000;
}
```

### 6.3 deprecation 元数据

老前缀 `gateway.*`（参考实现里只有这一档）继续可用一个版本，启动 warn：

```json
// gateway-gb28181/src/main/resources/META-INF/additional-spring-configuration-metadata.json
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

`Gb28181GatewayProperties` 同步使用 `@DeprecatedConfigurationProperty`，运行时 warn 一次。

### 6.4 错误码契约（保持现状语义）

| HTTP | 场景 | 业务侧动作 |
|------|------|-----------|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | type 不存在 | 修正 type 字符串 |
| 410 | 事务已终止/超时（INVITE / 订阅 / ...） | 重新发起原始命令 |
| 502 | 跨节点路由 nodeAddressMap 暂未刷新 | 200ms × 3 短重试 |
| 503 | 转发失败 / store 后端不可达 | 短重试 |
| 504 | （仅 sync 模式预留） | 重试或降级异步 |

错误码与协议无关，由 `GatewayDispatchController` 统一兜底；协议特殊端点（如 `/gateway/gb28181/invite/response`）继承相同语义。

---

## 七、业务方接入示例

### 7.1 档 1（推荐 95% 场景）：starter 一键

`pom.xml`：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>sip-gateway-bom</artifactId>
            <version>1.8.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>sip-gateway-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

`application.yml`：

```yaml
gateway:
  node-id: node-1
  gb28181:
    invite-context-ttl-ms: 30000

sip:
  server:
    ip: 0.0.0.0
    port: 5060
    external-ip: 1.2.3.4   # 多节点必填，单节点可省
  common:
    user-agent: my-gateway

gb28181:
  client-config: ...        # gb28181-server 现有配置不动
```

`Application.java`：

```java
@SpringBootApplication
@EnableSipServer        // gb28181-server 提供
public class MyGatewayApp {
    public static void main(String[] args) { SpringApplication.run(MyGatewayApp.class, args); }
}
```

业务侧实现 `BusinessNotifier`：

```java
@Component
@RequiredArgsConstructor
public class HttpWebhookNotifier extends AbstractProtocolBusinessNotifier {

    private final RestTemplate restTemplate;
    private final WebhookProperties props;

    @Override
    @Async
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        try {
            restTemplate.postForLocation(props.url(protocol), event);
        } catch (RestClientException e) {
            log.error("notify failed: type={}, deviceId={}", event.type(), event.deviceId(), e);
        }
    }
}
```

业务侧调命令：

```http
POST /gateway/command HTTP/1.1
Content-Type: application/json

{
  "type": "gb28181.Invite.Play",
  "deviceId": "34020000001320000001",
  "payload": {
    "mediaIp": "10.0.0.5",
    "mediaPort": 30001,
    "streamMode": "UDP"
  },
  "requestId": "trace-abc-125"
}
```

响应：

```json
{ "correlationId": "1234567890", "type": "gb28181.Invite.Play", "nodeId": "node-1" }
```

### 7.2 档 2：按需子模块（不要 web / 不要某协议）

```xml
<!-- 纯事件转发，不暴露 HTTP 端点 -->
<dependency><groupId>io.github.lunasaw</groupId><artifactId>gateway-core</artifactId></dependency>
<dependency><groupId>io.github.lunasaw</groupId><artifactId>gateway-gb28181</artifactId></dependency>
<!-- 不引 starter ⇒ 不带 spring-web，控制器 AutoConfig 自动跳过 -->
```

或反向，starter + 排除某协议：

```xml
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.github.lunasaw</groupId>
            <artifactId>gateway-gb28181</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>gateway-onvif</artifactId>
</dependency>
```

### 7.3 档 3：多协议组合

```xml
<!-- 1.10.0 之后生效，1.8.0 等同档 1 -->
<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>sip-gateway-spring-boot-starter</artifactId>
</dependency>
```

业务方 `AbstractProtocolBusinessNotifier` 自动按 `gb28181 / onvif / gt1078` 分支处理，对应命令通过同一 `POST /gateway/command` 入口分发，type 前缀决定路由目标。

---

## 八、AutoConfig 装配与守门条件

### 8.1 `GatewayCoreAutoConfiguration`

```java
@AutoConfiguration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BusinessNotifier businessNotifier() {
        return new NoopBusinessNotifier();   // 启动 warn "replace before production"
    }

    @Bean
    @ConditionalOnMissingBean
    public CommandHandlerRegistry commandHandlerRegistry(
            ApplicationContext ctx,
            ObjectProvider<ProtocolModule> moduleProvider) {
        return new CommandHandlerRegistry(ctx, moduleProvider.stream().toList());
    }

    /** Web 子配置：仅 servlet 环境启用 */
    @AutoConfiguration(after = GatewayCoreAutoConfiguration.class)
    @ConditionalOnWebApplication(type = SERVLET)
    @ConditionalOnClass(RestController.class)
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public GatewayDispatchController gatewayDispatchController(
                GatewayProperties props,
                CommandHandlerRegistry registry,
                @Qualifier("gatewayForwardRestTemplate") RestTemplate forwardRestTemplate) {
            return new GatewayDispatchController(props, registry, forwardRestTemplate);
        }

        @Bean("gatewayForwardRestTemplate")
        @ConditionalOnMissingBean(name = "gatewayForwardRestTemplate")
        public RestTemplate gatewayForwardRestTemplate() {
            return new RestTemplate();
        }
    }
}
```

### 8.2 `Gb28181GatewayAutoConfiguration`

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
3. `@AutoConfiguration(after = GatewayCoreAutoConfiguration.class)` — 保证 `CommandHandlerRegistry` 装配时 `ProtocolModule` 已注册

---

## 九、迁移步骤（一次性单 PR）

### 9.1 落地节奏

| Stage | 内容 | 工时 |
|-------|------|-----|
| 0 | 1.7.3 `rawSdp` 透传（已规划，见 [GB28181-GATEWAY-MODULE-PLAN §四 Stage 0](GB28181-GATEWAY-MODULE-PLAN.md#L596)） | 0.5 d |
| 1 | 建 `sip-gateway/` 父聚合 pom + 4 子模块骨架 + CI 纯度脚本 | 0.5 d |
| 2 | 实现 `gateway-core`：envelope/SPI/Registry/web/AutoConfig | 0.5 d |
| 3 | 实现 `gateway-gb28181`：CommandSpecs/WhitelistHandlers/EventForwarder/Store/Web | 1 d |
| 4 | 配置 `sip-gateway-bom` + `sip-gateway-spring-boot-starter` + AutoConfiguration.imports | 0.5 d |
| 5 | 测试覆盖（type 隔离 / starter exclusion / ProtocolModule fail-fast）+ JaCoCo aggregate | 0.5 d |
| 6 | 文档（[UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md) + [GB28181-GATEWAY-MODULE-PLAN](GB28181-GATEWAY-MODULE-PLAN.md) + [LAYERED-ARCHITECTURE](../../architecture/LAYERED-ARCHITECTURE.md) + [PROTOCOL-LAYERING-MATRIX](../../architecture/PROTOCOL-LAYERING-MATRIX.md) + 新 starter README）+ CHANGELOG type 对照表 | 0.5 d |

**合计：4 个工作日**（不含 1.7.3 前置）。

### 9.2 详细步骤

**Stage 1：建模块骨架（0.5 d）**

- 仓库根 pom 新增 `<module>sip-gateway</module>`
- `sip-gateway/pom.xml`（packaging=pom）声明 4 个子模块
- 4 个子模块各自建空 pom（gateway-core / gateway-gb28181 / sip-gateway-bom / sip-gateway-spring-boot-starter）
- 父 pom `<dependencyManagement>` 增加 4 个新坐标
- `scripts/check-gateway-core-purity.sh` 写好并挂在父 pom `verify` 阶段
- 验收：`mvn verify` 全绿、纯度脚本对空 src 通过

**Stage 2：实现 gateway-core（0.5 d）**

- 包名 `io.github.lunasaw.sipgateway.core`
- 落 envelope 三件套（GatewayCommand / GatewayCommandResult / GatewayEvent）
- 落 SPI：CommandHandler / CommandMapping / CommandSpec / ParamBinding / ProtocolModule / TransactionContextStore
- 落核心：CommandHandlerRegistry / ReflectiveCommandHandler / MethodInvokerHandler / PayloadCodec
- 落 web：GatewayDispatchController（POST /gateway/command + /gateway/whoami）含 1.8.0 兼容 shim
- 落 notifier：NoopBusinessNotifier（启动 warn）+ AbstractProtocolBusinessNotifier
- 落 config：GatewayProperties + GatewayCoreAutoConfiguration
- 验收：单测覆盖 envelope record、Registry fail-fast、Controller 错误码兜底

**Stage 3：实现 gateway-gb28181（1 d）**

- 包名 `io.github.lunasaw.sipgateway.gb28181`
- 把 [gb28181-test/.../gateway/](../../../gb28181-test/src/test/java/io/github/lunasaw/gbproxy/test/gateway/) 14 个文件迁过来，按 §3.3 重组目录
- type 全部加 `gb28181.` 前缀（39 行 CommandSpecs + 35 个 emit 调用）
- 落 Gb28181Module（implements ProtocolModule）
- 落 Gb28181WhitelistHandlers（约 20 个 @CommandMapping 方法，签名 `(GatewayCommand) -> String`）
- 落 Gb28181EventForwarder（替代 SipEventForwarder，4 listener × 35 方法）
- 落 InMemoryInviteContextStore（启动 warn "replace with Redis before multi-node"）
- 落 Gb28181InviteResponseController（POST /gateway/gb28181/invite/response）
- 落 Gb28181GatewayProperties（gateway.gb28181.*） + Gb28181GatewayAutoConfiguration
- 验收：39 spec 单测 + 20 whitelist 单测 + 35 forwarder 单测 + MockMvc 集成测

**Stage 4：BOM + Starter（0.5 d）**

- sip-gateway-bom：`<dependencyManagement>` 列全部协议子模块
- sip-gateway-spring-boot-starter：`<dependencies>` 引 gateway-core + gateway-gb28181（默认带）+ spring-boot-starter
- AutoConfiguration.imports 写入 2 行（core + gb28181）
- 验收：空白 spring-boot 项目引 starter，能注入 GatewayDispatchController + Gb28181EventForwarder

**Stage 5：测试（0.5 d）**

- 跨协议 type 重复 fail-fast 测：mock 两个 ProtocolModule 声明同 type → 启动期抛异常
- starter exclusion 测：排除 gateway-gb28181 后只剩 core，启动正常但 `/gateway/command` type=gb28181.* 返回 404
- @ConditionalOnBean(ServerCommandSender) 退化路径：业务方无 gb28181-server 时 starter 自动跳过 GB28181 模块
- 跨节点路由测：`MockRestServiceServer` mock 跨节点 HTTP，验证 `nodeId != local` 时正确转发
- 父 pom JaCoCo 改 aggregate report，全模块合计 ≥ 80%

**Stage 6：文档（0.5 d）**

- [UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md) 修订：§2.3 type 三段式 / §3.6-3.7 SPI 形态 / §5 全表加前缀 / §6 模块结构 / §7 HTTP path
- [GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md) 修订：§2 / §3.1-3.4 / §四迁移步骤
- [LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) §6 改写：sip-gateway = sip-proxy 提供的聚合 starter
- [PROTOCOL-LAYERING-MATRIX.md](../../architecture/PROTOCOL-LAYERING-MATRIX.md) 在 §三/§四 末尾追加"L3 网关 envelope 表"，cmdType 表加一列指向新 type
- 新增 `sip-gateway/README.md` 5 分钟快速开始
- CHANGELOG 1.8.0 列：① type 老→新对照表（94 条）② HTTP path 老→新对照（7 条）③ 配置前缀 deprecation

### 9.3 兼容性

**破坏性变更（CHANGELOG 强提示）**：
- HTTP 路径全部改 `/gateway/*`（老 `/sip/*` 删除）
- type 字符串改三段式（兼容 shim 一版后移除）
- `BusinessNotifier` 接口从 3 方法变 1 方法
- 配置前缀部分键名搬到 `gateway.gb28181.*`（保留一版 deprecation 别名）

**1.8.0 一次性切换由用户决定升级时机**。不提供长期过渡 sugar，避免维护两套语义。

---

## 十、风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| `gateway-core` 不慎依赖 `gb28181-*` | 多协议梦想破产，未来加协议会反向重构 | CI 纯度脚本 `check-gateway-core-purity.sh` 在 verify 阶段拦截，与 sip-common 纯度检查并列 |
| `Gb28181Module` 自报 protocol 与 type 前缀不一致 | 启动期 type 路由错乱 | `CommandHandlerRegistry` 启动期断言每个 spec.type 必须以 `module.protocol() + "."` 开头，否则抛 IllegalStateException |
| 业务方 `@CommandMapping` 误覆盖 ProtocolModule 注册的 type | 默认行为被静默改写 | Registry 启动期发现重复时输出 WARN；`@CommandMapping(overrideTable=true)` 显式声明覆盖意图 |
| starter 默认带 gb28181，纯 ONVIF 项目被动引入 gb28181-server jar | 包体积 +3MB | 文档明示 `<exclusions>` 用法；如反馈强烈，1.10.0 拆 `sip-gateway-spring-boot-starter-gb28181` 子 starter（加法，非破坏） |
| `@ConditionalOnBean(ServerCommandSender)` 漏判 | 业务方未启 SIP 但 starter 注册了 GB28181 bean，启动 NPE | AutoConfig 双重守门：先 `@ConditionalOnClass` 检查类是否在 classpath，再 `@ConditionalOnBean` 检查 bean 是否存在；二者都通过才装配 |
| 仓库下子树让 sip-proxy 仓库膨胀 | 维护负担 | 4 子模块代码量约 1500 行，跟现有 sip-common 同量级；CI 时间 +30s 可接受 |
| 包名 `sipgateway` 不带分隔符可读性差 | 命名审美 | Java package 规范禁止下划线/驼峰；`sipgateway` 是同类项目（spring-boot 等）的标准做法 |
| 业务方拼 payload 出错（字段名/类型） | 调用失败、错误码不直观 | handler 内严格校验 + 错误信息含 type 与字段路径；提供 OpenAPI/JSON Schema 文档生成（v1.9 候选） |
| Map 序列化丢类型信息（枚举/日期） | fastjson2 `@JSONField(format=)` 注解失效 | 文档约定：时间戳用毫秒 long、枚举用大写 String、自定义类型在文档说明；推荐业务侧用 `JSON.to(TargetClass.class, payload)` 一步反序列化 |
| 表驱动反射性能（每命令 1 次反射调用） | 微开销 | 启动期一次性 `Method.invoke` 句柄缓存；JIT 内联后 ≈ 直接调用；P99 增量 < 10μs，相对 SIP/HTTP IO 可忽略 |
| 1.8.0 兼容 shim（自动补 `gb28181.` 前缀）让用户惰于迁移 | 1.10.0 删 shim 时炸一片 | shim 路径每次触发都打 WARN；CHANGELOG 1.8.0/1.9.0 都提醒；1.10.0 实现移除并在 RELEASE NOTES 单独章节列出 |
| 多协议同时上线时 `BusinessNotifier` 业务侧分支爆炸 | switch type 写一坨 | 提供 `AbstractProtocolBusinessNotifier` 按 protocol 分发；文档 example 给出 GB28181 group 二级拆分的写法（`onLifecycle/onNotify/onSession/onResponse`） |

---

## 十一、决策清单（已确认）

| # | 问题 | 决策 |
|---|------|------|
| 1 | 模块结构 | sip-gateway 父聚合 + 4 子模块（core / gb28181 / bom / starter） |
| 2 | 业务方接入入口 | sip-gateway-spring-boot-starter（一键），辅以 BOM 锁版本 |
| 3 | 包名根 | `io.github.lunasaw.sipgateway.{core,gb28181,...}`（品牌统一） |
| 4 | type 命名 | `<protocol>.<Group>.<Name>` 三段式，1.8.0 起强制 |
| 5 | HTTP path | `/gateway/command`（核心） + `/gateway/{protocol}/...`（协议特殊端点） |
| 6 | 配置前缀 | `gateway.*` + `gateway.{protocol}.*` |
| 7 | CommandHandler 接口 | 单参数 `(GatewayCommand)`，sender 注入到 bean |
| 8 | 协议注册 SPI | `ProtocolModule#protocol()` + `commandSpecs()`（自报命名空间） |
| 9 | 事务存储 | `TransactionContextStore<K,V>` 泛化基类，每协议自定义具体接口 |
| 10 | payload 形态 | `Map<String,Object>` + 文档约定字段；fastjson2 二次反序列化 |
| 11 | 出站语义 | 全异步：返回 correlationId，业务侧靠回调匹配 |
| 12 | 1.8.0 兼容 shim | 自动补 `gb28181.` 前缀一版（1.9 仍存、1.10 移除） |
| 13 | 落代码节奏 | 一次性全量提交（单 PR），CHANGELOG 列全部对照表 |

---

## 十二、未来演进路线

| 版本 | 计划项 | 备注 |
|------|--------|------|
| **1.7.3** ✅ | 协议层小升级前置：`ServerSessionEvent.rawSdp` + `DeviceSessionListener.onServerInvite(rawSdp,...)` | 见 [GB28181-GATEWAY-MODULE-PLAN §六](GB28181-GATEWAY-MODULE-PLAN.md#L676) |
| **1.8.0** | sip-gateway 父聚合 + 4 子模块 + envelope 协议化（本方案） | 一次到位 |
| 1.9.0 | gateway-gb28181-redis 扩展模块（RedisInviteContextStore + 候选 RedisDeviceSessionCache） | 加法，BOM 追加坐标 |
| 1.10.0 | **gateway-onvif** 子模块（ONVIF SOAP Discovery + Imaging + PTZ） | 加法，starter 默认带、@ConditionalOnClass 守门；同时移除 1.8.0 兼容 shim |
| 1.11.0 | **gateway-gt1078** 子模块（GT1078 私有 TCP 长连接 + 流水号） | 加法 |
| 1.12.0 | gateway-rtsp 子模块（RTSP 直连） | 加法 |
| 待定 | sip-gateway-webhook（HttpWebhookBusinessNotifier，HMAC-SHA256 签名）作为可选 starter | 加法 |
| 待定 | sip-gateway-discovery（K8s Endpoints / Nacos `gateway.nodes` 动态发现） | 加法 |
| 待定 | 同步语义补齐（`?sync=true&timeoutMs=5000` 选项 + Future 超时 504） | 1.8 接口预留，按需启用 |
| 待定 | OpenAPI/Swagger 自动生成 HTTP 文档 | 加法 |
| 待定 | 内置 metrics（INVITE 成功率 / 跨节点转发耗时 / store 命中率，按 protocol 维度聚合） | 加法 |

**核心承诺**：1.8.0 之后所有协议加法都不需要改 [UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md)、[GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md)、本方案的 §四（SPI）/§五（type）/§六（HTTP/配置）。仅追加新协议子模块和 BOM/starter 一行声明。

---

## 十三、与现有 1.8.0 计划的关系

[UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md) 与 [GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md) 在 v1.8.0 的最终状态须遵循本方案：

| 章节 | UNIFIED-ENVELOPE-PLAN | GB28181-GATEWAY-MODULE-PLAN | 本方案对应章节 |
|------|----------------------|----------------------------|---------------|
| 模块结构 | §6.1 改为指向本方案 §三 | §3.1 改为指向本方案 §二、§三 | §二、§三 |
| type 命名 | §2.3 + §五 改为三段式 | — | §五 |
| HTTP API | §3.8 + §七 改为 `/gateway/*` | §3.3.4 改为 `/gateway/gb28181/invite/response` | §六 |
| 配置前缀 | — | §3.4 改为 `gateway.*` + `gateway.gb28181.*` | §六 |
| SPI 形态 | §3.6-3.7 改为 `ProtocolModule` + `CommandHandler(cmd)` | — | §四 |
| 事务存储 | — | §3.3.2 改为 `TransactionContextStore<K,V>` | §四 |
| AutoConfig | §6.1 | §3.4 | §八 |
| 落地步骤 | §9 | §四 | §九 |
| 风险 | §11 | §五 | §十 |

**三份文档关系**：
- 本方案（**SIP-GATEWAY-AGGREGATION-PLAN**）：主纲领，决定形态
- [UNIFIED-ENVELOPE-PLAN](UNIFIED-ENVELOPE-PLAN.md)：envelope schema 与命令映射表（具体语义）
- [GB28181-GATEWAY-MODULE-PLAN](GB28181-GATEWAY-MODULE-PLAN.md)：代码迁移与 1.7.3 前置（执行手册）

任何后续修订须先改本方案，再同步另两份。



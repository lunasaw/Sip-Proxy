# 命令发送层重构方案

> 目标版本：1.3.0 | 状态：待实施

## 背景与动机

本项目定位为 SIP 协议代理，支持 GB28181 及基于 SIP 的扩展协议。业务方接入后，**同一个 Spring 容器内可能同时运行 gb28181-client 和 gb28181-server**（业务侧既作服务端又作客户端中转）。

当前命令发送层存在四个核心问题：

1. **策略接口参数割裂**：client 用 `Object... params`（位置依赖，`RegisterCommandStrategy` 存在线程安全 bug），server 用 `Map<String,Object>`（魔法字符串）。
2. **工厂硬编码注册**：新增策略必须修改工厂类，扩展性差。
3. **CommandSender 是纯静态工具类**：无法注入、无法 mock、无法扩展。
4. **两套工厂在同容器内冲突**：client/server 各自独立工厂，同容器部署时无法统一路由，扩展协议也无法复用。

---

## 设计决策

### CommandContext 参数模型

废弃平铺的专用字段（`expires`、`callId`、`controlBody`、`subscribeInfo`），改用 `extras Map` + **类型安全的静态构建方法**：

- `CommandContext` 本身不随协议扩展而膨胀
- 调用方通过静态工厂方法获得类型安全入口
- 扩展协议在自己的模块内定义静态构建方法，不改核心类

### 工厂合并

`CommandStrategy` 接口增加 `getRole()` 方法，工厂 key 改为 `role:commandType`，解决同容器内 client/server 策略命名冲突，同时支持未来第三种 role（如 proxy、relay）。

---

## 新类层次结构

```
gb28181-common（新增）
└── transmit/cmd/
    ├── CommandContext            统一参数模型（role + commandType + extras）
    ├── CommandStrategy           公共策略接口（含 getRole()）
    ├── AbstractCommandStrategy   模板方法基类（validate → buildContent → doSend）
    └── CommandStrategyFactory    @Component，统一工厂，key = role:commandType

gb28181-client（改造）
└── transmit/cmd/
    ├── strategy/
    │   ├── AbstractClientCommandStrategy  extends AbstractCommandStrategy，body→XML 序列化
    │   └── impl/ 8个实现类               @Component，getRole() = "client"
    └── ClientCommandSender               @Component + ApplicationContextAware，静态方法委托给 INSTANCE

gb28181-server（改造）
└── transmit/cmd/
    ├── strategy/
    │   ├── AbstractServerCommandStrategy  extends AbstractCommandStrategy
    │   └── impl/ 7个实现类               @Component，getRole() = "server"
    └── ServerCommandSender               @Component + ApplicationContextAware，静态方法委托给 INSTANCE
```

**删除**：`ClientCommandStrategyFactory`、`ServerCommandStrategyFactory`、`ServerCommandStrategyReq`、`ClientCommandStrategy`（空子接口）、`ServerCommandStrategy`（空子接口）

模块依赖关系不变：`sip-common ← gb28181-common ← client/server`

---

## CommandContext 设计

包路径：`io.github.lunasaw.gb28181.common.transmit.cmd`

```java
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommandContext {
    private String role;           // "client" | "server"，工厂路由 key
    private String commandType;    // SIP method: MESSAGE/INVITE/BYE/ACK/REGISTER/SUBSCRIBE/INFO/NOTIFY
    private FromDevice fromDevice;
    private ToDevice toDevice;
    private String content;        // 已序列化内容（SDP/XML），优先使用
    private Object body;           // 待序列化业务对象，由 buildContent() 处理
    private Event errorEvent;
    private Event okEvent;
    @Builder.Default
    private Map<String, Object> extras = new HashMap<>();  // 所有命令专用参数

    // 类型安全的静态构建方法（替代平铺字段）
    public static CommandContext forRegister(String role, FromDevice from, ToDevice to, int expires) {
        return CommandContext.builder()
            .role(role).commandType("REGISTER")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("expires", expires)))
            .build();
    }

    public static CommandContext forSubscribe(String role, FromDevice from, ToDevice to, SubscribeInfo subscribeInfo, int expires) {
        return CommandContext.builder()
            .role(role).commandType("SUBSCRIBE")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("subscribeInfo", subscribeInfo, "expires", expires)))
            .build();
    }

    public static CommandContext forAckBye(String role, FromDevice from, ToDevice to, String callId, String commandType) {
        return CommandContext.builder()
            .role(role).commandType(commandType)
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("callId", callId)))
            .build();
    }

    public static CommandContext forInfo(String role, FromDevice from, ToDevice to, String controlBody) {
        return CommandContext.builder()
            .role(role).commandType("INFO")
            .fromDevice(from).toDevice(to)
            .extras(new HashMap<>(Map.of("controlBody", controlBody)))
            .build();
    }

    // extras 类型安全取值辅助
    @SuppressWarnings("unchecked")
    public <T> T getExtra(String key, Class<T> type) {
        return type.cast(extras.get(key));
    }
}
```

---

## 公共策略接口与基类

### CommandStrategy（gb28181-common）

```java
public interface CommandStrategy {
    String execute(CommandContext ctx);
    String getCommandType();
    String getRole();  // "client" | "server"，与 commandType 共同构成工厂路由 key
}
```

### AbstractCommandStrategy（gb28181-common）

```java
@Slf4j
public abstract class AbstractCommandStrategy implements CommandStrategy {

    @Override
    public final String execute(CommandContext ctx) {
        Assert.notNull(ctx.getFromDevice(), "fromDevice 不能为空");
        Assert.notNull(ctx.getToDevice(), "toDevice 不能为空");
        validateContext(ctx);
        if (ctx.getContent() == null) {
            ctx.setContent(buildContent(ctx));
        }
        return doSend(ctx);
    }

    protected void validateContext(CommandContext ctx) {}
    protected String buildContent(CommandContext ctx) { return ctx.getContent(); }
    protected abstract String doSend(CommandContext ctx);
}
```

### AbstractClientCommandStrategy（gb28181-client）

```java
public abstract class AbstractClientCommandStrategy extends AbstractCommandStrategy {

    @Override
    public String getRole() { return "client"; }

    @Override
    protected String buildContent(CommandContext ctx) {
        if (ctx.getContent() != null) return ctx.getContent();
        if (ctx.getBody() != null) return XmlUtils.toString("UTF-8", ctx.getBody());
        return null;
    }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doMessageRequest(ctx.getFromDevice(), ctx.getToDevice(),
            ctx.getContent(), ctx.getErrorEvent(), ctx.getOkEvent());
    }
}
```

`AbstractServerCommandStrategy` 结构对称，`getRole()` 返回 `"server"`。

---

## 统一工厂（gb28181-common）

```java
@Component
public class CommandStrategyFactory {
    private final Map<String, CommandStrategy> strategyMap;

    public CommandStrategyFactory(List<CommandStrategy> strategies) {
        this.strategyMap = strategies.stream()
            .collect(Collectors.toMap(
                s -> s.getRole() + ":" + s.getCommandType(),
                s -> s,
                (a, b) -> { throw new IllegalStateException("重复策略: " + a.getRole() + ":" + a.getCommandType()); }
            ));
    }

    public CommandStrategy getStrategy(String role, String commandType) {
        CommandStrategy s = strategyMap.get(role + ":" + commandType);
        Assert.notNull(s, "未找到策略: " + role + ":" + commandType);
        return s;
    }
}
```

**新增策略只需：** 创建实现类 + `@Component` + 实现 `getCommandType()` 和 `getRole()`，无需修改工厂。

---

## CommandSender 改造（Spring Bean + 静态委托）

```java
@Component
public class ClientCommandSender implements ApplicationContextAware {
    private static ClientCommandSender INSTANCE;
    private final CommandStrategyFactory factory;

    public ClientCommandSender(CommandStrategyFactory factory) {
        this.factory = factory;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        INSTANCE = this;
    }

    // Bean 方法：可注入、可 mock
    public String send(CommandContext ctx) {
        return factory.getStrategy(ctx.getRole(), ctx.getCommandType()).execute(ctx);
    }

    // 静态便利方法：现有调用方无需修改
    public static String sendAlarmCommand(FromDevice from, ToDevice to, DeviceAlarm alarm) {
        Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
        return INSTANCE.send(CommandContext.builder()
            .role("client").commandType("MESSAGE")
            .fromDevice(from).toDevice(to).body(alarm).build());
    }
    // ... 其余静态方法同理，role 固定为 "client"
}
```

`ServerCommandSender` 结构完全对称，静态方法中 role 固定为 `"server"`。

---

## 策略实现类改造要点

### client 侧（8个，getRole() = "client"）

| 实现类 | getCommandType() | doSend() 关键变化 |
|--------|-----------------|-----------------|
| `MessageCommandStrategy` | `"MESSAGE"` | 默认，body→XML |
| `InviteCommandStrategy` | `"INVITE"` | `SipSender.doInviteRequest(from, to, ctx.getContent())` |
| `RegisterCommandStrategy` | `"REGISTER"` | `ctx.getExtra("expires", Integer.class)`，**修复线程安全 bug**（删除实例变量） |
| `AckCommandStrategy` | `"ACK"` | `ctx.getExtra("callId", String.class)` |
| `ByeCommandStrategy` | `"BYE"` | `ctx.getExtra("callId", String.class)` |
| `SubscribeCommandStrategy` | `"SUBSCRIBE"` | `ctx.getExtra("subscribeInfo", SubscribeInfo.class)` |
| `InfoCommandStrategy` | `"INFO"` | `ctx.getExtra("controlBody", String.class)` |
| `NotifyCommandStrategy` | `"NOTIFY"` | 默认 |

### server 侧（7个，getRole() = "server"，无 NOTIFY）

结构对称，`ServerCommandStrategyReq` 废弃删除。

---

## 改动文件清单

### gb28181-common（新增 4 个文件）

| 文件 | 操作 |
|------|------|
| `transmit/cmd/CommandContext.java` | 新增 |
| `transmit/cmd/CommandStrategy.java` | 新增 |
| `transmit/cmd/AbstractCommandStrategy.java` | 新增 |
| `transmit/cmd/CommandStrategyFactory.java` | 新增 |

### gb28181-client（改造 10 个文件，删除 2 个）

| 文件 | 操作 |
|------|------|
| `strategy/ClientCommandStrategy.java` | **删除**（空子接口，职责由 getRole() 承接） |
| `strategy/AbstractClientCommandStrategy.java` | 改造：extends AbstractCommandStrategy |
| `strategy/ClientCommandStrategyFactory.java` | **删除**（由 CommandStrategyFactory 统一替代） |
| `strategy/impl/` 8个实现类 | 改造：@Component，实现 getRole()，适配 CommandContext |
| `cmd/ClientCommandSender.java` | 改造：@Component，ApplicationContextAware，注入 CommandStrategyFactory |

### gb28181-server（改造 10 个文件，删除 3 个）

| 文件 | 操作 |
|------|------|
| `strategy/ServerCommandStrategy.java` | **删除** |
| `strategy/AbstractServerCommandStrategy.java` | 改造 |
| `strategy/ServerCommandStrategyFactory.java` | **删除** |
| `strategy/ServerCommandStrategyReq.java` | **删除** |
| `strategy/impl/` 7个实现类 | 改造：@Component，实现 getRole()，适配 CommandContext |
| `cmd/ServerCommandSender.java` | 改造：@Component，ApplicationContextAware，注入 CommandStrategyFactory |

---

## 执行顺序

```
Step 1  gb28181-common 新增 4 个文件（无依赖，可独立编译）
Step 2  gb28181-client 策略层（AbstractClientCommandStrategy → impl 8个）
Step 3  gb28181-server 策略层（与 Step 2 并行）
Step 4  gb28181-client CommandSender
Step 5  gb28181-server CommandSender（与 Step 4 并行）
Step 6  删除废弃文件（两个工厂、三个空接口/DTO）
Step 7  mvn clean install 验证
```

---

## 验证

```bash
mvn clean install -pl gb28181-common
mvn clean install -pl gb28181-client,gb28181-server --also-make
mvn test -pl gb28181-test
```

**检查点：**
- 现有业务方法签名（`sendAlarmCommand`、`deviceInfoQuery` 等）调用不变
- 新增策略只需 `@Component` + 实现接口，无需修改工厂
- `RegisterCommandStrategy` 不再有实例变量，线程安全
- 同容器内 client/server 策略通过 `role:commandType` 隔离，无冲突
- `ServerCommandStrategyReq`、两个独立工厂、两个空子接口已删除，无残留引用
- `INSTANCE == null` 时抛出明确错误而非 NPE

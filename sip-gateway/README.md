# sip-gateway

> SIP 网关父聚合：协议中立内核 + 多协议适配器，业务方一键接入。

## 快速开始（5 分钟）

### 1. 引入依赖

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

### 2. 配置 application.yml

```yaml
gateway:
  node-id: node-1
  gb28181:
    invite-context-ttl-ms: 30000

sip:
  server:
    ip: 0.0.0.0
    port: 5060
    external-ip: 1.2.3.4    # 多节点部署必填，单节点可省

gb28181:
  client-config: ...
```

### 3. 启用 SIP Server

```java
@SpringBootApplication
@EnableSipServer
public class MyGatewayApp {
    public static void main(String[] args) {
        SpringApplication.run(MyGatewayApp.class, args);
    }
}
```

### 4. 实现 BusinessNotifier

```java
@Component
@RequiredArgsConstructor
public class HttpWebhookNotifier extends AbstractProtocolBusinessNotifier {

    private final RestTemplate restTemplate;

    @Override
    @Async
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        try {
            restTemplate.postForLocation(buildWebhookUrl(protocol), event);
        } catch (RestClientException e) {
            log.error("notify failed: type={}, deviceId={}",
                event.type(), event.deviceId(), e);
        }
    }
}
```

> ⚠️ `BusinessNotifier#notify` 必须异步，否则会阻塞 SIP 事件线程导致设备超时重传。

### 5. 调命令

```http
POST /gateway/command HTTP/1.1
Content-Type: application/json

{
  "type": "gb28181.Query.Catalog",
  "deviceId": "34020000001320000001",
  "payload": {},
  "requestId": "trace-abc-123"
}
```

响应：

```json
{ "correlationId": "1234567890", "type": "gb28181.Query.Catalog", "nodeId": "node-1" }
```

## 模块拓扑

```
sip-gateway/                                    (packaging=pom)
├── gateway-core/                               # 协议中立内核
│   ├── envelope/  GatewayCommand/Result/Event
│   ├── api/       SPI 接口（CommandHandler/Mapping/Spec/...）
│   ├── core/      Registry/Reflective/Method/PayloadCodec
│   ├── web/       GatewayDispatchController
│   ├── notifier/  Noop + AbstractProtocol
│   └── config/    GatewayProperties + AutoConfig
├── gateway-gb28181/                            # GB28181 协议适配器
│   ├── handler/   Module + CommandSpecs + WhitelistHandlers
│   ├── forwarder/ Gb28181EventForwarder（4 listener × 35 emit）
│   ├── store/     InviteContextStore + InMemoryStore
│   ├── web/       Gb28181InviteResponseController
│   └── config/    Gb28181GatewayProperties + AutoConfig
├── sip-gateway-bom/                            # 依赖管理
└── sip-gateway-spring-boot-starter/            # 一键接入
```

## type 命名规则（三段式）

```
type ::= <protocol>.<Group>.<Name>

protocol ∈ { gb28181 | onvif | gt1078 | rtsp | ... }
Group    ∈ { Query | Subscribe | Control | Config | Invite | Device
           | Lifecycle | Notify | Response | Session }
```

| 命令含义 | type |
|---------|------|
| GB28181 设备目录查询 | `gb28181.Query.Catalog` |
| GB28181 PTZ 控制 | `gb28181.Control.Ptz` |
| GB28181 INVITE 实时点播 | `gb28181.Invite.Play` |
| GB28181 设备上线事件 | `gb28181.Lifecycle.Online` |
| GB28181 报警通知 | `gb28181.Notify.Alarm` |
| GB28181 INVITE 200 OK 回执 | `gb28181.Session.InviteOk` |

完整 cmdType 表见 [doc/plans/1.8.0/UNIFIED-ENVELOPE-PLAN.md §五](../doc/plans/1.8.0/UNIFIED-ENVELOPE-PLAN.md#五全量映射表)。

## 错误码契约

| HTTP | 场景 | 业务侧动作 |
|------|------|-----------|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | type 不存在 | 修正 type 字符串 |
| 410 | 事务已终止/超时（INVITE/订阅）| **禁止重试**，重新发起原始命令 |
| 502 | 跨节点路由 nodeAddressMap 暂未刷新 | 200ms × 3 短重试 |
| 503 | 转发失败 / store 后端不可达 | 短重试 |

## 多节点部署

> ⚠️ 多节点必填 `sip.server.external-ip: <VIP>`，否则设备回包绕过 VIP 导致源 IP 哈希失效。

```yaml
gateway:
  node-id: node-1
  nodes:
    node-1: http://10.0.0.1:8080
    node-2: http://10.0.0.2:8080

sip:
  server:
    ip: 0.0.0.0
    external-ip: <VIP>
```

**生产环境必须替换 `InMemoryInviteContextStore` 为 Redis 实现**：

```java
@Component
public class RedisInviteContextStore implements InviteContextStore {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String callId, InviteContext value, long ttlMs) {
        redisTemplate.opsForValue().set("sip:invite:ctx:" + callId,
                value.nodeId() + ":" + value.ctxKey(),
                Duration.ofMillis(ttlMs));
    }

    @Override
    public InviteContext find(String callId) {
        try {
            String value = redisTemplate.opsForValue().get("sip:invite:ctx:" + callId);
            if (value == null) return null;
            int sep = value.indexOf(':');
            return new InviteContext(value.substring(0, sep), value.substring(sep + 1));
        } catch (RedisConnectionFailureException e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "redis unavailable", e);
        }
    }

    @Override
    public void remove(String callId) {
        redisTemplate.delete("sip:invite:ctx:" + callId);
    }
}
```

## 添加新 cmdType

**简单命令**（`deviceId + ≤4 个标量/无重载/无默认值`）→ 改 `Gb28181CommandSpecs.declare()`：

```java
spec("gb28181.Query.NewCommand", "newDeviceMethod", arg("deviceId"), arg("param:int"))
```

**复杂命令**（重载/强制字段/默认值/Date 类型）→ 加 `@CommandMapping` 方法：

```java
@CommandMapping("gb28181.Custom.Foo")
public String foo(GatewayCommand cmd) {
    return sender.deviceFoo(cmd.deviceId(), ...);
}
```

## 添加新协议（v1.10+）

1. 新建 `sip-gateway/gateway-{proto}/` 子模块
2. 实现 `ProtocolModule` 自报 `protocol()` + `commandSpecs()`
3. 实现 `{Proto}EventForwarder` 桥接协议事件源到 `BusinessNotifier`
4. `sip-gateway-bom` 加坐标，`sip-gateway-spring-boot-starter` 加 `<dependency>`
5. starter `META-INF/spring/AutoConfiguration.imports` 加一行
6. `gateway-core` **不动**，本文档**不动**

## 关键约束

- ⚠️ **必须与 sip-proxy 同 JVM**：`SipTransactionRegistry`、`Dialog` 都是进程内对象，跨进程后无法回包
- ⚠️ **`gateway-core` 协议中立**：CI 强制纯度检查（`scripts/check-gateway-core-purity.sh`）
- ⚠️ **`BusinessNotifier#notify` 必须异步**：否则阻塞 SIP 事件线程
- ⚠️ **多节点必须 Redis**：`InMemoryInviteContextStore` 仅单机演示用

## 文档索引

- [SIP-GATEWAY-AGGREGATION-PLAN.md](../doc/plans/1.8.0/SIP-GATEWAY-AGGREGATION-PLAN.md) — 父聚合主纲领
- [UNIFIED-ENVELOPE-PLAN.md](../doc/plans/1.8.0/UNIFIED-ENVELOPE-PLAN.md) — envelope schema 与全量映射
- [GB28181-GATEWAY-MODULE-PLAN.md](../doc/plans/1.8.0/GB28181-GATEWAY-MODULE-PLAN.md) — 代码迁移执行手册
- [LAYERED-ARCHITECTURE.md](../doc/architecture/LAYERED-ARCHITECTURE.md) — 整体分层架构
- [HORIZONTAL-SCALING.md](../doc/architecture/HORIZONTAL-SCALING.md) — 多节点部署与 VIP 拓扑

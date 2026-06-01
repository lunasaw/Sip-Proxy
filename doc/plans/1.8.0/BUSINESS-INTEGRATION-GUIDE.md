# 业务方接入指南（1.8.0）

> 版本：1.0 | 日期：2026-05-29
> 关联：[SIP-GATEWAY-AGGREGATION-PLAN.md](SIP-GATEWAY-AGGREGATION-PLAN.md) | [GB28181-GATEWAY-MODULE-PLAN.md](GB28181-GATEWAY-MODULE-PLAN.md) | [UNIFIED-ENVELOPE-PLAN.md](UNIFIED-ENVELOPE-PLAN.md) | [LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md)
>
> 本文档面向**业务方实施工程师**，按角色给出 1.8.0 三类典型场景的接入清单。每个角色都标注：
> - **必须**完成的步骤（缺则启动失败 / 上线即翻车）
> - **生产必换**（默认实现仅供单机演示）
> - **建议** / **可选**（按业务复杂度选）

---

## 一、三种角色的语义边界

| 角色 | 业务定位 | SIP 视角 | sip-gateway-spring-boot-starter 直接覆盖？ |
|------|---------|---------|---------|
| **服务端**（Server）  | 视频监控**平台**（如 voglander），等设备注册上来 | 收 REGISTER / 收 NOTIFY / 主动下发 INVITE/Query/Control | ✅ 完整覆盖 |
| **客户端**（Client）  | 模拟设备 / 边缘网关 / 国标接入器，主动注册到上级平台 | 主动 REGISTER / 主动 keepalive / 收 INVITE/Query 并应答 | ❌ 不在 starter 范围，需 `@EnableSipClient` 手装 |
| **中转平台**（Relay）  | 中级国标平台：下级设备来注册，自身又注册到上级国标 | 同时是 server（对下）+ client（对上） | ⚠️ server 侧自动；client 侧手装 |

**起决定作用的两个注解（来自 sip-gb28181，与 starter 解耦）：**

```java
@EnableSipServer    // 启用平台侧能力（接收 REGISTER / 处理 NOTIFY / 下发命令）
@EnableSipClient    // 启用客户端能力（主动注册 / 主动 keepalive / 应答查询）
```

它们**正交**：单角色只加一个；中转平台两个都加��`sip-gateway-spring-boot-starter` 的 AutoConfig 走 `@ConditionalOnBean(ServerCommandSender)` 守门，**只在加了 `@EnableSipServer` 时才装配 GB28181 模块**——这是有意的边界。

---

## 二、角色 A：服务端（视频监控平台）

> 典型场景：voglander 这类管理大量摄像头的平台。设备主动 REGISTER 进来，平台维护设备目录、主动下发查询和 INVITE 取流。

### 2.1 引依赖（一行）

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

starter 自动带上：`gateway-core` + `gateway-gb28181` + `gb28181-server` + `sip-common` + `gb28181-common`。

### 2.2 启用 server 角色（一行）

```java
@SpringBootApplication
@EnableSipServer
public class MyPlatformApp {
    public static void main(String[] args) {
        SpringApplication.run(MyPlatformApp.class, args);
    }
}
```

### 2.3 必填配置（application.yml）

```yaml
gateway:
  node-id: node-1                    # 必填：本节点身份
  gb28181:
    invite-context-ttl-ms: 30000     # INVITE 跨节点路由 TTL
    invite-idempotency-window-ms: 5000  # UDP 重传幂等窗口

sip:
  server:
    ip: 0.0.0.0                      # SIP 监听地址
    port: 5060                       # SIP 监听端口
    external-ip: 1.2.3.4             # 多节点必填：VIP 地址写到 Via/Contact 头
    serverId: 34020000002000000001   # 平台 GB28181 编号
    domain: 34020000                 # 平台 SIP 域
  common:
    user-agent: my-platform          # 标识自己

# 业务自定义：用于鉴权
sip.server.password: <每个设备的明文或映射表>
```

### 2.4 必须实现的两个 bean

#### ① ServerDeviceSupplier — 平台身份 + 设备鉴权

```java
@Component
@Primary    // 覆盖框架默认的 DefaultServerDeviceSupplier
public class MyServerDeviceSupplier implements ServerDeviceSupplier {

    private final DeviceSessionCache sessionCache;
    @Value("${sip.server.serverId}")  private String serverId;
    @Value("${sip.server.ip}")        private String serverIp;
    @Value("${sip.server.port}")      private int serverPort;

    private FromDevice serverFromDevice;

    @Override
    public FromDevice getServerFromDevice() {
        if (serverFromDevice == null) {
            serverFromDevice = FromDevice.getInstance(serverId, serverIp, serverPort);
        }
        return serverFromDevice;
    }

    @Override public void setServerFromDevice(FromDevice f) { this.serverFromDevice = f; }

    /** 取设备寻址，委托给 DeviceSessionCache（设备注册时已写入） */
    @Override
    public Device getDevice(String userId) {
        return sessionCache.getToDevice(userId);
    }

    @Override
    public ToDevice getToDevice(String deviceId) {
        return sessionCache.getToDevice(deviceId);
    }

    /**
     * 设备注册鉴权（HTTP Digest）。
     * <strong>生产环境必须实现真鉴权</strong>，按 userId 查库取明文密码。
     */
    @Override
    public boolean authenticate(String userId, SIPRequest request) {
        String password = lookupPasswordFromDb(userId);   // 业务自定义
        if (password == null) return false;
        return DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword(request, password);
    }
}
```

#### ② DeviceSessionCache — 设备寻址表

```java
@Component
public class MyDeviceSessionCache implements DeviceSessionCache {

    /**
     * 单机演示：内存版。
     * 生产必换 Redis/数据库，否则重启丢全部设备会话。
     */
    private final Map<String, ToDevice> cache = new ConcurrentHashMap<>();

    @Override
    public ToDevice getToDevice(String deviceId) {
        return cache.get(deviceId);
    }

    /** 业务侧：在 onDeviceRegister 事件回调里写入 */
    public void put(String deviceId, String ip, int port, String transport) {
        ToDevice toDevice = ToDevice.getInstance(deviceId, ip, port);
        toDevice.setTransport(transport);
        cache.put(deviceId, toDevice);
    }

    /** NAT IP 切换：onRemoteAddressChanged 时覆盖（ToDevice 是 mutable 的） */
    public void update(String deviceId, String newIp, int newPort) {
        ToDevice device = cache.get(deviceId);
        if (device != null) { device.setIp(newIp); device.setPort(newPort); }
    }
}
```

> ⚠️ **多节点部署**：`DeviceSessionCache` 必须落 Redis/数据库。同一个设备只会注册到 VIP 后某一台节点（源 IP 哈希），但跨节点 INVITE 回包需要任一节点都能查到设备会话——所以共享存储不可省。

### 2.5 强烈建议替换的 bean

#### ③ BusinessNotifier — 把 envelope 推给业务系统

starter 默认装 `NoopBusinessNotifier`（仅日志，启动时 warn）。**生产必换**，否则设备事件全丢。

```java
@Component
@Primary    // 覆盖 NoopBusinessNotifier
public class HttpWebhookNotifier extends AbstractProtocolBusinessNotifier {

    private final RestTemplate restTemplate;
    private final ApplicationProperties props;

    @Override
    @Async    // 必须异步，否则阻塞 SIP 事件线程导致设备超时重传
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        String type = event.type();
        try {
            // 按业务自定义路由：可以分协议 / 分 Group 推到不同 webhook
            if (type.startsWith("gb28181.Lifecycle.")) {
                restTemplate.postForLocation(props.lifecycleHook(), event);
            } else if (type.startsWith("gb28181.Notify.")) {
                restTemplate.postForLocation(props.notifyHook(), event);
            } else {
                restTemplate.postForLocation(props.defaultHook(), event);
            }
        } catch (RestClientException e) {
            log.error("notify failed: type={}, deviceId={}", type, event.deviceId(), e);
            // 业务侧自管重试 / 死信队列
        }
    }
}
```

> 业务方也可以用 Spring Cloud Stream / RocketMQ / Kafka 模板，不绑定 RestTemplate。

### 2.6 多节点强烈建议替换的 bean

#### ④ InviteContextStore — 跨节点 INVITE 上下文

starter 默认装 `InMemoryInviteContextStore`（启动时 warn）。**多节点必换 Redis**，否则跨节点 INVITE 回包路由失败。

```java
@Component
@Primary
@RequiredArgsConstructor
public class RedisInviteContextStore implements InviteContextStore {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(String callId, InviteContext value, long ttlMs) {
        redisTemplate.opsForValue().set(
                "sip:invite:ctx:" + callId,
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
        } catch (RedisConnectionFailureException | RedisCommandTimeoutException e) {
            // 必须抛 503 让业务侧识别"可重试"
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "redis unavailable", e);
        }
    }

    @Override
    public void remove(String callId) {
        try {
            redisTemplate.delete("sip:invite:ctx:" + callId);
        } catch (RedisConnectionFailureException e) {
            log.warn("redis remove failed (already responded, ignore): {}", callId, e);
        }
    }
}
```

### 2.7 业务方具体调用 / 接收示例

#### 下发命令（业务 → gateway）：HTTP POST

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

业务侧用 `correlationId` 关联回调（sn 或 callId）。

#### 接收事件（gateway → 业务）：BusinessNotifier.notify(event)

| 时机 | event.type | event.payload 关键字段 |
|------|-----------|----------------------|
| 设备完成 REGISTER | `gb28181.Lifecycle.Register` | `RegisterInfo` |
| 设备 keepalive 验活完成 | `gb28181.Lifecycle.Online` | `transaction` |
| 设备主动告警 | `gb28181.Notify.Alarm` | `DeviceAlarmNotify` |
| 收到 Catalog 应答 | `gb28181.Response.Catalog` | `DeviceResponse`（含 `deviceItems`） |
| INVITE 200 OK | `gb28181.Session.InviteOk` | 空（correlationId=callId） |

完整 35 个 type 表见 [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#五全量映射表)。

#### 设备主动 INVITE（如语音对讲）：异步回包

```http
POST /gateway/gb28181/invite/response HTTP/1.1
Content-Type: application/json

{
  "callId": "abc123@10.0.0.100",
  "sdp": "v=0\r\no=...",
  "statusCode": 200
}
```

业务侧从 `gb28181.Session.ServerInvite` 事件拿到 `callId` + `rawSdp`，准备好 SDP 后调此端点。错误码契约：

| HTTP | 场景 | 业务侧动作 |
|------|------|-----------|
| 410 | 事务超时 | **禁止重试**，重新走 INVITE |
| 502 | 跨节点路由表暂未刷新 | 200ms × 3 短重试 |
| 503 | store 后端不可达（Redis 故障） | 短重试 |

### 2.8 服务端接入清单（Checklist）

| # | 项 | 必填？ | 单机够用？ | 多节点必须？ |
|---|---|--------|-----------|-------------|
| 1 | 引 `sip-gateway-spring-boot-starter` | ✅ | ✅ | ✅ |
| 2 | 启用 `@EnableSipServer` | ✅ | ✅ | ✅ |
| 3 | 配置 `gateway.node-id` / `sip.server.*` | ✅ | ✅ | ✅ |
| 4 | 实现 `ServerDeviceSupplier` | ✅ | ✅ | ✅ |
| 5 | 实现 `DeviceSessionCache` | ✅ | ✅ 内存版 | ⚠️ 必须 Redis |
| 6 | 实现 `BusinessNotifier` | ⚠️ 生产必填 | 默认 Noop 仅日志 | ⚠️ 生产必填 |
| 7 | 实现 `InviteContextStore` | ⚠️ 生产必填 | ✅ 内存版 | ⚠️ 必须 Redis |
| 8 | 配置 `sip.server.external-ip` | — | — | ⚠️ 必须填 VIP |
| 9 | 配置 `gateway.nodes` 节点表 | — | — | ��️ 必须填全节点 |

---

## 三、角色 B：客户端（边缘设备 / 国标接入器）

> 典型场景：设备模拟器、车载终端、国标转码器，主动注册到上级国标平台，被动响应平台下发的 Query/Control，主动报送 keepalive/告警/位置。

### 3.1 重要边界：starter 不覆盖 client 角色

`sip-gateway-spring-boot-starter` 当前版本（1.8.0）的 envelope 化 + Registry 只覆盖 **server 侧出站命令**。client 侧无对等 envelope（暂时），原因是：

- client 侧"出站"主要是 REGISTER / keepalive 这种自维护行为，业务方很少手动触发
- client 侧"入站"是平台下发的 Query/Control，业务方实现 `QueryListener` 等接口直接应答即可，无需走 envelope

所以 client 接入仍然是**经典 sip-proxy 用法**：`@EnableSipClient` + 实现 `ClientDeviceSupplier` + 实现 5 个 listener。**不需要引 sip-gateway 模块**——除非同进程同时承担 server 角色（中转平台场景，见第四节）。

### 3.2 引依赖

```xml
<dependencies>
    <!-- 仅用 client：不需要 sip-gateway-spring-boot-starter -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gb28181-client</artifactId>
        <version>1.8.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 3.3 启用 client 角色

```java
@SpringBootApplication
@EnableSipClient
public class MyDeviceApp {
    public static void main(String[] args) {
        SpringApplication.run(MyDeviceApp.class, args);
    }
}
```

### 3.4 必填配置

```yaml
sip:
  client:
    clientId: 34020000001320000001     # 本设备 GB28181 编号
    domain: 192.168.1.100              # 本设备 IP（注册到平台的 Contact）
    port: 5061                         # 本地 SIP 端口
    register-expires: 3600             # 注册有效期
    keep-alive-interval: 60            # 心跳间隔（秒）
  server:                              # 上级平台地址（注册目标）
    serverId: 34020000002000000001
    ip: 1.2.3.4
    port: 5060
    domain: 34020000
    password: <平台为本设备分配的密码>
```

### 3.5 必须实现的 1 个 bean

#### ① ClientDeviceSupplier — 本设备身份 + 上级平台寻址

```java
@Component
@Primary    // 覆盖框架默认的 DefaultClientDeviceSupplier
public class MyClientDeviceSupplier implements ClientDeviceSupplier {

    private final String serverId;
    private final String serverIp;
    private final int    serverPort;
    private final String serverPassword;     // 用于 Digest 鉴权

    private FromDevice clientFromDevice;     // 本设备身份

    public MyClientDeviceSupplier(
            @Value("${sip.client.clientId}") String clientId,
            @Value("${sip.client.domain}")   String clientIp,
            @Value("${sip.client.port}")     int clientPort,
            @Value("${sip.server.serverId}") String serverId,
            @Value("${sip.server.ip}")       String serverIp,
            @Value("${sip.server.port}")     int serverPort,
            @Value("${sip.server.password}") String serverPassword) {
        this.serverId = serverId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.serverPassword = serverPassword;
        this.clientFromDevice = FromDevice.getInstance(clientId, clientIp, clientPort);
    }

    @Override
    public Device getDevice(String userId) {
        // 业务侧：返回上级平台的寻址 + 密码（Digest 鉴权时用）
        if (serverId.equals(userId)) {
            ToDevice toDevice = ToDevice.getInstance(serverId, serverIp, serverPort);
            toDevice.setPassword(serverPassword);
            return toDevice;
        }
        return null;
    }

    @Override
    public FromDevice getClientFromDevice() { return clientFromDevice; }

    @Override
    public void setClientFromDevice(FromDevice f) { this.clientFromDevice = f; }
}
```

### 3.6 实现 5 个 Listener（按需 override）

最简单的方式：继承 `ClientGb28181Adapter`，只 override 关心的方法。

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MyClientHandler extends ClientGb28181Adapter {

    private final DeviceState deviceState;     // 业务侧设备状态

    // ============== QueryListener（返回非 null 框架自动回包） ==============

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        // 上级平台查询 Catalog，返回本设备目录
        DeviceResponse resp = new DeviceResponse();
        resp.setDeviceId(deviceState.getDeviceId());
        resp.setSumNum(deviceState.getChannelCount());
        resp.setDeviceItems(deviceState.buildCatalogItems());
        return resp;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        DeviceInfo info = new DeviceInfo();
        info.setDeviceId(deviceState.getDeviceId());
        info.setDeviceName(deviceState.getName());
        info.setManufacturer("MyVendor");
        info.setModel("DVR-2025");
        info.setFirmware("v1.0.0");
        return info;
    }

    @Override
    public DeviceStatus onDeviceStatusQuery(String platformId, DeviceQuery query) {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceId(deviceState.getDeviceId());
        status.setOnline("ONLINE");
        status.setStatus("OK");
        return status;
    }

    // ============== ControlListener（PTZ/Reboot/...） ==============

    @Override
    public void onPtzControl(String platformId, String hexCmd) {
        // 解析 PTZ hex 指令，调底层电机驱动
        log.info("PTZ from {}: {}", platformId, hexCmd);
        deviceState.executePtz(hexCmd);
    }

    @Override
    public void onReboot(String platformId) {
        log.info("Reboot from {}", platformId);
        // 业务侧异步重启
    }

    // ============== NotifyListener（INVITE 来流） ==============

    // INVITE 类不在 client listener 里：通过 ApplicationEvent 监听 ClientInviteEvent
    // 详见 §3.7
}
```

可选粒度：业务方只关心 Query 时，可只 `implements QueryListener`，不必继承 Adapter。

### 3.7 INVITE / 流媒体场景

平台对设备发 INVITE 取流时，client 收到事件 `ClientInviteEvent`，业务侧异步准备 SDP 并应答：

```java
@Component
@RequiredArgsConstructor
public class MyInviteHandler {

    private final ClientCommandSender clientSender;
    private final MediaServerClient zlmClient;     // 业务侧 ZLM 控制

    @EventListener
    public void onClientInvite(ClientInviteEvent event) {
        String callId = event.getCallId();
        GbSessionDescription sdp = event.getSessionDescription();

        // 1. 业务侧让 ZLM 开始推流
        String streamId = zlmClient.startPush(sdp.getSsrc(), sdp.getMediaIp(), sdp.getMediaPort());

        // 2. 准备本端 SDP
        String responseSdp = buildResponseSdp(streamId, sdp);

        // 3. 异步回 200 OK（client 侧通过 ResponseCmd 直接回包）
        clientSender.send(CommandContext.forInviteResponse(callId, responseSdp));
    }
}
```

### 3.8 客户端接入清单（Checklist）

| # | 项 | 必填？ | 备注 |
|---|---|--------|------|
| 1 | 引 `gb28181-client` | ✅ | 不需要 sip-gateway-* |
| 2 | 启用 `@EnableSipClient` | ✅ | |
| 3 | 配置 `sip.client.*` + `sip.server.*` | ✅ | server 段是上级平台地址 |
| 4 | 实现 `ClientDeviceSupplier` | ✅ | 本设备身份 + 上级平台寻址 |
| 5 | 继承 `ClientGb28181Adapter` 或实现 5 个 listener | ⚠️ 至少 QueryListener | 否则平台查询全无回包 |
| 6 | `@EventListener` 监听 `ClientInviteEvent` | ⚠️ 取流场景必须 | 同时实现 `Client*Event` 中其他事件 |
| 7 | 启动后框架自动 REGISTER 到 `sip.server.*` | — | 框架自管，业务方不操心 |
| 8 | 业务方主动报送告警 / 移动位置 | 按场景 | 用 `ClientCommandSender.send(CommandContext.for*)` |

---

## 四、角色 C：中转平台（Relay / 级联国标）

> 典型场景：中级国标平台。下级设备来注册（角色 A），平台又把这些设备虚拟为通道，注册到上级国标（角色 B）；上级查询 Catalog 时，平台把下级设备目录翻译并应答；上级 INVITE 时，平台向下级 INVITE，把流媒体通道串联。

### 4.1 关键约束：必须同 JVM、双向引擎

中转平台 = **同进程同时**启用 server + client。这是必须的：

- `SipTransactionRegistry` 是进程内对象，跨进程后服务端无法把上级 INVITE 关联到对下级 INVITE
- `Dialog` 状态机也是进程内
- 共享 `nodeAddressMap` / Redis 用来跨**节点**（同一进程的多副本）扩展，不解决跨**角色**

### 4.2 引依赖（同时引 starter + client）

```xml
<dependencies>
    <!-- 服务端能力（含 envelope/HTTP API） -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>sip-gateway-spring-boot-starter</artifactId>
        <version>1.8.0</version>
    </dependency>

    <!-- 客户端能力（向上级注册） -->
    <dependency>
        <groupId>io.github.lunasaw</groupId>
        <artifactId>gb28181-client</artifactId>
        <version>1.8.0</version>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 4.3 启用两个角色

```java
@SpringBootApplication
@EnableSipServer    // 接受下级设备注册
@EnableSipClient    // 主动注册到上级平台
public class RelayApp {
    public static void main(String[] args) {
        SpringApplication.run(RelayApp.class, args);
    }
}
```

> ⚠️ **同 JVM 双角色的 SIP 端口**：server 与 client 必须使用**不同的本地端口**，否则 JAIN-SIP 端口冲突启动失败。例如 server `5060`，client `5061`。

### 4.4 必填配置（双段）

```yaml
sip:
  # 下级看到的"我"——server 视角
  server:
    ip: 0.0.0.0
    port: 5060
    external-ip: <VIP>                      # 多节点时填
    serverId: 34020000002000000001          # 本平台对外 ID
    domain: 34020000
  # 上级看到的"我"——client 视角
  client:
    clientId: 34020000002000000001          # 通常等于 serverId（同一个平台 ID）
    domain: 192.168.1.50                    # 本机内网 IP
    port: 5061                              # client 本地端口（≠ server.port）
    register-expires: 3600
  upstream:                                 # 业务自定义命名空间：上级平台
    serverId: 34020000003000000001
    ip: 10.10.10.10
    port: 5060
    password: <上级平台分配给本平台的密码>

gateway:
  node-id: relay-node-1
  gb28181:
    invite-context-ttl-ms: 30000
```

### 4.5 必须实现的 4 个 bean

#### ① ServerDeviceSupplier（同 §2.4 ①）

```java
@Component @Primary
public class MyServerDeviceSupplier implements ServerDeviceSupplier { ... }
```

#### ② DeviceSessionCache（同 §2.4 ②）

```java
@Component
public class MyDeviceSessionCache implements DeviceSessionCache { ... }
```

#### ③ ClientDeviceSupplier — **注意 client 看到的是上级平台**

```java
@Component @Primary
public class MyClientDeviceSupplier implements ClientDeviceSupplier {

    private final FromDevice clientFromDevice;
    private final String upstreamId;
    private final ToDevice upstreamDevice;

    public MyClientDeviceSupplier(
            @Value("${sip.client.clientId}") String clientId,
            @Value("${sip.client.domain}")   String clientIp,
            @Value("${sip.client.port}")     int clientPort,
            @Value("${sip.upstream.serverId}") String upstreamId,
            @Value("${sip.upstream.ip}")       String upstreamIp,
            @Value("${sip.upstream.port}")     int upstreamPort,
            @Value("${sip.upstream.password}") String upstreamPassword) {
        this.clientFromDevice = FromDevice.getInstance(clientId, clientIp, clientPort);
        this.upstreamId = upstreamId;
        this.upstreamDevice = ToDevice.getInstance(upstreamId, upstreamIp, upstreamPort);
        this.upstreamDevice.setPassword(upstreamPassword);
    }

    @Override
    public Device getDevice(String userId) {
        // client 视角下：getDevice 返回上级平台
        return upstreamId.equals(userId) ? upstreamDevice : null;
    }

    @Override public FromDevice getClientFromDevice()         { return clientFromDevice; }
    @Override public void setClientFromDevice(FromDevice f)   { this.clientFromDevice = f; }
}
```

#### ④ BusinessNotifier — 通常用作"内部桥"

中转平台特殊点：BusinessNotifier 不一定真把事件推到外部 webhook，更常见的是**内部桥接到 client 侧**。

```java
@Component @Primary
@RequiredArgsConstructor
@Slf4j
public class RelayBridgeNotifier extends AbstractProtocolBusinessNotifier {

    private final UpstreamRelayService upstream;     // 业务侧：管理与上级的对接

    @Override
    @Async
    protected void onProtocolEvent(String protocol, GatewayEvent event) {
        switch (event.type()) {
            case "gb28181.Lifecycle.Online":
                // 下级设备上线：把它作为通道，加进上级 Catalog
                String deviceId = event.deviceId();
                upstream.addChannel(deviceId);
                break;

            case "gb28181.Notify.Alarm":
                // 下级告警：转发给上级
                upstream.forwardAlarm(event.deviceId(),
                        (Map<String, Object>) event.payload());
                break;

            case "gb28181.Session.ServerInvite":
                // 上级 INVITE 流（场景：上级取流），转下级 INVITE
                Map<String, Object> p = event.payload();
                String callId = event.correlationId();
                String rawSdp = (String) p.get("rawSdp");
                upstream.relayInviteToDownstream(callId, rawSdp);
                break;

            // 其他事件按需桥接
        }
    }
}
```

### 4.6 上级查询的应答桥

中转平台 client 侧实现 `QueryListener`，把上级查询翻译成对下级数据的查询：

```java
@Component
@RequiredArgsConstructor
public class RelayQueryHandler implements QueryListener {

    private final RelayDeviceRegistry registry;     // 本平台维护的下级设备表

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        // 上级查询 Catalog → 返回本平台聚合后的下级设备目录
        DeviceResponse resp = new DeviceResponse();
        resp.setDeviceId(registry.getPlatformId());
        resp.setSumNum(registry.getChannelCount());
        resp.setDeviceItems(registry.buildAggregatedCatalog());
        return resp;
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        DeviceInfo info = new DeviceInfo();
        info.setDeviceId(registry.getPlatformId());
        info.setDeviceName("Relay-Platform");
        info.setManufacturer("MyRelay");
        return info;
    }
}
```

### 4.7 INVITE 取流的双向对接

最复杂的场景：上级 INVITE → 中转平台 → 下级 INVITE → 流媒体串联。

```
上级平台
   ↓ INVITE （callId-A，目标=本平台某通道）
中转平台 server 侧收：
   - Gb28181EventForwarder 发出 gb28181.Session.ServerInvite（callId-A）
   - InviteContextStore.save(callId-A, ...)
   ↓
RelayBridgeNotifier 收到事件：
   ↓ 异步：从 callId-A 关联出真正的下级设备 deviceId
   ↓
中转平台 client 侧（业务包装层）：
   ↓ INVITE（callId-B，目标=下级设备）通过 client 出站
下级设备
   ↓ 200 OK + SDP-下
中转平台 client 侧收到 SDP-下
   ↓ 业务转：把 SDP-下 重写成 SDP-中（媒体转发或直通）
   ↓
HTTP POST /gateway/gb28181/invite/response { callId: callId-A, sdp: SDP-中 }
   ↓
上级平台收到 200 OK + SDP-中，开始推流
```

中转平台业务方需自管 `callId-A ↔ callId-B` 的映射表（通常带 TTL 的 Caffeine / Redis）。

### 4.8 中转平台接入清单（Checklist）

| # | 项 | 必填？ | 备注 |
|---|---|--------|------|
| 1 | 引 `sip-gateway-spring-boot-starter` + `gb28181-client` | ✅ | 两个都要 |
| 2 | `@EnableSipServer` + `@EnableSipClient` | ✅ | 两个注解都加 |
| 3 | server 与 client SIP 端口不同 | ✅ | 否则启动冲突 |
| 4 | 实现 `ServerDeviceSupplier`（角色 A 视角） | ✅ | 同 §2.4 |
| 5 | 实现 `DeviceSessionCache` | ✅ | 同 §2.4 |
| 6 | 实现 `ClientDeviceSupplier`（视角=上级平台） | ✅ | 同 §3.5，但 getDevice 返回**上级** |
| 7 | 实现 `BusinessNotifier`（桥接：下级事件→上级动作） | ✅ | 中转的核心逻辑 |
| 8 | 实现 `QueryListener`（上级查询→下级聚合应答） | ✅ | Catalog/DeviceInfo/Status 至少 |
| 9 | 自管 `上级 callId ↔ 下级 callId` 映射 | ✅ | INVITE 双向对接的关键 |
| 10 | `BusinessNotifier#onProtocolEvent` 必须 `@Async` | ✅ | 中转尤其敏感，否则雪崩 |

---

## 五、三种角色对照表

| 维度 | 服务端 | 客户端 | 中转平台 |
|------|--------|--------|---------|
| 启用注解 | `@EnableSipServer` | `@EnableSipClient` | 两个都加 |
| starter 覆盖 | ✅ 完整 | ❌ 不覆盖 | 仅 server 侧自动 |
| 必装 bean 数 | 2 必填 + 2 生产必换 | 1 必填 | 4 必填 + Notifier 桥接 |
| 设备身份 bean | `ServerDeviceSupplier` | `ClientDeviceSupplier` | 两个都要 |
| 寻址 bean | `DeviceSessionCache` | 无（client 自管自身） | `DeviceSessionCache` |
| HTTP API | ✅ POST /gateway/* | ❌ 无 | ✅ POST /gateway/* |
| BusinessNotifier 用途 | 推业务系统 | 不涉及 | **桥接下级事件→上级动作** |
| listener 实现 | 内部由 EventForwarder 处理 | 业务方实现 5 个 listener | server 侧自动；client 侧业务实现 |
| 多节点要点 | Redis 化 store + cache | 单节点常见 | server 侧 Redis 化 |

---

## 六、共同的非功能性要求

### 6.1 异步约束

| 接口 | 是否必须异步 | 后果 |
|------|------------|------|
| `BusinessNotifier#notify` | ✅ 必须 | 阻塞 SIP 事件线程 → 设备超时重传 |
| `QueryListener#onXxxQuery` | 推荐异步（返回快） | 上级查询超时（默认 8s） |
| `DeviceSessionCache#getToDevice` | 必须 < 100ms | 命令出站全部受影响 |
| `InviteContextStore#find/save` | 必须 < 50ms | 跨节点 INVITE 回包慢 |

`@Async` 用法提醒：`@EnableAsync` 必须显式开启，否则注解失效。

### 6.2 鉴权

- **服务端**：`ServerDeviceSupplier#authenticate` 默认放行（`return true`），**生产必须实现真鉴权**。常见做法是按 userId 查库取明文密码，调 `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword`。
- **客户端**：`ClientDeviceSupplier#getDevice` 返回的 `Device.password` 用于上级 Digest 鉴权。
- **中转平台**：两侧鉴权独立，互不影响。

### 6.3 多节点部署

- `sip.server.external-ip` 必须填 VIP 地址，否则设备回包绕过 VIP 导致源 IP 哈希失效（详见 [HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)）
- `gateway.nodes` 节点表必须包含所有 sip-gateway 节点的内网地址，缺节点时跨节点 INVITE 回包返回 502
- 三个共享存储必须 Redis 化：`DeviceSessionCache` / `InviteContextStore` / 业务侧的 callId 映射

### 6.4 错误码契约（HTTP）

| HTTP | 触发 | 业务侧动作 |
|------|------|-----------|
| 400 | payload 字段缺失/类型错误 | 修正请求 |
| 404 | 未知 type | 修正 type，必须三段式 `<protocol>.<Group>.<Name>` |
| 410 | 事务已超时 | **禁止重试**，重新走原始命令 |
| 502 | nodeAddressMap miss | 200ms × 3 短重试 |
| 503 | store 故障 | 短重试 |

### 6.5 排查建议

- `GET /gateway/whoami` → 确认本节点 nodeId
- 启动日志找 `CommandHandlerRegistry ready: N types from M protocols`，N 应 = 30 表条目 + 17 注解 = 47（GB28181 单协议）
- 启动 warn `NoopBusinessNotifier active` / `InMemoryInviteContextStore active` → 生产前必须替换
- DEBUG `io.github.lunasaw.sipgateway`：详细看 envelope 流向
- 设备注册不通：先看 `ServerDeviceSupplier#authenticate` 是否有真实现

---

## 七、参考实现索引

| 角色 | 完整可运行示例 |
|------|---------------|
| Server + Client（同 JVM 双角色） | [sip-test/gb28181-test](../../../sip-test/gb28181-test/) — `TestApplication` + `SipBusinessConfig` + `TestServerDeviceSupplier` + `TestClientDeviceSupplier` |
| Server 单角色 envelope | [GatewayContextLoadTest](../../../sip-test/gb28181-test/src/test/java/io/github/lunasaw/gbproxy/test/sipgateway/GatewayContextLoadTest.java) — 验证 starter 自动装配 |
| Client 5 listener 实现 | [TestClientImpl / TestClientRegisterHandler](../../../sip-test/gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/handler/) |
| Server 事件处理 | [TestServerEventHandler / TestServerRegisterHandler](../../../sip-test/gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/handler/) |
| sip-gateway README（5 分钟快速开始） | [sip-gateway/README.md](../../../sip-gateway/README.md) |
| 完整 type 表（59 出站 + 35 入站） | [UNIFIED-ENVELOPE-PLAN §五](UNIFIED-ENVELOPE-PLAN.md#五全量映射表) |
| 父聚合形态 | [SIP-GATEWAY-AGGREGATION-PLAN](SIP-GATEWAY-AGGREGATION-PLAN.md) |
| 分层架构（同 JVM 约束） | [LAYERED-ARCHITECTURE](../../architecture/LAYERED-ARCHITECTURE.md) |
| 多节点 / VIP 拓扑 | [HORIZONTAL-SCALING](../../architecture/HORIZONTAL-SCALING.md) |

---

## 八、未来演进影响

| 版本 | 计划 | 对接入的影响 |
|------|------|-------------|
| 1.9.0 | gateway-gb28181-redis 子模块 | InviteContextStore / DeviceSessionCache 直接引依赖即用，不必业务方手写 Redis 实现 |
| 1.10.0 | gateway-onvif 子模块 | 多协议接入时业务方 `BusinessNotifier#onProtocolEvent` 多一个 protocol 分支即可，gateway-core / gateway-gb28181 零改动 |
| 1.11.0 | gateway-gt1078 子模块 | 同 1.10.0 |
| 待定 | gateway-discovery（K8s/Nacos） | `gateway.nodes` 静态配置改为动态发现，业务方仅改配置 |
| 待定 | client-side envelope 化 | 中转平台的 client 侧 listener 也走 envelope，统一 BusinessNotifier 桥接体验 |

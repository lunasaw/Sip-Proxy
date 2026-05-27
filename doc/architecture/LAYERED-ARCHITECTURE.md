# SIP 分层架构设计方案

> 版本：2.5 | 日期：2026-05-24

> ✅ **实施状态**：v1.3.0 协议层改造已落地，且 sip-gateway 业务侧参考实现已落到 [gb28181-test/.../gateway/](../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/)（含 `GatewayProperties`、`InviteContextStore` + `InMemoryInviteContextStore`、`SipEventForwarder`、`SipCommandController`、`BusinessNotifier` 抽象 + `NoopBusinessNotifier`、`GatewayConfig`）。`SipTransactionRegistry`、`DeviceSessionCache`、`external-ip`、`@EnableSipServer`、INVITE 异步化、`extendContext` 续期、`ServerDeviceSupplier.authenticate`、BYE 200 OK 协议合规、Handler 接口全量删除均已完成（详见第八、九节对照表）。生产部署仍需将 `InMemoryInviteContextStore` 替换为 Redis 实现、`NoopBusinessNotifier` 替换为实际 HTTP/MQ 推送、`nodeAddressMap` 替换为 K8s/Nacos 动态发现。
>
> v2.4 → v2.5 变更：补充 `InviteContextStore` 接口的 503 错误语义约定，`SipCommandController.inviteResponse` 增加 store 故障兜底（未受控 `RuntimeException` 一律转 `503 Service Unavailable`，避免冒成 500 让业务侧无法识别"可重试"语义），同步 §3、§6.4。
>
> v2.3 → v2.4 变更：补充共享 NAT 聚集效应、Redis 高可用要求、INVITE 重传幂等示例代码、`nodeAddressMap` 刷新窗口的重试约定，并修订 §7.2 关于 1xx 响应与 Timer B 关系的描述。

---

## 一、整体分层

```
┌─────────────────────────────────────────────────────────┐
│                     业务服务器                            │
│  处理业务逻辑（设备管理、录像、告警、流媒体调度等）          │
│  调 sip-gateway HTTP/MQ 接口触发 SIP 命令                 │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP / MQ
┌──────────────────────────▼──────────────────────────────┐
│                     sip-gateway                          │
│  业务方实现的网关服务（Spring Boot 应用，多节点部署）        │
│  ├── 实现 DeviceSessionCache  → Redis（共享）             │
│  ├── 实现 ServerDeviceSupplier → Redis（共享）            │
│  ├── 监听 Spring Event → 推送给业务服务器（HTTP/MQ）       │
│  └── 暴露 HTTP API → 接收业务服务器的 SIP 命令指令         │
└──────────────────────────┬──────────────────────────────┘
                           │ 引入 Maven 依赖（同一 JVM）
┌──────────────────────────▼──────────────────────────────┐
│                     sip-proxy（框架库）                   │
│  ├── sip-common：SIP 协议栈、事务管理、发送工具             │
│  ├── gb28181-common：GB28181 数据模型                     │
│  ├── gb28181-server：服务端处理器、Spring Event 发布       │
│  └── gb28181-client：客户端处理器、Spring Event 发布       ��
└─────────────────────────────────────────────────────────┘
```

---

## 二、各层职责边界

### 2.1 sip-proxy（框架库）

**只做协议层**，不含任何业务逻辑：

- 接收 SIP 消息，解析协议字段
- 发布 Spring Event（`DeviceRegisterEvent`、`DeviceInviteOkEvent`、`DeviceAlarmEvent` 等）
- 提供 `ServerCommandSender` / `ClientCommandSender` 发送 SIP 命令
- 定义需业务方实现的接口：`DeviceSessionCache`、`ServerDeviceSupplier`、`ClientDeviceSupplier`

**不做**：设备持久化、业务路由、跨进程通信、流媒体调度。

### 2.2 sip-gateway（业务方网关）

**协议层与业务层的桥梁**，与 sip-proxy 运行在**同一 JVM 进程**中。同进程是硬约束，不可拆分：

- `SipTransactionRegistry` 持有的 `ServerTransaction` 是 JAIN-SIP 实现类（`gov.nist.javax.sip.stack.SIPServerTransactionImpl`），不可序列化
- `RequestEvent` 内部持有 socket 引用（`SipProvider`、`Dialog`），跨进程后无法回包
- 因此 `sip-proxy` 必须以 Maven 依赖方式嵌入 `sip-gateway`，业务方与协议栈共享 JVM

| 职责 | 实现方式 |
|------|---------|
| 设备会话持久化 | 实现 `DeviceSessionCache`，存 Redis |
| 设备信息提供 | 实现 `ServerDeviceSupplier`，读 Redis |
| 入站事件转发 | `@EventListener` 监听 Spring Event，推送给业务服务器（HTTP/MQ） |
| 出站命令接收 | 暴露 HTTP API，接收业务服务器指令，调 `ServerCommandSender` |
| INVITE 异步回包 | 暴露 `/sip/invite/response` API，接收 SDP，通过 `SipTransactionRegistry` 取回事务发 200 OK |
| 跨节点路由表 | 装配 `Map<String, String> nodeAddressMap`（业务方自行实现，见 §6.5） |

### 2.3 业务服务器

**纯业务逻辑**，不感知 SIP 协议：

- 接收 sip-gateway 推送的设备事件（注册、告警、心跳等）
- 处理业务（设备管理、录像检索、流媒体调度等）
- 调 sip-gateway HTTP API 触发 SIP 命令（点播、控制、查询等）

---

## 三、状态分层与存储策略

这是水平扩容的核心约束，**必须严格遵守**：

| 状态类型 | 存储位置 | 说明 |
|---------|---------|------|
| `ServerTransaction` / `SipTransactionRegistry`（**入站** INVITE 事务上下文） | **进程内**（不可外化） | JAIN-SIP 实现类不可序列化，且持有 socket 引用；同一设备消息必须打同一节点 |
| `DialogRegistry`（**出站** INVITE / SUBSCRIBE dialog 引用，1.7.0+） | **进程内**（不可外化） | JAIN-SIP `Dialog` 持有 socket / transaction 引用，与 `SipTransactionRegistry` 同构。INVITE 由 `processDialogTerminated` 主路径清理，SUBSCRIBE 由 `DialogRegistryCleaner` 定时清理。详见 [`OUTBOUND-DIALOG-PLAN.md`](../plans/1.7.0/OUTBOUND-DIALOG-PLAN.md) §5 |
| `DeviceSessionCache`（设备注册信息） | **Redis**（共享，需高可用） | 业务方实现，节点间共享，节点故障后新节点可接管 |
| `ServerDeviceSupplier`（设备信息） | **Redis**（共享，需高可用） | 业务方实现，读 Redis |
| 设备订阅状态 | 业务方自管 | 框架不再提供 `SubscribeHolder`（v1.3.0 已移除），由业务方根据需要存 Redis 或自行管理 |
| INVITE 事务上下文 | **进程内** + Redis 存路由映射（需高可用） | `transactionContextKey`（`callId_fromTag_cseq`）只在收到 INVITE 的节点有效；Redis 用 `callId` 作键存 `{nodeId}:{contextKey}` 供跨节点回包路由，见 §5.3 |

**违反此约束会导致节点故障时业务状态丢失且无法恢复。**

> ⚠️ **`callId` ≠ `transactionContextKey`**：业务服务器只感知 `callId`，但框架内部用 `callId_fromTag_cseq` 作上下文键。Redis 中以 `callId` 为键便于业务侧查询，值里携带 `contextKey` 让节点能反查 `SipTransactionRegistry`。

> ⚠️ **Redis 是新的 SPOF**：跨节点 INVITE 路由、设备会话、注册鉴权读取全部依赖 Redis。30s INVITE TTL 内 Redis 故障 = 进行中的设备主动 INVITE 全部回包失败。**生产环境必须使用 Redis Sentinel 或 Cluster**，并对关键调用做超时与降级（如 `/sip/invite/response` 在 Redis 不可达时直接返回 503，由业务侧重试）。`InviteContextStore` 接口约定：实现方需把后端故障显式抛成 `ResponseStatusException(SERVICE_UNAVAILABLE)`，否则 `SipCommandController` 会在公共兜底里把未受控 `RuntimeException` 一律转 503，避免冒成 500 让业务侧无法识别"可重试"语义。

---

## 四、关键流程设计

### 4.1 设备注册

```
设备 → REGISTER
  → sip-proxy: ServerRegisterRequestProcessor
      发布 DeviceRegisterEvent（含 deviceId、ip、port、transport）
  → sip-gateway: @EventListener
      1. 存 Redis: "sip:device:{deviceId}" → {ip, port, transport}
      2. 推送 HTTP/MQ → 业务服务器（设备上线通知）
```

### 4.2 实时点播（平台主动发起 INVITE）

```
业务服务器 → POST /sip/invite/start {deviceId, mediaIp, mediaPort}
  → sip-gateway: 调 ServerCommandSender.deviceInvitePlay()
  → sip-proxy: 发 INVITE → 设备

设备 → 100 Trying / 200 OK (SDP)
  → sip-proxy: InviteResponseProcessor
      发布 DeviceInviteOkEvent（含 callId、SDP）
  → sip-gateway: @EventListener
      推送 HTTP/MQ → 业务服务器（含 callId、设备 SDP）

业务服务器 → POST /sip/invite/ack {callId}
  → sip-gateway: 调 ServerCommandSender.deviceAck(callId)
  → sip-proxy: 发 ACK → 设备开始推流
```

### 4.3 设备主动发起 INVITE（语音对讲场景）

INVITE 需要**异步回包**（业务方需要时间准备 SDP），处理分两步：

```
设备 → INVITE
  → sip-proxy: ServerInviteRequestProcessor
      1. 立即发 100 Trying（防止对端重传）
      2. 存 SipTransactionRegistry（contextKey = callId_fromTag_cseq → RequestEvent，进程内）
      3. 发布 ServerInviteEvent（含 callId、contextKey、SDP）
  → sip-gateway: @EventListener
      1. 存 Redis: "sip:invite:ctx:{callId}" → "{nodeId}:{contextKey}"（30s TTL）
      2. 推送 HTTP/MQ → 业务服务器（含 callId、SDP）

业务服务器 → POST /sip/invite/response {callId, sdp}
  → sip-gateway:
      1. 从 Redis 取 "sip:invite:ctx:{callId}"，得 "{nodeId}:{contextKey}"
      2. 若 nodeId == 本节点：用 contextKey 取 SipTransactionRegistry 回包
         若 nodeId != 本节点：通过 nodeAddressMap 转发 HTTP 到对应节点
      3. ResponseCmd.sendResponse(200, sdp, ctx.getOriginalEvent())
```

### 4.4 设备控制

```
业务服务器 → POST /sip/control/ptz {deviceId, ptzCmd}
  → sip-gateway: 调 ServerCommandSender.deviceControlPtzCmd()
  → sip-proxy: 发 MESSAGE(DeviceControl) → 设备 → 200 OK
```

---

## 五、水平扩容部署

### 5.1 部署拓扑

```
设备 ──→ VIP 1.2.3.4:5060 ──→ Node-1 (sip-gateway + sip-proxy)
         (keepalived + ipvs)  └→ Node-2 (sip-gateway + sip-proxy)
         源 IP 哈希                    │
                                    Redis（共享 DeviceSessionCache）
                                    业务服务器
```

**关键点**：
- VIP 四层透明转发，不修改 SIP 包内容
- 按**源 IP 哈希**分配节点，同一设备永远打到同一节点
- 节点故障时 keepalived 自动摘除，设备重新注册分到存活节点

> ⚠️ **NAT 公网 IP 切换的脏数据问题**：设备走 NAT 时公网 IP 可能因运营商重连/路由器重启而变化，源 IP 哈希会重分配节点，触发设备重注册。此时 Redis 中可能同时存在旧节点和新节点的 `DeviceSessionCache` 条目（同一 `deviceId` 但 `ip:port` 不同），需通过以下任一方式收敛：
> - 重注册时按 `deviceId` 覆盖写入（不带 nodeId 区分），让新值替换旧值
> - 设置较短 TTL（如 24h），等过期后自然清理
> - 在 `RedisDeviceSessionCache.save()` 中先 `DEL` 再 `SET`

### 5.2 NAT 穿透配置

```yaml
sip:
  server:
    ip: 0.0.0.0           # 监听地址（内网/所有网卡）
    port: 5060
    external-ip: 1.2.3.4  # VIP 地址，填入 Via/Contact（多节点必填）
    external-port: 5060
```

多节点时 `external-ip` 填 VIP，设备后续消息发到 VIP，ipvs 源 IP 哈希保证还是打到同一节点。

### 5.3 INVITE 事务的跨节点问题

`SipTransactionRegistry` 是进程内存储，`contextKey` 只在收到 INVITE 的节点有效。业务服务器回包时需路由到正确节点：

**方案 A（推荐）：Redis 存节点标识**

```
sip-gateway 收到 ServerInviteEvent 时：
  Redis.set("sip:invite:ctx:{callId}", "{nodeId}:{contextKey}", 30s)

业务服务器回包时：
  1. 从 Redis 取 "{nodeId}:{contextKey}"
  2. 若 nodeId == 本节点 → 直接处理
  3. 若 nodeId != 本节点 → HTTP 转发到 http://{nodeId}/sip/invite/response
```

**方案 B：MQ 广播**

```
业务服务器发 MQ 消息（含 callId、sdp）
各节点订阅，自行判断 SipTransactionRegistry 中是否有该 callId
有则处理，无则忽略
```

方案 A 更精确，方案 B 更简单但有广播开销。

### 5.4 扩容粒度

水平扩容的粒度是**设备 NAT 出口**，不是物理设备数：

- 不同 NAT 出口分散到不同节点，总容量随节点数线性增长
- 单个设备的所有消息始终在同一节点，事务正常
- 对 GB28181 场景足够：单设备并发请求极少，瓶颈在设备总数

> ⚠️ **共享 NAT 出口的聚集效应**：`-s sh` 按客户端源 IP 哈希分片，企业园区/家庭网络下多个 GB28181 设备共享同一 NAT 公网 IP 时会全部落到同一节点。规划容量时应按 **NAT 出口数** 而非物理设备数估算节点负载。极端场景（单 NAT 出口下设备数远超单节点容量）需要在 sip-gateway 之上做 deviceId 二次分发，但需保证同 deviceId 始终打同一 sip-gateway 节点（破坏源 IP 哈希后由业务方自行维护粘性）。

---

## 六、sip-gateway 实现示例

### 6.1 DeviceSessionCache（必须实现）

> 框架接口仅定义 `getToDevice(deviceId)` 一个方法。`save` 是业务方根据需要新增的写入方法，由 `SipEventForwarder.onRegister` 调用，不属于 `DeviceSessionCache` 接口。

```java
@Component
public class RedisDeviceSessionCache implements DeviceSessionCache {
    @Autowired private RedisTemplate<String, ToDevice> redis;

    @Override
    public ToDevice getToDevice(String deviceId) {
        return redis.opsForValue().get("sip:device:" + deviceId);
    }

    /** 业务方自定义写入方法，由 @EventListener 调用 */
    public void save(String deviceId, ToDevice device) {
        // NAT IP 切换会产生脏数据，先 DEL 再 SET 让新值覆盖旧值
        redis.delete("sip:device:" + deviceId);
        redis.opsForValue().set("sip:device:" + deviceId, device, 24, TimeUnit.HOURS);
    }
}
```

### 6.2 ServerDeviceSupplier（必须实现）

```java
@Component
public class GatewayServerDeviceSupplier implements ServerDeviceSupplier {
    @Autowired private RedisTemplate<String, Device> redis;
    @Value("${sip.server.serverId}") private String serverId;
    @Value("${sip.server.external-ip}") private String externalIp;
    @Value("${sip.server.port}") private int port;

    @Override
    public Device getDevice(String userId) {
        return redis.opsForValue().get("sip:device:" + userId);
    }

    @Override
    public FromDevice getServerFromDevice() {
        return FromDevice.getInstance(serverId, externalIp, port);
    }
}
```

### 6.3 Spring Event 监听器

```java
@Component
public class SipEventForwarder {
    @Value("${gateway.node-id}") private String nodeId;
    @Autowired private RedisTemplate<String, String> redis;
    @Autowired private RedisDeviceSessionCache sessionCache;

    /** 已处理 callId 缓存，UDP INVITE 重传时避免重复推送给业务服务器 */
    private final Cache<String, Boolean> processedInvites =
        Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(10_000).build();

    @EventListener
    public void onRegister(DeviceRegisterEvent e) {
        sessionCache.save(e.getDeviceId(), buildToDevice(e));
        businessClient.post("/device/online", e);
    }

    @EventListener
    public void onServerInvite(ServerInviteEvent e) {
        // UDP 下设备会按 T1 退避重传 INVITE，框架按相同 contextKey 覆盖写入安全，
        // 但事件会被多次 publish。按 callId 幂等，避免向业务服务器重复推送。
        if (processedInvites.asMap().putIfAbsent(e.getCallId(), Boolean.TRUE) != null) {
            log.debug("INVITE 重传，跳过重复推送: callId={}", e.getCallId());
            return;
        }

        // 存节点标识 + contextKey，供跨节点回包路由
        redis.opsForValue().set(
            "sip:invite:ctx:" + e.getCallId(),
            nodeId + ":" + e.getTransactionContextKey(),
            30, TimeUnit.SECONDS
        );
        businessClient.post("/invite/incoming", e);
    }

    @EventListener
    public void onInviteOk(DeviceInviteOkEvent e) {
        businessClient.post("/invite/ok", e);
    }

    @EventListener
    public void onAlarm(DeviceAlarmEvent e) {
        businessClient.post("/alarm/notify", e);
    }
}
```

### 6.4 HTTP API（供业务服务器调用）

```java
@RestController
@RequestMapping("/sip")
public class SipCommandController {
    @Value("${gateway.node-id}") private String nodeId;
    @Autowired private ServerCommandSender commandSender;
    @Autowired private RedisTemplate<String, String> redis;
    @Autowired private Map<String, String> nodeAddressMap; // nodeId → http://ip:port，见 §6.5
    @Autowired private RestTemplate restTemplate;          // 业务方自行注入

    @PostMapping("/invite/start")
    public String invitePlay(@RequestBody InviteRequest req) {
        return commandSender.deviceInvitePlay(req.getDeviceId(), req.getMediaIp(), req.getMediaPort(), StreamModeEnum.UDP);
    }

    @PostMapping("/invite/bye")
    public void bye(@RequestBody ByeRequest req) {
        commandSender.deviceBye(req.getDeviceId(), req.getCallId());
    }

    @PostMapping("/invite/response")
    public void inviteResponse(@RequestBody InviteResponseRequest req) {
        String val = redis.opsForValue().get("sip:invite:ctx:" + req.getCallId());
        if (val == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "invite ctx expired or unknown callId");
        }
        String[] parts = val.split(":", 2);
        String targetNode = parts[0], ctxKey = parts[1];

        if (nodeId.equals(targetNode)) {
            // 本节点处理
            SipTransactionRegistry.TransactionContextInfo ctx = SipTransactionRegistry.getContext(ctxKey);
            if (ctx == null) {
                throw new ResponseStatusException(HttpStatus.GONE, "transaction terminated");
            }
            ResponseCmd.sendResponse(Response.OK, req.getSdp(),
                ContentTypeEnum.APPLICATION_SDP.getContentTypeHeader(), ctx.getOriginalEvent());
        } else {
            // 转发到目标节点。nodeAddressMap 在节点上下线/IP 变更时存在数秒刷新窗口，
            // 此时映射不到目标节点会返回 502，业务侧需配合短间隔重试（建议 200ms x 3）。
            String targetUrl = nodeAddressMap.get(targetNode);
            if (targetUrl == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "unknown node: " + targetNode);
            }
            restTemplate.postForObject(targetUrl + "/sip/invite/response", req, Void.class);
        }
    }

    @PostMapping("/control/ptz")
    public void ptz(@RequestBody PtzRequest req) {
        commandSender.deviceControlPtzCmd(req.getDeviceId(), req.getCmd(), req.getSpeed());
    }

    @PostMapping("/query/catalog")
    public String catalog(@RequestBody QueryRequest req) {
        return commandSender.deviceCatalogQuery(req.getDeviceId());
    }
}
```

> **业务服务器侧重试策略**：调 `/sip/invite/response` 收到 `502 Bad Gateway` 或 `503 Service Unavailable` 时应短间隔（200ms）重试 2~3 次，覆盖 nodeAddressMap 刷新与 Redis 短暂抖动；收到 `410 Gone` 表示事务已超时，**禁止重试**，需重新发起 INVITE。`503` 来源有两类：(a) 跨节点 RestTemplate 转发失败；(b) `InviteContextStore` 后端（Redis 等）不可达——`SipCommandController` 已统一映射成 503，业务侧无需区分。

### 6.5 nodeAddressMap 装配

跨节点 INVITE 回包路由依赖 `nodeId → 内网地址` 映射，**框架不提供该 bean**，业务方按部署形态选择装配方式：

**静态配置（开发/小规模）**：

```yaml
gateway:
  node-id: node-1
  nodes:
    node-1: http://10.0.0.1:8080
    node-2: http://10.0.0.2:8080
```

```java
@ConfigurationProperties("gateway")
@Data
public class GatewayProperties {
    private String nodeId;
    private Map<String, String> nodes = new HashMap<>();
}

@Bean
public Map<String, String> nodeAddressMap(GatewayProperties props) {
    return props.getNodes();
}
```

**服务发现（生产/动态扩缩）**：

- K8s：通过 `Endpoints` API 监听 `sip-gateway` Service 的 Pod 列表，pod 注解 `gateway.node-id` 作为 key
- Nacos/Consul：每个节点注册时携带 `node-id` 元数据，监听服务变更刷新 `Map`

无论哪种方式，**`nodeId` 必须与 `RedisDeviceSessionCache.save()` / `SipEventForwarder.onServerInvite()` 写入 Redis 的 `nodeId` 严格一致**，否则跨节点路由会找不到目标。

---

## 七、INVITE 事务超时处理

`SipTransactionRegistry.TransactionContextInfo.checkAndUpdateValidity()` 默认 32 秒后将上下文标记为无效（`age > 32000`）。这是**框架自检阈值**，不是 SIP 协议层超时——JAIN-SIP 的 `ServerTransaction` 在 INVITE 的 `Proceeding` 状态可以无限期等待最终响应（RFC 3261 §17.2.1，只要持续发 `1xx` 即可保活，但 NIST 实现实际不严格 enforce）。

### 7.1 客户端 Timer B 才是硬约束

⚠️ **关键事实**：即使框架侧续期 `TransactionContextInfo` 到 90 秒，设备侧（INVITE 客户端事务）按 RFC 3261 §17.1.1.2 在 `Timer B = 64*T1 = 32s` 后就会放弃事务并向上层报错。**此后即使服务端发出 200 OK，设备也已不再 ACK**，链路依然失败。

因此 `SipTransactionRegistry.extendContext` 的实际意义不是"无限延长 INVITE 处理"，而是：
- **30s 内**业务正常处理 → 框架侧不超时（无需续期）
- **业务预计 < 60s** → 续期 + 在 30s 边界前主动发 `180 Ringing`：按 RFC 3261 §17.1.1.2，客户端事务收到 1xx 后从 Calling 切到 Proceeding，Timer B 在 Proceeding 状态由具体实现决定是否继续计时（NIST 协议栈停止计时，但摄像头厂商实现差异较大）
- **业务必然 > 60s** → 改为先回 `200 OK` 携带占位 SDP，后续走 `re-INVITE` 更新真实 SDP；或返回 `408 Request Timeout` 让业务侧重新发起

### 7.2 业务超时分级处理

| 场景 | 处理方式 |
|------|---------|
| 业务处理 < 30s | 直接回包，无需特殊处理 |
| 业务处理 30~60s | sip-gateway 收到 `ServerInviteEvent` 后**主动续期** `SipTransactionRegistry.extendContext(callId, 60_000)`，并在 25s 边界发 `180 Ringing`（依赖设备 SIP 栈在 Proceeding 状态停止 Timer B，国产摄像头多数遵守，但**不能假定 100%**） |
| 业务处理 > 60s（不推荐） | 改为先回 200 OK + 占位 SDP，后续走 re-INVITE 更新真实 SDP；或返回 408 让设备重发 |
| 事务已超时 | `getContext` 返回 null，`/sip/invite/response` 返回 410 Gone，业务服务器需重新发起 |

> ⚠️ **关于 1xx 与 Timer B 的精确说法**：
> - 框架侧 `extendContext` 只解决 "`SipTransactionRegistry` 不被 32s 硬超时摘除"，与设备侧事务状态无关。
> - 设备侧 Timer B 是否被 1xx 停止由 RFC 3261 §17.1.1.2 规定（"transition to the Proceeding state"——状态机进入 Proceeding 后由具体实现决定 Timer B 行为）。NIST 协议栈作为客户端在 Proceeding 状态停止 Timer B，但摄像头厂商实现可能不一致。
> - **保守做法**：业务可能 > 30s 的场景预先回 `180 Ringing` 占位 + `extendContext` 续期，但仍按 60s 总处理时间设计上限；超过则改用 200 OK + 占位 SDP + re-INVITE 二阶段方案。

---

## 八、sip-proxy 框架层配套改造

为支持上述分层架构，框架层需完成以下改造（详见 [INVITE-REFACTOR-PLAN.md](../plans/1.3.0/INVITE-REFACTOR-PLAN.md) v1.1）：

| 改造项 | 目的 | 状态 |
|--------|------|------|
| `ServerInviteRequestProcessor` 改为 100 Trying + 发 `ServerInviteEvent` | 支持异步回包 | ✅ 已完成 |
| 新增 `ServerInviteEvent`（携带 `transactionContextKey`） | 业务方取回进程内事务 | ✅ 已完成 |
| `InviteRequestProcessor`（client）改为 100 Trying + 发 `ClientInviteEvent` | 客户端 INVITE 异步化 | ✅ 已完成 |
| 新增 `ClientInfoEvent` + `InfoRequestProcessor` 改为发事件 | INFO 走事件总线 | ✅ 已完成 |
| 删除 `InviteRequestHandler` / `ServerInviteRequestHandler` 等 Handler 接口 | 统一事件总线模式，降低接入复杂度 | ✅ 已完成 |
| `ServerRegisterRequestProcessor.extractPassword` 改为调 `ServerDeviceSupplier.authenticate(userId, SIPRequest)` | 让密码验证有真实输入（原返回空串，鉴权形同虚设） | ✅ 已完成 |
| `ServerDeviceSupplier.authenticate(userId, SIPRequest)` 默认方法 | 注册鉴权下沉到 Supplier，业务方调 `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword` 完成摘要校验 | ✅ 已完成 |
| `SipTransactionRegistry.extendContext(ctxKey, ttlMs)` + `TransactionContextInfo.validUntilOverride` 字段 | 业务处理 > 30s 时续期上下文（受设备 Timer B 限制，详见 §七） | ✅ 已完成 |
| `ByeRequestProcessorServer` 修复 200 OK 漏发 | 原实现违反 RFC 3261 §15.1.2，设备会重传 BYE | ✅ 已完成 |
| `ClientDeviceSupplier.checkDevice(evt)` 默认方法 | 同 JVM 同时启用 client/server 时按 To-Header userId 隔离两侧 INVITE ���理 | ✅ 已完成 |

**实施状态**：第四、五节描述的端到端流程所需的协议层改造已全部合入，剩余工作在业务方（sip-gateway）侧落地：实现 `ServerDeviceSupplier.authenticate`、`@EventListener ServerInviteEvent` + 跨节点 `nodeAddressMap` 路由。

---

## 九、当前实现状态对照表

| 方案要素 | 代码位置 | 状态 |
|---------|---------|------|
| `SipTransactionRegistry`（进程内事务上下文） | [`sip-common/.../SipTransactionRegistry.java`](../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/SipTransactionRegistry.java) | ✅ 就绪 |
| `SipTransactionRegistry.extendContext` 续期接口 | 同上 | ✅ 就绪 |
| `DeviceSessionCache` 接口 | [`gb28181-server/.../DeviceSessionCache.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/cmd/DeviceSessionCache.java) | ✅ 就绪 |
| `ServerCommandSender` 注入 `DeviceSessionCache` | [`gb28181-server/.../ServerCommandSender.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/cmd/ServerCommandSender.java) | ✅ 就绪 |
| `external-ip/port` NAT 配置 | [`SipServerProperties.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/config/SipServerProperties.java) | ✅ 就绪 |
| `@EnableSipServer` / `@EnableSipClient` 注解 | [`EnableSipServer.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/config/EnableSipServer.java) | ✅ 就绪 |
| `DigestServerAuthenticationHelper.doAuthenticatePlainTextPassword` | [`DigestServerAuthenticationHelper.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/register/DigestServerAuthenticationHelper.java) | ✅ 就绪（业务方直接调用即可） |
| `ClientInviteEvent.transactionContextKey` 字段 | [`ClientInviteEvent.java`](../../gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/eventbus/event/ClientInviteEvent.java) | ✅ 就绪 |
| `InviteRequestProcessor`（client）发布事件 | [`InviteRequestProcessor.java`](../../gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/transmit/request/invite/InviteRequestProcessor.java) | ✅ 100 Trying + 发 `ClientInviteEvent` |
| `ServerInviteRequestProcessor` 异步化 | [`ServerInviteRequestProcessor.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/invite/ServerInviteRequestProcessor.java) | ✅ 100 Trying + 发 `ServerInviteEvent` |
| `ServerInviteEvent` 类 | [`ServerInviteEvent.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/event/ServerInviteEvent.java) | ✅ 就绪 |
| `ClientInfoEvent` 类 | [`ClientInfoEvent.java`](../../gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/eventbus/event/ClientInfoEvent.java) | ✅ 就绪 |
| `ServerDeviceSupplier.authenticate(userId, SIPRequest)` | [`ServerDeviceSupplier.java`](../../sip-common/src/main/java/io/github/lunasaw/sip/common/service/ServerDeviceSupplier.java) | ✅ 就绪 |
| `ServerRegisterRequestProcessor` 鉴权改造 | [`ServerRegisterRequestProcessor.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/register/ServerRegisterRequestProcessor.java) | ✅ 调 `serverDeviceSupplier.authenticate`，删除空串 `extractPassword` |
| `ByeRequestProcessorServer` 200 OK 协议合规 | [`ByeRequestProcessorServer.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/bye/ByeRequestProcessorServer.java) | ✅ 收到 BYE 立即回 200 OK 再发事件 |
| `ServerInfoRequestProcessor` 事件化 | [`ServerInfoRequestProcessor.java`](../../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/request/info/ServerInfoRequestProcessor.java) | ✅ 删除 handler，直接发 `DeviceInfoRequestEvent` |
| Handler 接口清理（`*ProcessorHandler` / `InviteRequestHandler` / `InfoRequestHandler` 等） | gb28181-server / gb28181-client | ✅ 全部删除，业务方一律 `@EventListener` |
| `SubscribeHolder` / `SubscribeTask` | sip-common | ✅ 已删除（v1.3.0），下放业务方 |
| `nodeAddressMap` 装配示例 | [`gb28181-test/.../gateway/GatewayProperties.java`](../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/GatewayProperties.java) | ✅ 参考实现就绪（gb28181-test 模块），生产仍需替换为 K8s/Nacos 动态发现 |
| sip-gateway 参考实现（`SipEventForwarder` / `SipCommandController` / `InviteContextStore`） | [`gb28181-test/.../gateway/`](../../gb28181-test/src/main/java/io/github/lunasaw/gbproxy/test/gateway/) | ✅ 单机参考实现就绪，多节点须替换 `InviteContextStore` 为 Redis 实现 |
| `DialogRegistry`（出站 INVITE / SUBSCRIBE dialog 注册表，1.7.0+） | [`sip-common/.../DialogRegistry.java`](../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/DialogRegistry.java) | ✅ 1.7.0 就绪，Entry 含 `expiresAtMs` / `kind`；INVITE 由 `processDialogTerminated` 清理，SUBSCRIBE 由 `DialogRegistryCleaner` 定时清理 |
| `SipSender.doByeRequest(String callId)` / `doSubscribeRefresh(callId, content, expires)` | [`sip-common/.../SipSender.java`](../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/SipSender.java) | ✅ 1.7.0 dialog-aware 入口，旧 `doByeRequest(FromDevice, ToDevice)` 已删除 |


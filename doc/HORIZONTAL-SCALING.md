# SIP Proxy 水平扩容方案

## 核心约束

SIP 事务（`ServerTransaction`）是 JAIN-SIP 的内存对象，绑定在单进程内，无法跨节点共享。
**同一设备的所有 SIP 消息必须打到同一节点**，这是 SIP 协议的有状态特性决定的，框架层面无法绕过。

---

## 推荐架构

```
设备 A ──┐
设备 B ──┤                          ┌─ Node-1 (10.0.0.1:5060)
设备 C ──┼──→ VIP 1.2.3.4:5060 ──→ │
设备 D ──┤    (keepalived + ipvs)   └─ Node-2 (10.0.0.2:5060)
设备 E ──┘    源IP哈希
                                         │
                                       Redis
                                    (共享业务状态)
```

**关键点：**
- VIP 是四层透明转发，不修改 SIP 包内容
- 按**源 IP 哈希**分配节点，同一设备永远打到同一节点
- 节点故障时 keepalived 自动摘除，设备重新注册分到存活节点

---

## 状态分层

### 第一层：进程内状态（不可跨节点）

| 组件 | 说明 |
|---|---|
| `ServerTransaction` | JAIN-SIP 事务对象 |
| `SipTransactionRegistry` | 事务上下文，含 `ServerTransaction` |
| `SipTransactionContext` | ThreadLocal 事务传递 |

**处理方式：** 靠源 IP 哈希保证同一设备打同一节点，无需改造。

### 第二层：进程内但可外化（建议接 Redis）

| 组件 | 说明 |
|---|---|
| `SipSubscribe.okSubscribes/errorSubscribes` | 按 callId 存的响应回调 |
| `SubscribeHolder.catalogMap/mobilePositionMap` | 订阅状态 |

**处理方式：** 将 `ConcurrentHashMap` 替换为 Redis 实现，节点故障后新节点能接管业务状态。

### 第三层：已支持外化（接口化）

| 组件 | 说明 |
|---|---|
| `DeviceSessionCache` | 设备会话（ip/port/transport），业务方实现 |
| `DeviceSupplier` | 设备信息提供，业务方实现 |
| `ServerDeviceSupplier.authenticate(userId, SIPRequest)` | 注册鉴权，业务方实现（v1.3.0 替代旧 `ServerRegisterProcessorHandler`） |

---

## NAT 穿透

### 设备侧 NAT（设备在内网）

设备发包经过 NAT，服务器收到的源地址是 NAT 出口地址。

- **入方向（设备→服务器）**：JAIN-SIP 自动处理 `rport`/`received`，回包发到 NAT 出口，透明。
- **出方向（服务器→设备）**：`ServerRegisterRequestProcessor` 从 `Via` 头的 `received`/`rport` 取 NAT 出口地址存入 `DeviceSessionCache`，主动发消息时用此地址，可达。
- **地址保活**：设备心跳（GB28181 默认 60s）持续刷新 NAT 映射，地址始终有效。

### 服务器侧 NAT（节点在虚拟机/内网）

节点监听内网地址，但 `Via`/`Contact` 头需填对外可达地址，否则设备无法回包。

配置方式：

```yaml
sip:
  server:
    ip: 0.0.0.0          # 监听地址（内网/所有网卡）
    port: 5060           # 监听端口
    external-ip: 1.2.3.4 # 对外可达地址（VIP 或公网 IP），填入 Via/Contact
    external-port: 5060  # 对外端口（端口映射时填映射后的端口）
```

`external-ip` 不配置时 fallback 到 `ip`，不影响现有部署。

**多节点时 `external-ip` 填 VIP 地址**，设备后续消息发到 VIP，ipvs 源 IP 哈希保证还是打到同一节点。

---

## ipvs 配置示例

```bash
# 安装
apt install keepalived ipvsadm

# 添加虚拟服务（源IP哈希）
ipvsadm -A -u 1.2.3.4:5060 -s sh
ipvsadm -a -u 1.2.3.4:5060 -r 10.0.0.1:5060 -m
ipvsadm -a -u 1.2.3.4:5060 -r 10.0.0.2:5060 -m

# UDP 同样配置
ipvsadm -A -u 1.2.3.4:5060 -s sh
ipvsadm -a -u 1.2.3.4:5060 -r 10.0.0.1:5060 -m
ipvsadm -a -u 1.2.3.4:5060 -r 10.0.0.2:5060 -m
```

`-s sh` = source hash，`-m` = masquerade（NAT 模式）。

---

## NAT IP 变化的处理

设备 NAT 出口 IP 变化（重拨、网络切换）时：

1. 旧 NAT 映射失效，心跳超时
2. 设备触发重新注册，新 REGISTER 打到 VIP
3. ipvs 按新源 IP 哈希，可能分到不同节点
4. 新节点处理注册，更新 `DeviceSessionCache`
5. 进行中的会话丢失（网络切换本身导致，任何架构下相同）

---

## 扩容粒度

水平扩容的粒度是**设备**，不是单个设备的并发请求。

- 不同设备分散到不同节点，总容量随节点数线性增长
- 单个设备的所有消息始终在同一节点，事务正常
- 对 GB28181 场景足够：单设备并发请求极少，瓶颈在设备总数

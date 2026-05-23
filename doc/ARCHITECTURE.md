# SIP Proxy 架构说明

> 版本：1.2.5 | Java 17 + Spring Boot 3.3.1

## 模块依赖关系

```
gb28181-test
    ├── gb28181-client
    │       └── gb28181-common
    │               └── sip-common
    └── gb28181-server
            └── gb28181-common
                    └── sip-common
```

---

## 模块职责

### sip-common — SIP 协议基础层

纯 SIP 协议栈封装，不含 GB28181 业务逻辑。

**核心组件：**

| 类 | 职责 |
|---|---|
| `SipLayer` | JAIN-SIP 协议栈封装，管理 UDP/TCP 监听点（`SipProviderImpl`） |
| `AbstractSipListener` | 统一 SIP 事件分发，维护 method → processor 映射表 |
| `SipSender` | 静态工具类，封装所有 SIP 方法的消息构建与发送 |
| `SipRequestBuilderFactory` | 请求构建器工厂，按 method 路由到对应 `SipRequestBuilder` |
| `SipRequestStrategyFactory` | 发送策略工厂，按 method 路由到对应 `SipRequestStrategy` |
| `SipTransactionManager` | 事务管理，维护 `SipTransactionContext` |
| `SubscribeHolder` | 订阅状态持久化 |
| `CacheService` | 基于 Caffeine 的缓存服务 |
| `SipConnectionPool` / `SipPoolManager` | SIP 连接池 |
| `SipMetrics` | Micrometer 指标采集 |
| `DeviceSupplier` | 设备信息提供器接口（`getDevice`、`getToDevice`） |
| `ClientDeviceSupplier` | 客户端本地设备接口（`getClientFromDevice`） |
| `ServerDeviceSupplier` | 服务端本地设备接口（`getServerFromDevice`、`checkDevice`） |

**自动配置：** `SipProxyAutoConfig`（`spring.factories`）

---

### gb28181-common — GB28181 协议模型层

纯数据模型，无业务逻辑，基于 JAXB 做 XML 序列化。

**实体包结构：**

```
entity/
├── base/          DeviceBase
├── control/       PTZ、录像、告警、拖拽、守卫、复位等控制命令
│   └── instruction/  PTZ 指令构建、加密、序列化工具
├── notify/        告警、心跳、广播、目录更新、移动位置通知
├── query/         设备信息、目录、录像、状态、告警等查询
├── response/      各类查询的响应实体（目录、录像、设备信息等）
├── enums/         CmdTypeEnum、StreamModeEnum、TransModeEnum 等
└── utils/         GbUtil、PtzUtils、XmlUtils
```

---

### gb28181-client — 设备客户端

模拟 GB28181 设备端行为。

**消息处理流水线：**

```
SIP Message
  → AbstractSipListener
  → ClientXxxRequestProcessor      # 按 SIP method 路由
  → ClientMessageRequestProcessor  # MESSAGE 类型再按 cmdType 路由
  → XxxMessageClientHandler        # 具体业务处理
```

**主动命令发送：**

```
业务代码
  → ClientCommandSender            # 当前推荐入口
  → ClientCommandStrategyFactory   # 按 method 选择策略
  → XxxCommandStrategy             # 构建参数
  → SipSender                      # 发送
```

> `ClientSendCmd` 为旧版静态工具类，已删除，新代码使用 `ClientCommandSender`。

**请求处理器（inbound）：**

| 处理器 | 处理的 SIP Method |
|---|---|
| `ClientAckRequestProcessor` | ACK |
| `ByeRequestProcessorClient` | BYE |
| `CancelRequestProcessor` | CANCEL |
| `InfoRequestProcessor` | INFO |
| `InviteRequestProcessor` | INVITE |
| `ClientMessageRequestProcessor` | MESSAGE（再分发到下列 Handler） |
| `SubscribeRequestProcessor` | SUBSCRIBE |

**MESSAGE 子处理器（按 cmdType）：**

- 控制类：`DeviceControlMessageHandler`、`KeepaliveMessageClientHandler`
- 通知类：`BroadcastNotifyMessageHandler`
- 查询类：`AlarmQueryMessageClientHandler`、`CatalogQueryMessageClientHandler`、`DeviceInfoQueryMessageClientHandler`、`DeviceStatusQueryMessageClientHandler`、`RecordInfoQueryMessageClientHandler`、`PresetQueryMessageClientHandler`、`ConfigDownloadMessageHandler`、`ConfigDownloadQueryMessageClientHandler`、`DeviceMobileQueryMessageClientHandler`

**响应处理器（inbound）：**

`ClientRegisterResponseProcessor`、`ClientAckResponseProcessor`、`ByeResponseProcessor`、`CancelResponseProcessor`

**扩展点：**

- 实现 `ClientDeviceSupplier` 提供本地设备信息
- 实现 `DeviceSupplier` 提供目标设备信息
- 继承 `MessageClientHandlerAbstract` 自定义 MESSAGE 处理逻辑（`BaseMessageClientHandler` 为示例实现）

**自动配置：** `SipProxyClientAutoConfig`（`spring.factories`）

---

### gb28181-server — 平台服务端

提供 GB28181 平台级服务。

**消息处理流水线：**

```
SIP Message
  → AbstractSipListener
  → ServerXxxRequestProcessor      # 按 SIP method 路由
  → ServerMessageRequestProcessor  # MESSAGE 类型再按 cmdType 路由
  → XxxMessageServerHandler        # 具体业务处理
```

**主动命令发送：**

```
业务代码
  → ServerCommandSender            # 当前推荐入口
  → ServerCommandStrategyFactory   # 按 method 选择策略
  → XxxCommandStrategy             # 构建参数
  → SipSender                      # 发送
```

> `ServerSendCmd` 为旧版静态工具类，已删除，新代码使用 `ServerCommandSender`。

**请求处理器（inbound）：**

| 处理器 | 处理的 SIP Method |
|---|---|
| `ServerRegisterRequestProcessor` | REGISTER（含摘要认证 `DigestServerAuthenticationHelper`） |
| `ServerInviteRequestProcessor` | INVITE |
| `ServerInfoRequestProcessor` | INFO |
| `ByeRequestProcessorServer` | BYE |
| `ServerMessageRequestProcessor` | MESSAGE（再分发到下列 Handler） |
| `ServerNotifyRequestProcessor` | NOTIFY |

**MESSAGE 子处理器（按 cmdType）：**

- 通知类：`AlarmNotifyMessageHandler`、`KeepaliveNotifyMessageHandler`、`MediaStatusNotifyMessageHandler`
- 响应类：`DeviceInfoMessageServerHandler`、`DeviceStatusMessageServerHandler`、`DeviceConfigMessageServerHandler`、`RecordInfoMessageHandler`、`ResponseCatalogMessageHandler`

**NOTIFY 子处理器：** `CatalogNotifyHandler`

**响应处理器（inbound）：**

`ServerAckResponseProcessor`、`InviteResponseProcessor`、`SubscribeResponseProcessor`

**扩展点：**

- 实现 `ServerDeviceSupplier` 提供本地平台设备信息及设备鉴权（`checkDevice`）
- 实现 `DeviceSupplier` 提供已注册设备信息
- 继承 `BaseMessageServerHandler` 自定义 MESSAGE 处理逻辑

**自动配置：** `SipProxyServerAutoConfig`（`spring.factories`）

---

## 完整消息处理流程

### 设备注册（REGISTER）

```
设备 → REGISTER
  → ServerRegisterRequestProcessor
  → DigestServerAuthenticationHelper  # 401 摘要认证
  → DefaultServerRegisterProcessorHandler
  → ServerDeviceSupplier.checkDevice  # 业务鉴权
  → 200 OK
```

### 实时点播（INVITE）

```
平台 → ServerCommandSender.invite(InviteRequest)
     → SipSender.doInviteRequest(fromDevice, toDevice, sdp)
     → 设备

设备 → 100 Trying / 200 OK (SDP)
     → InviteResponseProcessor
     → InviteResponseProcessorHandler
     → 业务回调（流地址）

平台 → SipSender.doAckRequest
     → 设备开始推流
```

### 设备控制（MESSAGE/DeviceControl）

```
平台 → ServerCommandSender.deviceControl(ptzCmd)
     → SipSender.doMessageRequest(fromDevice, toDevice, xml)
     → 设备

设备 → 200 OK
     → MESSAGE Response（DeviceControlResponse）
     → DeviceControlMessageHandler（客户端侧）
```

---

## 配置属性

| 属性类 | 命名空间 | 模块 |
|---|---|---|
| `SipCommonProperties` | `sip` | sip-common |
| `SipClientProperties` | `sip.client` | gb28181-client |
| `SipServerProperties` | `sip.server` | gb28181-server |

---

## 关键设计决策

1. **静态工具类 vs 策略模式**：旧版 `ClientSendCmd`/`ServerSendCmd` 为静态工具类（已废弃），新版通过 `XxxCommandSender` + `XxxCommandStrategyFactory` 实现策略模式，便于扩展和测试。

2. **双层路由**：SIP method 级路由（`AbstractSipListener`）+ GB28181 cmdType 级路由（`XxxMessageRequestProcessor`），两层解耦。

3. **供应者模式**：`DeviceSupplier` / `ClientDeviceSupplier` / `ServerDeviceSupplier` 将设备信息获取与协议处理解耦，业务方只需实现接口即可接入。

4. **JaCoCo 覆盖率门禁**：行覆盖率 ≥ 80%，保证核心协议逻辑的测试质量。

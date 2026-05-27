# 出站 Dialog 维护方案（方案 C：JAIN-SIP Dialog API 落地）

> 版本：1.2 | 日期：2026-05-25 | 目标版本：**1.7.0（BREAKING CHANGES：BYE 签名变更 + SUBSCRIBE 续订/退订 API 重构，server 与 client 双侧）**
>
> 1.2 修订（vs 1.1）：
> - **SUBSCRIBE 出站方向纳入 1.7.0**（不再推迟到 1.7.x）—— 同源同病的 dialog 内消息一次性补齐
> - `DialogRegistry` 升级为承载 `Entry`（含 `expiresAtMs` / `kind`）的注册表，覆盖 SUBSCRIBE 自然超时无 `DialogTerminatedEvent` 的清理路径（§3.2.1 改写 + §7.7 新增）
> - 新增 `SipSender.doSubscribeRefresh(callId, content, expires)` 走 `dialog.createRequest(SUBSCRIBE)`，与 BYE 路径对称（§3.2.12）
> - `SubscribeRequestStrategy` 改走 `transmitStatefulPreRegister`，初始 SUBSCRIBE 同 INVITE 注册到 `DialogRegistry`（§3.2.10 + §3.2.11）
> - `ServerCommandSender` / `ClientCommandSender` 新增 `refreshSubscribe(callId, expires)` / `unsubscribe(callId)` 入口（§3.2.13）
> - `DialogRegistry.cleanupExpired` 从可选兜底转为必选清理路径，新增 `@Scheduled` 定时任务（§3.2.14）
>
> 1.1 修订（vs 1.0）：
> - 补充 client 侧 `ByeCommandStrategy` / `ClientCommandSender.sendByeCommand` 同步改造（§3.2.9 + §4.2）
> - 补充 gateway 测试代码 `SipCommandController.deviceBye` 同步改造（§4.2）
> - `SipSender.doByeRequest(FromDevice, ToDevice)` 直接删除，不留 deprecated 桥接（§3.2.5 + §10）
> - 新增 `transmitStatefulPreRegister`：先注册 dialog 再 send，规避响应竞态（§3.2.3 + §3.5）
> - `resolveSipProvider(dialog)` 改用 `DialogExt#getSipProvider()`，避免按 IP 反查 SipLayer（§3.2.5）
> - 状态分层表显式区分入站 `SipTransactionRegistry` 与出站 `DialogRegistry`（§5）
>
> 关联方案：
> - [INVITE-REFACTOR-PLAN.md](../1.3.0/INVITE-REFACTOR-PLAN.md)（v1.2 已完成入站 INVITE 异步化、入站 BYE 200 OK 修复，本方案补齐**出站方向**的 Dialog 状态维护）
> - [LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md)（§3 状态分层表新增 `DialogRegistry` 一行）
> - [HORIZONTAL-SCALING.md](../../architecture/HORIZONTAL-SCALING.md)（出站 Dialog 与现有 `SipTransactionRegistry` 同构，遵循同一节点亲和性约束）
>
> **解决问题**：服务端发出 INVITE → 设备回 200 OK → 服务端发 BYE 时，To 头**不携带 200 OK 协商出的 to-tag**，导致设备返回 `481 Call leg/Transaction does not exist`。当前 `InvitePlayFlowTest.invitePlay_thenBye_shouldEndSession` 因不断言 BYE 状态码掩盖了此 bug。

---

## 一、背景与目标

### 1.1 当前问题（详细诊断）

完整链路：

```
ServerCommandSender.deviceInvitePlay(deviceId, ...)
  → ToDevice to = sessionCache.getToDevice(deviceId)   // to.toTag = null
  → SipSender.doInviteRequest(from, to, sdp, subject)
  → SipMessageTransmitter.transmitMessage(ip, request)
  → sipProvider.sendRequest(request)                   // ⚠️ 无状态发送，不创建 ClientTransaction，不创建 Dialog
设备收到 INVITE → 200 OK（携带 to-tag = a44c7dcc）

InviteResponseProcessor.process(200 OK)
  ✅ sendAck(evt, callId)                              // 基于 ResponseEventExt 反向构造，ACK 自动带 tag
  ✅ publishEvent(ServerSessionEvent.inviteOk(deviceId, callId))
  ❌ 不回写 ToDevice.toTag
  ❌ 不向 DeviceSessionCache 存 dialog 信息
  ❌ 不向任何注册表登记出站 dialog

ServerCommandSender.deviceBye(deviceId, callId)
  → ToDevice to = sessionCache.getToDevice(deviceId)   // 复用同一个 ToDevice 实例，toTag 仍 = null
  → to.setCallId(callId)                               // 只设了 callId
  → SipSender.doByeRequest(from, to)
  → AbstractSipRequestBuilder.buildBaseRequest()       // line 144: toDevice.getToTag() = null
  → SipRequestUtils.createToHeader(userId, host, null) // ToHeader 不带 tag
  → 发出 BYE：To: <sip:34020000001320000001@127.0.0.1:5061>  // ❌ 缺 ;tag=a44c7dcc

设备按 (Call-ID, From-tag, To-tag) 三元组匹配 dialog → 找不到 → 481
```

### 1.2 为什么"修一下 ToDevice.toTag 回写"不够

最小修复（即 v0.x 设想的方案 B：在 `InviteResponseProcessor` 收到 200 OK 时把 `to-tag` 写回 `ToDevice` 或新建出站注册表）能修这一处 481，但**协议层还有一连串隐式状态**手撸都得维护：

| RFC 3261 dialog 状态 | 仅修 to-tag 的代价 | 被遗漏会怎样 |
|---|---|---|
| **Route Set**（200 OK 中的 `Record-Route` 反序后填入后续请求的 `Route`） | 自己解析 + 反序 + 注入 | 级联（双层级 GB28181 中间网关）BYE 路由错误 |
| **Remote Target**（200 OK 中的 `Contact` URI，作为后续请求的 Request-URI） | 自己解析 + 跟踪 NAT IP 切换时的 re-INVITE | NAT IP 变更后 BYE 发到旧地址 |
| **Local CSeq 自增** | 自己加锁维护 dialog 内 CSeq 计数器 | re-INVITE / INFO / UPDATE 触发 CSeq 倒序 → 设备 491 |
| **Dialog 状态机**（`null` → `Early`(1xx) → `Confirmed`(2xx) → `Terminated`） | 自己实现一份 | 在 Early 状态发 BYE → 协议错误 |
| **Same-dialog 后续请求**（INFO 暂停/恢复推流、re-INVITE 改码率、UPDATE） | 每加一个 method 都重写一遍 to-tag/route/cseq 注入 | 框架长期堆积"应用层重新发明协议栈"的代码 |

JAIN-SIP 已经实现了这一切（`gov.nist.javax.sip.stack.SIPDialog`），关键开关 `javax.sip.AUTOMATIC_DIALOG_SUPPORT` 默认为 `ON`。**框架只需要让 INVITE 走 ClientTransaction，JAIN-SIP 就会自动创建并维护 Dialog**。

### 1.3 方案 C 目标

把出站 INVITE 从**无状态发送（stateless）**升级为**带 Dialog 的有状态发送（dialog-aware）**：

- INVITE 走 `provider.getNewClientTransaction(req).sendRequest()`，JAIN-SIP 自动建 Dialog
- 注册 `Dialog` 引用到进程内 `DialogRegistry`，以 `callId` 为键
- BYE / re-INVITE / INFO / UPDATE 等 dialog 内消息一律走 `dialog.createRequest(METHOD) + dialog.sendRequest(ct)`
- ACK 走 `dialog.createAck(cseq) + dialog.sendAck(ack)`，与 BYE 路径对称
- `processDialogTerminated` 钩子自动 GC 注册表

**适用范围**：方案对**出站 INVITE / BYE / ACK / SUBSCRIBE 续订 / 退订** 全方向生效，server 与 client 两侧对称改造（GB28181 中 server 主动发 INVITE 用于点播 / 回放、SUBSCRIBE 用于目录/位置/报警/PTZ 位置订阅；client 主动发 INVITE 用于语音对讲；两侧的 BYE/SUBSCRIBE refresh 都依赖同一个 dialog）。

**非目标**：

- **不**改造 REGISTER / MESSAGE / NOTIFY 等非 dialog 请求的发送方式（保留 stateless 路径）
- **不**改造 NOTIFY 出站方向（NOTIFY 入站走 `evt.getDialog()`，本方案不涉及）
- **不**修改入站 INVITE 异步回包（已由 `SipTransactionRegistry` 解决，与本方案正交）
- **不**为 1.6.x 提供过渡期兼容 —— `deviceBye(String deviceId, String callId)` 直��改签名为 `deviceBye(String callId)`，旧 `SipSender.doByeRequest(FromDevice, ToDevice)` **直接删除**（不保留 deprecated 桥接），业务方按 CHANGELOG 一次性修复

---

## 二、现状分析

### 2.1 JAIN-SIP 调用模式三层级

| 模式 | 调用方式 | 维护状态 | 当前框架使用情况 |
|---|---|---|---|
| **Stateless** | `sipProvider.sendRequest(request)` | 无 | ⚠️ **所有出站请求**（INVITE/BYE/MESSAGE/REGISTER/...）都走这里 |
| Transactional | `provider.getNewClientTransaction(req).sendRequest()` | ClientTransaction | 未使用 |
| **Dialog-aware** | 上一行 + `provider.getNewClientTransaction(req)` 启用 dialog；后续 `dialog.sendRequest(ct)` | ClientTransaction + Dialog（自动维护 to-tag/CSeq/Route Set/Remote Target） | 未使用 |

证据：[`SipMessageTransmitter.sendUdpMessage`](../../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/SipMessageTransmitter.java#L173-L185) 直接 `sipProvider.sendRequest((Request)message)`。

### 2.2 已具备的前置条件

| 前置条件 | 现状 |
|---|---|
| `AUTOMATIC_DIALOG_SUPPORT=ON` | ✅ JAIN-SIP 默认值，无需改 [config.properties](../../../sip-common/src/main/resources/sip/config.properties) |
| `AUTOMATIC_DIALOG_ERROR_HANDLING=false` | ✅ 已设（不影响 dialog 创建，只影响 dialog 内错误的自动处理） |
| `processDialogTerminated` 钩子 | ✅ 已实现 [AbstractSipListener.java:494](../../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/AbstractSipListener.java#L494)，需补充 DialogRegistry 清理 |
| ResponseEventExt 包含 dialog 引用 | ✅ JAIN-SIP 自动填充，可通过 `evt.getDialog()` 取 |
| 入站 BYE 处理已能拿到 dialog | ✅ [ByeRequestProcessorClient.java:45](../../../gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/transmit/request/bye/ByeRequestProcessorClient.java#L45) 已用 `evt.getDialog()` |

### 2.3 与水平扩容的关系

[LAYERED-ARCHITECTURE.md §3 状态分层表](../../architecture/LAYERED-ARCHITECTURE.md#三状态分层与存储策略)：

| 状态 | 存储 |
|---|---|
| `ServerTransaction` / `SipTransactionRegistry` | 进程内（不可外化） |
| `DeviceSessionCache`（设备注册信息） | Redis（共享） |
| INVITE 入站事务上下文 | 进程内 + Redis 路由映射 |

`Dialog` 持有 `Transaction` + socket 引用，与 `ServerTransaction` 同构。**出站 dialog 的对端是设备，设备后续 BYE/200 OK 经 VIP 源 IP 哈希一定回到原节点**，无需跨节点协调。新增的 `DialogRegistry` 与 `SipTransactionRegistry` 处于同一存储层级，沿用同一约束，**不引入新的部署假设**。

---

## 三、详细设计

### 3.1 总体结构

```
┌─────────────────────────────────────────────────────────────────────┐
│ sip-common（协议层）                                                  │
│                                                                       │
│ ┌────────────────────────────────────────────────────────────────┐  │
│ │ DialogRegistry   ← 新建                                          │  │
│ │   ConcurrentMap<String callId, javax.sip.Dialog>                │  │
│ │   + register(callId, dialog)                                    │  │
│ │   + get(callId) -> Dialog                                       │  │
│ │   + remove(callId) -> Dialog                                    │  │
│ │   + size() / stats()                                            │  │
│ └────────────────────────────────────────────────────────────────┘  │
│                          ▲                       ▲                    │
│                          │ register              │ remove            │
│                          │                       │                    │
│ ┌────────────────────┴──────────────┐ ┌─────────┴──────────────┐  │
│ │ SipMessageTransmitter            │ │ AbstractSipListener      │  │
│ │   .transmitStateful(ip, req)     │ │   .processDialogTerminated│  │
│ │     ↓                             │ │     ↓                     │  │
│ │   provider.getNewClientTx(req)   │ │   DialogRegistry.remove   │  │
│ │   ct.sendRequest()               │ └──────────────────────────┘  │
│ │   return ct.getDialog()          │                                 │
│ └──────────────────────────────────┘                                 │
│                                                                       │
│ ┌────────────────────────────────────────────────────────────────┐  │
│ │ SipSender                                                        │  │
│ │   doInviteRequest(from, to, sdp, subject)                       │  │
│ │     ↓ 走 InviteRequestStrategy（stateful）                       │  │
│ │     ↓ 发出后 DialogRegistry.register(callId, ct.getDialog())     │  │
│ │                                                                  │  │
│ │   doByeRequest(callId)            ← 新签名                       │  │
│ │     ↓ Dialog dialog = DialogRegistry.get(callId)                │  │
│ │     ↓ if (dialog == null) throw DialogNotFoundException         │  │
│ │     ↓ Request bye = dialog.createRequest(BYE)                   │  │
│ │     ↓ ClientTransaction ct = provider.getNewClientTx(bye)       │  │
│ │     ↓ dialog.sendRequest(ct)                                    │  │
│ │                                                                  │  │
│ │   doByeRequest(FromDevice, ToDevice)  ← @Deprecated，标删除       │  │
│ └────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                ▲
                                │ 调用
┌──────────────────────────────┴──────────────────────────────────────┐
│ gb28181-server                                                        │
│                                                                       │
│ ServerCommandSender                                                   │
│   deviceInvitePlay(deviceId, ip, port, mode)   ← 不变（透明启用 dialog）│
│   deviceBye(callId)                            ← 新签名（去掉 deviceId）│
│                                                                       │
│ InviteResponseProcessor                                               │
│   sendAck(evt, callId)                                                │
│     ↓ 改用 dialog.sendAck(dialog.createAck(cseq))                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 关键代码

#### 3.2.1 `DialogRegistry`（新建）

`sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/DialogRegistry.java`：

```java
package io.github.lunasaw.sip.common.transmit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sip.Dialog;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 出站 Dialog 注册表（进程内）。
 *
 * <p>JAIN-SIP 在 INVITE / SUBSCRIBE 走 ClientTransaction 时自动创建 Dialog，框架持有引用以便后续
 * BYE / re-INVITE / INFO / UPDATE / SUBSCRIBE refresh / unsubscribe 等 dialog 内消息能复用 dialog
 * 状态（to-tag / CSeq / Route Set / Remote Target）。
 *
 * <p>清理路径双轨：
 * <ul>
 *   <li><b>INVITE 主路径</b>：{@code AbstractSipListener.processDialogTerminated} 收到 BYE / dialog
 *       自然终结时触发 {@link #remove(String)}</li>
 *   <li><b>SUBSCRIBE 主路径</b>：JAIN-SIP 在订阅 expires 自然超时无续订时静默终结（RFC 6665 §4.4.1
 *       case 3，dialog 状态机不变），不会发 DialogTerminatedEvent。必须依赖
 *       {@link #cleanupExpired()} 定时清理</li>
 * </ul>
 *
 * <p>水平扩容约束：与 {@link SipTransactionRegistry} 同构（持有 socket 引用，进程内不可外化）。
 * 同设备消息靠 VIP 源 IP 哈希粘到同一节点，BYE / refresh / NOTIFY 来包自然回到注册节点。
 *
 * <p>命名空间：当前以 callId 为全局唯一键。callId 由 {@link io.github.lunasaw.sip.common.utils.SipRequestUtils#getNewCallId()}
 * 生成，含主机名 + 时间戳 + 随机数，跨 client/server 形态不会冲突。如未来需显式区分，可在 key 上加
 * "client:" / "server:" 前缀。
 *
 * @author luna
 */
@Slf4j
public final class DialogRegistry {

    public static final String KIND_INVITE = "INVITE";
    public static final String KIND_SUBSCRIBE = "SUBSCRIBE";

    /** Long.MAX_VALUE 表示无业务超时（INVITE，靠 DialogTerminatedEvent 清理） */
    public static final long NO_EXPIRY = Long.MAX_VALUE;

    private static final ConcurrentMap<String, Entry> BY_CALL_ID = new ConcurrentHashMap<>();

    private DialogRegistry() {}

    /**
     * Dialog 注册项。INVITE 与 SUBSCRIBE 共用同一注册表，但通过 kind 区分清理策略。
     */
    @Getter
    @AllArgsConstructor
    public static final class Entry {
        private final Dialog dialog;
        /** 业务侧期望的过期时刻（ms）。INVITE = NO_EXPIRY；SUBSCRIBE = now + (expires + grace)*1000 */
        private final long expiresAtMs;
        /** "INVITE" or "SUBSCRIBE" */
        private final String kind;
        private final long createTimeMs;
    }

    /**
     * 注册 INVITE dialog（业务侧无 expires 概念，靠 DialogTerminatedEvent 清理）。
     */
    public static void register(String callId, Dialog dialog) {
        register(callId, dialog, NO_EXPIRY, KIND_INVITE);
    }

    /**
     * 注册 dialog。同 callId 重复注册会覆盖，记 warn 日志便于排查重复 INVITE 场景。
     *
     * @param callId       Call-ID
     * @param dialog       JAIN-SIP Dialog
     * @param expiresAtMs  业务侧期望过期时刻（ms）；INVITE 用 {@link #NO_EXPIRY}
     * @param kind         {@link #KIND_INVITE} 或 {@link #KIND_SUBSCRIBE}
     */
    public static void register(String callId, Dialog dialog, long expiresAtMs, String kind) {
        if (callId == null || dialog == null) {
            return;
        }
        Entry entry = new Entry(dialog, expiresAtMs, kind, System.currentTimeMillis());
        Entry prev = BY_CALL_ID.put(callId, entry);
        if (prev != null && prev.dialog != dialog) {
            log.warn("Dialog 覆盖注册: callId={}, prevState={}, prevKind={}, newKind={}",
                    callId, prev.dialog.getState(), prev.kind, kind);
        }
    }

    /**
     * 获取 dialog，可能为 null（callId 未建过 dialog 或已 terminate）。
     */
    public static Dialog get(String callId) {
        if (callId == null) {
            return null;
        }
        Entry entry = BY_CALL_ID.get(callId);
        return entry == null ? null : entry.dialog;
    }

    /**
     * 获取注册项（含 expires / kind 元数据），可能为 null。
     */
    public static Entry getEntry(String callId) {
        return callId == null ? null : BY_CALL_ID.get(callId);
    }

    /**
     * 移除 dialog（一般由 processDialogTerminated 钩子或 cleanupExpired 调用）。
     */
    public static Dialog remove(String callId) {
        if (callId == null) {
            return null;
        }
        Entry removed = BY_CALL_ID.remove(callId);
        return removed == null ? null : removed.dialog;
    }

    public static int size() {
        return BY_CALL_ID.size();
    }

    /**
     * 按 kind 统计，便于监控（如 SUBSCRIBE 占比异常告警）。
     */
    public static int sizeByKind(String kind) {
        if (kind == null) {
            return 0;
        }
        int count = 0;
        for (Entry e : BY_CALL_ID.values()) {
            if (kind.equals(e.kind)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 清理已过期 entry —— SUBSCRIBE 主清理路径。
     *
     * <p>JAIN-SIP 在 SUBSCRIBE 订阅 expires 自然超时（无续订）时不会触发 DialogTerminatedEvent，
     * 必须由本方法定时清理，否则 DialogRegistry 会持续增长。INVITE 类型用 NO_EXPIRY 标记，本方法不清理。
     *
     * @return 清理掉的 entry 数
     */
    public static int cleanupExpired() {
        long now = System.currentTimeMillis();
        int cleaned = 0;
        Iterator<Map.Entry<String, Entry>> it = BY_CALL_ID.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            Entry entry = e.getValue();
            if (entry.expiresAtMs != NO_EXPIRY && now > entry.expiresAtMs) {
                it.remove();
                cleaned++;
                log.debug("DialogRegistry.cleanupExpired: callId={}, kind={}, expiredAt={}",
                        e.getKey(), entry.kind, entry.expiresAtMs);
            }
        }
        return cleaned;
    }

    /**
     * 仅供测试调用 —— 清空整个注册表。
     */
    static void clearForTest() {
        BY_CALL_ID.clear();
    }
}
```

#### 3.2.2 `DialogNotFoundException`（新建）

`sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/DialogNotFoundException.java`：

```java
package io.github.lunasaw.sip.common.transmit;

/**
 * dialog 内消息（BYE / re-INVITE / INFO / UPDATE）找不到对应 dialog 时抛出。
 *
 * <p>常见原因：
 * <ul>
 *   <li>INVITE 还未收到 200 OK，dialog 处于 Early 状态被取消</li>
 *   <li>对端先发 BYE 终结了 dialog，本���再发 BYE</li>
 *   <li>callId 输入错误或已过期</li>
 * </ul>
 *
 * <p>区别于 {@code SipException}：这是**业务层调用错误**，不是网络/协议错误，
 * 应快速抛出，不做兜底（避免发出协议非法的 BYE 让设备返回 481，被掩盖成"已成功"）。
 *
 * @author luna
 */
public class DialogNotFoundException extends RuntimeException {
    public DialogNotFoundException(String message) {
        super(message);
    }
}
```

#### 3.2.3 `SipMessageTransmitter` 新增 stateful 通道

```java
/**
 * 有状态发送：走 ClientTransaction，JAIN-SIP 在 INVITE/SUBSCRIBE 等会自动创建 Dialog。
 * 调用方负责在发送后处理返回的 Dialog（如注册到 DialogRegistry）。
 *
 * <p>仅供需要后续 dialog 内消息的方法使用（INVITE / SUBSCRIBE）。REGISTER / MESSAGE /
 * NOTIFY 等无 dialog 语义的请求继续用 {@link #transmitMessage} 无状态发送，避免 Timer F
 * 重传与现有重试逻辑冲突。
 */
public static javax.sip.Dialog transmitStateful(String ip, Request request) {
    preprocessMessage(request);
    String transport = getTransport(request);
    try {
        SipProviderImpl provider = Constant.TCP.equalsIgnoreCase(transport)
                ? SipLayer.getTcpSipProvider(ip)
                : SipLayer.getUdpSipProvider(ip);
        if (provider == null) {
            log.error("[发送信息失败] 未找到 {}://{} 的监听信息", transport, ip);
            return null;
        }
        ClientTransaction ct = provider.getNewClientTransaction(request);
        ct.sendRequest();
        return ct.getDialog();   // INVITE 时非 null（Null 状态，收到 1xx 切 Early，2xx 切 Confirmed）
    } catch (TransactionUnavailableException | SipException e) {
        log.error("有状态发送 SIP 消息失败", e);
        throw new RuntimeException("有状态发送 SIP 消息失败", e);
    }
}

/**
 * 有状态发送（先注册 dialog 再发送，消除响应竞态）。
 *
 * <p>JAIN-SIP `getNewClientTransaction(req)` 已经创建 Dialog 引用，不必等到 sendRequest
 * 之后再注册。先注册后发送可避免 200 OK 在本地 register 之前到达 ResponseProcessor 的
 * 罕见但可能��窗口（同机回环 / 极低延迟链路）。
 *
 * @param ip 目标 IP
 * @param request 请求
 * @param callId 提前透传给 register 回调，避免再从 request 头里取
 * @param register 注册回调，由调用方决定注册到哪个 registry（一般是 DialogRegistry::register）
 * @return 已注册的 Dialog（可能为 null，如 provider 缺失）
 */
public static javax.sip.Dialog transmitStatefulPreRegister(
        String ip, Request request, String callId,
        java.util.function.BiConsumer<String, javax.sip.Dialog> register) {
    preprocessMessage(request);
    String transport = getTransport(request);
    try {
        SipProviderImpl provider = Constant.TCP.equalsIgnoreCase(transport)
                ? SipLayer.getTcpSipProvider(ip)
                : SipLayer.getUdpSipProvider(ip);
        if (provider == null) {
            log.error("[发送信息失败] 未找到 {}://{} 的监听信息", transport, ip);
            return null;
        }
        ClientTransaction ct = provider.getNewClientTransaction(request);
        Dialog dialog = ct.getDialog();
        if (dialog != null && register != null) {
            register.accept(callId, dialog);   // 先注册，再发送
        }
        ct.sendRequest();
        return dialog;
    } catch (TransactionUnavailableException | SipException e) {
        log.error("有状态发送 SIP 消息失败", e);
        throw new RuntimeException("有状态发送 SIP 消息失败", e);
    }
}
```

#### 3.2.4 `InviteRequestStrategy` 改走 stateful + 注册 dialog

```java
@Slf4j
public class InviteRequestStrategy extends AbstractSipRequestStrategy {

    @Override
    protected Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SipRequestBuilderFactory.createInviteRequest(fromDevice, toDevice, content, null, callId);
    }

    @Override
    public String sendRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId,
                              Event errorEvent, Event okEvent) {
        if (StringUtils.isBlank(callId)) {
            callId = SipRequestUtils.getNewCallId();
        }
        Request request = buildRequest(fromDevice, toDevice, content, callId);
        // 订阅事件（callId 维度，与原逻辑一致）
        SipMessageTransmitter.setupEventSubscriptions(request, errorEvent, okEvent);
        // 有状态发送：先创建 ClientTransaction → 取出 dialog 注册 → 再 sendRequest
        // 顺序原因：避免 200 OK 响应早于本地 register 到达造成的 zombie 项（详见 §3.5）
        Dialog dialog = SipMessageTransmitter.transmitStatefulPreRegister(
                fromDevice.getIp(), request, callId, DialogRegistry::register);
        if (dialog != null) {
            log.debug("INVITE 已注册 dialog: callId={}, dialogId={}", callId, dialog.getDialogId());
        } else {
            log.warn("INVITE 发送后未获得 dialog: callId={}", callId);
        }
        return callId;
    }

    // sendRequestWithSubject / sendRequestWithSubscribe 同理改造
}
```

> **注意**：`SipMessageTransmitter.setupEventSubscriptions` 当前是 private，需要提取为 package-private 或 public 静态方法，让 strategy 在调 stateful 前能注册 callId 维度的成功/失败回调。

#### 3.2.5 `SipSender.doByeRequest(String callId)` 新签名（旧签名直接删除）

```java
/**
 * 发送 BYE 请求（dialog-aware 路径）。
 *
 * <p>必须基于已 confirmed 的 dialog，自动携带 to-tag / Route Set / 正确 CSeq。
 * 不再接受 FromDevice / ToDevice 参数 —— dialog 已包含全部信息。
 *
 * @param callId INVITE 200 OK 的 Call-ID
 * @return callId
 * @throws DialogNotFoundException 当 callId 找不到对应 dialog 时
 * @throws IllegalStateException 当 dialog 不在 CONFIRMED 状态时
 */
public static String doByeRequest(String callId) {
    Dialog dialog = DialogRegistry.get(callId);
    if (dialog == null) {
        throw new DialogNotFoundException("no dialog for callId=" + callId
                + " — INVITE 200 OK 未建立 dialog 或已 terminate");
    }
    if (dialog.getState() != DialogState.CONFIRMED) {
        throw new IllegalStateException("dialog not confirmed: callId=" + callId
                + ", state=" + dialog.getState() + " — 早 dialog 阶段不应发 BYE，应发 CANCEL");
    }
    try {
        Request bye = dialog.createRequest(Request.BYE);
        // 走 dialog 自带的 SipProvider，避免按 IP 反查 SipLayer
        SipProviderImpl provider = (SipProviderImpl) ((DialogExt) dialog).getSipProvider();
        ClientTransaction ct = provider.getNewClientTransaction(bye);
        dialog.sendRequest(ct);
        return callId;
    } catch (SipException e) {
        throw new RuntimeException("发送 BYE 失败: callId=" + callId, e);
    }
}
```

> **旧签名直接删除**，不保留 deprecated 桥接：
>
> - `SipSender.doByeRequest(FromDevice, ToDevice)` —— **删除**
> - `SipSender.doByeRequest(...)` 系列若有其它 FromDevice/ToDevice 重载 —— **一并删除**
>
> 删除而不留 `@Deprecated(forRemoval=true) + UnsupportedOperationException` 的理由：
>
> 1. 1.7.0 是显式 BREAKING，本就要让接入方编译失败；保留 deprecated 桥接反而让 IDE 把所有调用点标黄而不是报错，接入方更容易遗漏；
> 2. 方法体只是抛运行时异常，对协议正确性没有任何贡献，留下纯属语义噪音；
> 3. CHANGELOG.md 与 §八 BREAKING 表已经覆盖迁移路径，编译失败本身是最直接的"迁移指引"。

#### 3.2.6 `AbstractSipListener.processDialogTerminated` 清理钩子

```java
@Override
public void processDialogTerminated(DialogTerminatedEvent evt) {
    Dialog dialog = evt.getDialog();
    if (dialog != null) {
        String callId = dialog.getCallId() != null ? dialog.getCallId().getCallId() : null;
        if (callId != null) {
            Dialog removed = DialogRegistry.remove(callId);
            if (removed != null) {
                log.debug("DialogTerminatedEvent 清理 DialogRegistry: callId={}", callId);
            }
        }
    }

    EventResult eventResult = new EventResult(evt);
    Event timeOutSubscribe = SipSubscribe.getErrorSubscribe(eventResult.getCallId());
    if (timeOutSubscribe != null) {
        timeOutSubscribe.response(eventResult);
    }
}
```

#### 3.2.7 `InviteResponseProcessor.sendAck` 改用 Dialog API

```java
private void sendAck(ResponseEventExt evt, String callId) {
    try {
        Dialog dialog = evt.getDialog();
        if (dialog == null) {
            log.warn("INVITE 200 OK 不带 dialog，跳过 ACK：callId={}", callId);
            return;
        }
        long cseq = ((CSeqHeader) evt.getResponse().getHeader(CSeqHeader.NAME)).getSeqNumber();
        Request ack = dialog.createAck(cseq);
        // SDP 已在 INVITE 请求里发过，ACK 通常 body 为空；如需要可在此 setContent
        dialog.sendAck(ack);
        log.info("发送 ACK 响应（dialog-aware）：callId={}, dialogState={}", callId, dialog.getState());
    } catch (Exception e) {
        log.error("ACK 处理异常：callId={}", callId, e);
    }
}
```

#### 3.2.8 `ServerCommandSender.deviceBye` 新签名

```java
/**
 * 发送 BYE 终结点播会话。
 *
 * <p>1.7.0 起 BYE 不再接受 deviceId —— 信息全部从 dialog 取回。
 *
 * @param callId INVITE 阶段记录的 Call-ID
 */
public String deviceBye(String callId) {
    return factory.getStrategy("server", "BYE")
            .execute(CommandContext.forBye(callId));
}
```

`ByeCommandStrategy` 同步改：

```java
@Component("serverByeCommandStrategy")
public class ByeCommandStrategy extends AbstractServerCommandStrategy {

    @Override
    public String getCommandType() { return "BYE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doByeRequest(ctx.getCallId());
    }
}
```

`CommandContext.forBye(callId)` 是新增的便捷构造方法，内部只设置 `callId`，不再要求 `fromDevice` / `toDevice`。

#### 3.2.9 Client 侧对称改造

GB28181 中 client（设备）也存在主动发出 INVITE / BYE 的场景：
- **语音对讲（双向 INVITE）**：[Q.5 推流端](../../protocol/2016/GB28181-2016.md) 协议 §10.2 允许 device 作为 INVITE-ing UA。
- **设备主动结束会话**：device 在故障 / 资源回收时主动发 BYE 终结媒体流。

[gb28181-client/.../ByeCommandStrategy.java](../../../gb28181-client/src/main/java/io/github/lunasaw/gbproxy/client/transmit/cmd/strategy/impl/ByeCommandStrategy.java) 当前同样调用 `SipSender.doByeRequest(ctx.getFromDevice(), ctx.getToDevice())`，与 server 同病同源。删除旧签名后此处会编译失败，必须同步改造：

```java
@Component("clientByeCommandStrategy")
public class ByeCommandStrategy extends AbstractClientCommandStrategy {

    @Override
    public String getCommandType() { return "BYE"; }

    @Override
    protected String doSend(CommandContext ctx) {
        return SipSender.doByeRequest(ctx.getCallId());
    }
}
```

`ClientCommandSender.sendByeCommand(FromDevice, ToDevice)` 也改为 `sendByeCommand(String callId)`：

```java
public static String sendByeCommand(String callId) {
    Assert.notNull(INSTANCE, "ClientCommandSender 尚未初始化，请确保 Spring 容器已启动");
    return INSTANCE.send(CommandContext.forBye("client", callId));
}
```

`CommandContext.forBye(role, callId)` 接受 role 区分 client / server，内部只填充 role + callId + commandType=BYE，不再要求 device 信息。

**关于 client 侧 INVITE 是否需要 stateful**：

由于 INVITE 走的是同一个 `InviteRequestStrategy`（[SipRequestStrategyFactory.java:23](../../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/strategy/SipRequestStrategyFactory.java#L23) `STRATEGY_MAP.put("INVITE", new InviteRequestStrategy())` 是单实例 strategy），**client 侧 INVITE 自动同步启用 stateful + dialog 注册**，无需单独改造，client BYE 自然能查到 dialog。这是 strategy 层面共享的天然好处。SUBSCRIBE 同理。

#### 3.2.10 `SubscribeRequestStrategy` 改走 stateful + 注册 dialog

```java
@Slf4j
public class SubscribeRequestStrategy extends AbstractSipRequestStrategy {

    /** SUBSCRIBE 自然过期后给定的清理宽限期（秒），覆盖 NOTIFY: terminated 延迟到达 */
    private static final int CLEANUP_GRACE_SECONDS = 60;

    @Override
    protected Request buildRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return SipRequestBuilderFactory.createSubscribeRequest(fromDevice, toDevice, content, null, callId);
    }

    @Override
    protected Request buildRequestWithSubscribe(FromDevice fromDevice, ToDevice toDevice, String content,
                                                 SubscribeInfo info, String callId) {
        return SipRequestBuilderFactory.createSubscribeRequest(fromDevice, toDevice, content, info, callId);
    }

    @Override
    public String sendRequestWithSubscribe(FromDevice from, ToDevice to, String content,
                                            SubscribeInfo info, String callId,
                                            Event errorEvent, Event okEvent) {
        if (StringUtils.isBlank(callId)) {
            callId = SipRequestUtils.getNewCallId();
        }
        Request request = buildRequestWithSubscribe(from, to, content, info, callId);
        SipMessageTransmitter.setupEventSubscriptions(request, errorEvent, okEvent);

        // expires=0 是退订（终止订阅），仍走 stateful 注册一个短期 entry，等 NOTIFY: terminated 后清理
        // expires>0 是初始订阅或续订，按 expires + grace 设过期时刻
        int expires = info != null ? info.getExpires() : 0;
        long expiresAtMs = expires > 0
                ? System.currentTimeMillis() + (expires + CLEANUP_GRACE_SECONDS) * 1000L
                : System.currentTimeMillis() + CLEANUP_GRACE_SECONDS * 1000L;

        final String finalCallId = callId;
        Dialog dialog = SipMessageTransmitter.transmitStatefulPreRegister(
                from.getIp(), request, callId,
                (cid, dlg) -> DialogRegistry.register(cid, dlg, expiresAtMs, DialogRegistry.KIND_SUBSCRIBE));
        if (dialog != null) {
            log.debug("SUBSCRIBE 已注册 dialog: callId={}, expires={}s", finalCallId, expires);
        } else {
            log.warn("SUBSCRIBE 发送后未获得 dialog: callId={}", finalCallId);
        }
        return callId;
    }

    @Override
    public String sendRequest(FromDevice from, ToDevice to, String content,
                              String callId, Event errorEvent, Event okEvent) {
        // 无 SubscribeInfo 的 SUBSCRIBE 退化为 stateless（兼容旧调用），不进 DialogRegistry
        return super.sendRequest(from, to, content, callId, errorEvent, okEvent);
    }
}
```

> 关键点：初始 SUBSCRIBE 200 OK 协商出 to-tag 后，所有 dialog 内续订 / 退订都必须基于这个 dialog；
> 后续 NOTIFY 由设备端发起到平台，框架走入站 NOTIFY 处理路径用 `evt.getDialog()` 自动取回，
> 不需要 DialogRegistry 反查（设计上与 INVITE 入站 BYE 处理同模式）。

#### 3.2.11 `SubscribeRequestStrategy` 与外层 `subscribeInfo == null` 的兼容性

[`SipSender.SipRequestBuilder.send()`](../../../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/SipSender.java#L325) 当前在 `subscribeInfo != null` 时走 `sendRequestWithSubscribe`，否则走 `sendRequest`。GB28181 业务侧四个订阅入口（catalog / mobile / alarm / ptz）都明确传 `SubscribeInfo`，因此**实战路径都会走 stateful**。`sendRequest`（兼容性 fallback，无 SubscribeInfo）保留 stateless 行为，避免破坏当前框架内极少数无 SubscribeInfo 的 SUBSCRIBE 调用（grep 结果显示业务侧均带 SubscribeInfo）。

#### 3.2.12 `SipSender.doSubscribeRefresh(callId, content, expires)` 新增

```java
/**
 * 发送 SUBSCRIBE 续订 / 退订（dialog-aware 路径）。
 *
 * <p>必须基于已 confirmed 的 SUBSCRIBE dialog（初始 SUBSCRIBE 200 OK 后注册到 DialogRegistry）。
 *
 * @param callId   初始 SUBSCRIBE 的 Call-ID
 * @param content  body（XML），通常与初始 SUBSCRIBE 相同
 * @param expires  续订时长（秒）；0 表示退订
 * @return callId
 * @throws DialogNotFoundException 当 callId 找不到对应 dialog 时
 * @throws IllegalStateException 当 dialog 不在 CONFIRMED 状态时
 */
public static String doSubscribeRefresh(String callId, String content, int expires) {
    Dialog dialog = DialogRegistry.get(callId);
    if (dialog == null) {
        throw new DialogNotFoundException("no SUBSCRIBE dialog for callId=" + callId
                + " — 初始 SUBSCRIBE 200 OK 未建立 dialog 或已自然过期");
    }
    if (dialog.getState() != DialogState.CONFIRMED) {
        throw new IllegalStateException("SUBSCRIBE dialog not confirmed: callId=" + callId
                + ", state=" + dialog.getState());
    }
    try {
        Request req = dialog.createRequest(Request.SUBSCRIBE);
        // expires header
        ExpiresHeader expHeader = SipFactory.getInstance().createHeaderFactory().createExpiresHeader(expires);
        req.removeHeader(ExpiresHeader.NAME);
        req.addHeader(expHeader);
        // event header（必须与初始 SUBSCRIBE 一致）—— 由 dialog 已保存的事件信息处理，调用方按需 setHeader
        if (StringUtils.isNotBlank(content)) {
            ContentTypeHeader ct = SipFactory.getInstance().createHeaderFactory()
                    .createContentTypeHeader("Application", "MANSCDP+xml");
            req.setContent(content, ct);
        }
        SipProviderImpl provider = (SipProviderImpl) ((DialogExt) dialog).getSipProvider();
        ClientTransaction ct = provider.getNewClientTransaction(req);
        dialog.sendRequest(ct);
        // 若是退订（expires=0），等设备发回 NOTIFY: Subscription-State: terminated 后由
        // processDialogTerminated 清理；为兜底，这里把 entry 的 expiresAt 重置为 grace 期内
        if (expires == 0) {
            DialogRegistry.Entry entry = DialogRegistry.getEntry(callId);
            if (entry != null) {
                DialogRegistry.register(callId, dialog,
                        System.currentTimeMillis() + 60_000L, DialogRegistry.KIND_SUBSCRIBE);
            }
        }
        return callId;
    } catch (SipException | ParseException e) {
        throw new RuntimeException("发送 SUBSCRIBE 失败: callId=" + callId, e);
    }
}
```

#### 3.2.13 `ServerCommandSender` / `ClientCommandSender` 新增 refresh / unsubscribe 入口

```java
// ServerCommandSender
public String refreshSubscribe(String callId, int expires) {
    return SipSender.doSubscribeRefresh(callId, null, expires);
}

public String refreshSubscribe(String callId, String content, int expires) {
    return SipSender.doSubscribeRefresh(callId, content, expires);
}

public String unsubscribe(String callId) {
    return SipSender.doSubscribeRefresh(callId, null, 0);
}
```

ClientCommandSender 同样镜像三个静态方法（命名一致）。

#### 3.2.14 `@Scheduled` 定时清理 SUBSCRIBE dialogs

`gb28181-common` / `sip-common` 内新增简单的定时任务（依赖 Spring Boot `@EnableScheduling`，框架已具备）：

```java
@Component
@Slf4j
public class DialogRegistryCleaner {

    /**
     * 每 60s 跑一次，清理 SUBSCRIBE 自然过期的 entry。INVITE 类型用 NO_EXPIRY 标记，
     * 此方法不会动它们 —— INVITE 由 processDialogTerminated 主路径清理。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void run() {
        int cleaned = DialogRegistry.cleanupExpired();
        if (cleaned > 0) {
            log.info("DialogRegistryCleaner: cleaned {} expired entries, current size={}, subscribeSize={}",
                    cleaned, DialogRegistry.size(), DialogRegistry.sizeByKind(DialogRegistry.KIND_SUBSCRIBE));
        }
    }
}
```

>  Cleaner 放在 `sip-common`（协议层），由 `Gb28181CommonAutoConfig` 装配。业务方若不希望框架自动跑清理，可关闭 `@EnableScheduling` 或在自己的 `@Configuration` 中 `@ConditionalOnMissingBean` 替换。

### 3.3 INVITE 重传幂等

JAIN-SIP `provider.getNewClientTransaction(request)` 对**同一 SIPRequest 实例**重复调用会抛 `TransactionUnavailableException`。当前 INVITE 单次发送，不存在此问题。但若业务方因超时手动触发"再发一次 INVITE"（同 callId 同 cseq），需要重新构造 `Request` 实例（新 branch），框架的 `SipRequestBuilderFactory.createInviteRequest` 内部生成新 `viaTag`，天然满足。

DialogRegistry 按 callId 覆盖注册，旧 dialog 引用被替换，由 JAIN-SIP `processDialogTerminated` 自然清理。日志记 warn 提醒运维侧。

### 3.4 ACK 路径选择

ACK 有两种构造方式：

| 方式 | 调用 | 适用场景 |
|---|---|---|
| Dialog API | `dialog.createAck(cseq) + dialog.sendAck(ack)` | dialog 已建立（200 OK 已收到） |
| 手动构造 | 当前 `SipSender.doAckRequest(from, sipURI, response)`（基于 SipURI） | 早期阶段 / 临时响应 ACK（已废弃路径） |

方案 C 把 INVITE 200 OK 的 ACK 切到 Dialog API，**保留 `SipSender.doAckRequest(from, sipURI, response)`** 用于不基于 dialog 的特殊场景（例如收到 200 OK 时本地 dialog 已被删除的边界情况）。

### 3.5 register-after-send 竞态分析

§3.2.4 `transmitStatefulPreRegister` 之所以把 register 放在 sendRequest 之前，覆盖以下边界条件：

| 场景 | sendRequest 后注册 | sendRequest 前注册 |
|---|---|---|
| 同机回环（127.0.0.1）/ 设备同节点 | 200 OK 可能在毫秒级回到 listener，register 还未执行 → DialogRegistry.get 返回 null，但 ResponseProcessor 走 `evt.getDialog()` 不受影响（ACK 仍能发） | 安全 |
| `processDialogTerminated` 在 register 之前触发（极端） | DialogRegistry.remove 拿不到 entry，留下后续到达的 register 写入 → zombie 项 | 安全（terminate 时 entry 已存在） |
| 正常 RTT >10ms | 无问题 | 无问题 |

提前注册零成本（`getNewClientTransaction` 之后 `getDialog()` 立即可用），收益是覆盖罕见但真实存在的窗口。是否启用 prefix 策略 → **采纳**。

---

## 四、文件变更清单

### 4.1 新增

| 文件 | 模块 | 说明 |
|---|---|---|
| `transmit/DialogRegistry.java` | sip-common | 出站 dialog 注册表（Entry 含 expiresAtMs / kind） |
| `transmit/DialogNotFoundException.java` | sip-common | dialog 找不到时的明确异常 |
| `transmit/DialogRegistryCleaner.java` | sip-common | `@Scheduled` 清理 SUBSCRIBE 过期 entry |

### 4.2 修改

| 文件 | 改动 |
|---|---|
| `sip-common/transmit/SipMessageTransmitter.java` | 新增 `transmitStateful(ip, request)` 与 `transmitStatefulPreRegister(ip, request, callId, register)` 方法；`setupEventSubscriptions` 改 package-private 供 strategy 调用 |
| `sip-common/transmit/strategy/impl/InviteRequestStrategy.java` | 改走 `transmitStatefulPreRegister`（先 register 再 send，规避响应竞态）；`sendRequestWithSubject` 同理改造 |
| `sip-common/transmit/strategy/impl/SubscribeRequestStrategy.java` | `sendRequestWithSubscribe` 改 stateful + register（kind=SUBSCRIBE，expiresAt=now+(expires+grace)\*1000） |
| `sip-common/transmit/SipSender.java` | 新增 `doByeRequest(String callId)`、`doSubscribeRefresh(String callId, String content, int expires)`；**删除** `doByeRequest(FromDevice, ToDevice)` 旧签名（不留 deprecated 桥接） |
| `sip-common/transmit/AbstractSipListener.java` | `processDialogTerminated` 内调 `DialogRegistry.remove` |
| `sip-common/transmit/AsyncSipListener.java` | `processDialogTerminated` 调用 super 前确保 DialogRegistry 清理在异步路径同样生效（与 `AbstractSipListener.processDialogTerminated` 复用同一钩子） |
| `gb28181-server/.../InviteResponseProcessor.java` | `sendAck` 改用 `dialog.sendAck(dialog.createAck(cseq))` |
| `gb28181-server/.../ServerCommandSender.java` | `deviceBye(String, String)` 改为 `deviceBye(String callId)`；新增 `refreshSubscribe(callId, expires)` / `refreshSubscribe(callId, content, expires)` / `unsubscribe(callId)` |
| `gb28181-server/.../strategy/impl/ByeCommandStrategy.java` | 调 `SipSender.doByeRequest(ctx.getCallId())` |
| `gb28181-client/.../strategy/impl/ByeCommandStrategy.java` | **同步改造** —— 同样调 `SipSender.doByeRequest(ctx.getCallId())`，否则编译失败 |
| `gb28181-client/.../ClientCommandSender.java` | `sendByeCommand(FromDevice, ToDevice)` 改为 `sendByeCommand(String callId)`；新增 `refreshSubscribe(callId, expires)` / `unsubscribe(callId)` |
| `gb28181-common/.../CommandContext.java` | 新增 `forBye(String role, String callId)` 静态构造方法（供 client/server 共��，role 取 `"client"` / `"server"`） |
| `gb28181-test/.../InvitePlayFlowTest.java` | `commandSender.deviceBye(callId)`；新增 BYE 200 OK 断言（监听 `ClientByeEvent`） |
| `gb28181-test/.../AlarmSubscribeFlowTest.java`（如需要） / 新增 `SubscribeRefreshFlowTest.java` | 验证初始 SUBSCRIBE → refresh → unsubscribe → DialogRegistry 清空 |
| `gb28181-test/.../gateway/SipCommandController.java` | **同步改造** —— `commandSender.deviceBye(req.getDeviceId(), req.getCallId())` 改为 `commandSender.deviceBye(req.getCallId())`，否则编译失败 |
| `gb28181-test/.../handler/TestServerEventHandler.java`（如需要） | 新增 `getLastByeStatus()` 暴露 BYE 响应码供测试断言 |
| `doc/architecture/LAYERED-ARCHITECTURE.md` | §3 状态分层表加 `DialogRegistry` 行（标注"出站"以与 `SipTransactionRegistry`/"入站"区分）；§九 实现状态表加 `DialogRegistry` ✅ 行 |
| `doc/plans/1.3.0/INVITE-REFACTOR-PLAN.md` | 顶部 v1.2 → v1.3 变更说明追加"出站 BYE Dialog 化 + SUBSCRIBE 续订/退订 dialog 化" |
| `CHANGELOG.md` | 1.7.0 BREAKING：`deviceBye` 签名 + `SipSender.doByeRequest` 签名 + `ClientCommandSender.sendByeCommand` 签名 + 新增 SUBSCRIBE refresh / unsubscribe API |

### 4.3 不变

- `BYE` 入站处理（`ByeRequestProcessorClient` / `ByeRequestProcessorServer`）—— 已用 `evt.getDialog()`，无需改
- `ACK` 出站非 dialog 路径 —— `SipSender.doAckRequest(from, sipURI, response)` 保留，覆盖 dialog 已删除的边界场景
- NOTIFY 入站处理 —— 已通过 `evt.getDialog()` 自动取回 dialog 上下文，与本方案正交
- REGISTER / MESSAGE / NOTIFY 出站等非本期范围的请求 —— 保留无状态发送
- `SipTransactionRegistry`（入站异步回包用）—— 与本方案正交，名称相似但管的是入站 INVITE 上下文，不要混淆

---

## 五、与分层架构的契合度审查

按 [LAYERED-ARCHITECTURE.md](../../architecture/LAYERED-ARCHITECTURE.md) 逐节比对：

| 分层契约 | 方案 C 是否合规 | 说明 |
|---|---|---|
| §2.1 sip-proxy 只做协议层 | ✅ | `DialogRegistry` 是纯协议层组件，不感知业务；Dialog 状态机由 JAIN-SIP 维护，框架只持有引用 |
| §2.2 sip-gateway 与 sip-proxy 同 JVM 是硬约束 | ✅ 加强 | `Dialog` 同 `ServerTransaction` 一样不可序列化、持有 socket，与现有约束同构 |
| §3 状态分层表 | ✅ 加一行 | `DialogRegistry` 进程内存储，与 `SipTransactionRegistry` 同性质 |
| §4.2 实时点播流程 | ✅ | INVITE/BYE 调用路径不变（`commandSender.deviceInvitePlay` / `deviceBye`），底层透明启用 dialog |
| §5.1 VIP 源 IP 哈希 | ✅ | 出站 dialog 对端是设备，BYE/200 OK 经 VIP 回到原节点，零跨节点协调 |
| §5.3 INVITE 跨节点路由 | ✅ 不影响 | §5.3 针对**入站** INVITE 跨节点回包；**出站** INVITE 由发起节点持有 dialog，响应自然回到该节点 |
| §7 INVITE 超时 | ✅ 正交 | `extendContext` 解决入站 INVITE 异步回包；出站 dialog 由 JAIN-SIP 按 RFC 3261 §12.3 自然终结，互不干扰 |
| §九 实现状态表 | ✅ 加一行 | `DialogRegistry` ✅ 就绪 |

新增的状态层级：

| 状态类型 | 方向 | 存储位置 | 说明 |
|---|---|---|---|
| **`SipTransactionRegistry`（入站 INVITE 异步回包上下文）** | 入站 | 进程内 + Redis 路由映射 | 处理设备发来的 INVITE，跨节点路由 100/200/180 响应 |
| **`DialogRegistry`（出站 dialog 引用）** | **出站** | **进程内**（不可外化） | JAIN-SIP `Dialog` 持有 socket / transaction 引用，与 `SipTransactionRegistry` 同构。同设备消息靠 VIP 源 IP 哈希粘到同一节点，自然不需要外化 |

**两者键都用 `callId`，但作用域完全独立**（一个管入站，一个管出站），不会冲突。后续若考虑合并需明确区分方向 namespace。

**结论**：方案 C 完全符合现有分层架构，不引入新的部署假设。LAYERED-ARCHITECTURE.md 的修订仅限于在 §3 状态表加一行、§九 实现状态加一行。

---

## 六、实施顺序

每一步都是独立 commit，每步完成后跑 `mvn test -pl sip-common,gb28181-client,gb28181-server,gb28181-test`：

```
1. 新建 DialogRegistry（含 Entry / cleanupExpired）+ DialogNotFoundException（纯数据结构，无副作用，可单元测试）
2. SipMessageTransmitter.transmitStateful + transmitStatefulPreRegister + setupEventSubscriptions 提权
3. InviteRequestStrategy 改走 transmitStatefulPreRegister + DialogRegistry::register（KIND_INVITE）
   ↑ 此时 dialog 已建立（client / server 共用同一 strategy 同步生效），但 BYE 仍走旧路径
   ↑ 测试应观察：DialogRegistry 中能查到 INVITE 后的注册项
4. SubscribeRequestStrategy 改走 transmitStatefulPreRegister + DialogRegistry::register（KIND_SUBSCRIBE）
   ↑ 初始 SUBSCRIBE 200 OK 后注册到 DialogRegistry，refresh / unsubscribe 仍走旧路径
5. AbstractSipListener.processDialogTerminated 接入 DialogRegistry.remove
   （AsyncSipListener 通过 super 调用复用同一钩子，无需单独改）
6. SipSender.doByeRequest(callId) 新增 + 旧 doByeRequest(FromDevice, ToDevice) 删除
   + SipSender.doSubscribeRefresh(callId, content, expires) 新增
   ↑ 此时编译会失败，server/client/gateway 三处调用点同步在第 7 步修
7. ByeCommandStrategy（server + client 两侧）+ ServerCommandSender.deviceBye(callId)
   + ServerCommandSender.refreshSubscribe / unsubscribe + ClientCommandSender 同步
   + SipCommandController 同步改造 + CommandContext.forBye(role, callId) 新增
   ↑ 此时端到端 BYE 链路改造完成，BYE 应能收到 200 OK；SUBSCRIBE refresh / unsubscribe 已可用
8. InviteResponseProcessor.sendAck 改用 dialog.sendAck（与 BYE 路径对称）
9. DialogRegistryCleaner��@Scheduled）落地 + 在 Gb28181CommonAutoConfig 装配
10. InvitePlayFlowTest 升级断言（新增 BYE 200 OK 验证 + DialogRegistry 清理验证）
    + 新增 SubscribeRefreshFlowTest（initial → refresh → unsubscribe → DialogRegistry 空）
11. 同步更新 LAYERED-ARCHITECTURE.md / INVITE-REFACTOR-PLAN.md / CHANGELOG.md
```

**第 6、7 步必须在同一 PR 内完成**，否则任何中间提交都无法编译。其余步骤可单独 PR。

### 6.1 单元测试覆盖要点

| 测试 | 验证内容 |
|---|---|
| `DialogRegistryTest` | register/get/remove/size；同 callId 覆盖告警；null 安全 |
| `InviteRequestStrategyTest` | stateful 发送后 DialogRegistry 中可查；callId 与 dialog 的 callId 一致 |
| `ByeCommandStrategyTest`（mock） | `dialog.createRequest(BYE)` 被调用；callId 找不到时抛 `DialogNotFoundException` |
| `InvitePlayFlowTest`（IT） | INVITE → 200 OK → ACK → BYE → **200 OK**（不再 481）；DialogRegistry 在 BYE 后被清空 |
| `processDialogTerminatedTest` | DialogTerminatedEvent 触发后 DialogRegistry 中无残留 |

### 6.2 验证 481 修复

`InvitePlayFlowTest.invitePlay_thenBye_shouldEndSession` 新增断言：

```java
@Test
void invitePlay_thenBye_shouldEndSession() throws InterruptedException {
    CountDownLatch inviteLatch = new CountDownLatch(1);
    eventHandler.reset(inviteLatch);
    commandSender.deviceInvitePlay(clientId, "127.0.0.1", 10000, StreamModeEnum.UDP);
    assertThat(inviteLatch.await(5, TimeUnit.SECONDS)).isTrue();

    String callId = eventHandler.getLastInviteOkCallId();

    CountDownLatch byeLatch = new CountDownLatch(1);
    eventHandler.resetByeLatch(byeLatch);
    commandSender.deviceBye(callId);   // 新签名

    assertThat(byeLatch.await(2, TimeUnit.SECONDS)).as("BYE 应在 2 秒内完成").isTrue();
    assertThat(eventHandler.getLastByeStatus()).as("BYE 应收到 200 OK 而非 481").isEqualTo(200);

    // 验证 DialogRegistry 已清理（依赖 processDialogTerminated 钩子）
    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> DialogRegistry.size() == 0);
}
```

---

## 七、风险与已知边界

### 7.1 stateful 发送对非 dialog 请求的影响

JAIN-SIP 在 ClientTransaction 模式下会按 RFC 3261 Timer F（64*T1=32s）维护 transaction 状态。**REGISTER / MESSAGE / NOTIFY 等无 dialog 语义的请求若强制走 stateful**，可能引入：

- Timer F 超时重传与现有 SipSubscribe 重试逻辑冲突
- ClientTransaction 占用更多内存

**对策**：仅 INVITE / SUBSCRIBE 启用 stateful，其余继续 stateless。`SipMessageTransmitter` 同时保留 `transmitMessage` 和 `transmitStateful` 两个入口。

### 7.2 DialogRegistry 内存占用

`gov.nist.javax.sip.stack.SIPDialog` 实例约 500B（含 transaction / socket 引用 / route set 列表）。

| 并发 dialog | 内存占用 |
|---|---|
| 1K | ~500KB |
| 10K | ~5MB |
| 100K | ~50MB |

可接受，但**必须依赖 `processDialogTerminated` 钩子清理**。若 JAIN-SIP 因 bug 不发 DialogTerminatedEvent，需追加超时清理：

```java
// DialogRegistry 可选追加 cleanupExpired，与 SipTransactionRegistry.cleanupExpiredContexts 同模式
public static void cleanupExpired(long maxAgeMs) {
    long deadline = System.currentTimeMillis() - maxAgeMs;
    BY_CALL_ID.entrySet().removeIf(e -> {
        Dialog d = e.getValue();
        return d.getState() == DialogState.TERMINATED
                || d.getState() == null
                || (lastActivity(d) < deadline);
    });
}
```

`lastActivity` 需自定义元数据记录（dialog 没有标准的 lastUsed 字段），实现成本高。1.7.0 先依赖 JAIN-SIP 钩子，监控 `DialogRegistry.size()` 趋势，必要时再补。

### 7.3 同 Call-ID 重复 INVITE

日志中观察到的现象（v1.6.x）：BYE 失败后业务方/设备又发了一次 INVITE，复用同 Call-ID 但新 CSeq。JAIN-SIP 会创建新 Dialog（新 to-tag 协商），DialogRegistry 覆盖旧引用，旧 Dialog 由 JAIN-SIP 自然 GC。

**这是正确行为**，但要 warn 日志记录便于排查（已在 `register` 方法中实现）。

### 7.4 AUTOMATIC_DIALOG_ERROR_HANDLING 配置

当前 [`config.properties:4`](../../../sip-common/src/main/resources/sip/config.properties#L4) 设 `false`，意为 dialog 内协议错误不自动处理（如 481 不自动重发 CANCEL）。**保持 false**，避免 JAIN-SIP 行为不可预期。框架自己在 `DialogNotFoundException` 等明确节点处理错误。

### 7.5 与 `SipTransactionContext`（ThreadLocal）的关系

[`SipTransactionContext`](../../../sip-common/src/main/java/io/github/lunasaw/sip/common/context/SipTransactionContext.java) 是 ThreadLocal 模式，记录当前线程的 Call-ID 用于 `SipRequestBuilder` 自动复用。**dialog 内 BYE 不再依赖 ThreadLocal**（callId 直接从 dialog 取），但 ThreadLocal 仍服务于其他场景（如同 callId 的连续 MESSAGE 上报），**保留**。

### 7.6 ~~SUBSCRIBE 出站方向暂不改造~~（**1.2 修订：纳入 1.7.0**）

历史原因：SUBSCRIBE 与 BYE 同病同源（dialog 内消息没带 to-tag 会触发设备 481），但 1.0 / 1.1 版本担心 NOTIFY 反向 dialog 取用与订阅过期管理拖慢节奏，将其推迟到 1.7.x。

1.2 修订理由：
1. NOTIFY 入站走 `evt.getDialog()` 自动取回（同入站 BYE 路径），**不需要 DialogRegistry 反查**
2. SUBSCRIBE 自然超时清理由 `DialogRegistry.cleanupExpired()` + `@Scheduled` 兜底
3. 业务侧 catalog/mobile/alarm/ptz 4 个订阅入口共用同一 `SubscribeRequestStrategy`，改造成本约为 INVITE/BYE 的 30%
4. 推迟会让"业务侧每次发新 SUBSCRIBE 而非续订"的隐性脏数据继续累积（设备端订阅不断重复创建，浪费带宽）

1.7.0 一并落地，详见 §3.2.10–§3.2.14、§4 文件清单、§6 实施顺序 step 4。

### 7.7 SUBSCRIBE dialog 自然过期的清理路径

**关键差异**：JAIN-SIP 在 SUBSCRIBE 订阅 expires 自然超时（无续订）时**不会**触发 `DialogTerminatedEvent`（RFC 6665 §4.4.1 case 3：subscription 静默终结，dialog 状态机不变）。这意味着 INVITE 的"BYE → DialogTerminatedEvent → DialogRegistry.remove" 路径对 SUBSCRIBE 不适用。

**对策**：`DialogRegistry.cleanupExpired()` 必跑（不再是兜底），由 `DialogRegistryCleaner` 每 60s 调用一次。每个 SUBSCRIBE entry 注册时携带 `expiresAtMs = now + (info.expires + 60s grace) * 1000`，过期后由 cleaner 移除。

| 场景 | 清理路径 |
|---|---|
| 业务侧主动 unsubscribe（expires=0） | NOTIFY: Subscription-State: terminated 进站 → JAIN-SIP 触发 DialogTerminatedEvent → 主路径清理 |
| 业务侧续订（refresh） | DialogRegistry.register 覆盖更新 expiresAtMs |
| 业务侧忘记续订（自然超时） | cleaner 在 expiresAtMs + 下次扫描间隔内清理 |
| 设备主动 NOTIFY: terminated（订阅被设备拒绝/取消） | 同 unsubscribe 路径 |

**监控阈值**：
- `DialogRegistry.sizeByKind("SUBSCRIBE")` 长期 > 5 × 业务侧活跃订阅数 → cleaner 失效嫌疑
- `DialogRegistry.sizeByKind("INVITE")` 长期 > 5 × QPS × 平均会话时长(秒) → DialogTerminatedEvent 漏发嫌疑

---

## 八、对接入方的影响

### 8.1 BREAKING CHANGES

| API | v1.6.x | v1.7.0 | 迁移方式 |
|---|---|---|---|
| `ServerCommandSender.deviceBye(String deviceId, String callId)` | 接受 deviceId + callId | 改为 `deviceBye(String callId)` | 删除调用方的 `deviceId` 参数 |
| `ClientCommandSender.sendByeCommand(FromDevice, ToDevice)` | 接受设备对 | 改为 `sendByeCommand(String callId)` | 改用 callId（client 主动 BYE 场景：对讲挂断 / 设备主动结束会话） |
| `SipSender.doByeRequest(FromDevice, ToDevice)` | 直接发 BYE | **直接删除**（不留 deprecated 桥接） | 改用 `doByeRequest(String callId)` |
| `CommandContext.forAckBye(role, from, to, callId, "BYE")` | BYE 场景使用此构造 | BYE 场景改用 `CommandContext.forBye(role, callId)` | ACK 场景仍可用 forAckBye；BYE 场景必须迁移 |
| 业务方对 BYE 的失败兜底 | 无（默默 481） | `DialogNotFoundException` 显式抛 | 业务方需 catch 或先校验 callId 仍在会话中 |

**编译期暴露策略**：旧 `SipSender.doByeRequest(FromDevice, ToDevice)` 直接删除而非保留 deprecated 桥接，让所有调用点在 mvn compile 阶段直接报错，避免接入方误以为还能用。BREAKING 一次到位。

### 8.2 隐式正确性提升（无需改业务代码）

- INVITE 后所有 dialog 内消息自动携带正确 to-tag、Route Set、CSeq
- NAT IP 切换时（设备 Contact 更新），dialog 自动跟踪 Remote Target
- 级联场景（GB28181 多级平台中转）的 Record-Route 自动反序填入 BYE/INFO 的 Route 头

### 8.3 监控

新增可观测指标（业务方按需暴露 Micrometer / Prometheus）：

```java
Gauge.builder("sip.dialog.registry.size", DialogRegistry::size).register(registry);
```

监控阈值建议：单节点 dialog 数若长期 > 5 × 平均并发会话数，说明清理钩子失效，需排查 JAIN-SIP DialogTerminatedEvent 是否未发。

---

## 九、未来工作

### 9.1 dialog 内消息全面迁移

| Method | 当前实现 | 1.7.x 目标 |
|---|---|---|
| BYE | 手撸（出站不带 to-tag） | ✅ 本方案修复 |
| ACK（200 OK 响应） | `SipSender.doAckRequest(from, sipURI, response)` 反向构造 | ✅ 本方案改用 `dialog.sendAck` |
| **SUBSCRIBE 续订/退订** | 手撸（与 BYE 同病同源） | ✅ **1.2 修订：本方案一并修复** |
| INFO（dialog 内的播放控制） | 手撸 | 改用 `dialog.createRequest(INFO)` |
| re-INVITE（码率切换） | 暂未实现 | 改用 `dialog.createRequest(INVITE)` |
| UPDATE（早 dialog 内媒体协商） | 暂未实现 | 改用 `dialog.createRequest(UPDATE)` |

INFO / re-INVITE / UPDATE / SUBSCRIBE 落地时需扩展 `OutboundDialogCommandStrategy`（待 1.7.x 规划），方案 C 的 `DialogRegistry` + `transmitStatefulPreRegister` 是这些扩展的基础设施。

### 9.2 与 OutboundDialogCommandStrategy 的关系

考虑把 BYE / re-INVITE / INFO / UPDATE 这些 **dialog 内出站请求**抽出统一的策略基类：

```java
public abstract class OutboundDialogCommandStrategy {
    protected final Dialog requireDialog(String callId) {
        Dialog d = DialogRegistry.get(callId);
        if (d == null) throw new DialogNotFoundException(...);
        if (d.getState() != DialogState.CONFIRMED) throw new IllegalStateException(...);
        return d;
    }

    protected final void sendInDialog(Dialog dialog, Request request) {
        SipProviderImpl provider = ...;
        ClientTransaction ct = provider.getNewClientTransaction(request);
        dialog.sendRequest(ct);
    }
}
```

各 method 子类只需 `dialog.createRequest(METHOD)` + 设置 body。本方案先在 `SipSender.doByeRequest(callId)` 内联实现，待 INFO / re-INVITE 出现时再抽。

### 9.3 单元测试覆盖率门槛

新增 `DialogRegistry` / `DialogNotFoundException` 后，JaCoCo 80% 行覆盖门槛（[INVITE-REFACTOR-PLAN.md §三](../1.3.0/INVITE-REFACTOR-PLAN.md)）需保持。`DialogRegistry` 的 register/get/remove/size 全路径走通即可。

---

## 十、CHANGELOG 草稿

```markdown
## [1.7.0] - 2026-XX-XX

### 🚨 BREAKING CHANGES

- **`ServerCommandSender.deviceBye` 签名变更**：从 `deviceBye(String deviceId, String callId)` 改为 `deviceBye(String callId)`。deviceId 已包含在 dialog 中，无需再传。
- **`ClientCommandSender.sendByeCommand` 签名变更**：从 `sendByeCommand(FromDevice, ToDevice)` 改为 `sendByeCommand(String callId)`。client 主动 BYE 同样要求 dialog 已建立。
- **`SipSender.doByeRequest(FromDevice, ToDevice)` 删除**：改为 `doByeRequest(String callId)`，必须先有已建立的 dialog。无 dialog 时抛 `DialogNotFoundException`。**直接删除而非 deprecated 桥接**，让编译期一次性暴露所有调用点。
- **`CommandContext.forAckBye` 在 BYE 场景退役**：BYE 改用新增的 `CommandContext.forBye(role, callId)`，ACK 场景仍可用 forAckBye。

### 🐛 Bug Fixes

- 修复出站 BYE 不携带 to-tag 导致设备返回 `481 Call leg/Transaction does not exist` 的协议合规问题（详见 [doc/plans/1.7.0/OUTBOUND-DIALOG-PLAN.md](../../../../plans/1.7.0/OUTBOUND-DIALOG-PLAN.md)）。问题对 server 主动 BYE 与 client 主动 BYE 同源同病，本次一并修复。
- 修复 `InvitePlayFlowTest.invitePlay_thenBye_shouldEndSession` 因不断言 BYE 状态码掩盖上述 bug 的测试盲区。

### ✨ Features

- 新增 `DialogRegistry`（进程内出站 dialog 注册表），由 `processDialogTerminated` 钩子自动清理。
- 新增 `SipMessageTransmitter.transmitStatefulPreRegister(...)` —— 先注册 dialog 再 sendRequest，覆盖同机回环 / 极低延迟链路下的响应竞态。
- INVITE 改走 `ClientTransaction` 有状态发送（client 与 server 同步生效，共用同一 `InviteRequestStrategy`），自动建立 JAIN-SIP Dialog。
- INVITE 200 OK 的 ACK 改用 `dialog.sendAck`，与 BYE 路径对称。
- 新增 `DialogNotFoundException`，让 BYE 调用错误在第一时间暴露而不是被 481 掩盖。

### 📦 Migration Guide

**Server 侧**：

```diff
- commandSender.deviceBye(deviceId, callId);
+ commandSender.deviceBye(callId);
```

**Client 侧**：

```diff
- ClientCommandSender.sendByeCommand(fromDevice, toDevice);
+ ClientCommandSender.sendByeCommand(callId);
```

`deviceId` / `FromDevice` / `ToDevice` 不再需要 —— 框架从 dialog 中取所有信息。

业务方对 `deviceBye` / `sendByeCommand` 调用应增加 try-catch：

```java
try {
    commandSender.deviceBye(callId);
} catch (DialogNotFoundException e) {
    // dialog 已不存在（如对端先发 BYE / INVITE 还未 200 OK / callId 错误）
    log.warn("BYE 失败：dialog 不存在", e);
}
```
```

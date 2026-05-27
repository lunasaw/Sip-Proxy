# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIP Proxy is a GB28181-2016 communication framework built on Java 17 and Spring Boot 3.3.1. It is a multi-module Maven project providing complete SIP protocol communication for video surveillance systems.

It is delivered as a **Maven library**, not a standalone service. Business systems (typically a `sip-gateway` layer the user implements on top) embed it in-process and interact via Spring Events (inbound) and `CommandSender` beans (outbound). A single JVM can act as platform server (`gb28181-server`) and device client (`gb28181-client`) simultaneously for cascading scenarios.

Current version: **1.7.0** (see `CHANGELOG.md`). Recent breaking changes worth knowing before touching outbound code:

- **1.7.0** — Outbound dialog rewrite. `BYE` and `SUBSCRIBE` refresh/unsubscribe are now dialog-aware: `ServerCommandSender.deviceBye(callId)` (deviceId removed), `ClientCommandSender.sendByeCommand(callId)`, `SipSender.doByeRequest(callId)` (old `(FromDevice, ToDevice)` overload **deleted**, not deprecated). Calls without an established dialog throw `DialogNotFoundException` instead of getting a silent `481`. New `DialogRegistry` + `DialogRegistryCleaner` are core to INVITE/SUBSCRIBE flows. See [doc/OUTBOUND-DIALOG-PLAN.md](doc/OUTBOUND-DIALOG-PLAN.md).
- **1.5.0** — Listener-layered API. Removed 4 client business handler interfaces + 10 old client events; added `QueryListener` / `ControlListener` / `ConfigListener` / `SubscribeListener` / `NotifyListener` (client) and `DeviceResponseListener` / `DeviceNotifyListener` / `DeviceLifecycleListener` / `DeviceSessionListener` (server). See [doc/LISTENER-LAYERED-DESIGN.md](doc/LISTENER-LAYERED-DESIGN.md) and [doc/LISTENER-MIGRATION-GUIDE.md](doc/LISTENER-MIGRATION-GUIDE.md).

## Build and Development Commands

```bash
# Build
mvn clean compile
mvn clean install

# Test
mvn test                                    # unit tests (all modules)
mvn verify                                  # integration tests (failsafe plugin)
mvn test -Dspring.profiles.active=test      # with test profile

# Single test class
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest

# Single test method
mvn test -pl gb28181-client -Dtest=CancelRequestProcessorTest#methodName

# Single module
mvn clean install -pl gb28181-test

# Protocol-purity check (CI gate, run during `mvn verify`)
bash scripts/check-sip-common-purity.sh
```

## Module Structure

```
sip-proxy
├── sip-common          # Core SIP protocol stack (JAIN-SIP wrapper, listeners, caching, metrics)
├── gb28181-common      # GB28181 data models (JAXB XML entities, no business logic)
├── gb28181-client      # Device client (ClientSendCmd, inbound request/response processors)
├── gb28181-server      # Platform server (ServerSendCmd, inbound request/response processors)
└── gb28181-test        # Integration tests and runnable examples
```

Dependency order: `sip-common` ← `gb28181-common` ← `gb28181-client` / `gb28181-server` ← `gb28181-test`

## Architecture

### SIP Message Processing Pipeline

```
SIP Message
  → AbstractSipListener          (unified event dispatch, TraceId propagation)
  → XXXRequestProcessor          (message type: REGISTER, INVITE, MESSAGE, NOTIFY, BYE…)
  → XXXRequestSubProcessor       (MESSAGE only: routes by GB28181 cmdType)
  → XXXRequestHandler            (business logic implementation)
```

### Outbound Commands

- **`ClientCommandSender`** / **`ServerCommandSender`** — strategy-pattern command senders for outbound SIP messages. `ServerCommandSender` requires `DeviceSessionCache` to look up device sessions.

### Dialog-Aware Outbound (1.7.0+)

INVITE and SUBSCRIBE go through **stateful** transmission (`SipMessageTransmitter.transmitStateful` / `transmitStatefulPreRegister`), which uses a `ClientTransaction` so JAIN-SIP auto-creates a `Dialog`. The dialog is recorded in `DialogRegistry` (in-process, keyed by `callId`, with `kind=INVITE|SUBSCRIBE` and `expiresAtMs`).

- **BYE** must reference an existing dialog: `deviceBye(callId)` / `sendByeCommand(callId)` / `doByeRequest(callId)`. No dialog → `DialogNotFoundException`. Dialog cleanup: `AbstractSipListener.processDialogTerminated` → `DialogRegistry.remove` (INVITE primary path).
- **SUBSCRIBE refresh / unsubscribe** must also be dialog-aware: `refreshSubscribe(callId, expires)` / `unsubscribe(callId)` on both senders, plus `SipSender.doSubscribeRefresh(callId, content, expires)` and `CommandContext.forSubscribeRefresh(...)`.
- **`DialogRegistryCleaner`** (`@Scheduled` 60s) sweeps expired SUBSCRIBE entries because RFC 6665 §4.4.1 case 3 has no `DialogTerminatedEvent`.
- **INVITE 200 OK ACK** uses `dialog.sendAck` (see `InviteResponseProcessor.sendAck`), symmetric with BYE.

**Implication for new outbound logic:** if you're adding any in-dialog request (re-INVITE, UPDATE, INFO, NOTIFY-from-server, etc.), use the stateful path and look up the dialog from `DialogRegistry` — never construct a fresh `From`/`To` pair without the to-tag.

### Event Bus & Listener API

**v1.5.0+**: Business接入有两种等价方式：

1. **Listener 接口（推荐，client/server 形态对称）**
   - Client 侧：实现 `gb28181-client/api/QueryListener` / `ControlListener` / `ConfigListener` / `SubscribeListener` / `NotifyListener`，或直接继承 `ClientGb28181Adapter`
   - Server 侧：实现 `gb28181-server/api/DeviceResponseListener` / `DeviceNotifyListener` / `DeviceLifecycleListener` / `DeviceSessionListener`，或直接继承 `ServerGb28181Adapter`
   - 业务方只 override 关心的方法，框架自动派发与回包（QueryListener 返回非 null 时 Adapter 自动 sendXxxCommand）
2. **直接监听 L1 协议事件（跨切层 / 高级用法）**
   - Client 侧 6 个外层事件：`ClientQueryEvent` / `ClientControlEvent` / `ClientKeepaliveEvent` / `ClientConfigEvent` / `ClientSubscribeEvent` / `ClientNotifyEvent`
   - Client 侧 8 个 SIP method 系事件保留：`ClientInviteEvent` / `ClientByeEvent` / `ClientAckEvent` / `ClientCancelEvent` / `ClientInfoEvent` / `ClientRegister{Success,Failure,Challenge}Event`
   - Server 侧 32 个 typed `Device*Event` / `ServerInviteEvent` 全部保留（业务侧已广泛使用）
   - 业务/metrics/audit/tracing 可同时监听同一事件，互不干扰

QueryListener 通过 `ObjectProvider#getIfUnique()` 强制单 bean —— 多实例 fail fast；缺失时首次告警一次后静默走默认空响应。Control / Config / Subscribe / Notify listener 全部调用（观察者模式）。

历史接口 `MessageRequestHandler` / `DeviceControlRequestHandler` / `SubscribeRequestHandler` / `CustomMessageRequestHandler` 已在 v1.5.0 删除。详见 [doc/LISTENER-LAYERED-DESIGN.md](doc/LISTENER-LAYERED-DESIGN.md) 与 [doc/LISTENER-MIGRATION-GUIDE.md](doc/LISTENER-MIGRATION-GUIDE.md)。

### Bootstrapping

Annotate your `@SpringBootApplication` class with `@EnableSipClient` (device side) or `@EnableSipServer` (platform side). Both import `Gb28181CommonAutoConfig` plus their respective auto-config. `@EnableSipClient` requires a `ClientDeviceSupplier` bean; `@EnableSipServer` requires `ServerDeviceSupplier` + `DeviceSessionCache`.

### Required Beans (must be supplied by the embedding application)

| Bean | When required | Implementation guidance |
|------|---------------|--------------------------|
| `DeviceSessionCache` | Always (server side) | Stores device session info (ip / port / transport). **Multi-node deployments must back this with shared storage (Redis), never an in-memory map.** Defaults are demo-only. |
| `ServerDeviceSupplier` | `@EnableSipServer` | Supplies platform-side device identity. `DefaultServerDeviceSupplier` reads from config — single-node only. |
| `ClientDeviceSupplier` | `@EnableSipClient` | Supplies client-side device identity. `DefaultClientDeviceSupplier` reads from config — single-node only. |

### Key Extension Points

| Interface/Base Class | Purpose |
|---|---|
| `DeviceSupplier` | Provide device identity info; override `DefaultClientDeviceSupplier` or `DefaultServerDeviceSupplier` |
| `AbstractSipRequestProcessor` | Base for new inbound message type processors |
| `XXXProcessorHandler` | Business logic interface per message type |
| `ServerAbstractSipResponseProcessor` | Base for server-side response processors |
| `ClientAbstractSipResponseProcessor` | Base for client-side response processors |
| `ClientCommandStrategy` / `ServerCommandStrategy` | Add custom outbound command strategies via `ClientCommandStrategyFactory` / `ServerCommandStrategyFactory` |

## Development Conventions

- **`jakarta.*` packages only** — Spring Boot 3.x; never `javax.*`
- **`@MockitoBean`** in tests — `@MockBean` is deprecated
- Cast `Request` → `SIPRequest` when accessing JAIN-SIP implementation-specific methods
- TraceId (SkyWalking) must be propagated explicitly in async executor threads
- JSON serialization uses **fastjson2**
- JaCoCo enforces **80% line coverage** — tests must meet this threshold

### Protocol Layer Purity (sip-common)

**`sip-common` must contain zero GB28181-specific code.** It is the generic SIP transport layer, GB28181 lives in `gb28181-common` / `gb28181-client` / `gb28181-server`.

This is enforced in CI by `scripts/check-sip-common-purity.sh`, which fails the build if any of these tokens appear in `sip-common/src/main/java`:

```
gb28181 | GB28181 | gbproxy | Catalog | MobilePosition | GbSession | GbSip | GbUtil
```

When working in `sip-common`, route GB28181 logic into `gb28181-common` (e.g. `GbSdpUtils.parseGbSdp`, `GbUtil.generateGB28181Code`). See `doc/PROTOCOL-DECOUPLING-PLAN.md`.

### Test Setup Pattern

```java
@BeforeEach
void setup() {
    sipLayer.setSipListener(...);
    sipLayer.addListeningPoint(...);  // must precede any message send
}
```

Use separate client/server device configurations in tests to avoid port/identity conflicts.

## Configuration Namespaces

- `sip:` — SIP protocol settings (server `ip` / `port`, plus `external-ip` / `external-port` for NAT or VIP — these go into outbound `Via` / `Contact` headers; fallback to `ip` / `port` when unset)
- `sip.common.*` — common framework knobs (e.g. `user-agent`, `time-sync.*`)
- `gb28181:` — GB28181 protocol settings
- Environment overrides: `application-{env}.yml`

> **1.3.0 migration:** `sip.gb28181.time-sync.*` was renamed to `sip.common.time-sync.*`. Default `User-Agent` changed from `LunaSaw-GB28181-Proxy` to `sip-proxy`.

## Horizontal Scaling Constraints

When working on multi-node features, respect this state-locality rule (see `doc/HORIZONTAL-SCALING.md` and `doc/LAYERED-ARCHITECTURE.md`):

| State | Where it lives | Notes |
|-------|----------------|-------|
| `ServerTransaction` / `SipTransactionRegistry` | In-process only | SIP protocol constraint — same-device messages must hit the same node (source-IP-hash at the VIP) |
| `DeviceSessionCache` | **Shared store (Redis)** | Required for multi-node; in-memory implementations break failover |
| `ServerDeviceSupplier` data | Shared store (Redis) | Read from shared backend |
| INVITE async response context | In-process key + Redis node-mapping | For cross-node async response routing |

**Rule of thumb:** local node keeps SIP transaction state; everything business-visible must be externalized.

## Reference Documentation

Key docs in `doc/` (consult before non-trivial changes):

- `LAYERED-ARCHITECTURE.md` — sip-proxy ↔ sip-gateway ↔ business server architecture
- `HORIZONTAL-SCALING.md` — multi-node deployment, state locality, VIP topology
- `PROTOCOL-DECOUPLING-PLAN.md` — sip-common / gb28181-common boundary rules (1.3.0)
- `BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md` — 1.3.0 removal of `*Handler` interfaces in favor of pure Spring Events
- `INVITE-REFACTOR-PLAN.md` — INVITE async refactor (1.3.0)
- `LISTENER-LAYERED-DESIGN.md` / `LISTENER-MIGRATION-GUIDE.md` — 1.5.0 listener API (canonical for new business code)
- `OUTBOUND-DIALOG-PLAN.md` — 1.7.0 dialog-aware BYE / SUBSCRIBE refresh (canonical for outbound flows)
- `GB28181-2016.md` / `GBT-28181-2022.md` — protocol references

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIP Proxy is a GB28181-2016 communication framework built on Java 17 and Spring Boot 3.3.1. It is a multi-module Maven project providing complete SIP protocol communication for video surveillance systems.

It is delivered as a **Maven library**, not a standalone service. Business systems (typically a `sip-gateway` layer the user implements on top) embed it in-process and interact via Spring Events (inbound) and `CommandSender` beans (outbound). A single JVM can act as platform server (`gb28181-server`) and device client (`gb28181-client`) simultaneously for cascading scenarios.

Current version: **1.3.0** (see `CHANGELOG.md` for breaking changes vs 1.2.x).

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

### Event Bus

Business logic is implemented via Spring `@EventListener`. Client-side events (e.g. `ClientRegisterSuccessEvent`, `CatalogEvent`) extend `ApplicationEvent`; server-side events (e.g. `DeviceOnlineEvent`, `DeviceInfoEvent`) extend `DeviceEvent(source, deviceId)`.

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
- `GB28181-2016.md` / `GBT-28181-2022.md` — protocol references

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIP Proxy is a GB28181-2016 communication framework built on Java 17 and Spring Boot 3.3.1. It is a multi-module Maven project providing complete SIP protocol communication for video surveillance systems.

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

# Single module
mvn clean install -pl gb28181-test
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

- **`ClientSendCmd`** / **`ServerSendCmd`** — strategy-pattern command senders for outbound SIP messages.

### Bootstrapping

Annotate your `@SpringBootApplication` class with `@EnableSipProxy` to activate auto-configuration for either client or server.

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
- JaCoCo enforces **80% line coverage** — tests must meet this threshold

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

- `sip:` — SIP protocol settings
- `gb28181:` — GB28181 protocol settings
- Environment overrides: `application-{env}.yml`

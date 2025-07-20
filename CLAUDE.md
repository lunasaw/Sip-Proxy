# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SIP Proxy is a GB28181-2016 communication framework built with Java 17 and Spring Boot 3.3.1. It's a multi-module Maven
project that provides complete SIP protocol communication capabilities for building video surveillance and security
systems.

## Build & Development Commands

### Build Commands

- `mvn clean compile` - Clean and compile all modules
- `mvn clean package` - Build all modules into JAR files
- `mvn clean install` - Install modules to local repository

### Test Commands

- `mvn test` - Run unit tests
- `mvn verify` - Run integration tests (failsafe plugin)
- `mvn test -Dspring.profiles.active=test` - Run tests with test profile

### Run Commands

- `mvn spring-boot:run` - Run Spring Boot application
- Main class for test module: `io.github.lunasaw.gbproxy.test.TestFrameworkVerifyApplication`

### Module Build

- Individual modules can be built with: `mvn clean install -pl <module-name>`
- Build specific module: `mvn clean install -pl gb28181-test`

## Architecture Overview

### Module Structure

```
sip-proxy (parent)
├── sip-common          - Core SIP protocol stack and utilities
├── gb28181-common      - GB28181 protocol data models  
├── gb28181-client      - GB28181 device client implementation
├── gb28181-server      - GB28181 platform server implementation
└── gb28181-test        - Integration tests and examples
```

### Dependency Hierarchy

- `gb28181-client` and `gb28181-server` depend on `sip-common` and `gb28181-common`
- `gb28181-test` depends on all other modules
- `sip-common` is the foundation module with no internal dependencies

### Core Technology Stack

- **Java 17** (Jakarta EE packages, not javax)
- **Spring Boot 3.3.1**
- **JAIN-SIP 1.3.0-91** - SIP protocol stack
- **Caffeine 3.1.8** - High-performance caching
- **Micrometer 1.12.0** - Metrics and monitoring
- **Maven** - Build tool

## Key Architecture Patterns

### SIP Message Processing Flow

```
SIP Message → AbstractSipListener → XXXRequestProcessor → XXXRequestHandler → Business Logic
```

### Active vs Passive Services

- **Active Services**: `ClientSendCmd`, `ServerSendCmd` - Send SIP commands outbound
- **Passive Processing**: `XXXRequestProcessor`, `XXXResponseProcessor` - Handle incoming SIP messages

### Message Processing Layers

1. **SIP Listener Layer**: `AbstractSipListener` - Unified SIP event dispatch
2. **Protocol Layer**: `SipRequest` handling - Protocol-level processing
3. **Message Type Layer**: `XXXRequestProcessor` - Handle different SIP message types
4. **Business Subtype Layer**: `XXXRequestSubProcessor` - Handle cmdType routing within MESSAGE
5. **Business Logic Layer**: `XXXRequestHandler` - Actual business implementation

## Important Development Conventions

### Java/Spring Requirements

- **Mandatory**: Use `jakarta` packages instead of `javax` (Spring Boot 3.x requirement)
- **Mandatory**: Use `@MockitoBean` instead of deprecated `@MockBean` in tests
- Use `@Slf4j` for logging, Lombok for boilerplate code
- Follow the established processor/handler separation pattern

### SIP Protocol Specifics

- Client and server modules have separate device configurations for testing
- SIP request handling requires strong typing: cast `Request` to `SIPRequest` when accessing implementation-specific
  methods
- Async SIP listeners must properly handle TraceId propagation in executor threads

### Testing Patterns

- Set up SIP listening points before message sending tests: `sipLayer.setSipListener()` and
  `sipLayer.addListeningPoint()`
- Use separate client/server device configs in tests to avoid confusion
- Integration tests use TestContainers and include both unit and integration test plugins

## Key Interfaces and Extension Points

### Device Management

- `DeviceSupplier` - Interface for providing device information
- `ClientDeviceSupplier`, `ServerDeviceSupplier` - Module-specific implementations

### Message Processing Extension

- Extend `AbstractSipRequestProcessor` for new SIP message types
- Implement `XXXProcessorHandler` interfaces for business logic
- Server processors extend `ServerAbstractSipResponseProcessor`
- Client processors extend `ClientAbstractSipResponseProcessor`

### Configuration

- Main config: `application.yml`
- SIP protocol config under `sip:` namespace
- GB28181 protocol config under `gb28181:` namespace
- Environment-specific configs: `application-{env}.yml`

## Common Development Tasks

### Adding New SIP Message Handler

1. Create `XXXRequestProcessor` extending appropriate base class
2. Create `XXXRequestHandler` interface for business logic
3. Implement business logic in handler
4. Register processor with SIP listener

### Testing SIP Functionality

1. Set up test listening points in `@BeforeEach`
2. Use separate client/server device configurations
3. Cast `Request` to `SIPRequest` for implementation-specific methods
4. Verify TraceId handling in async operations

### Performance Optimization

- Leverage Caffeine caching for frequently accessed data
- Use async processing patterns with proper TraceId management
- Monitor with Micrometer metrics integration
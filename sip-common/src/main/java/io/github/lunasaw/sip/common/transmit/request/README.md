# SIP请求构建器重构说明

## 概述

本次重构将原有的 `SipRequestProvider` 类进行了优化改造，采用了构建器模式（Builder Pattern）和工厂模式（Factory
Pattern），提高了代码的可维护性、可扩展性和可读性。

## 重构目标

1. **消除代码重复**：抽象出共用的构建逻辑
2. **提高可维护性**：每个构建器职责单一，便于维护
3. **增强可扩展性**：新增SIP方法类型时，只需添加新的构建器
4. **改善可读性**：代码结构更清晰，方法命名更直观

## 架构设计

### 核心组件

1. **AbstractSipRequestBuilder**：抽象基类，定义通用的构建逻辑和模板方法
2. **具体构建器类**：继承抽象基类，实现特定SIP方法的构建逻辑
3. **SipRequestBuilderFactory**：工厂类，提供统一的构建器获取接口

### 类图结构

```
AbstractSipRequestBuilder (抽象基类)
    ├── MessageRequestBuilder (MESSAGE请求构建器)
    ├── InviteRequestBuilder (INVITE请求构建器)
    ├── ByeRequestBuilder (BYE请求构建器)
    ├── RegisterRequestBuilder (REGISTER请求构建器)
    ├── SubscribeRequestBuilder (SUBSCRIBE请求构建器)
    ├── InfoRequestBuilder (INFO请求构建器)
    ├── AckRequestBuilder (ACK请求构建器)
    └── NotifyRequestBuilder (NOTIFY请求构建器)

SipRequestBuilderFactory (工厂类)
```

## 使用方式

### 1. 使用工厂类（推荐）

```java
// 创建MESSAGE请求
Request messageRequest = SipRequestBuilderFactory.createMessageRequest(
                fromDevice, toDevice, content, callId
        );

// 创建INVITE请求
Request inviteRequest = SipRequestBuilderFactory.createInviteRequest(
        fromDevice, toDevice, content, subject, callId
);

// 创建BYE请求
Request byeRequest = SipRequestBuilderFactory.createByeRequest(
        fromDevice, toDevice, callId
);

// 创建REGISTER请求
Request registerRequest = SipRequestBuilderFactory.createRegisterRequest(
        fromDevice, toDevice, expires, callId
);
```

### 2. 使用具体构建器

```java
// 获取MESSAGE构建器
MessageRequestBuilder messageBuilder = SipRequestBuilderFactory.getMessageBuilder();
Request messageRequest = messageBuilder.buildMessageRequest(fromDevice, toDevice, content, callId);

// 获取INVITE构建器
InviteRequestBuilder inviteBuilder = SipRequestBuilderFactory.getInviteBuilder();
Request inviteRequest = inviteBuilder.buildInviteRequest(fromDevice, toDevice, content, subject, callId);
```

### 3. 通用构建方法

```java
// 创建自定义SipMessage
SipMessage sipMessage = SipMessage.getMessageBody();
sipMessage.

setMethod(Request.MESSAGE);
sipMessage.

setContent(content);
sipMessage.

setCallId(callId);

// 使用通用构建方法
Request request = SipRequestBuilderFactory.createSipRequest(fromDevice, toDevice, sipMessage);
```

## 重构优势

### 1. 代码复用

- **抽象基类**：将通用的SIP请求构建逻辑抽象到 `AbstractSipRequestBuilder` 中
- **模板方法**：使用模板方法模式，子类只需实现特定的定制化逻辑
- **减少重复**：消除了原代码中大量的重复构建逻辑

### 2. 职责分离

- **单一职责**：每个构建器只负责一种SIP方法的构建
- **开闭原则**：新增SIP方法时，只需添加新的构建器，无需修改现有代码
- **依赖倒置**：高层模块不依赖低层模块，都依赖抽象

### 3. 易于维护

- **模块化**：每个构建器都是独立的模块，便于单独维护和测试
- **可读性**：代码结构清晰，方法命名直观
- **可测试性**：每个构建器都可以独立进行单元测试

### 4. 向后兼容

- **保留原类**：`SipRequestProvider` 类保留并标记为 `@Deprecated`
- **渐进迁移**：可以逐步将现有代码迁移到新的构建器模式
- **平滑过渡**：不会影响现有功能的正常运行

## 迁移指南

### 从旧版本迁移

```java
// 旧版本用法
Request request = SipRequestProvider.createMessageRequest(fromDevice, toDevice, content, callId);

// 新版本用法
Request request = SipRequestBuilderFactory.createMessageRequest(fromDevice, toDevice, content, callId);
```

### 推荐迁移步骤

1. **第一阶段**：在新代码中使用新的构建器模式
2. **第二阶段**：逐步将现有代码迁移到新模式
3. **第三阶段**：完全移除旧的 `SipRequestProvider` 类

## 扩展指南

### 添加新的SIP方法构建器

1. **创建新的构建器类**：

```java
public class NewMethodRequestBuilder extends AbstractSipRequestBuilder {
    public Request buildNewMethodRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        SipMessage sipMessage = SipMessage.getNewMethodBody();
        sipMessage.setMethod(Request.NEW_METHOD);
        sipMessage.setContent(content);
        sipMessage.setCallId(callId);

        return build(fromDevice, toDevice, sipMessage);
    }

    @Override
    protected void customizeRequest(Request request, FromDevice fromDevice, ToDevice toDevice, SipMessage sipMessage) {
        // 添加特定的头部信息
    }
}
```

2. **在工厂类中添加支持**：

```java
public class SipRequestBuilderFactory {
    private static final NewMethodRequestBuilder NEW_METHOD_BUILDER = new NewMethodRequestBuilder();

    public static NewMethodRequestBuilder getNewMethodBuilder() {
        return NEW_METHOD_BUILDER;
    }

    public static Request createNewMethodRequest(FromDevice fromDevice, ToDevice toDevice, String content, String callId) {
        return NEW_METHOD_BUILDER.buildNewMethodRequest(fromDevice, toDevice, content, callId);
    }
}
```

## 总结

本次重构通过引入构建器模式和工厂模式，成功地将原有的单体类重构为模块化的架构。新的架构具有更好的可维护性、可扩展性和可读性，同时保持了向后兼容性，为后续的功能扩展和维护奠定了良好的基础。
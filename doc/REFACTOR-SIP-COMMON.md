# sip-common 重构方案

> 版本：1.0 | 基于架构分析：2026-05-23

---

## 一、背景与目标

当前 sip-common 存在以下核心问题：
1. 两个同名类 `SipTransactionContext` 导致导入混乱
2. `SipRequestProvider`（已弃用）与 `SipRequestBuilderFactory` 并存，两套 API
3. `SipSender` 内嵌 `SipRequestBuilder` 内部类，职责混乱
4. `SipRequestStrategyFactory` 与 `SipRequestBuilderFactory` 功能重叠

**目标**：消除重复、统一 API、不破坏上层模块（gb28181-client/server）的现有调用。

---

## 二、问题清单与优先级

| 优先级 | 问题 | 影响范围 |
|--------|------|---------|
| P0 | 两个同名 `SipTransactionContext` | 全模块导入混乱 |
| P1 | `SipRequestProvider` 残留 | 4 个调用文件 |
| P1 | `SipSender` 内嵌 Builder 类 | SipSender 本身 |
| P2 | Strategy 与 Builder 两套工厂并存 | SipSender 调用路径不一致 |
| P3 | `SipTransactionManager` 命名不符 | 轻微，可选 |

---

## 三、重构方案

### Stage 1：解决同名类冲突（P0）

**问题**：
- `context/SipTransactionContext` — ThreadLocal 模式，管理当前线程的事务上下文（Call-ID、CSeq、From/To）
- `transmit/SipTransactionContext` — ConcurrentHashMap 模式，管理跨线程的事务生命周期

两者职责不同，但命名相同，任何 `import` 都会产生歧义。

**方案**：将 `transmit/SipTransactionContext` 重命名为 `SipTransactionRegistry`。

```
重命名前：io.github.lunasaw.sip.common.transmit.SipTransactionContext
重命名后：io.github.lunasaw.sip.common.transmit.SipTransactionRegistry
```

`context/SipTransactionContext` 保持不变（ThreadLocal 模式，是主要使用路径）。

**涉及文件**：
- `transmit/SipTransactionContext.java` → 重命名为 `SipTransactionRegistry.java`
- 搜索所有 `import io.github.lunasaw.sip.common.transmit.SipTransactionContext` 并替换

---

### Stage 2：清理 SipRequestProvider（P1）

**问题**：`SipRequestProvider` 标记了 `@Deprecated` 但仍被调用，与 `SipRequestBuilderFactory` 功能重叠。

**方案**：
1. 搜索所有调用 `SipRequestProvider` 的位置
2. 逐一替换为对应的 `SipRequestBuilderFactory.getXxxBuilder().build(...)` 调用
3. 删除 `SipRequestProvider.java`

**预期调用方**（需确认）：
- `AbstractSipRequestBuilder` 的各子类
- `SipSender` 中的兼容性方法

---

### Stage 3：统一 SipSender 的请求构建路径（P1 + P2）

**问题**：`SipSender` 内部定义了 `SipRequestBuilder` 内部类，同时还通过 `SipRequestStrategyFactory` 和 `SipRequestBuilderFactory` 两条路径构建请求，调用路径不一致。

**现状**：
```
SipSender.request(from, to, method)
  → new SipRequestBuilder(...)   ← 内部类，走 SipRequestStrategyFactory
SipSender.doRegisterRequest(...)
  → SipRequestBuilderFactory.getRegisterBuilder().build(...)  ← 走 Builder
```

**方案**：统一走 `SipRequestStrategyFactory`，删除内部 `SipRequestBuilder` 类。

```java
// 重构后 SipSender.request() 直接委托给 Strategy
public static String send(FromDevice from, ToDevice to, String method, SipMessage msg) {
    SipRequestStrategy strategy = SipRequestStrategyFactory.getStrategy(method);
    Request request = strategy.buildRequest(from, to, msg);
    return SipLayer.transmitRequest(from.getIp(), request);
}
```

`SipRequestBuilderFactory` 降级为内部实现细节，不再作为公开 API。

---

### Stage 4：SipTransactionManager 重命名（P3，可选）

`SipTransactionManager` 实际只提供获取 `ServerTransaction` 的功能，命名夸大了职责。

```
重命名前：SipTransactionManager
重命名后：SipServerTransactionProvider
```

此项改动影响范围小，可在其他 Stage 完成后单独处理。

---

## 四、执行顺序与验证

```
Stage 1 → Stage 2 → Stage 3 → Stage 4（可选）
```

每个 Stage 完成后执行：
```bash
mvn test -pl sip-common,gb28181-client,gb28181-server
```

确保上层模块测试全部通过后再进入下一 Stage。

---

## 五、不在本次重构范围内

- `AbstractSipListener` 职责拆分（影响范围过大，需单独立项）
- `SipLayer` 静态方法改造（涉及网络层，风险高）
- 性能优化、日志规范化

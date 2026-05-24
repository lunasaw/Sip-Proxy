# SIP 与 GB28181 协议解耦方案

> 版本：2.1 | 日期：2026-05-24 | 目标版本：**1.3.0（全面重构，不保留兼容）**
>
> **v2.1 修订**：在 v2.0 基础上修正自洽性问题。
> - 3.4 节 UserAgent 改造完整闭环：补齐 `SipMessageTransmitter:87` / `FromDevice:36` 改为读取 `SipCommonProperties.userAgent`，否则配置项无法生效
> - 4.2 修改清单同步追加上述两处
> - 阶段二补充 sip-common 侧 `parseSdp` 标准 SDP 解析用例，避免 JaCoCo 80% 覆盖率门槛跌破
>
> **v2.0 修订**：基于 v1.1 代码核对结果，确认 1.3.0 走 BREAKING CHANGES 路线，移除所有兼容期逻辑。
> - 删除"双 key 兼容"、"UserAgent 双轨"、"Deprecated 警告"等过渡设计
> - `SubscribeHolder` / `SubscribeTask` 经全仓库核对**确认为死代码**，方案改为直接删除（不下沉）
> - 修正 `parseSdp` 调用方为 4 处（v1.1 漏列 `InviteResponseProcessor`）
> - 工作量由 ~9h 精简至 ~5h

---

## 一、背景与目标

### 1.1 当前问题

模块依赖关系上 `sip-common` 是协议无关的通用层，但代码上**已经偷偷依赖了 GB28181 概念**：

- 通用工具 `SipUtils.parseSdp()` 硬编码返回 `GbSessionDescription`
- 通用实体包下放着 `GbSessionDescription`、`GbSipDate` 这种 GB 专用类
- 通用订阅持有器 `SubscribeHolder` 含 `catalogMap`、`putCatalogSubscribe` 这种 GB 概念
- 通用配置注释、常量、配置 key 都带 GB28181 字样

这导致：
1. README 宣称的"基于 sip-common 接入私有 SIP 扩展协议"场景**不成立**——一接就被 GB 概念污染
2. 后续接入 SIP Trunk、IMS、私有协议时，要么忍受冗余，要么大改通用层
3. 模块边界与代码边界不一致，长期会让通用层继续累积非通用代码

### 1.2 解耦目标

`sip-common` 真正只含 SIP 标准协议相关的代码：

- 不出现 `Gb`、`GB28181`、`Catalog`、`MobilePosition`、`gbproxy` 字样
- `SipUtils.parseSdp()` 返回标准 `SdpSessionDescription`，GB 扩展（`y=`、`f=`、`ssrc`）下沉到 `gb28181-common`
- 校时、订阅、UserAgent 等通用能力剥离协议特定语义

### 1.3 1.3.0 版本策略：BREAKING CHANGES

本次为 1.3.0 主版本升级，**不保留任何兼容逻辑**：

- 配置 key 直接替换（不双 key 兜底）
- 默认值直接修改（不通过 auto-config 还原历史值）
- API 签名直接变更（编译期暴露所有调用方）
- 死代码直接删除（不为"未来可能用到"保留）

业务方升级路径：跟随 CHANGELOG 中 Migration 段一次性修复。

### 1.4 非目标

- **不**重命名 Maven artifact（`sip-common` 名字保留）
- **不**改动 `gb28181-client` / `gb28181-server` 的对外注解（`@EnableSipClient` / `@EnableSipServer` 保留）
- **不**为了解耦而牺牲性能或增加运行时复杂度

---

## 二、问题清单

按影响范围分级：

### 2.1 P0：硬编码导致的强耦合（必改）

| # | 位置 | 问题 |
|---|------|------|
| 1 | [SipUtils.java:188](../sip-common/src/main/java/io/github/lunasaw/sip/common/utils/SipUtils.java#L188) | `parseSdp()` 强制返回 `GbSessionDescription` |
| 2 | [SipUtils.java:147](../sip-common/src/main/java/io/github/lunasaw/sip/common/utils/SipUtils.java#L147) | `generateGB28181Code()` 出现在通用工具类 |
| 3 | [GbSessionDescription.java](../sip-common/src/main/java/io/github/lunasaw/sip/common/entity/GbSessionDescription.java) | GB 专用 SDP（含 ssrc、mediaDescription）放在通用层 |
| 4 | [GbSipDate.java](../sip-common/src/main/java/io/github/lunasaw/sip/common/entity/GbSipDate.java) | GB 专用日期格式放在通用层 |

### 2.2 P1：包含 GB 业务概念（建议改）

| # | 位置 | 问题 |
|---|------|------|
| 5 | [SubscribeHolder.java](../sip-common/src/main/java/io/github/lunasaw/sip/common/subscribe/SubscribeHolder.java) | `catalogMap` / `mobilePositionMap` / `putCatalogSubscribe` 等业务字段写在通用层。**全仓库零调用，确认为死代码** |
| 6 | [NtpTimeSyncScheduler.java:19,32](../sip-common/src/main/java/io/github/lunasaw/sip/common/service/impl/NtpTimeSyncScheduler.java#L19) | 配置 key 是 `sip.gb28181.time-sync`，与类所在包不一致（`sip.common`）。全仓库 yml/properties 实际无人配置此 key |
| 7 | [SipRequestUtils.java:285](../sip-common/src/main/java/io/github/lunasaw/sip/common/utils/SipRequestUtils.java#L285) | UserAgent 默认值写死 `"gbproxy"` |
| 8 | [Constant.java:13](../sip-common/src/main/java/io/github/lunasaw/sip/common/constant/Constant.java#L13) | `AGENT = "LunaSaw-GB28181-Proxy"` 硬编码到通用层 |

### 2.3 P2：注释和文档（清理类）

| # | 位置 | 问题 |
|---|------|------|
| 9 | [SipCommonProperties.java:8](../sip-common/src/main/java/io/github/lunasaw/sip/common/config/SipCommonProperties.java#L8) | 类注释 `"GB28181通用配置属性类"` |
| 10 | [SipRequestProcessorAbstract.java:100](../sip-common/src/main/java/io/github/lunasaw/sip/common/transmit/event/request/SipRequestProcessorAbstract.java#L100) | 注释 `"// GB28181协议要求：..."` |
| 11 | [DynamicTask.java:104](../sip-common/src/main/java/io/github/lunasaw/sip/common/utils/DynamicTask.java#L104) | 注释中提及 `cycleForCatalog` |

---

## 三、详细解耦方案

### 3.1 SDP 解析解耦（P0，影响最大）

**当前**：`SipUtils.parseSdp()` 返回 `GbSessionDescription`，所有调用方拿到的都是 GB 类型。

调用方 4 处（v1.1 漏列了 `InviteResponseProcessor`）：
- `gb28181-client/InviteRequestProcessor.java`
- `gb28181-server/ServerInviteRequestProcessor.java`
- `gb28181-server/InviteResponseProcessor.java`（line 100，未做强转，返回类型变化对其透明，但 import 需更新）
- `sip-common/test/SipUtilsTest.java`（2 个测试方法，迁移到 gb28181-common）

#### 步骤 1：迁移实体类

| 当前位置 | 目标位置 |
|---------|---------|
| `sip-common/.../entity/GbSessionDescription.java` | `gb28181-common/.../entity/sdp/GbSessionDescription.java` |
| `sip-common/.../entity/GbSipDate.java` | `gb28181-common/.../entity/GbSipDate.java` |

#### 步骤 2：拆分 `parseSdp` 方法

`sip-common/SipUtils.parseSdp()` 改为只解析 SIP 标准 SDP，**直接删除 y=/f= 剥离逻辑**：

```java
// sip-common：返回标准 SDP，不含 GB 扩展
public static SdpSessionDescription parseSdp(String sdpStr) {
    SessionDescription sdp = SipRequestUtils.createSessionDescription(sdpStr);
    return SdpSessionDescription.getInstance(sdp);
}
```

`gb28181-common` 新增 `GbSdpUtils.parseGbSdp()`，**y=/f= 剥离逻辑整体迁过来**：

```java
// gb28181-common：剥离 GB 扩展字段后调用 sip-common 解析标准部分
public class GbSdpUtils {
    public static GbSessionDescription parseGbSdp(String sdpStr) {
        String ssrc = null, mediaDescription = null;
        StringBuilder standard = new StringBuilder();
        for (String line : sdpStr.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("y=")) {
                ssrc = trimmed.substring(2);
            } else if (trimmed.startsWith("f=")) {
                mediaDescription = trimmed.substring(2);
            } else {
                standard.append(trimmed).append("\r\n");
            }
        }
        SdpSessionDescription base = SipUtils.parseSdp(standard.toString());
        return GbSessionDescription.getInstance(base.getBaseSdb(), ssrc, mediaDescription);
    }
}
```

#### 步骤 3：迁移 `generateGB28181Code` / `genSsrc`

合并到既有 [GbUtil.java](../gb28181-common/src/main/java/io/github/lunasaw/gb28181/common/entity/utils/GbUtil.java)（已存在 `generateGbCode` 等方法），保持工具集中。

#### 步骤 4：调用方更新（4 处）

```java
// InviteRequestProcessor.java（gb28181-client）
GbSessionDescription sdp = GbSdpUtils.parseGbSdp(new String(request.getRawContent()));

// ServerInviteRequestProcessor.java（gb28181-server）同上
// InviteResponseProcessor.java（gb28181-server）：仅更新 import，不需改逻辑

// InviteRequest.java（gb28181-server）：SipUtils.genSsrc → GbUtil.genSsrc（2 处）
```

> 不写兼容层，编译器会一次性暴露所有调用方。

---

### 3.2 死代码清理：SubscribeHolder（P1）

**核对结论**：`SubscribeHolder` 与 `SubscribeTask` 在整个 monorepo（含 gb28181-client/server/test）**零调用**。

**方案**：**直接删除**两个文件，不做下沉。理由：
1. 无任何调用方，下沉到 gb28181-server 等同于把死代码搬家
2. 真正的订阅持有器需求出现时，再按 GB 业务模型重新设计（届时可参考 git history）
3. `SubscribeInfo`（SIP 标准 SUBSCRIBE/NOTIFY 概念）继续保留在 sip-common，被 SipSender / 多个 Builder 深度依赖

**`DynamicTask`**：真正通用（任何延迟任务都能用），保留在 `sip-common`。

---

### 3.3 校时配置 key 直接替换（P1）

**现状**：

```java
@ConditionalOnProperty(prefix = "sip.gb28181.time-sync", ...)
@Scheduled(fixedRateString = "#{${sip.gb28181.time-sync.ntp-sync-interval:3600} * 1000}")
```

**核对结果**：全仓库 yml/properties **无任何 `sip.gb28181.time-sync.*` 实际配置**——这个 key 从未被业务方使用，没有兼容包袱。

**方案**：直接替换为 `sip.common.time-sync.*`，与 `SipCommonProperties` 前缀对齐。

```java
// NtpTimeSyncScheduler.java
@ConditionalOnProperty(
    prefix = "sip.common.time-sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Scheduled(fixedRateString = "#{${sip.common.time-sync.ntp-sync-interval:3600} * 1000}")
```

附加项：
- `application.yml` 示例改为新 key
- CHANGELOG 标注：`Removed: sip.gb28181.time-sync.*`

> 不再提供双 key 兜底、不打 WARN 日志、不需 `EnvironmentPostProcessor`。

---

### 3.4 UserAgent / Constant 通用化（P1）

**现状**：

```java
// SipRequestUtils.java:285
return createUserAgentHeader("gbproxy");

// Constant.java:13
public static final String AGENT = "LunaSaw-GB28181-Proxy";
```

**方案**：sip-common 默认值统一改为通用值 `"sip-proxy"`。**不再通过 `Gb28181CommonAutoConfig` 还原历史值**。GB 接入方如需要保持原 UserAgent，通过 `sip.common.user-agent` 配置覆盖。

```java
// sip-common/SipCommonProperties.java
@Data
public class SipCommonProperties {
    /** UserAgent 头默认值，可通过 sip.common.user-agent 覆盖 */
    private String userAgent = "sip-proxy";
    // ... 其余字段
}

// sip-common/Constant.java
public static final String AGENT = "sip-proxy";

// sip-common/SipRequestUtils.java:285
return createUserAgentHeader("sip-proxy");
```

**关键**：`SipMessageTransmitter:87` 与 `FromDevice:36` 必须改为优先读取 `SipCommonProperties.userAgent`，否则配置项不会生效。注入 Spring 上下文持有器（如 `ApplicationContextHolder`）或在 sip-common 自动配置中提供静态访问点：

```java
// sip-common/SipMessageTransmitter.java:87
String agent = SipCommonContextHolder.getUserAgent(); // 读 SipCommonProperties.userAgent，未注入时退化为 Constant.AGENT
message.addHeader(SipRequestUtils.createUserAgentHeader(agent));

// sip-common/FromDevice.java:36
fromDevice.setAgent(SipCommonContextHolder.getUserAgent());
```

`SipCommonContextHolder` 由 sip-common 自动配置注入 `SipCommonProperties` 引用；未启用 Spring 上下文（如纯单元测试）时返回 `Constant.AGENT` 兜底。

效果：
- 所有接入方 UserAgent 默认 `"sip-proxy"`（**对外可见行为变化，BREAKING**）
- 任何接入方都可通过 `sip.common.user-agent=LunaSaw-GB28181-Proxy` 还原

> 不创建 `Gb28181Constant.DEFAULT_AGENT`，不在 auto-config 中注入覆盖 Bean。

---

### 3.5 注释清理（P2）

| 文件 | 改动 |
|------|------|
| `SipCommonProperties.java:8` | `"GB28181通用配置属性类"` → `"SIP 协议通用配置属性类"` |
| `SipRequestProcessorAbstract.java:100` | `"// GB28181协议要求：..."` → `"// SIP 标准要求：..."` |
| `DynamicTask.java:104` | 移除 `cycleForCatalog` 字样 |

---

## 四、实体迁移清单

### 4.1 从 `sip-common` 迁出

| 文件 | 目标 | 备注 |
|------|---------|------|
| `entity/GbSessionDescription.java` | 迁入 `gb28181-common/entity/sdp/` | — |
| `entity/GbSipDate.java` | 迁入 `gb28181-common/entity/` | 仅 `ServerRegisterRequestProcessor.java:191` 引用 |
| `subscribe/SubscribeHolder.java` | **直接删除** | 全仓库零调用 |
| `subscribe/SubscribeTask.java` | **直接删除** | 仅 `SubscribeHolder` 引用 |

> ⚠️ **`SubscribeInfo` 不动**：在 sip-common 内 `SipSender` / `AbstractSipRequestBuilder` / `SipRequestBuilderFactory` / `SubscribeRequestBuilder` / `NotifyRequestBuilder` / `AbstractSipRequestStrategy` 多处引用，且 SUBSCRIBE/NOTIFY 是 SIP 标准动词。下沉将形成 `sip-common → gb28181-server` 反向依赖。

### 4.2 在 `sip-common` 修改

| 文件 | 改动 |
|------|------|
| `utils/SipUtils.java` | `parseSdp` 返回 `SdpSessionDescription`（删除 y=/f= 剥离）；删除 `generateGB28181Code`、`genSsrc` |
| `utils/SipRequestUtils.java` | UserAgent 默认值 `"gbproxy"` → `"sip-proxy"` |
| `constant/Constant.java` | `AGENT` 默认值 `"LunaSaw-GB28181-Proxy"` → `"sip-proxy"` |
| `service/impl/NtpTimeSyncScheduler.java` | 配置 key 直接替换为 `sip.common.time-sync.*` |
| `config/SipCommonProperties.java` | 新增 `userAgent` 字段（默认 `"sip-proxy"`）；类注释去 GB |
| `config/SipCommonContextHolder.java`（新增） | 静态持有 `SipCommonProperties`，提供 `getUserAgent()`，未注入时返回 `Constant.AGENT` |
| `transmit/SipMessageTransmitter.java:87` | `Constant.AGENT` → `SipCommonContextHolder.getUserAgent()` |
| `entity/FromDevice.java:36` | `Constant.AGENT` → `SipCommonContextHolder.getUserAgent()` |
| `transmit/event/request/SipRequestProcessorAbstract.java` | 注释清理 |
| `utils/DynamicTask.java` | 注释清理 |

### 4.3 在 `gb28181-common` 新增

| 文件 | 内容 |
|------|------|
| `entity/sdp/GbSessionDescription.java` | 从 sip-common 迁入 |
| `entity/GbSipDate.java` | 从 sip-common 迁入 |
| `entity/utils/GbSdpUtils.java` | 新增，含 `parseGbSdp(String)`（迁入 y=/f= 剥离逻辑） |
| `entity/utils/GbUtil.java` 追加 | `generateGB28181Code`、`genSsrc` |

### 4.4 调用方更新

| 文件 | 改动 |
|------|------|
| `gb28181-client/InviteRequestProcessor.java` | `(GbSessionDescription) SipUtils.parseSdp(...)` → `GbSdpUtils.parseGbSdp(...)`（去掉强转） |
| `gb28181-server/ServerInviteRequestProcessor.java` | 同上 |
| `gb28181-server/InviteResponseProcessor.java` | 仅更新 `GbSessionDescription` import 路径（不强转，逻辑无需改） |
| `gb28181-server/ServerRegisterRequestProcessor.java` | `import GbSipDate` 路径更新 |
| `gb28181-server/entity/InviteRequest.java` | `SipUtils.genSsrc(userId)` → `GbUtil.genSsrc(userId)`（line 62, 80） |
| `sip-common/test/.../SipUtilsTest.java` | 删除涉及 `GbSessionDescription` 强转、`generateGB28181Code`、`genSsrc` 的 6 个用例；在 `gb28181-common` 新建 `GbSdpUtilsTest` / `GbUtilTest` 接管 |

### 4.5 配置文件

| 文件 | 改动 |
|------|------|
| 各模块 `application.yml` 示例 | `sip.gb28181.time-sync.*` → `sip.common.time-sync.*` |

---

## 五、模块间依赖与边界

解耦后的依赖图（与现状一致，但代码归属更清晰）：

```
gb28181-test
    ├── gb28181-client ──┐
    │                    ├── gb28181-common ── sip-common
    └── gb28181-server ──┘                          ↑
                                                    │
                                          只含 SIP 标准协议
                                          可独立用于其他扩展协议
```

**边界规则**（Code Review 检查项）：

| 模块 | 允许出现的关键词 | 禁止出现的关键词 |
|------|--------------|--------------|
| `sip-common` | SIP, Sdp, Transaction, Subscribe, Registry | GB28181, gb28181, Gb, gbproxy, Catalog, MobilePosition |
| `gb28181-common` | 所有 | 业务编排（仅 JAXB 实体 + 工具） |
| `gb28181-server` | 所有 | — |
| `gb28181-client` | 所有 | — |

CI grep 校验脚本（白名单式精确匹配，避免误报合法标识符）：

```bash
# scripts/check-sip-common-purity.sh
set -euo pipefail

PATTERN='gb28181|GB28181|gbproxy|Catalog|MobilePosition|GbSession|GbSip|GbUtil'

# 排除注释行（行首可有空白 + // 或 *）
hits=$(grep -rEn "$PATTERN" sip-common/src/main/java --include="*.java" \
       | grep -vE '^\s*//|^\s*\*' || true)

if [ -n "$hits" ]; then
  echo "❌ sip-common 存在 GB28181 耦合："
  echo "$hits"
  exit 1
fi
echo "✅ sip-common 协议纯净"
```

集成方式：在 `pom.xml` 配置 `exec-maven-plugin` 在 `verify` 阶段调用，或独立 GitHub Action job。

---

## 六、实施计划

分 3 个阶段，每阶段独立可发布、独立可回滚。**总工作量约 5h**。

### 阶段一：清理 + 实体迁移（~1.5h，低风险）

| 任务 | 文件数 | 工作量 |
|------|-------|--------|
| **删除** `SubscribeHolder` + `SubscribeTask`（死代码） | 2 | 0.1h |
| 迁 `GbSessionDescription` 到 `gb28181-common/entity/sdp/` | 1 + 调用方 | 0.5h |
| 迁 `GbSipDate` 到 `gb28181-common/entity/` | 1 + 1 调用方 | 0.3h |
| 编译验证 + `mvn test` 全量回归 | — | 0.5h |

**风险**：低。仅是 import 路径变化与文件删除，编译器全程兜底。

### 阶段二：核心工具方法解耦（~2.5h，中风险）

| 任务 | 文件数 | 工作量 |
|------|-------|--------|
| `SipUtils.parseSdp` 改返回 `SdpSessionDescription`，**直接删除** y=/f= 剥离逻辑 | 1 | 0.5h |
| `gb28181-common` 新增 `GbSdpUtils.parseGbSdp`（迁入 y=/f= 剥离逻辑） | 1 | 0.5h |
| 迁移 `generateGB28181Code` / `genSsrc` 到 `GbUtil` | 1 + 2 处调用 | 0.3h |
| 4 处 INVITE Processor + InviteRequest 切换调用 | 4 | 0.4h |
| sip-common 测试用例迁移（`SipUtilsTest` 6 个 GB case → `GbSdpUtilsTest` / `GbUtilTest`），sip-common 新增 `parseSdp` 标准 SDP 用例保覆盖率 | 1 + 新建 2 | 0.6h |
| 集成测试回归（`gb28181-test/InvitePlayFlowTest` 等） | — | 0.3h |

**风险**：中。`parseSdp` 是底层方法，调用方虽只有 4 处，但 SDP 字段处理细节多，需要专门回归测试 `gb28181-test/InvitePlayFlowTest`。

### 阶段三：配置与命名清理（~1h，低风险）

| 任务 | 文件数 | 工作量 |
|------|-------|--------|
| 校时配置 key **直接替换**：`sip.gb28181.time-sync.*` → `sip.common.time-sync.*` | 1 | 0.1h |
| `SipCommonProperties.userAgent` 字段 + sip-common 默认值改 `"sip-proxy"` | 2 | 0.2h |
| `Constant.AGENT` / `SipRequestUtils:285` 默认值改 `"sip-proxy"` | 2 | 0.1h |
| 注释清理 | 3 | 0.2h |
| CI grep 检测脚本接入 | 1 | 0.2h |
| CHANGELOG 撰写（`BREAKING CHANGES` 段） | 1 | 0.2h |

**风险**：低。

---

## 七、对接入方的影响（BREAKING CHANGES）

| 变更点 | 接入方感知 | 迁移方式 |
|--------|----------|---------|
| `GbSessionDescription` 移到 `gb28181-common` | 编译期 import 路径错误 | IDE 自动修复 import |
| `SipUtils.parseSdp()` 返回类型不再是 `GbSessionDescription` | 强转处会运行时异常；如果在 try-catch 中可能被掩盖 | **改用 `GbSdpUtils.parseGbSdp()`**，编译期签名变更会强制迁移 |
| `generateGB28181Code` / `genSsrc` 迁到 `GbUtil` | 调用方需更新 import | IDE 自动修复 |
| 校时配置 key `sip.gb28181.time-sync.*` 移除 | 旧 key 不再生效 | 改为 `sip.common.time-sync.*`（核对仓库无人使用此 key，预期影响极小） |
| UserAgent 默认值 `"LunaSaw-GB28181-Proxy"` → `"sip-proxy"` | 网络对端看到的 UA 头变更 | 如需保持原值：`sip.common.user-agent=LunaSaw-GB28181-Proxy` |
| `SubscribeHolder` / `SubscribeTask` 删除 | 编译错误 | 仓库核对确认无调用方；如有外部业务方依赖，自行复制保留 |

**版本号**：1.3.0。CHANGELOG 字段：

- **BREAKING CHANGES**：
  - `SipUtils.parseSdp()` 返回类型变更（`GbSessionDescription` → `SdpSessionDescription`）
  - `SipUtils.generateGB28181Code` / `genSsrc` 迁移到 `gb28181-common/GbUtil`
  - `GbSessionDescription` / `GbSipDate` import 路径变更
  - 配置 key `sip.gb28181.time-sync.*` 替换为 `sip.common.time-sync.*`
  - 默认 UserAgent 由 `"LunaSaw-GB28181-Proxy"` 改为 `"sip-proxy"`
- **Removed**：`SubscribeHolder`、`SubscribeTask`（未启用的死代码）
- **Migration**：列出 IDE 可自动修复的 import 变化清单与配置 key 替换映射

---

## 八、未来扩展性验证

完成解耦后，应该能用 `sip-common` 独立支持非 GB28181 协议。验证方式：

新建一个 demo 模块（不并入 release），仅依赖 `sip-common`，实现一个简单的 SIP REGISTER + MESSAGE 流程：

```
demo-sip-trunk
    └── sip-common  (无需 gb28181-common / gb28181-server)
```

如果该 demo 能编译通过、运行 REGISTER + 自定义 MESSAGE 流程，即证明解耦完成。

---

## 九、不在本方案范围内的事项

以下耦合**不属于"协议耦合"**，不在本方案改造范围：

- `Device` / `FromDevice` / `ToDevice` 实体在 `sip-common`：SIP 通用寻址概念
- `DeviceSessionCache` 接口名带 "Device"：SIP 寻址抽象
- `SipTransactionRegistry` 中的事务管理：纯 SIP 协议层
- `ServerCommandSender` / `ClientCommandSender` 在 `gb28181-server` / `gb28181-client`：本来就是 GB 专有，符合分层
- `EnableSipProxy` 注解：核对确认无 GB 概念，注释引用的均为 `ClientDeviceSupplier` / `ServerDeviceSupplier` 等 SIP 通用接口

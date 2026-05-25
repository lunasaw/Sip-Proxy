# 前端设备控制协议（GBT-28181-2022 §A.3）实现方案

> 版本：v1.0 → **v1.6.0 已交付** | 日期：2026-05-25 | 状态：**实施完成（historical record）**
>
> **实施结果**：本方案 Stage 1-4 已于 2026-05-25 全量交付，详见 [PROTOCOL-LAYERING-MATRIX.md §九 v1.6.0 条目](PROTOCOL-LAYERING-MATRIX.md)。
> - **代码**：3469 → 1331 LOC（净减 2138 行）
> - **覆盖率**：§A.3 子节高层 API 1/7 → 7/7
> - **测试**：29 单元测试 + 6 端到端 FlowTest，全量回归 `gb28181-test` 59/59 GREEN
> - **决策项**：用户选择「无兼容期」破坏性升级，`PtzUtils` / `PtzCmdEnum` 直接删除，callers 同步迁移
>
> 关联：[GBT-28181-2022.md §A.3](GBT-28181-2022.md)、[PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md)、[LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md)
>
> 本文档以**代码事实**为准。下文为实施前的方案与审计依据，保留作历史记录。

---

## 一、协议范围与分层定位

GBT-28181-2022 §A.3「前端设备控制协议」定义了 **8 字节二进制 PTZ 控制指令**（即 `<PTZCmd>` XML 标签内承载的 16 位十六进制字符串），分 6 个子节：

| 节号 | 主题 | 指令首字节 | 子操作数 |
|---|---|---|---|
| §A.3.1 | 指令格式（8 字节通用结构） | A5H | — |
| §A.3.2 | PTZ 指令（云台 + 变倍） | 字节 4 高 2 位=00 | 7 种基础 + 任意组合 |
| §A.3.3 | FI 指令（光圈 + 聚焦） | 字节 4 高 2 位=01 | 4 种基础 + 任意组合 |
| §A.3.4 | 预置位指令（设置/调用/删除） | 81H/82H/83H | 3 |
| §A.3.5 | 巡航指令（5 种） | 84H~88H | 5 |
| §A.3.6 | 扫描指令（4 种） | 89H/8AH | 4 |
| §A.3.7 | 辅助开关控制（2 种） | 8CH/8DH | 2 |

**协议分层定位（与 [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md) 三层映射对应）**：

§A.3 不是独立的 SIP 信令层协议（它没有 cmdType / dispatcher key），而是 **§A.2.3.1.2「摄像机云台控制 PTZCmd」命令的 payload 编码格式**。也就是说：

```
平台 → 设备的 PTZ 控制信令链路：
  ServerCommandSender.deviceControlPtzCmd(deviceId, hexString)
    ↓ SIP MESSAGE method
  rootType=Control / cmdType=DeviceControl / inner element <PTZCmd>16-char hex</PTZCmd>
    ↓ DeviceControlMessageHandler
  ControlListener.onPtz(...)
```

**§A.3 整体属于 [LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md) 的 L0 协议解析层之下的「编码 utility 层」**，不引入新的 listener 接口或 dispatcher key。本方案仅梳理"如何把 8 字节二进制规则正确序列化进 `<PTZCmd>` 字段"。

---

## 二、现状审计（2026-05-25 代码事实）

### 2.1 既有实现盘点

按 `gb28181-common/.../entity/control/instruction/` 目录扫描，共 **15 个文件 / 3469 行**：

| 文件 | LOC | 角色 | 状态 |
|---|---|---|---|
| `PTZInstructionFormat.java` | 217 | 8 字节指令模型（§A.3.1） | ✅ 实现完整、API 可用 |
| `builder/PTZInstructionBuilder.java` | 364 | 流式构建器（§A.3.2-7 全套） | ✅ 实现完整、API 可用 |
| `manager/PTZInstructionManager.java` | 308 | 指令分类/查询管理 | ✅ 实现 |
| `serializer/PTZInstructionSerializer.java` | 236 | 二进制 ↔ JSON / hex 序列化 | ⚠️ 过度设计（详见 §2.3） |
| `crypto/PTZInstructionCrypto.java` | 290 | AES-GCM 加密 | ❌ **协议未要求**（详见 §2.3） |
| `examples/PTZInstructionExamples.java` | 401 | 使用示例（含 main） | ❌ 应迁移到 doc/ 或 test/ |
| `PTZInstructionDemo.java` | 54 | 演示（含 main） | ❌ 死代码 |
| `PTZInstructionCoreValidation.java` | 382 | 验证（含 main） | ❌ 应改为 JUnit |
| `PTZInstructionCompleteValidation.java` | 467 | 完整验证（含 main） | ❌ 应改为 JUnit |
| `enums/PTZControlEnum.java` | 193 | §A.3.2 枚举 | ✅ |
| `enums/FIControlEnum.java` | 147 | §A.3.3 枚举 | ✅ |
| `enums/PresetControlEnum.java` | 71 | §A.3.4 枚举 | ✅ |
| `enums/CruiseControlEnum.java` | 101 | §A.3.5 枚举 | ✅ |
| `enums/ScanControlEnum.java` | 138 | §A.3.6 枚举 | ✅ |
| `enums/AuxiliaryControlEnum.java` | 100 | §A.3.7 枚举 | ✅ |

### 2.2 生产线路上实际使用的 PTZ 实现

`ServerCommandSender` 高层 API 实际使用的是 **`gb28181-common/.../utils/PtzUtils.java`**（独立的 50 行老工具）：

```java
// gb28181-common/utils/PtzUtils.java
public static String getPtzCmd(int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed) {
    StringBuilder builder = new StringBuilder("A50F01");  // 硬编码组合码1=0F + 地址低8位=01
    // ... 拼装字节 4-7 + 校验码
}

// gb28181-server/.../ServerCommandSender.java:203
public String deviceControlPtzCmd(String deviceId, PtzCmdEnum ptzCmdEnum, Integer speed) {
    return deviceControlPtzCmd(deviceId, PtzUtils.getPtzCmd(ptzCmdEnum, speed));
}
```

`PtzUtils` + `PtzCmdEnum`（10 个方向枚举）只覆盖 **§A.3.2 PTZ 基础指令**的子集（缺失：组合速度差异化、字节 7 高 4 位变焦速度细化）。

### 2.3 关键发现：代码孤岛

`grep -rIin "PTZInstructionFormat\|PTZInstructionBuilder\|PTZInstructionManager"` 在 `gb28181-server/src/main` / `gb28181-client/src/main` / `gb28181-test/src` **零结果** —— 3469 行 `instruction/*` 子树**没有被任何生产代码或集成测试引用**。

具体问题：

1. **零生产引用**：`ServerCommandSender.deviceControlPtzCmd` 走的是老 `PtzUtils.getPtzCmd`，新 `PTZInstructionBuilder` 没有任何 caller。
2. **零 JUnit 测试**：`gb28181-common/src/test/.../control/instruction/` 目录不存在，4 个 `*Validation`/`*Demo` 文件用 `main()` 做断言，违反 [LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md) 与 voglander 项目「Manager 类必须用 `@SpringBootTest` 集成测试」的规范，��无法纳入 JaCoCo 80% 覆盖率门槛。
3. **过度设计**：`PTZInstructionCrypto` 给二进制指令加 AES-GCM 加密 —— GBT-28181-2022 §A.3 全文未要求加密，§8.2 数据加密走的是 IPSec/TLS 传输层方案（详见 [PROTOCOL-LAYERING-MATRIX.md §0(C)](PROTOCOL-LAYERING-MATRIX.md)），与 8 字节 payload 无关。
4. **示例污染主源码**：`PTZInstructionExamples.java`（401 行）以 `@Slf4j` + `main()` 形式存在于 `src/main`，按规范应在 `doc/` 或 `src/test`。
5. **高层 API 缺失**：`ServerCommandSender` 只有 `deviceControlPtzCmd` 一个 §A.3 入口；§A.3.3 FI / §A.3.4 Preset / §A.3.5 Cruise / §A.3.6 Scan / §A.3.7 Aux 全部需要业务方自己拼 hex 串再调 `deviceControlPtzCmd(deviceId, hex)`。

### 2.4 §A.3 协议覆盖度真实数据

| 子节 | 二进制规则代码 | 高层 API（ServerCommandSender） | JUnit 测试 |
|---|---|---|---|
| §A.3.1 8 字节格式 | ✅ `PTZInstructionFormat` + 老 `PtzUtils` 重复实现 | — | ❌ |
| §A.3.2 PTZ | ✅ Builder + 老 PtzUtils | ✅ `deviceControlPtzCmd` | ❌ |
| §A.3.3 FI | ✅ Builder | ❌ 无 | ❌ |
| §A.3.4 Preset | ✅ Builder | ❌ 无（仅 `devicePresetQuery` 是查询，非控制） | ❌ |
| §A.3.5 Cruise | ✅ Builder | ❌ 无 | ❌ |
| §A.3.6 Scan | ✅ Builder | ❌ 无 | ❌ |
| §A.3.7 Aux | ✅ Builder | ❌ 无 | ❌ |

**结论：协议规则代码"看起来全套"但实际生产 1/7 子节可用，业务方对 FI/Preset/Cruise/Scan/Aux 5 类前端控制无可用入口。**

---

## 三、整理目标

围绕"代码事实可用、符合架构规范、协议 100% 覆盖" 三条主线：

1. **统一二进制编码源**：删除 `PtzUtils` 老实现，所有 PTZ hex 生成统一走 `PTZInstructionBuilder`
2. **打通生产线路**：`ServerCommandSender` 增加 5 类高层 API（FI / Preset / Cruise / Scan / Aux），业务方一行调用搞定
3. **JUnit 化测试覆盖**：删除 4 个 `main()` validation/demo 类，改为 `gb28181-common/src/test/.../instruction/` 下的标准 JUnit 测试，纳入 JaCoCo 门槛
4. **删除过度设计**：移除 `PTZInstructionCrypto`（协议未要求）+ `PTZInstructionExamples`（迁移到 doc/）
5. **架构规范对齐**：保持 sip-common 协议层纯净（§A.3 是 GBT 协议特定，必须在 gb28181-common 内）；新 API 不引入 L1 事件 / L2 listener（§A.3 是单向下行控制，无应答业务语义）

---

## 四、落地方案（按 TDD 顺序）

### Stage 1: TDD 测试先行（红相）

**目标**：给 `PTZInstructionFormat` / `PTZInstructionBuilder` / 6 个 enum 写完整的 JUnit 测试，参照 GBT-28181-2022 §A.3 表 A.5/A.7/A.8/A.9/A.10/A.11 的官方"指令举例"做断言。

**新增文件**（`gb28181-common/src/test/java/.../entity/control/instruction/`）：

| 文件 | 测试范围 | 估算 LOC |
|---|---|---|
| `PTZInstructionFormatTest.java` | 8 字节构造、校验码计算、地址 0x000-0xFFF 边界、hex 串往返、`fromByteArray`/`toByteArray` 一致性 | ~120 |
| `PTZInstructionBuilderTest.java` | 6 类指令构建对照表 A.5/A.7/A.8/A.9/A.10/A.11 验证 hex 输出 | ~250 |
| `PTZControlEnumTest.java` | §A.3.2 表 A.5 8 个示例（含组合 PTZ）→ enum 解析往返 | ~80 |
| `FIControlEnumTest.java` | §A.3.3 表 A.7 6 个示例 | ~60 |
| `PresetControlEnumTest.java` | §A.3.4 表 A.8 3 个 + 边界（presetNumber 1-255 / 0 拒绝） | ~40 |
| `CruiseControlEnumTest.java` | §A.3.5 表 A.9 5 个 + 字节 6/7 数据高低 4 位拼接 | ~80 |
| `ScanControlEnumTest.java` | §A.3.6 表 A.10 4 个 + 扫描组号边界 | ~50 |
| `AuxiliaryControlEnumTest.java` | §A.3.7 表 A.11 2 个 + 雨刷（switch=1）特化 | ~40 |
| `PtzUtilsLegacyCompatTest.java` | 老 `PtzUtils.getPtzCmd` 与新 `PTZInstructionBuilder` 对相同输入产生**完全相同** hex 输出（迁移前的等价性闸门） | ~80 |

**合计**：~800 LOC。

**断言模式**（使用 §A.3 spec 给出的真实数据）：

```java
@Test
@DisplayName("§A.3.2 表 A.5 序号 1：镜头变倍缩小（字节 4=20H，速度=8H）")
void zoomOutAtSpeed8() {
    String hex = PTZInstructionBuilder.create()
        .address(0x001)
        .addPTZControl(PTZControlEnum.ZOOM_OUT)
        .zoomSpeed(8)
        .buildToHex();
    // A5 0F 01 20 00 00 80 [checksum]
    // checksum = (0xA5 + 0x0F + 0x01 + 0x20 + 0x00 + 0x00 + 0x80) % 256 = 0x55
    assertThat(hex).isEqualToIgnoringCase("A50F0120000080A5");
}

@Test
@DisplayName("§A.3.4 表 A.8：调用预置位 5 号")
void invokePreset5() {
    String hex = PTZInstructionBuilder.create()
        .address(0x001)
        .addPresetControl(PresetControlEnum.GOTO_PRESET, 5)
        .buildToHex();
    // A5 0F 01 82 00 05 00 [checksum]
    assertThat(hex).startsWith("A50F0182000500");
}
```

### Stage 2: 高层 API 补全（绿相）

**目标**：`ServerCommandSender` 增加 5 类前端控制 API，`gb28181-test` 新增 `FrontEndControlFlowTest` 端到端验证。

**新增方法**（[ServerCommandSender.java](../gb28181-server/src/main/java/io/github/lunasaw/gbproxy/server/transmit/cmd/ServerCommandSender.java)）：

```java
/** GBT-28181-2022 §A.3.3 FI 控制（光圈 + 聚焦）。 */
public String deviceControlFI(String deviceId,
                                FIControlEnum.IrisDirection iris,
                                FIControlEnum.FocusDirection focus,
                                int focusSpeed, int irisSpeed) {
    String hex = PTZInstructionBuilder.create()
        .address(0x001)
        .addFIControl(iris, focus)
        .focusSpeed(focusSpeed)
        .irisSpeed(irisSpeed)
        .buildToHex();
    return deviceControlPtzCmd(deviceId, hex);
}

/** GBT-28181-2022 §A.3.4 预置位（设置/调用/删除）。 */
public String deviceControlPreset(String deviceId, PresetControlEnum action, int presetNumber) {
    String hex = PTZInstructionBuilder.create()
        .address(0x001)
        .addPresetControl(action, presetNumber)
        .buildToHex();
    return deviceControlPtzCmd(deviceId, hex);
}

/** GBT-28181-2022 §A.3.5 巡航指令（5 种）。 */
public String deviceControlCruise(String deviceId, CruiseControlEnum action,
                                    int groupNumber, int presetNumber, int speedOrStayTime) { ... }

/** GBT-28181-2022 §A.3.6 扫描指令（4 种）。 */
public String deviceControlScan(String deviceId, ScanControlEnum action, int groupNumber, int speed) { ... }

/** GBT-28181-2022 §A.3.7 辅助开关。 */
public String deviceControlAuxiliary(String deviceId, AuxiliaryControlEnum action, int switchNumber) { ... }
```

**新增端到端测试**（[gb28181-test/.../FrontEndControlFlowTest.java](../gb28181-test/src/test/java/io/github/lunasaw/gbproxy/test/FrontEndControlFlowTest.java)）：

参照已有的 `PtzPositionFlowTest` 模式，验证每类高层 API 触发 `ControlListener.onPtz` 回调，且回调收到的 `<PTZCmd>` hex 串经 `PTZInstructionFormat.fromHexString().isValid()` 校验通过。

```java
@Test
void deviceControlPreset_invokePreset_shouldReachClient() {
    CountDownLatch latch = new CountDownLatch(1);
    testClient.reset(latch);

    commandSender.deviceControlPreset(clientId, PresetControlEnum.GOTO_PRESET, 5);

    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    DeviceControlPtz received = (DeviceControlPtz) testClient.getLastCommand();
    String hex = received.getPtzCmd();

    PTZInstructionFormat parsed = PTZInstructionFormat.fromHexString(hex);
    assertThat(parsed.isValid()).isTrue();
    assertThat(parsed.getInstructionCode()).isEqualTo((byte) 0x82);
    assertThat(parsed.getData2()).isEqualTo((byte) 5);
}
```

### Stage 3: 死代码清理 + 等价性迁移

**删除**（违反规范的 4 个 `main()` 类 + 过度设计 + 主源码示例）：

| 文件 | LOC | 删除理由 |
|---|---|---|
| `PTZInstructionDemo.java` | 54 | 含 `main()` 演示，已被 Stage 1 单元测试覆盖 |
| `PTZInstructionCoreValidation.java` | 382 | 含 `main()` 验证，已被 Stage 1 单元测试覆盖 |
| `PTZInstructionCompleteValidation.java` | 467 | 含 `main()` 验证，已被 Stage 1 单元测试覆盖 |
| `examples/PTZInstructionExamples.java` | 401 | 主源码内的 `main()` 示例，迁移内容到本文档 §六「使用示例」 |
| `crypto/PTZInstructionCrypto.java` | 290 | GBT-28181-2022 §A.3 + §8.2 均未要求 8 字节指令加密，纯粹过度设计 |

**合计删除**：~1594 LOC。

**等价性迁移**（确保删除老 `PtzUtils` 后业务方调用不变）：

```java
// 老 API（删除前）
PtzUtils.getPtzCmd(PtzCmdEnum.LEFT, 100);

// 新 API（删除后等价替换 — 在 PtzUtils 内部实现委托）
public class PtzUtils {
    /** @deprecated v1.6.0 起统一走 PTZInstructionBuilder，本方法保留作为 thin shim */
    @Deprecated
    public static String getPtzCmd(PtzCmdEnum ptzCmdEnum, int speed) {
        return PTZInstructionBuilder.create()
            .address(0x001)
            .addPTZControl(...) // 由 PtzCmdEnum.cmdCode 反查 PTZControlEnum
            .horizontalSpeed(speed)
            .verticalSpeed(speed)
            .zoomSpeed(speed)
            .buildToHex();
    }
}
```

**`PtzUtilsLegacyCompatTest`（Stage 1 已写）确保**：上述委托产生与原 `PtzUtils.getPtzCmd` 字节级一致的 hex 输出，业务方升级 sip-proxy 1.6.0 时零 break。

### Stage 4: 矩阵文档同步

**更新** [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md)：

1. §0(D) 附录 B–O 表新增一行（如果还没有）：

   | 附录 | 性质 | 内容 | 状态 | 落点 |
   |---|---|---|---|---|
   | 附录 A.3 | 规范性 | 前端设备控制协议（8 字节二进制 PTZ 指令编码） | ✅ | `PTZInstructionBuilder` + `ServerCommandSender.deviceControl{Ptz,FI,Preset,Cruise,Scan,Auxiliary}` |

2. §A.2.3.1.2 摄像机云台控制 PTZCmd 行的「落点」追加：`<PTZCmd>` hex 编码遵循 §A.3.1-7（详见 `PTZInstructionBuilder`）。

3. §九 版本变更记录追加 v1.6.0 条目。

---

## 五、整体代价估算

| 阶段 | 新增 LOC | 删除 LOC | 净增 | 兼容性 |
|---|---|---|---|---|
| Stage 1 单元测试 | ~800 | 0 | +800 | 加性 |
| Stage 2 高层 API + 端到端测试 | ~250（API）+ ~150（test） | 0 | +400 | 加性 |
| Stage 3 死代码清理 | ~30（PtzUtils thin shim） | ~1594 | -1564 | `@Deprecated` 兼容期保留 PtzUtils API |
| Stage 4 矩阵文档 | ~20 行 markdown | 0 | — | — |
| **合计** | **~1080 Java + 20 md** | **~1594** | **-514** | 老 API 全部 deprecated 但不删，下个 major 版本（2.0.0）再清理 |

净减 514 行 Java + 800 行真测试覆盖率 + 5 个新高层 API + 9 个 JUnit 测试类 — 工程质量净改善。

---

## 六、§A.3 使用示例（迁移自 `PTZInstructionExamples`）

> 取代删除的 `examples/PTZInstructionExamples.java` 中 401 行 main() 示例代码。

### 6.1 §A.3.2 PTZ 基础控制

```java
// 单方向：云台向上以速度 100 运动（表 A.5 序号 3）
String hex = PTZInstructionBuilder.create()
    .address(0x001)
    .addPTZControl(PTZControlEnum.UP)
    .verticalSpeed(100)
    .buildToHex();

// 组合方向：右上 + 变倍缩小（表 A.5 序号 8 完整组合）
String comboHex = PTZInstructionBuilder.create()
    .address(0x001)
    .addPTZControl(PTZControlEnum.PanDirection.RIGHT,
                   PTZControlEnum.TiltDirection.UP,
                   PTZControlEnum.ZoomDirection.OUT)
    .horizontalSpeed(120)
    .verticalSpeed(80)
    .zoomSpeed(8)
    .buildToHex();

// 高层 API（推荐）
serverCommandSender.deviceControlPtzCmd(deviceId, PtzCmdEnum.UP, 100);
```

### 6.2 §A.3.3 FI 光圈/聚焦

```java
// 缩小光圈 + 聚焦远（表 A.7 序号 6 完整组合）
serverCommandSender.deviceControlFI(deviceId,
    FIControlEnum.IrisDirection.NARROW,
    FIControlEnum.FocusDirection.FAR,
    /*focusSpeed=*/ 100, /*irisSpeed=*/ 80);
```

### 6.3 §A.3.4 预置位

```java
// 设置预置位 5
serverCommandSender.deviceControlPreset(deviceId,
    PresetControlEnum.SET_PRESET, 5);

// 调用预置位 5
serverCommandSender.deviceControlPreset(deviceId,
    PresetControlEnum.GOTO_PRESET, 5);

// 删除预置位 5
serverCommandSender.deviceControlPreset(deviceId,
    PresetControlEnum.CLEAR_PRESET, 5);
```

### 6.4 §A.3.5 巡航

```java
// 加入巡航点：组 1，预置位 5
serverCommandSender.deviceControlCruise(deviceId,
    CruiseControlEnum.ADD_CRUISE_POINT, 1, 5, 0);

// 设置巡航速度：组 1，速度 50
serverCommandSender.deviceControlCruise(deviceId,
    CruiseControlEnum.SET_CRUISE_SPEED, 1, 0, 50);

// 设置巡航停留时间：组 1，停留 10 秒
serverCommandSender.deviceControlCruise(deviceId,
    CruiseControlEnum.SET_CRUISE_STAY_TIME, 1, 0, 10);

// 开始巡航：组 1
serverCommandSender.deviceControlCruise(deviceId,
    CruiseControlEnum.START_CRUISE, 1, 0, 0);

// 停止巡航：复用 PTZ 全 0 停止指令
serverCommandSender.deviceControlPtzCmd(deviceId, PtzCmdEnum.STOP, 0);
```

### 6.5 §A.3.6 扫描

```java
serverCommandSender.deviceControlScan(deviceId,
    ScanControlEnum.START_AUTO_SCAN, 1, 0);
serverCommandSender.deviceControlScan(deviceId,
    ScanControlEnum.SET_LEFT_BOUNDARY, 1, 0);
serverCommandSender.deviceControlScan(deviceId,
    ScanControlEnum.SET_RIGHT_BOUNDARY, 1, 0);
serverCommandSender.deviceControlScan(deviceId,
    ScanControlEnum.SET_SCAN_SPEED, 1, /*speed=*/ 50);
```

### 6.6 §A.3.7 辅助开关（雨刷等）

```java
// 开启雨刷（switch=1 = 雨刷标准约���）
serverCommandSender.deviceControlAuxiliary(deviceId,
    AuxiliaryControlEnum.AUXILIARY_ON, 1);

// 关闭雨刷
serverCommandSender.deviceControlAuxiliary(deviceId,
    AuxiliaryControlEnum.AUXILIARY_OFF, 1);
```

---

## 七、协议层纯净性约束

按 [PROTOCOL-LAYERING-MATRIX.md](PROTOCOL-LAYERING-MATRIX.md) §十「维护承诺」，本方案严格遵守：

1. **§A.3 实现必须留在 `gb28181-common`**，sip-common 协议层不引入任何 PTZ/FI/Preset 字节码逻辑（CI 闸门 `scripts/check-sip-common-purity.sh` 不允许 `gb28181 / GB28181 / Catalog / MobilePosition` 等关键词出现在 sip-common，本方案不触发该规则）。
2. **不引入新 dispatcher key**：§A.3 是 §A.2.3.1.2 PTZCmd 命令的 inner payload 编码，复用既有 `Control / MESSAGE_DeviceControl` dispatch 入口。
3. **不引入新 listener 接口**：§A.3 是单向下行控制（平台→设备），无应答业务语义，复用既有 `ControlListener.onPtz`。

---

## 八、与 v1.5.6 已交付的关系

v1.5.6 完成了 GBT-28181-2022 附录 A **命令集** 100% 覆盖（详见 [PROTOCOL-LAYERING-MATRIX.md §0(A)](PROTOCOL-LAYERING-MATRIX.md)），但 §A.3 **二进制指令编码层** 不在该范围 —— 它是 §A.2.3.1.2 命令的 payload 子规则，与"协议命令是否覆盖"是正交的两个维度。

本方案补的是 **payload 编码工程质量**：从"代码孤岛 + 1/7 子节可用"提升到"统一编码源 + 7/7 子节高层 API + 9 个 JUnit 测试 + 老 API `@Deprecated` 兼容"。

---

## 九、实施次序与里程碑

按 TDD 顺序、每个 Stage 自包含可验证：

| 里程碑 | 内容 | 验证手段 |
|---|---|---|
| M1 | Stage 1 完成（9 个 JUnit 测试类，~800 LOC） | `mvn test -pl gb28181-common -Dtest='PTZInstruction*Test,Ptz*EnumTest,*LegacyCompatTest'` GREEN |
| M2 | Stage 2 完成（5 个高层 API + 1 FlowTest，~400 LOC） | `mvn test -pl gb28181-test -Dtest=FrontEndControlFlowTest` GREEN |
| M3 | Stage 3 完成（删 ~1594 LOC + PtzUtils thin shim） | 全量 `mvn test` GREEN（含 LegacyCompatTest 保护老 API） |
| M4 | Stage 4 完成（矩阵 v1.6.0 + 本文档归档为 historical） | `mvn install -Dgpg.skip=true` 全模块 BUILD SUCCESS |

---

## 十、问题与权衡

### Q1：为什么不直接删除 `PtzUtils` + 老 `PtzCmdEnum`？

A：`ServerCommandSender.deviceControlPtzCmd(deviceId, PtzCmdEnum, speed)` 是公开 API，业务方代码大量引用。一次性删除是 break change，应走 `@Deprecated` 兼容期 → 下个 major 版本再清理（与 v1.5.0 删除 4 个 client 业务接口的节奏一致）。

### Q2：为什么不保留 `PTZInstructionCrypto` 作为可选项？

A：协议未要求 + 8 字���明文 PTZ 命令在 SIP 信令中已经被 §8.2 IPSec/TLS 传输层加密保护，应用层再加 AES-GCM 是冗余。保留它会让业务方误以为这是协议要求，反而劣化合规判断。如未来真有应用层加密需求，由业务方在 sip-gateway 层叠加，不在 sip-proxy 范围。

### Q3：为什么不把 `PTZInstructionBuilder` 提升到 `ControlListener` 业务接口层？

A：§A.3 是编码细节、不是协议契约。业务方接收 `<PTZCmd>` hex 时（client 侧）通过 `ControlListener.onPtz(cmd)` 拿到的就是 `DeviceControlPtz.ptzCmd` 字符串，需要解码时业务方自行调 `PTZInstructionFormat.fromHexString(hex)` 即可。提升到 listener 会让 sip-proxy 替业务方做语义解释，违反「协议层不上诠释」的 [LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md) 约定。

### Q4：表 A.5/A.7 给的"hex 字符串举例"在哪里？

A：GBT-28181-2022 §A.3 给出的是「字节 4-7 的位值」举例，没有给完整 8 字节 hex 字符串。本方案 Stage 1 测试的"金标准 hex 串"通过遵守 §A.3.1 校验码规则（`(byte1+...+byte7) % 256`）+ 组合码 1 规则（`(0xA + 0x5 + 0x0) % 16 = 0xF`，所以组合码 1 固定为 `0x0F`）+ 地址低 8 位（按本方案约定取 `0x01`）三条规则推导出来，`PTZInstructionFormat.calculateChecksum()` 已实现这些规则，可由计算机生成 + 人工抽检几条对照 §A.3 表的字节 4 描述。

---

## 十一、变更影响域

| 模块 | 影响 | 说明 |
|---|---|---|
| `sip-common` | 0 | 不动，协议层纯净不被破坏 |
| `gb28181-common` | -1564 LOC + ~830 LOC test | 主源码净减、测试净增 |
| `gb28181-server` | +250 LOC | 5 个高层 API |
| `gb28181-client` | 0 | 客户端解析现有路径不变 |
| `gb28181-test` | +150 LOC | FrontEndControlFlowTest 端到端 |
| `doc/PROTOCOL-LAYERING-MATRIX.md` | +20 行 | §0(D) + §九版本日志 |
| 业务方代码 | 0 | 老 API 全部 `@Deprecated` 但保留实现 |

---

## 十二、决策项（待确认）

执行此方案前需要确认 1 个开放点：

**老 PtzUtils API 兼容期长度**：

- **方案 A（推荐）**：保留 `PtzUtils` + `PtzCmdEnum` 标 `@Deprecated`，下个 major 版本（2.0.0）删除。优点：业务方零 break。缺点：1.6.0 内有两套 API。
- **方案 B**：直接删除 `PtzUtils` + `PtzCmdEnum`，业务方一次性迁移到 `PTZInstructionBuilder` / `PTZControlEnum`。优点：API 单一。缺点：1.6.0 是 break release，需要业务方协同。

> 默认按 A 走（与 v1.5.x 历次升级节奏一致）。如选 B，Stage 3 删除范围扩大 ~50 LOC，业务方需要同步改 PR。

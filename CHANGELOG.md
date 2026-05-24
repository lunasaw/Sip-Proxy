# Changelog

本文档记录 sip-proxy 各版本的对外可见变更。版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [1.3.0] - 2026-05-24

### BREAKING CHANGES

本次为协议解耦主版本升级，**不保留兼容期逻辑**。详见
[doc/PROTOCOL-DECOUPLING-PLAN.md](doc/PROTOCOL-DECOUPLING-PLAN.md)。

- **`SipUtils.parseSdp()` 返回类型变更**：由 `GbSessionDescription` 变为标准
  `SdpSessionDescription`，y= / f= 字段剥离逻辑下沉到 gb28181-common。
  GB28181 接入方需改用 `GbSdpUtils.parseGbSdp()`。
- **`SipUtils.generateGB28181Code` / `SipUtils.genSsrc` 迁移**：从 sip-common
  迁到 `gb28181-common/io.github.lunasaw.gb28181.common.entity.utils.GbUtil`。
- **`GbSessionDescription` / `GbSipDate` 包路径变更**：
  - `io.github.lunasaw.sip.common.entity.GbSessionDescription`
    → `io.github.lunasaw.gb28181.common.entity.sdp.GbSessionDescription`
  - `io.github.lunasaw.sip.common.entity.GbSipDate`
    → `io.github.lunasaw.gb28181.common.entity.GbSipDate`
- **校时配置 key 替换**：`sip.gb28181.time-sync.*` → `sip.common.time-sync.*`
  （含 `.enabled`、`.ntp-sync-interval` 等所有子键）。
- **默认 UserAgent 变更**：由 `LunaSaw-GB28181-Proxy` 改为 `sip-proxy`。
  网络对端看到的 User-Agent 头会变化。
  如需保持原值，配置 `sip.common.user-agent: LunaSaw-GB28181-Proxy`。

### Added

- 新增 `SipCommonProperties.userAgent` 字段（默认 `sip-proxy`）。
- 新增 `SipCommonContextHolder`，为 `SipMessageTransmitter` /
  `FromDevice` 等静态调用点提供 `SipCommonProperties.userAgent` 的访问入口。
- `gb28181-common` 新增 `GbSdpUtils.parseGbSdp(String)`，承接原 sip-common
  的 GB SDP 解析职责。
- `gb28181-common/GbUtil` 新增 `generateGB28181Code` / `genSsrc`。
- 新增 CI 脚本 `scripts/check-sip-common-purity.sh`，校验 sip-common
  不含 GB28181 关键词。

### Removed

- 删除 `sip-common` 中的 `SubscribeHolder` / `SubscribeTask`（全仓库零调用，确认死代码）。
- 删除 `SipUtils.generateGB28181Code` / `SipUtils.genSsrc`。
- 删除 `SipUtils.parseSdp` 中的 y= / f= 剥离逻辑。
- 移除配置 key `sip.gb28181.time-sync.*`。

### Migration

| 变更点 | 迁移方式 |
|--------|---------|
| `GbSessionDescription` import | IDE 自动修复至 `io.github.lunasaw.gb28181.common.entity.sdp` |
| `GbSipDate` import | IDE 自动修复至 `io.github.lunasaw.gb28181.common.entity` |
| `SipUtils.parseSdp` 强转处 | 改用 `GbSdpUtils.parseGbSdp(...)`（编译期签名变更强制迁移） |
| `SipUtils.generateGB28181Code` / `genSsrc` | 改用 `GbUtil.generateGB28181Code` / `GbUtil.genSsrc` |
| 配置 `sip.gb28181.time-sync.*` | 改为 `sip.common.time-sync.*` |
| `SubscribeHolder` / `SubscribeTask` | 仓库核对确认无调用方；如有外部业务方依赖，自行复制保留 |
| 默认 UserAgent 还原 | 配置 `sip.common.user-agent: LunaSaw-GB28181-Proxy` |

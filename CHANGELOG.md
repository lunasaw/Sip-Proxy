# Changelog

本文档记录 sip-proxy 各版本的对外可见变更。版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [1.4.0] - 2026-05-24

### Added — GB/T 28181-2022 协议扩展

按 [doc/GBT-28181-2022-IMPLEMENTATION-PLAN.md](doc/GBT-28181-2022-IMPLEMENTATION-PLAN.md) 落地，13 个阶段全部完成：

**P0 优先级（独立流程，可单独跑通）**
- §9.13 设备软件升级：`DeviceUpgradeControl` 控制 + `UpgradeResultNotify` 结果通知
  - sender：`ServerCommandSender.deviceUpgrade(...)` / `ClientCommandSender.sendUpgradeResultNotify(...)`
  - 事件：`DeviceUpgradeResultEvent`
  - cmdType 新增：`DeviceUpgradeResult`
- §9.14 图像抓拍：`SnapShotConfig`（cmdType=DeviceConfig）+ `UploadSnapShotFinishedNotify`
  - sender：`ServerCommandSender.deviceSnapShot(...)` / `ClientCommandSender.sendSnapShotFinishedNotify(...)`
  - 事件：`DeviceSnapShotFinishedEvent` / `ClientSnapShotConfigEvent`
  - cmdType 新增：`UploadSnapShotFinished`

**P1 优先级（沿用现有 MessageCommandStrategy）**
- A.2.3.1.11 / A.2.4.13 / A.2.6.15 PTZ 精准控制 + 精确状态：
  - `DeviceControlPTZPrecise`、`PTZPositionQuery`、`PTZPositionResponse`
  - sender：`deviceControlPtzPrecise(...)`, `devicePtzPositionQuery(...)`, `sendPtzPositionResponse(...)`
  - 事件：`DevicePtzPositionEvent` / `ClientPtzPositionQueryEvent`
  - cmdType 新增：`PTZPosition`
- A.2.4.14 / A.2.6.16 / A.2.3.1.13 存储卡状态查询 + 格式化：
  - `SDCardStatusQuery`、`SDCardStatusResponse`、`DeviceControlSDCardFormat`
  - sender：`deviceSdCardStatusQuery(...)`, `deviceControlFormatSDCard(...)`, `sendSdCardStatusResponse(...)`
  - 事件：`DeviceSdCardStatusEvent` / `ClientSdCardStatusQueryEvent`
  - cmdType 新增：`SDCardStatus`

**P2 优先级（涉及新 CmdType 路由分支）**
- A.2.4.10 / A.2.6.12 / A.2.3.1.10 看守位查询 + 控制：
  - `HomePositionQuery`、`HomePositionResponse`（控制实体复用现有 `DeviceControlPosition`）
  - sender：`deviceControlHomePosition(...)`, `deviceHomePositionQuery(...)`, `sendHomePositionResponse(...)`
  - 事件：`DeviceHomePositionEvent` / `ClientHomePositionQueryEvent`
  - cmdType 新增：`HomePositionQuery`
- A.2.4.11 / A.2.4.12 / A.2.6.13 / A.2.6.14 巡航轨迹查询：
  - `CruiseTrackListQuery`、`CruiseTrackQuery`、`CruiseTrackListResponse`、`CruiseTrackResponse`
  - sender：`deviceCruiseTrackListQuery(...)`, `deviceCruiseTrackQuery(...)`, `sendCruiseTrackListResponse(...)`, `sendCruiseTrackResponse(...)`
  - 事件：`DeviceCruiseTrackEvent`（type=LIST/SINGLE）/ `ClientCruiseTrackListQueryEvent` / `ClientCruiseTrackQueryEvent`
  - cmdType 新增：`CruiseTrackListQuery`、`CruiseTrackQuery`
- A.2.3.1.7 / A.2.3.1.8 / A.2.3.1.9 强制关键帧 + 拉框放大/缩小 server sender 补齐
  - sender：`deviceControlIFrame(...)`, `deviceControlDragZoomIn(...)`, `deviceControlDragZoomOut(...)`
  - client 入站路由原已存在
- A.2.3.1.14 目标跟踪：
  - `DeviceControlTargetTrack`（含 TargetArea 内嵌结构）
  - sender：`deviceControlTargetTrack(...)`
  - client 接口扩展：`DeviceControlRequestHandler.handleTargetTrack(...)`
- §9.11.1 / §9.11.2 报警事件订阅与通知：
  - sender：`deviceAlarmSubscribe(...)`, `sendAlarmNotify(...)`
  - 客户端订阅入站：`SubscribeAlarmQueryMessageHandler` → `ClientAlarmSubscribeEvent`
  - **附带修复**：`SubscribeCommandStrategy` 现在正确从 CommandContext.extras 提取
    `subscribeInfo` 并透传，之前 SUBSCRIBE 请求会因 `subscribeInfo is null` 失败

**P3 优先级（多消息会话或外部依赖）**
- A.2.3.2 设备配置扩展（代表性子集）：OSD / VideoAlarmRecord / AlarmReport
  - 实体：`OsdConfig`、`VideoAlarmRecordConfig`、`AlarmReportConfig`（`entity/control/cfg/`）
  - sender：`deviceConfigOsd(...)`, `deviceConfigVideoAlarmRecord(...)`, `deviceConfigAlarmReport(...)`
  - 事件：`ClientOsdConfigEvent` / `ClientVideoAlarmRecordConfigEvent` / `ClientAlarmReportConfigEvent`
  - 入站路由：`DeviceConfigControlMessageHandler` 通过 XML 子标签分发
  - 其余子配置（VideoRecordPlan / PictureMask / FrameMirror / SVAC* / VideoParamAttribute）按相同模式扩展即可
- §9.9 视音频文件下载：sender `deviceInviteDownload(...)` + `InviteEntity.getInviteDownloadBody(...)` 支持 `s=Download` + `a=downloadspeed`
- §9.8.3.2 INFO MANSRTSP 结构化：`ManSrtspRequest` + `ManSrtspParser`，
  `ClientInfoEvent` 新增 `contentType` / `parsed` 字段（保留 `content` 字段兼容）
- §9.12.2 语音对讲：`InviteSessionNameEnum.TALK` + `InviteEntity.getInviteTalkBody(...)`
  + sender `deviceInviteTalk(...)`，audio-only sendonly SDP（PCMA/8000）

### Added — 通用基础设施
- `CmdTypeEnum` 新增枚举值：`DEVICE_UPGRADE_RESULT`、`UPLOAD_SNAP_SHOT_FINISHED`、
  `PTZ_POSITION`、`SD_CARD_STATUS`、`HOME_POSITION_QUERY`、
  `CRUISE_TRACK_LIST_QUERY`、`CRUISE_TRACK_QUERY`
- `InviteSessionNameEnum` 新增 `TALK` / `DOWNLOAD`，`isValid` 同步扩展
- `DeviceControlRequestHandler` 接口默认方法扩展：`handleDeviceUpgrade`、
  `handlePtzPreciseCtrl`、`handleFormatSDCard`、`handleTargetTrack`
  （default 空实现，业务方按需 override）

### Fixed
- `SubscribeCommandStrategy.doSend(...)` 之前忽略 `CommandContext.extras["subscribeInfo"]`，
  导致所有带 SubscribeInfo 的 SUBSCRIBE（订阅类命令）抛 `subscribeInfo is null`。
  目录订阅（`deviceCatalogSubscribe`）虽然代码路径存在，但旧代码不会真正生效。

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

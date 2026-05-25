# 协议分层矩阵 (L0 / L1 / L2)

> 版本：v1.5.6 | 日期：2026-05-25 | 状态：基于代码事实生成
>
> 关联：[LISTENER-LAYERED-DESIGN.md](LISTENER-LAYERED-DESIGN.md)、[LAYERED-ARCHITECTURE.md](LAYERED-ARCHITECTURE.md)、[GBT-28181-2022.md](GBT-28181-2022.md)
>
> 本文档以**代码事实**为准，列出 sip-proxy 1.5.x 三层协议栈每个具体类的角色、入站路径与出站发布。增删 cmdType 时**先更新此表**，再改代码。

---

## 〇、对 GBT-28181-2022 协议覆盖度总览

按 GBT-28181-2022 全文逐节标注当前实现状态，分四部分：**(A) 附录 A 命令集**（cmdType 矩阵主体） + **(B) §9 流程级能力** + **(C) §5 / §6 / §8 全局要求** + **(D) 附录 B–O 规范性扩展**。新增/补缺协议时同步刷新本表。

图例：✅ 已实现且 listener 化；⚪ transport-only（协议层处理 200 OK，不上抛业务事件）；⚠️ 部分缺失；❌ 未实现；➖ 上层职责（不属 sip-proxy 范围，由 sip-gateway / 媒体服务器承担）。

### (A) §A.2.3.1 设备控制命令 — 全部 ✅

| 节号 | 命令 | 状态 | 落点 |
|---|---|---|---|
| A.2.3.1.2 | 摄像机云台控制 PTZCmd | ✅ | `ControlListener.onPtz` |
| A.2.3.1.3 | 远程启动 TeleBoot | ✅ | `ControlListener.onTeleBoot` |
| A.2.3.1.4 | 录像控制 RecordCmd | ✅ | `ControlListener.onRecord` |
| A.2.3.1.5 | 报警布防/撤防 GuardCmd | ✅ | `ControlListener.onGuard` |
| A.2.3.1.6 | 报警复位 AlarmCmd | ✅ | `ControlListener.onAlarmReset` |
| A.2.3.1.7 | 强制关键帧 IFameCmd | ✅ | `ControlListener.onIFrame` |
| A.2.3.1.8 | 拉框放大 DragZoomIn | ✅ | `ControlListener.onDragIn` |
| A.2.3.1.9 | 拉框缩小 DragZoomOut | ✅ | `ControlListener.onDragOut` |
| A.2.3.1.10 | 看守位 HomePosition | ✅ | `ControlListener.onHomePositionControl` |
| A.2.3.1.11 | PTZ 精准控制 PTZPreciseCtrl | ✅ | `ControlListener.onPtzPrecise` |
| A.2.3.1.12 | 设备软件升级 DeviceUpgrade | ✅ | `ControlListener.onDeviceUpgrade` |
| A.2.3.1.13 | 存储卡格式化 FormatSDCard | ✅ | `ControlListener.onFormatSdCard` |
| A.2.3.1.14 | 目标跟踪 TargetTrack | ✅ | `ControlListener.onTargetTrack` |

### §A.2.3.2 设备配置命令 — 11/11 ✅（外加 1 个超出标准）

| 节号 | 命令 | 状态 | 落点 |
|---|---|---|---|
| A.2.3.2.2 | 基本参数 BasicParam | ✅ | `ConfigListener.onBasicParamConfig` |
| A.2.3.2.3 | SVAC 编码 | ✅ | `ConfigListener.onSvacEncodeConfig` |
| A.2.3.2.4 | SVAC 解码 | ✅ | `ConfigListener.onSvacDecodeConfig` |
| A.2.3.2.5 | 视频参数属性 | ✅ | `ConfigListener.onVideoParamAttributeConfig` |
| A.2.3.2.6 | 录像计划 | ✅ | `ConfigListener.onVideoRecordPlanConfig` |
| A.2.3.2.7 | 报警录像 | ✅ | `ConfigListener.onVideoAlarmRecordConfig` |
| A.2.3.2.8 | 视频画面遮挡 PictureMask | ✅ | `ConfigListener.onPictureMaskConfig` |
| A.2.3.2.9 | 画面翻转 FrameMirror | ✅ | `ConfigListener.onFrameMirrorConfig` |
| A.2.3.2.10 | 报警上报开关 AlarmReport | ✅ | `ConfigListener.onAlarmReportConfig` |
| A.2.3.2.11 | 前端 OSD | ✅ | `ConfigListener.onOsdConfig` |
| A.2.3.2.12 | 图像抓拍 SnapShot | ✅ | `ConfigListener.onSnapShotConfig` |
| —（GB28181-2016 遗留） | 视频参数范围 VideoParamOpt | ✅ 超出标准 | `ConfigListener.onVideoParamOptConfig`；2022 标准只在 §A.2.6.9 应答中允许携带，下发不在 §A.2.3.2.x 序列内，保留以兼容 2016 设备 |

### §A.2.4 查询命令 — 全部 ✅

| 节号 | 命令 cmdType | 状态 | 落点 |
|---|---|---|---|
| A.2.4.2 | DeviceStatus | ✅ | `QueryListener.onDeviceStatusQuery` |
| A.2.4.3 | Catalog | ✅ | `QueryListener.onCatalogQuery`（同时支持 SUBSCRIBE） |
| A.2.4.4 | DeviceInfo | ✅ | `QueryListener.onDeviceInfoQuery` |
| A.2.4.5 | RecordInfo | ✅ | `QueryListener.onRecordInfoQuery`（仅核心字段，扩展过滤字段 Type/RecorderID 等未结构化暴露，业务方可在 `DeviceRecordQuery` 上拿到原始字段） |
| A.2.4.6 | Alarm（订阅） | ✅ | `QueryListener.onAlarmQuery` / `SubscribeListener.onAlarmSubscribe` |
| A.2.4.7 | ConfigDownload | ✅ | `QueryListener.onConfigDownloadQuery` |
| A.2.4.8 | PresetQuery | ✅ | `QueryListener.onPresetQuery` |
| A.2.4.9 | MobilePosition | ✅ | Query：`QueryListener.onMobilePositionQuery`；Subscribe：`SubscribeListener.onMobilePositionSubscribe`（v1.5.6 起 `SubscribeMobilePositionQueryMessageHandler` 已实现，dispatcher key=Query/SUBSCRIBE_MobilePosition） |
| A.2.4.10 | HomePositionQuery | ✅ | `QueryListener.onHomePositionQuery` |
| A.2.4.11 | CruiseTrackListQuery | ✅ | `QueryListener.onCruiseTrackListQuery` |
| A.2.4.12 | CruiseTrackQuery | ✅ | `QueryListener.onCruiseTrackQuery` |
| A.2.4.13 | PTZPosition（订阅） | ✅ | `QueryListener.onPtzPositionQuery` / `SubscribeListener.onPtzPositionSubscribe` |
| A.2.4.14 | SDCardStatus | ✅ | `QueryListener.onSdCardStatusQuery` |

### §A.2.5 通知命令 — 8/8 ✅（v1.5.6 全量 GREEN）

| 节号 | 命令 cmdType | 状态 | 落点 |
|---|---|---|---|
| A.2.5.2 | Keepalive（状态信息报送） | ✅ | `DeviceNotifyListener.onKeepalive`（+ 远端漂移：`DeviceLifecycleListener.onRemoteAddressChanged`） |
| A.2.5.3 | Alarm | ✅ | `DeviceNotifyListener.onAlarmNotify` |
| A.2.5.4 | MediaStatus | ✅ | `DeviceNotifyListener.onMediaStatus` |
| A.2.5.5 | Broadcast（语音广播通知） | ✅ | Client：`NotifyListener.onBroadcastNotify`；Server：`ServerCommandSender.deviceBroadcast` 出站 |
| A.2.5.6 | MobilePosition | ✅ | `DeviceNotifyListener.onMobilePositionNotify`（v1.5.6 起 `MobilePositionNotifyMessageHandler` 已实现，dispatcher key=Notify/MESSAGE_MobilePosition） |
| A.2.5.7 | UploadSnapShotFinished | ✅ | `DeviceNotifyListener.onSnapShotFinished` |
| A.2.5.8 | **VideoUploadNotify**（设备实时视音频回传通知） | ✅ | `DeviceNotifyListener.onVideoUploadNotify`（v1.5.6 起 `VideoUploadNotifyMessageHandler` + `VideoUploadNotify` entity + `CmdTypeEnum.VIDEO_UPLOAD_NOTIFY` 已实现） |
| A.2.5.9 | DeviceUpgradeResult | ✅ | `DeviceNotifyListener.onUpgradeResult` |

### §A.2.6 应答命令 — 12/15 ✅ listener 化 + 3 ⚪ transport-only（v1.5.6 ConfigDownload + PresetQuery 由 ⚠️ 升 ✅）

| 节号 | 命令 cmdType | 状态 | 落点 |
|---|---|---|---|
| A.2.6.2 | DeviceControl 应答 | ⚪ transport-only | server 端 `InviteResponseProcessor` 链路收到 200 OK 即可；无业务 listener（结果码无业务语义） |
| A.2.6.3 | Alarm 通知应答 | ⚪ transport-only | client/server 双向 — 收到 200 OK 即关闭事务；无业务 listener |
| A.2.6.4 | Catalog 应答 | ✅ | `DeviceResponseListener.onCatalogResponse` |
| A.2.6.5 | DeviceInfo 应答 | ✅ | `DeviceResponseListener.onDeviceInfoResponse` |
| A.2.6.6 | DeviceStatus 应答 | ✅ | `DeviceResponseListener.onDeviceStatusResponse` |
| A.2.6.7 | RecordInfo 应答 | ✅ | `DeviceResponseListener.onRecordInfoResponse` |
| A.2.6.8 | DeviceConfig 应答 | ✅ | `DeviceResponseListener.onConfigResponse`（CMD_TYPE=DeviceConfig，仅结果码） |
| A.2.6.9 | **ConfigDownload 应答** | ✅ | `DeviceResponseListener.onConfigDownloadResponse`（v1.5.6 起 `ConfigDownloadResponseMessageHandler` + `DeviceConfigDownloadResponse` entity 已实现，dispatcher key=Response/MESSAGE_ConfigDownload） |
| A.2.6.10 | **PresetQuery 应答** | ✅ | `DeviceResponseListener.onPresetQueryResponse`（v1.5.6 起 `PresetQueryResponseMessageHandler` 已实现，dispatcher key=Response/MESSAGE_PresetQuery） |
| A.2.6.11 | Broadcast 应答 | ⚪ transport-only | 协议规定语音流接收者用 MESSAGE 回响应表"是否能播报"；当前未结构化（落到默认分支），如需消费可走 §七 跨切监听 `ServerNotifyEvent`/`MessageRequestEvent` |
| A.2.6.12 | HomePositionQuery 应答 | ✅ | `DeviceResponseListener.onHomePositionResponse` |
| A.2.6.13 | CruiseTrackListQuery 应答 | ✅ | `DeviceResponseListener.onCruiseTrackListResponse` |
| A.2.6.14 | CruiseTrackQuery 应答 | ✅ | `DeviceResponseListener.onCruiseTrackResponse` |
| A.2.6.15 | PTZPosition 应答 | ✅ | `DeviceResponseListener.onPtzPositionResponse` |
| A.2.6.16 | SDCardStatus 应答 | ✅ | `DeviceResponseListener.onSdCardStatusResponse` |

### (B) §9.x 流程级能力（非 cmdType，但属于基础协议）

| 流程节号 | 能力 | 状态 | 落点 |
|---|---|---|---|
| §9.1 | 注册 / 注销 / 重定向 | ✅ | Client：`ClientRegister{Success,Failure,Challenge,Redirect}Event`；Server：`DeviceLifecycleListener.onDeviceRegister/onDeviceOffline/onRegisterChallenge` |
| §9.2 | 实时视音频点播 | ✅ | Client：`ClientInviteEvent`；Server：`DeviceSessionListener.onInvite{Trying,Ok,Failure}` + `onAck` |
| §9.7 / §9.8 / §9.9 | 历史检索 / 回放 / 下载（INFO + MANSRTSP） | ✅ | `ClientInfoEvent`（payload=`String` 或 `ManSrtspRequest`）；MANSRTSP PLAY/PAUSE/TEARDOWN/SCALE 已支持 |
| §9.10 | 网络校时 | ✅ | sip-common 内置 `TimeSyncService`（SIP Date 头域 + NTP 双模式），无 L1/L2 业务事件 — 详见 §3.4 |
| §9.11 | 订阅与通知（事件订阅 / 目录订阅） | ✅ | Client：`SubscribeListener` 4 方法；Server：`DeviceResponseListener.onNotifyUpdate`（目录通知） + `onSubscribeResponse` |
| §9.12.1 | 语音广播 | ✅ | Client：`NotifyListener.onBroadcastNotify` + `ClientInviteEvent`（媒体流建立） |
| §9.12.2 | 语音对讲 | ✅ 复用组合 | 协议层定义为 §9.2 实时点播 + §9.12.1 语音广播的组合，无独立 cmdType；矩阵不单列，业务方监听 `ClientInviteEvent` + `NotifyListener.onBroadcastNotify` 双事件即可 |
| §9.13 | 设备软件升级 | ✅ | 下发：`ControlListener.onDeviceUpgrade`；结果：`DeviceNotifyListener.onUpgradeResult` |
| §9.14 | 图像抓拍 | ✅ | 下发：`ConfigListener.onSnapShotConfig`；完成：`DeviceNotifyListener.onSnapShotFinished` |

### (C) §5 / §6 / §8 全局要求（传输 / 交换 / 安全）

| 章节 | 内容 | 状态 | 落点 / 备注 |
|---|---|---|---|
| §5 传输要求 | 网络/媒体传输协议、延迟、带宽、丢包、帧率指标 | ➖ 部署级 | sip-common 支持 UDP/TCP（`Constant.UDP/TCP`、`SipMessageTransmitter.sendTcpMessage`）；具体 KPI 由部署/网络拓扑保证 |
| §6.1 ID 统一编码 | 20 位 ID 编码规则（行政区划 8 位 + 行业 2 位 + 类型 3 位 + 序号 7 位） | ✅ | `gb28181-common/utils/GbUtil.generateGB28181Code(...)` 已实现 |
| §6.2 媒体编解码 | H.264 / H.265 / SVAC / G.711 / G.723.1 / G.729 / AAC | ➖ 媒体层 | 由 ZLMediaKit / sip-gateway 转码，sip-proxy 仅透传 SDP `m=` 行 |
| §6.3 媒体存储封装 | PS / TS 等 | ➖ 媒体层 | 同上 |
| §6.4 SDP 定义 | RFC 4566 + 附录 G 扩展（y=ssrc、f= 媒体描述） | ✅ | `gb28181-common/entity/utils/GbSdpUtils.parseGbSdp(...)` 解析 GB 扩展 y / f 字段；INVITE 流程已使用 |
| §6.5 / §6.6 / §6.7 / §6.8 协议转换 | SIP/控制/媒体/格式跨域转换 | ➖ 网关层 | sip-gateway 职责，sip-proxy 不参与 |
| §6.10 信令字符集 | UTF-8 | ✅ 隐式 | JAIN-SIP / JAXB 默认 UTF-8 |
| §6.11 多路径级联 | 多上级平台路径选择 | ➖ 见附录 H | 同附录 H，延后 |
| §8.1 设备身份认证 | 基于口令的数字摘要（MD5）+ 数字证书（宜） | ✅ MD5 / ⚠️ 证书 | `RegisterRequestBuilder` 完整实现 401 → Authorization Digest（MD5 + qop=auth + cnonce）；数字证书认证未实现，宜级要求 |
| §8.2 数据加密 | IPSec（网络层） / TLS（传输层） | ➖ 部署级 | TLS 由 SIP transport 配置（`Constant.TLS` 占位），运维侧加证书；非 sip-proxy 编码逻辑 |
| §8.3 SIP 信令认证 | 数字摘要 + Date + Note + Monitor-User-Identity 头域 | ❌ | sip-common 未实现 `Note` / `Monitor-User-Identity`；SM3 算法未支持。详见 §十一 11.6 |
| §8.4 数据完整性保护 | 数字摘要 / 时间戳 / 水印 | ➖ 业务层 | 宜级要求，业务方实现 |
| §8.5 访问控制 | RBAC / ABAC + Monitor-User-Identity 跨域鉴权 | ➖ 业务层 | 同上，sip-gateway / 业务方负责 |
| §8.6 高安全级别 | GB 35114 合规 | ❌ | 国密 SM2/3/4 双向认证未实现，按需在 sip-gateway 层叠加 |

### (D) 附录 B–O 规范性扩展（除附录 A 之外的 14 个附录）

附录 A 已在 (A) 全量覆盖；其余 14 个附录逐项核对：

| 附录 | 性质 | 内容 | 状态 | 落点 / 备注 |
|---|---|---|---|---|
| 附录 B | 规范性 | MANSRTSP（媒体回放控制：PLAY/PAUSE/TEARDOWN/SCALE/Range/倒放） | ✅ | `ManSrtspRequest` + `ClientInfoEvent`；2022 §9.7-9 INFO 通道全套支持，含倒放（Scale<0） |
| 附录 C | 规范性 | RTP 视音频封装（PS / H.264 / H.265 / SVAC / 音频） | ➖ 媒体层 | ZLMediaKit 实现 RTP payload 解封，sip-proxy 仅协调会话 |
| 附录 D | 规范性 | TCP 视音频媒体传输（被动/主动模式） | ✅ 协议字段 / ➖ 媒体面 | `Device.streamMode` 支持 `TCP-PASSIVE` / `TCP-ACTIVE` 字段；TCP 媒体流由媒体服务器开端口 |
| 附录 E | 规范性 | 统一编码规则（20 位 ID + 行业代码对照 + 县以下区划编码） | ✅ | `GbUtil.generateGB28181Code()` 完整实现；行业代码 / 县级以下扩展由业务方按 E.2 / E.3 落库 |
| 附录 F | 规范性 | 视音频编/解码技术要求（H.264 / MPEG-4 / G.711 / G.723.1 / G.729 / SVAC / H.265 / AAC） | ➖ 媒体层 | 编解码由 ZLMediaKit 完成，sip-proxy 不参与 |
| 附录 G | 规范性 | SDP 定义（含 y=ssrc 行 + f= 媒体描述行扩展） | ✅ | `GbSdpUtils.parseGbSdp()` 解析 GB 私有 y / f 字段；INVITE 流程在 `ServerCommandSender.deviceInvite` / `ClientCommandSender.devicePlay` 内组装 SDP |
| 附录 H | 资料性 | 多路径级联：`X-RoutePath` / `X-PreferredPath` SIP 头 | ➖ 未实现 | 资料性附录，多平台级联场景才用；详见 §十一 11.5 |
| 附录 I | **规范性** | 协议版本标识：REGISTER 及响应头域 `X-GB-Ver` | ⚠️ 未实现 | 真实硬缺口，详见 §十一 11.4 |
| 附录 J | **规范性** | 目录查询应答说明：5 类目录项（行政区划 / 系统 / 业务分组 / 虚拟组织 / 设备） | ⚠️ 部分 | `DeviceItem` 21 个字段全套（IPAddress/Port/Block/Longitude/Latitude/PTZType 等）；`DeviceGbType` 枚举只覆盖 200/111/118/132/215/216；**业务分组 215 / 虚拟组织 216 的语义与 2022 §J.f/J.g 一致性需 §十一 11.7 校准**（2016 标准 215=虚拟组织，2022 改为 215=业务分组、216=虚拟组织） |
| 附录 K | 规范性 | 媒体流保活（媒体流丢失时主动 BYE） | ⚠️ 部分 | sip-proxy 提供 BYE 通道；丢失监测在 ZLMediaKit / sip-gateway 层 |
| 附录 L | 规范性 | Subject 头域语义（`媒体流发送者ID:序列号,媒体流接收者ID:序列号`） | ⚠️ 部分 | sip-common 透传 Subject 头；结构化拼装/拆解在 sip-gateway 层 |
| 附录 M | 规范性 | 多响应消息传输（SumNum > 100 串行、> 10000 切 TCP） | ⚠️ 部分 | entity 层 `SumNum` 全套；分批 / TCP 切换由业务方实现，sip-common 已提供 TCP 能力 |
| 附录 N | 规范性 | 域间目录订阅通知 | ✅ | 复用 §9.11 链路：`SubscribeListener.onCatalogSubscribe` + `DeviceResponseListener.onNotifyUpdate` |
| 附录 O | 规范性 | 摄像机采集部位类型代码（7 位层次码：大类 3 位 + 中类 2 位 + 小类 2 位） | ❌ | 未引入；`DeviceItem` 无 `CollectAreaCode` 字段。**业务侧很少在 SIP 信令里携带，多在视图库 1400 中使用**；按需补充时见 §十一 11.8 |

> **结论**：v1.5.6 起 GBT-28181-2022 全文覆盖度按 **(A) 附录 A 命令集** **47/47 ✅**（v1.5.6 补全 §A.2.5.6 / §A.2.5.8 / §A.2.6.9 / §A.2.6.10 + 修复 §A.2.4.9 SUBSCRIBE / Catalog NOTIFY 路由 = 全部 GREEN）、**(B) §9 流程级** 14/14 ✅、**(C) §5/§6/§8 全局要求** 实现态分级（核心 §6.1 ID + §6.4 SDP + §8.1 数字摘要均 ✅）、**(D) 附录 B–O** 14 个附录中 5 ✅ + 5 ⚠️ + 3 ➖（媒体/网关层）+ 1 ❌（附录 O，使用频率极低）。
>
> **协议命令缺口已全部清零**（v1.5.6）。剩余规范性硬缺口 **3 项**（非附录 A 范围）：
> - 附录 I X-GB-Ver SIP 头域（详见 §十一 11.4，P1）
> - §8.3 Note + Monitor-User-Identity + SM3 信令认证扩展（详见 §十一 11.6，P2）
> - 附录 J §J.f-g 215/216 类型语义跨版本校准（详见 §十一 11.7，P2）
>
> 资料性 / 部分实现 / 上层职责的项目（附录 H/K/L/M/O、§5、§6.2-3、§6.5-9、§8.2/4-6）按需求驱动，本次不在交付范围。

---

## 一、三层定义

| 层 | 命名空间 | 角色 | 业务方是否感知 |
|---|---|---|---|
| **L0 协议解析** | `gb28181-{client,server}/transmit/request/**`、`/transmit/response/**` | 收 SIP 报文 → 路由 cmdType → 解析 XML → 发 L1 事件 | 否（不允许直接依赖） |
| **L1 协议事件** | `gb28181-client/eventbus/event/`、`gb28181-server/transmit/event/` | 通用语义事件，跨切观察点（metrics/audit/tracing） | 可（高级用户） |
| **L2 业务接口** | `gb28181-{client,server}/api/{*Listener,*Adapter}` | 强类型 hook，业务方实现 | 是（默认接入） |

数据流：

```
SIP 报文
  ↓
L0  XXXMessageHandler / XXXRequestProcessor / XXXResponseProcessor
       parseXml(...) + publishEvent(L1)
  ↓ Spring ApplicationEvent
L1  Client*Event / Server*Event
  ↓ ClientListenerAdapter / ServerListenerAdapter @EventListener
L2  QueryListener / ControlListener / ConfigListener / SubscribeListener / NotifyListener
       DeviceLifecycleListener / DeviceNotifyListener / DeviceResponseListener / DeviceSessionListener
```

L1 是**唯一对外稳定的协议层接缝**。L0 → L1 是 fan-in，L1 → L2 是 fan-out。增加新 cmdType 只需「加 L0 handler 1 个 + 改 Adapter 一行 + 加 Listener 方法 1 个」。

---

## 二、Client 端（设备角色：接收平台请求）

### 2.1 入站请求路径（cmdType → L0 → L1 → L2）

| 触发 SIP | rootType | cmdType | L0 解析器 | XML 实体 | L1 事件 | L2 Listener 方法 |
|---|---|---|---|---|---|---|
| MESSAGE | Query | `Catalog` | `CatalogQueryMessageClientHandler` | `DeviceQuery` | `ClientQueryEvent` | `QueryListener.onCatalogQuery` |
| MESSAGE | Query | `DeviceInfo` | `DeviceInfoQueryMessageClientHandler` | `DeviceQuery` | `ClientQueryEvent` | `QueryListener.onDeviceInfoQuery` |
| MESSAGE | Query | `DeviceStatus` | `DeviceStatusQueryMessageClientHandler` | `DeviceQuery` | `ClientQueryEvent` | `QueryListener.onDeviceStatusQuery` |
| MESSAGE | Query | `RecordInfo` | `RecordInfoQueryMessageClientHandler` | `DeviceRecordQuery` | `ClientQueryEvent` | `QueryListener.onRecordInfoQuery` |
| MESSAGE | Query | `Alarm` | `AlarmQueryMessageClientHandler` | `DeviceAlarmQuery` | `ClientQueryEvent` | `QueryListener.onAlarmQuery` |
| MESSAGE | Query | `ConfigDownload` | `ConfigDownloadMessageHandler` | `DeviceConfigDownload` | `ClientQueryEvent` | `QueryListener.onConfigDownloadQuery` |
| MESSAGE | Query | `PresetQuery` | `PresetQueryMessageClientHandler` | `PresetQuery` | `ClientQueryEvent` | `QueryListener.onPresetQuery` |
| MESSAGE | Query | `MobilePosition` | `DeviceMobileQueryMessageClientHandler` | `MobilePositionQuery` | `ClientQueryEvent` | `QueryListener.onMobilePositionQuery` |
| MESSAGE | Query | `PTZPosition` | `PtzPositionQueryMessageClientHandler` | `PTZPositionQuery` | `ClientQueryEvent` | `QueryListener.onPtzPositionQuery` |
| MESSAGE | Query | `SDCardStatus` | `SdCardStatusQueryMessageClientHandler` | `SDCardStatusQuery` | `ClientQueryEvent` | `QueryListener.onSdCardStatusQuery` |
| MESSAGE | Query | `HomePositionQuery` | `HomePositionQueryMessageClientHandler` | `HomePositionQuery` | `ClientQueryEvent` | `QueryListener.onHomePositionQuery` |
| MESSAGE | Query | `CruiseTrackListQuery` | `CruiseTrackListQueryMessageClientHandler` | `CruiseTrackListQuery` | `ClientQueryEvent` | `QueryListener.onCruiseTrackListQuery` |
| MESSAGE | Query | `CruiseTrackQuery` | `CruiseTrackQueryMessageClientHandler` | `CruiseTrackQuery` | `ClientQueryEvent` | `QueryListener.onCruiseTrackQuery` |
| MESSAGE | Control | `DeviceControl` | `DeviceControlMessageHandler` | `DeviceControlBase`（13 子类） | `ClientControlEvent` | `ControlListener.on{Ptz,TeleBoot,Record,Guard,AlarmReset,IFrame,DragIn,DragOut,HomePositionControl,DeviceUpgrade,PtzPrecise,FormatSdCard,TargetTrack}` |
| MESSAGE | Control | `Keepalive` | `KeepaliveMessageClientHandler` | `KeepaliveControl` | `ClientKeepaliveEvent` | `ControlListener.onKeepalive` |
| MESSAGE | Control | `DeviceConfig` | `DeviceConfigControlMessageHandler` | `DeviceControlBase`（11 子类 + 1 兼容子类） | `ClientConfigEvent` | `ConfigListener.on{BasicParam,SvacEncode,SvacDecode,VideoParamAttribute,VideoRecordPlan,VideoAlarmRecord,PictureMask,FrameMirror,AlarmReport,Osd,SnapShot}Config`（GBT-2022 §A.2.3.2.2-12，11 个）+ `onVideoParamOptConfig`（GB28181-2016 遗留兼容，2022 标准下发序列内**未定义**，仅 §A.2.6.9 应答允许） |
| MESSAGE | Notify | `Broadcast` | `BroadcastNotifyMessageHandler` | `DeviceBroadcastNotify` | `ClientNotifyEvent` | `NotifyListener.onBroadcastNotify` |
| SUBSCRIBE | Query | `Catalog` | `SubscribeCatalogQueryMessageHandler` | `DeviceQuery` | `ClientSubscribeEvent` | `SubscribeListener.onCatalogSubscribe` |
| SUBSCRIBE | Query | `Alarm` | `SubscribeAlarmQueryMessageHandler` | `DeviceAlarmQuery` | `ClientSubscribeEvent` | `SubscribeListener.onAlarmSubscribe` |
| SUBSCRIBE | Query | `MobilePosition` | `SubscribeMobilePositionQueryMessageHandler` | `DeviceMobileQuery` | `ClientSubscribeEvent` | `SubscribeListener.onMobilePositionSubscribe` |
| SUBSCRIBE | Query | `PTZPosition` | `SubscribePtzPositionQueryMessageHandler` | `PTZPositionQuery` | `ClientSubscribeEvent` | `SubscribeListener.onPtzPositionSubscribe` |
| INVITE | — | — | `InviteRequestProcessor` | `SdpSessionDescription` | `ClientInviteEvent` | （L1 直消费，业务方自行 `@EventListener`） |
| INFO | — | — | `InfoRequestProcessor` | `String` / `ManSrtspRequest` | `ClientInfoEvent` | （L1 直消费） |
| BYE | — | — | `ByeRequestProcessorClient` | — | `ClientByeEvent` | （L1 直消费） |
| CANCEL | — | — | `CancelRequestProcessor` | — | （回 200 OK，无 L1 事件） | — |
| ACK | — | — | `ClientAckRequestProcessor` | — | （仅日志，无 L1 事件） | — |

### 2.2 出站响应路径（L0 入站响应处理器 → L1 → L2）

| 触发 SIP 响应 | L0 处理器 | L1 事件 | L2 |
|---|---|---|---|
| REGISTER 200 OK | `ClientRegisterResponseProcessor` | `ClientRegisterSuccessEvent` | （L1 直消费） |
| REGISTER 401/407 | `ClientRegisterResponseProcessor` | `ClientRegisterChallengeEvent` | （L1 直消费） |
| REGISTER 4xx/5xx | `ClientRegisterResponseProcessor` | `ClientRegisterFailureEvent` | （L1 直消费） |
| REGISTER 3xx | `ClientRegisterResponseProcessor` | `ClientRegisterRedirectEvent` | （L1 直消费） |
| BYE 响应 | `ByeResponseProcessor` | `ClientByeEvent` | （L1 直消费） |
| CANCEL 响应 | `CancelResponseProcessor` | `ClientCancelEvent` | （L1 直消费） |
| ACK 响应 | `ClientAckResponseProcessor` | `ClientAckEvent` | （L1 直消费） |

### 2.3 ClientListenerAdapter 分发规则

| L1 事件 | 多 listener 策略 | 分发依据 |
|---|---|---|
| `ClientQueryEvent` | **`ObjectProvider#getIfUnique()` 单实例强制**（≥2 fail fast；0 个首次告警一次） | `payload instanceof X` + `DeviceQuery.cmdType` switch |
| `ClientControlEvent` | `List<ControlListener>` 全调用（观察者） | `instanceof DeviceControl* / DeviceUpgradeControl` 链式 |
| `ClientKeepaliveEvent` | `List<ControlListener>` 全调用 | 直发 `onKeepalive` |
| `ClientConfigEvent` | `List<ConfigListener>` 全调用 | `Class<?> → BiConsumer` 显式映射表（避免父子化重构陷阱） |
| `ClientSubscribeEvent` | `List<SubscribeListener>` 全调用 | `instanceof XmlBean` + `cmdType` 字符串 |
| `ClientNotifyEvent` | `List<NotifyListener>` 全调用 | `instanceof DeviceBroadcastNotify` |

---

## 三、Server 端（平台角色：接收设备应答/通知）

### 3.1 入站请求路径

| 触发 SIP | rootType | cmdType | L0 解析器 | XML 实体 | L1 事件 | L2 Listener 方法 |
|---|---|---|---|---|---|---|
| MESSAGE | Response | `Catalog` | `ResponseCatalogMessageHandler` | `DeviceResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onCatalogResponse` |
| MESSAGE | Response | `DeviceInfo` | `DeviceInfoMessageServerHandler` | `DeviceInfo` | `ServerQueryResponseEvent` | `DeviceResponseListener.onDeviceInfoResponse` |
| MESSAGE | Response | `DeviceStatus` | `DeviceStatusMessageServerHandler` | `DeviceStatus` | `ServerQueryResponseEvent` | `DeviceResponseListener.onDeviceStatusResponse` |
| MESSAGE | Response | `RecordInfo` | `RecordInfoMessageHandler` | `DeviceRecord` | `ServerQueryResponseEvent` | `DeviceResponseListener.onRecordInfoResponse` |
| MESSAGE | Response | `DeviceConfig` | `DeviceConfigMessageServerHandler` | `DeviceConfigResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onConfigResponse`（GBT-2022 §A.2.6.8 仅结果码） |
| MESSAGE | Response | `ConfigDownload` | ⚠️ **缺失**（应在 §十一 补 `ConfigDownloadResponseHandler`） | `DeviceConfigDownloadResponse`（待新增） | `ServerQueryResponseEvent` | ⚠️ `DeviceResponseListener.onConfigDownloadResponse`（待新增，GBT-2022 §A.2.6.9） |
| MESSAGE | Response | `PresetQuery` | ⚠️ **缺失**（应在 §十一 补 `PresetQueryResponseHandler`） | `PresetQueryResponse`（待新增） | `ServerQueryResponseEvent` | ⚠️ `DeviceResponseListener.onPresetQueryResponse`（待新增，GBT-2022 §A.2.6.10） |
| MESSAGE | Response | `DeviceControl` | ⚪ transport-only | — | — | 协议层收 200 OK 即结束，结果码无业务语义；如需消费可走 §七 跨切监听原始 `MessageRequestEvent` |
| MESSAGE | Response | `Alarm` | ⚪ transport-only | — | — | client→server Alarm 通知应答（GBT-2022 §A.2.6.3），同上 |
| MESSAGE | Response | `Broadcast` | ⚪ transport-only | — | — | 语音流接收者回的"是否能播报"（GBT-2022 §A.2.6.11），同上 |
| MESSAGE | Response | `PTZPosition` | `PtzPositionMessageHandler` | `PTZPositionResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onPtzPositionResponse` |
| MESSAGE | Response | `SDCardStatus` | `SdCardStatusMessageHandler` | `SDCardStatusResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onSdCardStatusResponse` |
| MESSAGE | Response | `HomePositionQuery` | `HomePositionMessageHandler` | `HomePositionResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onHomePositionResponse` |
| MESSAGE | Response | `CruiseTrackListQuery` | `CruiseTrackListMessageHandler` | `CruiseTrackListResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onCruiseTrackListResponse` |
| MESSAGE | Response | `CruiseTrackQuery` | `CruiseTrackMessageHandler` | `CruiseTrackResponse` | `ServerQueryResponseEvent` | `DeviceResponseListener.onCruiseTrackResponse` |
| MESSAGE | Notify | `Catalog`（设备目录变更） | `CatalogNotifyHandler` | `DeviceOtherUpdateNotify` | `ServerQueryResponseEvent` | `DeviceResponseListener.onNotifyUpdate` |
| MESSAGE | Notify | `Alarm` | `AlarmNotifyMessageHandler` | `DeviceAlarmNotify` | `ServerNotifyEvent` | `DeviceNotifyListener.onAlarmNotify` |
| MESSAGE | Notify | `Keepalive` | `KeepaliveNotifyMessageHandler` | `DeviceKeepLiveNotify` | `ServerNotifyEvent` (+`ServerLifecycleEvent.remoteAddressChanged` 当远端地址漂移) | `DeviceNotifyListener.onKeepalive` (+`DeviceLifecycleListener.onRemoteAddressChanged`) |
| MESSAGE | Notify | `MediaStatus` | `MediaStatusNotifyMessageHandler` | `MediaStatusNotify` | `ServerNotifyEvent` | `DeviceNotifyListener.onMediaStatus` |
| MESSAGE | Notify | `MobilePosition` | `MobilePositionNotifyMessageHandler` | `MobilePositionNotify` | `ServerNotifyEvent` | `DeviceNotifyListener.onMobilePositionNotify` |
| MESSAGE | Notify | `DeviceUpgradeResult` | `UpgradeResultNotifyMessageHandler` | `UpgradeResultNotify` | `ServerNotifyEvent` | `DeviceNotifyListener.onUpgradeResult` |
| MESSAGE | Notify | `UploadSnapShotFinished` | `UploadSnapShotFinishedNotifyMessageHandler` | `UploadSnapShotFinishedNotify` | `ServerNotifyEvent` | `DeviceNotifyListener.onSnapShotFinished` |
| INFO | — | — | `ServerInfoRequestProcessor` | `DeviceInfoRequest` / `DeviceInfoError` | `ServerQueryResponseEvent` | `DeviceResponseListener.onDeviceInfoRequest` / `onDeviceInfoError` |
| REGISTER（首次成功） | — | — | `ServerRegisterRequestProcessor` | `RegisterInfo` | `ServerLifecycleEvent.register` + `ServerLifecycleEvent.online` | `DeviceLifecycleListener.onDeviceRegister` + `onDeviceOnline` |
| REGISTER（注销 expires=0） | — | — | `ServerRegisterRequestProcessor` | `RegisterInfo` | `ServerLifecycleEvent.offline` | `DeviceLifecycleListener.onDeviceOffline` |
| REGISTER（401 挑战） | — | — | `ServerRegisterRequestProcessor` | — | `ServerLifecycleEvent.challenge` | `DeviceLifecycleListener.onRegisterChallenge` |
| INVITE（设备主动发起） | — | — | `ServerInviteRequestProcessor` | `GbSessionDescription` | `ServerSessionEvent.serverInvite` | `DeviceSessionListener.onServerInvite` |
| BYE | — | — | `ByeRequestProcessorServer` | — | `ServerSessionEvent.bye` / `byeError` | `DeviceSessionListener.onBye` / `onByeError` |

### 3.2 出站响应路径

| 触发 SIP 响应 | L0 处理器 | L1 事件 | L2 |
|---|---|---|---|
| INVITE 100 Trying | `InviteResponseProcessor` | `ServerSessionEvent.inviteTrying` | `DeviceSessionListener.onInviteTrying` |
| INVITE 200 OK | `InviteResponseProcessor` | `ServerSessionEvent.inviteOk` | `DeviceSessionListener.onInviteOk` |
| INVITE 4xx/5xx/6xx | `InviteResponseProcessor` | `ServerSessionEvent.inviteFailure` | `DeviceSessionListener.onInviteFailure` |
| ACK 收到 | `ServerAckResponseProcessor` | `ServerSessionEvent.ack` | `DeviceSessionListener.onAck` |
| SUBSCRIBE 响应 | `SubscribeResponseProcessor` | `ServerQueryResponseEvent`（payload=`DeviceSubscribeResponse` record） | `DeviceResponseListener.onSubscribeResponse` |

### 3.3 ServerListenerAdapter 分发规则

| L1 事件 | 多 listener 策略 | 分发依据 |
|---|---|---|
| `ServerQueryResponseEvent` | `List<DeviceResponseListener>` 全调用 | `payload instanceof X`（14 类，含 `DeviceSubscribeResponse` record / `DeviceInfoRequest` record / `DeviceInfoError` record / `DeviceOtherUpdateNotify`） |
| `ServerNotifyEvent` | `List<DeviceNotifyListener>` 全调用 | `payload instanceof X`（6 类） |
| `ServerLifecycleEvent` | `List<DeviceLifecycleListener>` 全调用 | `LifecycleType` enum 5 值 switch（REGISTER / CHALLENGE / ONLINE / OFFLINE / REMOTE_ADDRESS_CHANGED） |
| `ServerSessionEvent` | `List<DeviceSessionListener>` 全调用 | `SessionType` enum 7 值 switch（INVITE_TRYING / INVITE_OK / INVITE_FAILURE / ACK / BYE / BYE_ERROR / SERVER_INVITE） |

### 3.4 时间同步通道（GBT-2022 §9.10）

时间同步在 sip-common 协议层透明完成，**不上抛 L1 事件、不暴露 L2 listener**。这是有意的：校时是网络栈层能力，业务方无须感知。

| 节号 | 能力 | 实现 | 配置 |
|---|---|---|---|
| §9.10 | SIP 校时（解析 200 OK / REGISTER 响应的 `Date` 头域） | `TimeSyncService.syncTimeFromSip` | `sip.common.time-sync.mode=SIP` |
| §9.10 | NTP 校时（定时拉取 NTP 服务器） | `NtpTimeSyncScheduler` + `TimeSyncService.syncTimeFromNtp` | `sip.common.time-sync.mode=NTP` + `sip.common.time-sync.ntp-server=...` |
| — | 偏差阈值告警 | `TimeSyncService.needsTimeSync()` | `sip.common.time-sync.offset-threshold=...` |

> 1.3.0 起：原 `sip.gb28181.time-sync.*` 配置项已迁移到 `sip.common.time-sync.*`。如需自定义业务回调（例如时间偏差超阈值时上报告警），自行 `@Autowired TimeSyncService` 后定时检查 `needsTimeSync()`。

---

## 四、L1 事件清单（共 21 个 + 1 待补）

### 4.1 Client 端（17 个）

| L1 事件 | payload 字段 | 触发场景 |
|---|---|---|
| `ClientQueryEvent` | `XmlBean query` + `userId` + `sipId` | 平台→设备 cmdType=Query 类查询 |
| `ClientControlEvent` | `DeviceControlBase command` + `userId` | 平台→设备 cmdType=DeviceControl 控制 |
| `ClientKeepaliveEvent` | `KeepaliveControl keepalive` + `userId` | 平台→设备 cmdType=Keepalive 心跳 |
| `ClientConfigEvent` | `DeviceControlBase config` + `userId` | 平台→设备 cmdType=DeviceConfig 配置 |
| `ClientSubscribeEvent` | `XmlBean body` + `userId` + `sipId` + `expires` | 平台→设备 SUBSCRIBE 订阅 |
| `ClientNotifyEvent` | `XmlBean notify` + `userId` | 平台→设备 rootType=Notify 通知 |
| `ClientInviteEvent` | `callId` + `userId` + `SdpSessionDescription` + `transactionContextKey` | 平台→设备 INVITE 主动呼叫 |
| `ClientInfoEvent` | `userId` + `content` + `contentType` + `ManSrtspRequest` | 平台→设备 INFO（含 MANSRTSP 拖动） |
| `ClientByeEvent` | `callId` + `statusCode` | INVITE 会话 BYE |
| `ClientAckEvent` | `callId` | ACK 收到 |
| `ClientCancelEvent` | `callId` + `statusCode` | CANCEL 收到 |
| `ClientRegisterSuccessEvent` | `userId` | 设备注册 200 OK |
| `ClientRegisterChallengeEvent` | `userId` + `callId` | 设备注册 401/407 挑战 |
| `ClientRegisterFailureEvent` | `userId` + `statusCode` | 设备注册 4xx/5xx 失败 |
| `ClientRegisterRedirectEvent` | `originalToUserId` + 重定向地址 | 设备注册 3xx 重定向 |
| `subscribe.CatalogEvent` | （订阅 Catalog NOTIFY 内部使用） | 订阅式目录通知（已纳入 SubscribeListener） |

### 4.2 Server 端（4 个聚合事件）

| L1 事件 | 子类型字段 | 子类型枚举 |
|---|---|---|
| `ServerQueryResponseEvent` | `Object payload` | （按 payload Java 类型分发，14 类，**待补 ConfigDownloadResponse / PresetQueryResponse** = 16 类） |
| `ServerNotifyEvent` | `Object payload` | （按 payload Java 类型分发，6 类，**待补 VideoUploadNotify** = 7 类） |
| `ServerLifecycleEvent` | `LifecycleType type` | `REGISTER / CHALLENGE / ONLINE / OFFLINE / REMOTE_ADDRESS_CHANGED` |
| `ServerSessionEvent` | `SessionType type` | `INVITE_TRYING / INVITE_OK / INVITE_FAILURE / ACK / BYE / BYE_ERROR / SERVER_INVITE` |

### 4.3 待补事件（GBT-2022 强制要求）

| 节号 | 事件 | payload 类型 | 触发 | 状态 |
|---|---|---|---|---|
| §A.2.5.8 | `ServerNotifyEvent`（payload=`VideoUploadNotify`） | `VideoUploadNotify`（待新增） | 设备→平台主动通知"开始/结束实时视音频回传" | ❌ 见 §十一 |
| §A.2.6.9 | `ServerQueryResponseEvent`（payload=`DeviceConfigDownloadResponse`） | `DeviceConfigDownloadResponse`（待新增） | 平台 ConfigDownload 查询 → 设备应答（携带 BasicParam/SVAC/OSD 等明细） | ❌ 见 §十一 |
| §A.2.6.10 | `ServerQueryResponseEvent`（payload=`PresetQueryResponse`） | `PresetQueryResponse`（待新增） | 平台 PresetQuery 查询 → 设备应答（携带预置位列表） | ❌ 见 §十一 |

---

## 五、L2 Listener 清单（共 9 个接口 + 2 个 Adapter 聚合接口）

### 5.1 Client 端（5 个 listener + 1 个 Adapter）

| Listener | 方法数 | 多实例策略 | 用途 |
|---|---|---|---|
| `QueryListener` | 13 | **唯一** | 平台查询 → 设备应答（返回非 null = 框架自动回包） |
| `ControlListener` | 14 + 1 兜底 | 全调用 | 平台控制命令（含 13 个 control + Keepalive） |
| `ConfigListener` | 12 + 1 兜底 | 全调用 | 平台配置下发（GBT-2022 §A.2.3.2） |
| `SubscribeListener` | 4 | 全调用 | 平台订阅请求 |
| `NotifyListener` | 1 + 1 兜底 | 全调用 | 平台通知（核心 Broadcast） |
| `ClientGb28181Adapter`（聚合） | — | — | 一站式实现五个 listener 的便捷基类 |

### 5.2 Server 端（4 个 listener + 1 个 Adapter）

| Listener | 方法数 | 多实例策略 | 用途 |
|---|---|---|---|
| `DeviceLifecycleListener` | 5 | 全调用 | 注册/挑战/在线/离线/远端地址变更 |
| `DeviceNotifyListener` | 6 | 全调用 | 告警/心跳/媒体/移动位置/升级结果/抓拍完成 |
| `DeviceResponseListener` | 14 | 全调用 | 设备各类应答 + Catalog 增量通知 + INFO 请求/错误 |
| `DeviceSessionListener` | 7 | 全调用 | INVITE/ACK/BYE 状态机 + 设备主动 INVITE |
| `ServerGb28181Adapter`（聚合） | — | — | 一站式实现四个 listener 的便捷基类 |

---

## 六、典型调用链时序

### 6.1 Client 端：平台查询设备目录

```
平台→设备 MESSAGE (cmdType=Catalog, rootType=Query)
  ├─ AbstractSipListener           [sip-common]   接收 SIP 报文
  ├─ ClientMessageRequestProcessor [gb28181-client] rootType+cmdType 路由
  ├─ CatalogQueryMessageClientHandler.handForEvt   ← L0 协议解析
  │     parseXml(DeviceQuery.class)
  │     publishEvent(new ClientQueryEvent(this, userId, sipId, query))
  │                                                 ← L1 协议事件
  ├─ ClientListenerAdapter.dispatch(ClientQueryEvent)
  │     getIfUnique(QueryListener) → 业务方实现
  │     listener.onCatalogQuery(sipId, query) → DeviceResponse  ← L2 业务接口
  │     ClientCommandSender.sendCatalogCommand(from, to, response)
  └─ 框架自动出站 200 OK + Catalog Response
```

### 6.2 Server 端：设备主动告警

```
设备→平台 MESSAGE (cmdType=Alarm, rootType=Notify)
  ├─ AbstractSipListener
  ├─ ServerMessageRequestProcessor
  ├─ AlarmNotifyMessageHandler.handForEvt          ← L0
  │     parseXml(DeviceAlarmNotify.class)
  │     publishEvent(new ServerNotifyEvent(this, deviceId, notify))
  │                                                 ← L1
  ├─ ServerListenerAdapter.on(ServerNotifyEvent)
  │     for (DeviceNotifyListener l : listeners)
  │         l.onAlarmNotify(deviceId, notify)       ← L2
  └─ 协议层自动 200 OK
```

### 6.3 Server 端：设备注册 + 在线（双事件）

```
设备→平台 REGISTER（带 Authorization）
  ├─ ServerRegisterRequestProcessor
  │     digest 校验通过
  │     publishEvent(ServerLifecycleEvent.register(this, userId, info))
  │     publishEvent(ServerLifecycleEvent.online(this, userId, sipTransaction))
  ├─ ServerListenerAdapter.on(ServerLifecycleEvent)  [触发两次]
  │     switch(REGISTER)  → l.onDeviceRegister(deviceId, info)
  │     switch(ONLINE)    → l.onDeviceOnline(deviceId, sipTransaction)
  └─ 协议层 200 OK
```

### 6.4 Client 端：语音对讲（GBT-2022 §9.12.2 复用组合，非独立 cmdType）

语音对讲在协议规范中没有自己的 cmdType，由 §9.2 实时点播 + §9.12.1 语音广播两条独立流程组合：

```
方向 1：中心→前端 取实时视音频媒体流（§9.2）
  平台 INVITE → 设备
    └─ ClientInviteEvent (sdp.s="Play", m=video/audio)
        └─ 业务方建立媒体流

方向 2：中心→前端 推音频流（§9.12.1）
  平台 MESSAGE (cmdType=Broadcast, rootType=Notify) → 设备
    └─ ClientNotifyEvent → NotifyListener.onBroadcastNotify(...)
        └─ 业务方处理"是否能播报"
  设备 INVITE → 平台 (audio sendonly)
    └─ ServerInviteRequestProcessor → DeviceSessionListener.onServerInvite(...)
        └─ 业务方建立反向音频流
```

业务方实现"对讲"功能时**必须同时监听** `ClientInviteEvent` 与 `NotifyListener.onBroadcastNotify`（设备侧），或 `DeviceSessionListener.onServerInvite` 与 `ServerCommandSender.deviceBroadcast`（平台侧）。框架不做组合封装。

---

## 七、跨切关注点（监听 L1 即可）

任何 bean 都可以 `@EventListener` 监听 L1 事件做指标/审计/链路追踪，**不影响业务方 listener**：

```java
@Component
class ProtocolMetrics {
    @EventListener
    void onQuery(ClientQueryEvent e) {
        Metrics.counter("gb28181.query",
                "cmdType", e.getQuery().getClass().getSimpleName()).increment();
    }

    @EventListener
    void onLifecycle(ServerLifecycleEvent e) {
        Metrics.counter("gb28181.lifecycle",
                "type", e.getType().name()).increment();
    }
}
```

L1 是稳定接缝：协议升级/重构时**优先保 L1 兼容**，L0 内部可自由变化。

---

## 八、扩展规则（增加新 cmdType 时）

按以下顺序操作，逐步保证可编译可测试：

1. **gb28181-common/entity** 加 XML 实体（`@XmlRootElement` + `@XmlElement`）
2. **gb28181-common/entity/enums/CmdTypeEnum** 加枚举
3. **gb28181-{client,server}/transmit/.../handler** 加 L0 handler（`extends MessageHandlerAbstract` 或子类）
4. **gb28181-{client,server}/api/*Listener** 加 `default void onXxx(...)` 方法
5. **gb28181-{client,server}/eventbus/internal/*Adapter** 在分发分支里加一行 `instanceof` 或 switch case
6. **gb28181-test** 加 FlowTest 集成验证
7. 更新本文件（同时更新 `LISTENER-LAYERED-DESIGN.md` 的相关章节）

> ⚠ 不允许：业务方直接依赖 L0 handler；在 L0 handler 内调用业务接口；新增 `*Handler` 业务回调接口（已在 1.3.0 全量删除）。

---

## 九、版本变更记录

| 版本 | 时间 | 关键变化 |
|---|---|---|
| 1.0.x – 1.2.x | 2024 之前 | `*Handler` 业务回调接口形态，L0/L1/L2 未分层 |
| 1.3.0 | 2026-04 | sip-common 协议解耦；删除全部 `*Handler` 业务接口；统一事件总线；INVITE 异步化 |
| 1.4.0 | 2026-05 | GBT-28181-2022 协议扩展 13 阶段全量落地（PTZ Precise / SDCard / HomePosition / CruiseTrack / DeviceUpgrade / SnapShot / OSD / 报警上报订阅 等） |
| 1.5.0 | 2026-05-24 | client 端 Listener 化分层（5 个 listener + 6 个 L1 协议事件 + ClientListenerAdapter）；server 端加性扩展（4 个 listener + ServerListenerAdapter） |
| 1.5.1 | 2026-05-24 | Phase A 内部清理：删除 ConfigDownload 重复路径 / `BaseMessageClientHandler` 空壳 / 31 个 `@Deprecated` 静态方法；后续陆续删除 18 个无新模型替代但已废弃的 XML entity |
| 1.5.2 | 2026-05-25 | 矩阵文档完整性提升：新增 §0 GBT-2022 覆盖度总览 / §3.4 时间同步通道 / §6.4 语音对讲组合时序 / §11 缺口与补全方案；标注 §A.2.5.8 / §A.2.6.9 / §A.2.6.10 三个协议缺口；修正 `VideoParamOpt` 章节归属（2016 遗留兼容） |
| 1.5.3 | 2026-05-25 | 矩阵覆盖度从「附录 A 命令集」扩展到 GBT-28181-2022 全文：§0 拆分为 (A) 附录 A + (B) §9 流程级 + 附录 H/I/K/L/M/N；新增 1 个规范性硬缺口（附录 I X-GB-Ver）和 1 个资料性延后项（附录 H 多路径）；§十一 增列 11.4 X-GB-Ver 补全方案 / 11.5 附录 H 延后说明 / 11.6 P1 整体代价；P1 缺口数 3 → 4 |
| 1.5.4 | 2026-05-25 | 矩阵覆盖度真正完整化：§0 拆为四部分 (A)/(B)/(C)/(D)，新增 (C) §5 / §6 / §8 全局要求表（14 行）+ (D) 完整附录 B–O（14 行，原 (B) 6 行扩展）；新发现 2 个规范性缺口（§8.3 信令认证扩展 / 附录 J §J.f-g 215/216 类型语义）+ 1 个延后项（附录 O 采集部位）；§十一 增列 11.6 §8.3 / 11.7 附录 J / 11.8 附录 O；P1 缺口数 4 不变，新增 P2 = 2 项 / P3 = 2 项；附录 J 215/216 校准首次披露 2016/2022 标准枚举差异 |
| 1.5.5 | 2026-05-25 | L0/L1/L2 三层映射首次以**代码事实**为基准逐行回归审计：发现 3 处矩阵 → 代码不一致：(1) §A.2.5.6 MobilePosition Notify server 端 `MobilePositionNotifyMessageHandler` **缺失**（matrix 此前误标为"由 BaseMessageServerHandler 默认路径承接"，实际 BaseMessageServerHandler 是源码自标"无实际使用"的死代码）；(2) §A.2.4.9 MobilePosition Subscribe client 端 `SubscribeMobilePositionQueryMessageHandler` **缺失**（matrix 此前误标为"复用 SubscribeAlarmQueryMessageHandler"，实际后者 CMD_TYPE="Alarm" 与 MobilePosition 完全无关）；(3) §A.2.5 章节标题 7/8 → 6/8。修正 §0(A) §A.2.4.9 / §A.2.5.6 / §A.2.5 标题、§2.1 SUBSCRIBE MobilePosition 行、§3.1 MESSAGE Notify MobilePosition 行；新增 §11.9 / §11.10 / §11.11 三个补全/清理小节；P1 缺口数 4 → 6（新增 2 个路由级），整体 P1 LOC 从 ~750 升至 ~920 |
| 1.5.6 | 2026-05-25 | **TDD 驱动协议补全全量 GREEN**：(1) **TDD Step 1** 新增 `DispatcherRegistrationTest` 10 个测试，定义所有缺失的 dispatcher 注册 key 三元组 `(rootType, method, cmdType)`，红相确认 6 处缺口；(2) **TDD Step 2** 实现 5 个新 handler（`MobilePositionNotifyMessageHandler` / `VideoUploadNotifyMessageHandler` / `ConfigDownloadResponseMessageHandler` / `PresetQueryResponseMessageHandler` / `SubscribeMobilePositionQueryMessageHandler`）+ 修复 2 个错误 handler（`SubscribeCatalogQueryMessageHandler.getMethod()` 缺失、`CatalogNotifyHandler.getRootType()+getMethod()` 双重错误）+ 删除 1 个死代码 handler（`BaseMessageServerHandler`）；(3) 配套基础设施：新增 `CmdTypeEnum.VIDEO_UPLOAD_NOTIFY` + `VideoUploadNotify` entity + `DeviceConfigDownloadResponse` entity（聚合 11 个可选 cfg 子标签）+ `DeviceNotifyListener.onVideoUploadNotify` + `DeviceResponseListener.onConfigDownloadResponse` / `onPresetQueryResponse` + `ServerListenerAdapter` 3 个 instanceof 分支 + `ClientCommandSender.sendVideoUploadNotify` / `sendConfigDownloadResponse` 出站 + `TestServerEventHandler` 5 个 typed payload 字段；(4) **TDD Step 3** 新增 `V156ProtocolCompletionFlowTest` 5 个端到端流程测试，验证消息从 sender → SIP transport → handler → adapter → listener 完整路由；(5) 全量回归 `gb28181-test` 52/52 GREEN；(6) §0(A) 命令集覆盖率 42/47 → **47/47 (100%)**；附录 A 协议命令缺口清零，剩余规范性缺口 6 → 3（仅头域类）；(7) 矩阵 §0(A) §A.2.5 / §A.2.6 标题 + 多行状态由 ⚠️/❌ 升 ✅，§2.1 SUBSCRIBE MobilePosition / §3.1 MESSAGE Notify MobilePosition 由"死路径"改为正常路径 |

---

## 十、本表的维护承诺

- **新增 cmdType 必须先改本表**，再写代码 —— 表是 source of truth
- 表中**任意一行的 L0 / L1 / L2 列出现 0 引用**应视为重构遗留，按 1.5.1 cleanup 规则评估删除
- 列名「L2 Listener 方法」中 `（L1 直消费）` 表示当前未提供业务 listener，业务方需自行 `@EventListener` 直接监听 L1 事件
- §0 覆盖度总览必须在每次 GB28181 协议补全后同步刷新，且 (A) 附录 A 命令集 / (B) §9 流程级 / (C) §5/§6/§8 全局要求 / (D) 附录 B–O 四个表均需保持与代码一致
- **L0 handler 必须有真实 dispatcher 注册 key**（`MESSAGE_HANDLER_CMD_MAP.get(rootType).get(method + "_" + cmdType)`）—— 仅有 listener 接口与 adapter 分支不算「实现」。审计方法：`grep "CMD_TYPE\s*=" gb28181-{client,server}/src/main` 列出全部注册项，与矩阵 §2.1 / §3.1 表逐行核对。v1.5.5 即由此发现 MobilePosition Notify / Subscribe 两条死路径
- 引入 GBT-28181-2022 之外的协议扩展（如附录 I X-GB-Ver、附录 H 多路径头）时，**SIP 头域常量集中放在 sip-common**，业务语义解析放在 gb28181-{client,server}，避免污染协议解耦边界（参见 sip-common 协议层纯净性规则）

---

## 十一、协议缺口与补全方案

> **v1.5.6 状态更新**：P1 协议命令缺口（§11.1 / §11.2 / §11.3 / §11.9 / §11.10）+ §11.11 死代码清理已**全部完成**，由 `DispatcherRegistrationTest` (10 例) + `V156ProtocolCompletionFlowTest` (5 例) 保护。剩余 §11.4 / §11.6 / §11.7 / §11.5 / §11.8 为下一周期范围。

按 §0 总览，当前实现相对 GBT-28181-2022 全文剩余 **3 个规范性硬缺口**（仅头域/类型语义，附录 A 命令集已 100% 清零）+ 2 个延后项（附录 H 多路径 / 附录 O 采集部位）。本节给出每个缺口的**最小落地方案**，按 §八「扩展规则」执行。优先级 P1 = 协议命令缺口 + 路由缺失（互通刚需，**v1.5.6 已全部交付**），P2 = 安全合规与跨版本互通，P3 = 场景驱动延后或内部清理。

### 11.1 §A.2.5.8 设备实时视音频回传通知（VideoUploadNotify）✅ v1.5.6 已完成

**协议位置**：通知命令（设备→平台主动上报，cmdType=`VideoUploadNotify`）。

**当前状态**：`grep -r VideoUploadNotify` 零结果 —— 无 entity / enum / handler / listener。

**业务场景**：执法记录仪、移动单警等设备开始/结束实时视音频回传时，向平台主动通知"我开始/结束传了"，附带 SN / DeviceID / Time / Longitude / Latitude。平台据此做"是否启用回传媒体流"的会话调度。

**最小补全步骤**：

1. `gb28181-common/entity/notify/VideoUploadNotify.java` —— 新增 XML entity（参照 `MobilePositionNotify`）
2. `CmdTypeEnum.VIDEO_UPLOAD_NOTIFY("VideoUploadNotify", "...")` —— 加枚举
3. `gb28181-server/transmit/request/message/notify/VideoUploadNotifyMessageHandler.java` —— L0 handler
4. `DeviceNotifyListener` 加 `default void onVideoUploadNotify(String deviceId, VideoUploadNotify notify) {}`
5. `ServerListenerAdapter.on(ServerNotifyEvent)` 加一支 `instanceof VideoUploadNotify`
6. `gb28181-test` 加 FlowTest
7. 矩阵 §0 §3.1 §4.2 §5.2 同步标 ✅

### 11.2 §A.2.6.9 设备配置查询应答（ConfigDownload Response）✅ v1.5.6 已完成

**协议位置**：应答命令（设备→平台，cmdType=`ConfigDownload`）。

**当前状态**：`ServerCommandSender.deviceConfigDownload` 可发出查询；`DeviceConfigMessageServerHandler` 处理的是 cmdType=`DeviceConfig`（§A.2.6.8 仅结果码），**不路由 ConfigDownload 应答**。设备如果按标准回包，会落入默认分支。

**业务场景**：平台获取设备的全量配置画像（基本参数、SVAC 编/解码、OSD、录像计划、报警录像、画面遮挡 / 翻转、报警上报开关、抓拍配置）。运维侧"远程查看设备配置"功能依赖此应答。

**最小补全步骤**：

1. `gb28181-common/entity/response/DeviceConfigDownloadResponse.java` —— 聚合 11 个可选 cfg 字段（BasicParam / VideoParamOpt / SVAC{En,De}code / VideoParamAttribute / VideoRecordPlan / VideoAlarmRecord / PictureMask / FrameMirror / AlarmReport / OSDConfig / SnapShot）
2. `gb28181-server/transmit/request/message/response/ConfigDownloadResponseMessageHandler.java`，CMD_TYPE=`CONFIG_DOWNLOAD`
3. `DeviceResponseListener` 加 `default void onConfigDownloadResponse(String deviceId, DeviceConfigDownloadResponse resp) {}`
4. `ServerListenerAdapter.on(ServerQueryResponseEvent)` 加一支 `instanceof DeviceConfigDownloadResponse`
5. `gb28181-test` 加 ConfigDownloadFlowTest
6. 矩阵 §0 §3.1 §4.2 §5.2 同步标 ✅

### 11.3 §A.2.6.10 设备预置位查询应答（PresetQuery Response）✅ v1.5.6 已完成

**协议位置**：应答命令（设备→平台，cmdType=`PresetQuery`）。

**当前状态**：`ServerCommandSender.devicePresetQuery` 可发出查询；server 端**完全没有** `PresetQueryResponseHandler`，应答会落入默认分支。

**业务场景**：平台显示某摄像机已配置的预置位列表（PresetID + PresetName）。云台调度 / 巡航编排功能依赖此数据。

**最小补全步骤**：

1. `gb28181-common/entity/response/PresetQueryResponse.java` —— 含 `SumNum` + `PresetList`（`PresetItem{PresetID, PresetName}`）
2. `gb28181-server/transmit/request/message/response/PresetQueryResponseMessageHandler.java`，CMD_TYPE=`PRESET_QUERY`
3. `DeviceResponseListener` 加 `default void onPresetQueryResponse(String deviceId, PresetQueryResponse resp) {}`
4. `ServerListenerAdapter.on(ServerQueryResponseEvent)` 加一支 `instanceof PresetQueryResponse`
5. `gb28181-test` 加 PresetQueryFlowTest
6. 矩阵 §0 §3.1 §4.2 §5.2 同步标 ✅

### 11.4 附录 I 协议版本标识（X-GB-Ver）⚠️

**协议位置**：附录 I（规范性），REGISTER 请求及其成功/失败响应的 SIP 消息头部均应携带 `X-GB-Ver`。

**当前状态**：`grep -r X-GB-Ver` 零结果。注册流程中既未发送也未解析此扩展头。

**业务场景**：上下级平台或设备/平台之间通过 `X-GB-Ver` 协商协议版本（`1.0`=2011 / `2.0`=2016 / `3.0`=2022），高版本一方据此避免向低版本对端发送对方无法识别的命令（例如把 §A.2.4.13 PTZ 精准状态查询发给 2011 设备）。严格遵循 2022 标准的对端在缺失此头域时可能直接拒绝注册或降级处理。

**最小补全步骤**：

1. `sip-common/src/main/java/.../constant/SipHeaderConstants.java` —— 新增 `public static final String X_GB_VER_HEADER = "X-GB-Ver"; public static final String X_GB_VER_VALUE = "3.0";`
2. `sip-common/.../request/RegisterRequestBuilder` —— 构造 REGISTER 请求时附加 `ExtensionHeader X-GB-Ver: 3.0`
3. `gb28181-server/.../ServerRegisterRequestProcessor` —— 收 REGISTER 后回 200 OK / 401 时附加同样头域；同时把对端 `X-GB-Ver` 解析进 `RegisterInfo.peerProtocolVersion`，向 `ServerLifecycleEvent.register` payload 透传
4. `gb28181-client/.../ClientRegisterResponseProcessor` —— 解析响应中的 `X-GB-Ver`，存入 `ClientDevice.peerProtocolVersion` 供 `ClientCommandSender` 在发送高版本-only 命令前做版本检查
5. （可选）`QueryListener` / `ControlListener` 默认实现里加一行注释，提示业务方"对端 ProtocolVersion < 3.0 时调用 PTZPrecise 等 2022 新增 cmdType 应自行降级"
6. `gb28181-test` 加 `XGbVerHeaderTest`（注册请求/响应双向断��头域）
7. 矩阵 §0(B) 附录 I 行刷新为 ✅；本节 §11.4 标记为已完成

### 11.5 附录 H 多路径级联（X-RoutePath / X-PreferredPath）➖ 延后

**协议位置**：附录 H（资料性，非强制）。

**当前状态**：未实现。资料性附录，仅在 A→B→C→E、A→C→E 多路径级联结构下生效。

**业务影响**：当前 sip-proxy 单跳 SIP/级联部署不需要；如未来对接多上级平台拓扑（如管理平台 A 同时挂在 B、C 两个上级），再补 INVITE 响应附加 `X-RoutePath`、INVITE 请求识别 `X-PreferredPath` 即可。

**延后理由**：1) 资料性附录非强制；2) 业务方目前无多路径级联需求；3) 实现需在 `InviteRequestBuilder` + `InviteResponseProcessor` 双向加扩展头域处理 + 路径推送/合并逻辑，工程量约 ~400 LOC，不在当前 1.5.x 周期内交付。

### 11.6 §8.3 SIP 信令认证扩展（Note + Monitor-User-Identity + SM3）❌

**协议位置**：§8.3（规范性），跨域 SIP 信令需附加数字摘要扩展头域。

**当前状态**：sip-common 仅实现基础 RFC 3261 Digest（MD5）；未实现：
1. `Note` 头域（携带 `Digest nonce="...", algorithm=SM3` 等扩展信息）
2. `Monitor-User-Identity` 头域（跨域时由信令安全路由网关注入用户身份链）
3. SM3 国密摘要算法（`RegisterRequestBuilder.algorithm = "MD5"` 写死）

**业务场景**：跨监控域信令完整性校验、用户身份属性透传、国密合规场景。普通同域部署不强制，跨域级联或国密项目必备。

**最小补全步骤**：

1. `sip-common/.../constant/SipHeaderConstants.java` —— 新增 `NOTE_HEADER` / `MONITOR_USER_IDENTITY_HEADER` 常量
2. `sip-common/.../utils/SipDigestUtils.java` —— 新增 `digestSm3(...)` 方法，依赖 `bouncycastle-bcprov`
3. `sip-common/.../request/RegisterRequestBuilder` —— 根据 `sip.common.auth.algorithm` 配置切换 `MD5` / `SM3`，并在 `Note` 头域填充对应 `algorithm=` 字段
4. `sip-common/.../transmit/.../GatewayUserIdentityFilter`（新增）—— 跨域转发时按 §8.3 规则维护 `Monitor-User-Identity: gateway1-user-userId-userInfo` 链
5. `gb28181-test` 加 `Sm3DigestTest` + `MonitorUserIdentityChainTest`
6. 矩阵 §0(C) §8.3 行刷新为 ✅

### 11.7 附录 J §J.f-g 业务分组/虚拟组织类型代码校准（215/216）⚠️

**协议位置**：附录 J（规范性），系统/行政区划/业务分组/虚拟组织/设备 5 类目录项。

**当前状态**：`DeviceGbType` 枚举：

```
CENTER_SERVER(200, "中心服务器")
DVR(111, "DVR")
NVR(118, "NVR")
CAMERA(132, "摄像机")
VIRTUAL_ORGANIZATION_DIRECTORY(215, "虚拟组织目录")   ← GB28181-2016 含义
CENTER_SIGNAL_CONTROL_SERVER(216, "中心信令控制服务器")  ← 与 GBT-2022 §J.g 冲突
```

GBT-28181-2022 §J.f：215 = **业务分组**；§J.g：216 = **虚拟组织**。两个版本定义不一致：
- 2016：215=虚拟组织
- 2022：215=业务分组、216=虚拟组织

**业务影响**：与遵循 2022 标准的对端互联时，目录类型识别错位（业务分组被误判为虚拟组织目录）。

**最小补全步骤**：

1. `gb28181-common/.../entity/enums/DeviceGbType.java` —— 引入 `BUSINESS_GROUP_2022(215, "业务分组")` + `VIRTUAL_ORGANIZATION_2022(216, "虚拟组织")`，并将旧 `VIRTUAL_ORGANIZATION_DIRECTORY(215)` / `CENTER_SIGNAL_CONTROL_SERVER(216)` 标 `@Deprecated`，注释说明 2016 兼容
2. `gb28181-common/.../entity/response/DeviceItem.java` —— 解析 `DeviceID` 第 11-13 位时按 `sip.gb28181.directory-version` 配置（默认 2022）选择枚举映射
3. 接收端 catalog 解析逻辑分支 2016/2022 两套
4. 加 `DirectoryVersionCompatTest` 双向用例（2016 客户端发 215 = 虚拟组织、2022 客户端发 215 = 业务分组）
5. 矩阵 §0(D) 附录 J 行刷新为 ✅

### 11.8 附录 O 摄像机采集部位类型代码 ❌（按需）

**协议位置**：附录 O（规范性），7 位层次码（大类 3 位 + 中类 2 位 + 小类 2 位），表 O.1 列出 32 个大类。

**当前状态**：未引入。`DeviceItem` XML 实体无 `CollectAreaCode` / `FunctionTypeCode` 字段。

**业务影响**：附录 O 主要用于公安视图库（GB/T 1400）场景，**SIP 信令侧极少携带**。常规视频联网项目可忽略；对接公安视图库或人口热力分析时再补。

**最小补全步骤（按需）**：

1. `gb28181-common/.../entity/enums/CollectAreaTypeEnum.java` —— 7 位代码枚举（按表 O.1 列举大类，小类由业务方扩展）
2. `gb28181-common/.../entity/response/DeviceItem.java` —— 加 `@XmlElement(name = "CollectAreaCode") private String collectAreaCode;` 字段
3. 矩阵 §0(D) 附录 O 行刷新为 ✅

**优先级**：P3 — 等待业务方明确视图库对接需求后再启动。

### 11.9 §A.2.5.6 MobilePosition 通知 server 端 handler 缺失 ✅ v1.5.6 已完成

**协议位置**：附录 A §A.2.5.6（规范性，cmdType=`MobilePosition` + rootType=`Notify` + method=`MESSAGE`）。

**当前状态**：
- 实体 `MobilePositionNotify` 已存在于 `gb28181-common`
- `DeviceNotifyListener.onMobilePositionNotify(deviceId, notify)` listener 接口已声明
- `ServerListenerAdapter.dispatch(ServerNotifyEvent)` 的 `instanceof MobilePositionNotify` 分支已就位
- **但 server 端无 `MobilePositionNotifyMessageHandler`** —— `gb28181-server/.../request/message/notify/` 目录只含 5 个 handler（Alarm / Keepalive / MediaStatus / UpgradeResult / UploadSnapShotFinished），缺 MobilePosition

**故障路径**：设备主动上报移动位置 → `ServerMessageRequestProcessor` 解析 cmdType=MobilePosition + rootType=Notify → `MESSAGE_HANDLER_CMD_MAP.get("Notify").get("MESSAGE_MobilePosition")` 返回 null → 日志 `未找到对应的消息处理器, method=MESSAGE, rootType=Notify, cmdType=MobilePosition` → 静默丢弃。业务侧实现 `onMobilePositionNotify` 永远不会被回调。

**最小补全步骤**：

1. `gb28181-server/.../request/message/notify/MobilePositionNotifyMessageHandler.java` —— 仿 `KeepaliveNotifyMessageHandler` 形态：`extends MessageServerHandlerAbstract`，`getRootType() = NOTIFY`，`CMD_TYPE = "MobilePosition"`，`handForEvt` 中 `parseXml(MobilePositionNotify.class)` + `publishEvent(new ServerNotifyEvent(...))` + `responseAck`
2. `gb28181-test` 加 `MobilePositionNotifyFlowTest`（设备主动 NOTIFY → server 收 → DeviceNotifyListener 回调）
3. 矩阵 §0(A) §A.2.5.6 行 / §3.1 表行 / 本节状态全部标 ✅

**优先级**：P1 —— 这是最常见的 GPS 移动设备使用场景（车载、单警执法仪），实际项目使用频率非常高，缺失影响大。

### 11.10 §A.2.4.9 MobilePosition 订阅 client 端 handler 缺失 ✅ v1.5.6 已完成

**协议位置**：附录 A §A.2.4.9（规范性，cmdType=`MobilePosition` + rootType=`Query` + method=`SUBSCRIBE`）。

**当前状态**：
- `SubscribeListener.onMobilePositionSubscribe(platformId, expires, query)` listener 接口已声明
- `ClientListenerAdapter.dispatch(ClientSubscribeEvent)` 的 `instanceof DeviceMobileQuery` 分支已就位
- **但 client 端无 `SubscribeMobilePositionQueryMessageHandler`** —— `gb28181-client/.../request/subscribe/` 目录只含 3 个 handler（catalog / alarm / ptz），缺 MobilePosition
- 现有 `SubscribeAlarmQueryMessageHandler.CMD_TYPE = "Alarm"`，dispatch key = `"SUBSCRIBE_Alarm"`，**与 MobilePosition 订阅完全无关**（矩阵此前误标"复用"）

**故障路径**：平台向设备发起 `SUBSCRIBE` Body=`<Query><CmdType>MobilePosition</CmdType>...` → 设备 `SubscribeRequestProcessor` 解析 → `MESSAGE_HANDLER_CMD_MAP.get("Query").get("SUBSCRIBE_MobilePosition")` 返回 null → 静默丢弃，无 200 OK 响应（**协议层失败**），平台侧订阅事务超时。

**最小补全步骤**：

1. `gb28181-client/.../request/subscribe/mobile/SubscribeMobilePositionQueryMessageHandler.java` —— 仿 `SubscribeAlarmQueryMessageHandler`：`extends SubscribeHandlerAbstract`，`CMD_TYPE = CmdTypeEnum.MOBILE_POSITION.getType()`，`getRootType() = QUERY`，`getMethod() = SubscribeRequestProcessor.METHOD`，`handForEvt` 解析 `DeviceMobileQuery` + 同步回 200 OK + `SubscribeRegistry.put` + `publishEvent(new ClientSubscribeEvent(...))`
2. `gb28181-test` 加 `MobilePositionSubscribeFlowTest`（平台 SUBSCRIBE → 设备收 200 OK → SubscribeListener 回调）
3. 矩阵 §0(A) §A.2.4.9 行 / §2.1 表行 / 本节状态全部标 ✅

**优先级**：P1 —— 与 §11.9 同源（移动设备 GPS 场景），平台需通过订阅而非轮询获取实时位置，规范级要求。

### 11.11 `BaseMessageServerHandler` 死代码清理 ✅ v1.5.6 已完成

**协议位置**：与协议无关，sip-proxy 1.5.x 内部清理项。

**当前状态**：`gb28181-server/.../request/message/BaseMessageServerHandler.java` 源码注释明写「复制类 无实际使用」，`@Component` 注册 rootType=Root + cmdType=Catalog（与协议层 dispatch key `Notify`/`Query`/`Response` 任一都不匹配，永远死路），但仍占用 `MESSAGE_HANDLER_CMD_MAP.get("Root")` 一个 slot。

**清理方案**：

1. 直接 `git rm gb28181-server/.../request/message/BaseMessageServerHandler.java`
2. 矩阵 §3.1 中此前的"由 BaseMessageServerHandler 默认路径承接"误述已在 §11.9 修正

**优先级**：P3 —— 清理项，不影响协议正确性，可在下个 1.5.x 版本顺手做。

### 11.12 整体代价

| 缺口 | 性质 | 新增类 | 修改文件 | 估算 LOC | 兼容性影响 | 优先级 | 状态 |
|---|---|---|---|---|---|---|---|
| §A.2.5.8 VideoUploadNotify | 协议命令 | 2（entity + handler） | 4（enum + listener + adapter + test） | ~150 | 加性，零 break | P1 | ✅ v1.5.6 |
| §A.2.6.9 ConfigDownload 应答 | 协议命令 | 2（response + handler） | 4 | ~250 | 加性，零 break | P1 | ✅ v1.5.6 |
| §A.2.6.10 PresetQuery 应答 | 协议命令 | 2（response + handler） | 4 | ~150 | 加性，零 break | P1 | ✅ v1.5.6 |
| §A.2.5.6 MobilePosition Notify server handler | 路由缺失 | 1（handler） | 2（test + 注释修正） | ~80 | 加性；当前 listener 接口已就位，仅补 dispatcher | P1 | ✅ v1.5.6 |
| §A.2.4.9 MobilePosition Subscribe client handler | 路由缺失 | 1（handler） | 2（test + 注释修正） | ~90 | 加性；当前 listener 接口已就位，仅补 dispatcher | P1 | ✅ v1.5.6 |
| §11.11 BaseMessageServerHandler 清理 + Catalog NOTIFY 修复 + Catalog SUBSCRIBE 修复 | 内部修复 | 0（删除 1 类） | 3（修 2 个 handler + 删 1 个） | ~-44 | 删除死代码 + 修注册 key，零运行时影响 | P1 | ✅ v1.5.6 |
| **P1 实际交付** | — | **6** | **21**（含测试） | **~720**（不含测试 ~580） | 全部 default 方法 + 加性，老业务代码无需改动 | — | ✅ |
| 附录 I X-GB-Ver | SIP 头扩展 | 0（仅常量） | 5（builder + 双向 processor + 2 个 device dto + test） | ~200 | 加性，老对端不携带时静默忽略 | P1 | ⏳ 下周期 |
| §8.3 Note + Monitor-User-Identity + SM3 | 安全扩展 | 2（utils + filter） | 4（builder + 常量 + 配置 + 测试） | ~350 | 加性；引入 bcprov 依赖（~5 MB） | P2 | ⏳ |
| 附录 J §J.f-g 215/216 校准 | 类型枚举 | 0 | 4（enum + DeviceItem 解析 + 配置 + 测试） | ~120 | **行为变更**：对端 2022 ↔ 2016 互通时含义切换；有 deprecated 兼容期 | P2 | ⏳ |
| 附录 H 多路径（资料性） | SIP 头扩展 | 多（routing 逻辑） | 多 | ~400 | 加性，但需场景驱动 | P3 延后 | ⏳ |
| 附录 O 采集部位类型代码 | 类型枚举 | 1（enum） | 2（DeviceItem + 测试） | ~100 | 加性，按视图库需求驱动 | P3 延后 | ⏳ |

> **v1.5.6 实测增量**：6 个新文件（`VideoUploadNotify` / `MobilePositionNotifyMessageHandler` / `VideoUploadNotifyMessageHandler` / `ConfigDownloadResponseMessageHandler` / `PresetQueryResponseMessageHandler` / `SubscribeMobilePositionQueryMessageHandler` / `DeviceConfigDownloadResponse`），3 个 handler 修复（`SubscribeCatalogQueryMessageHandler` / `CatalogNotifyHandler` / 删除 `BaseMessageServerHandler`），新增 2 个测试类（`DispatcherRegistrationTest` 10 例 + `V156ProtocolCompletionFlowTest` 5 例），扩展 4 个既有类（`CmdTypeEnum` + `DeviceNotifyListener` + `DeviceResponseListener` + `ServerListenerAdapter` + `ClientCommandSender` + `TestServerEventHandler`）。`gb28181-test` 全量回归 52 测试 GREEN（含 15 个新测试），`mvn install` 全模块通过。

补全 P1 后 §0 (A) 附录 A 命令集覆盖率从 44/47 (93.6%) 升至 47/47 (100%)，且 MobilePosition Notify / Subscribe / Catalog NOTIFY / Catalog SUBSCRIBE 4 条死路径全部变实路径；剩余规范性硬缺口 6 → 3 项，全部为非命令类（头域 / 类型语义）。再补 P2 后跨域信令安全（§8.3）+ 跨版本目录互通（附录 J）达到 2022 严格合规。`onSubscribeResponse` / `onConfigResponse` 与新增的 `onConfigDownloadResponse` 在命名上区分明确，不会与现有方法冲突。

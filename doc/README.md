# sip-proxy 文档索引

本目录按 **协议规范 / 跨版本架构 / 方案版本** 三个维度组织。新增文档时请放入对应子目录。

## protocol/ — 协议规范原文

按国家标准发布年份分组，仅存放规范本身（PDF + 转写 Markdown）。

| 路径 | 内容 |
|------|------|
| [protocol/2016/GB28181-2016.md](protocol/2016/GB28181-2016.md) / `.pdf` | GB/T 28181-2016 完整原文 |
| [protocol/2016/GB28181-2016-pages-1.md](protocol/2016/GB28181-2016-pages-1.md) / `pages-{1,2}.pdf` | 2016 分册页面（差异留档） |
| [protocol/2022/GBT-28181-2022.md](protocol/2022/GBT-28181-2022.md) / `.pdf` | GB/T 28181-2022 完整原文 |

## architecture/ — 跨版本架构与设计基线

不与单一版本绑定的设计文档，随主线持续刷新；动手前先读这一栏。

| 路径 | 内容 |
|------|------|
| [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) | 整体架构总览（最新随主线版本） |
| [architecture/LAYERED-ARCHITECTURE.md](architecture/LAYERED-ARCHITECTURE.md) | sip-proxy ↔ sip-gateway ↔ 业务服务器分层方案 |
| [architecture/HORIZONTAL-SCALING.md](architecture/HORIZONTAL-SCALING.md) | 多节点部署、状态分层、VIP / NAT 处理 |
| [architecture/LISTENER-LAYERED-DESIGN.md](architecture/LISTENER-LAYERED-DESIGN.md) | Listener 化业务接口分层设计（1.5.0 起为接入主线） |
| [architecture/PROTOCOL-LAYERING-MATRIX.md](architecture/PROTOCOL-LAYERING-MATRIX.md) | L0 / L1 / L2 三层协议栈逐 cmdType 落地矩阵 |
| [architecture/LISTENER-MIGRATION-GUIDE.md](architecture/LISTENER-MIGRATION-GUIDE.md) | v1.4.0 → v1.5.0 业务侧迁移指南 |

## plans/ — 按方案版本分组的实施 / 重构计划

每份方案锚定��个目标版本。落地后保留作为历史记录，方便回溯设计动机。

| 版本 | 文档 | 主题 |
|------|------|------|
| 1.3.0 | [PROTOCOL-DECOUPLING-PLAN.md](plans/1.3.0/PROTOCOL-DECOUPLING-PLAN.md) | sip-common / gb28181-common 边界规则 |
| 1.3.0 | [REFACTOR-COMMAND-LAYER.md](plans/1.3.0/REFACTOR-COMMAND-LAYER.md) | 命令发送层重构 |
| 1.3.0 | [REFACTOR-SIP-COMMON.md](plans/1.3.0/REFACTOR-SIP-COMMON.md) | sip-common 重构 |
| 1.3.0 | [REFACTOR-HANDLER-TO-SPRING-EVENT.md](plans/1.3.0/REFACTOR-HANDLER-TO-SPRING-EVENT.md) | Handler → Spring Event 统一化 |
| 1.3.0 | [BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md](plans/1.3.0/BREAKING-CHANGE-REMOVE-HANDLER-INTERFACE.md) | 移除 *Handler 接口（前序方案） |
| 1.3.0 | [INVITE-REFACTOR-PLAN.md](plans/1.3.0/INVITE-REFACTOR-PLAN.md) | 入站 INVITE 异步化 |
| 1.3.0 | [TEST-PLAN.md](plans/1.3.0/TEST-PLAN.md) | 与 1.3.0 重构配套的测试方案 |
| 1.4.0 | [REFACTOR-BUSINESS-LAYER.md](plans/1.4.0/REFACTOR-BUSINESS-LAYER.md) | 业务接入层重构 |
| 1.6.0 | [FRONT-END-CONTROL-PROTOCOL-PLAN.md](plans/1.6.0/FRONT-END-CONTROL-PROTOCOL-PLAN.md) | GBT-28181-2022 §A.3 PTZ 二进制指令编码 |
| 1.7.0 | [OUTBOUND-DIALOG-PLAN.md](plans/1.7.0/OUTBOUND-DIALOG-PLAN.md) | 出站 Dialog 维护（BYE / SUBSCRIBE refresh dialog-aware） |
| 1.8.0 | [GB28181-GATEWAY-MODULE-PLAN.md](plans/1.8.0/GB28181-GATEWAY-MODULE-PLAN.md) | gb28181-gateway 模块化（含 1.7.3 协议层小升级） |

## 维护约定

- **新增方案**：在 `plans/<目标版本>/` 下新建文件；如版本目录不存在则同时创建。
- **跨版本演进的设计文档**：放 `architecture/`，文档头部用版本日志标注变化。
- **协议规范升级**：在 `protocol/<年份>/` 下新增对应 PDF / Markdown，不要替换旧版本。
- **新增 cmdType**：先更新 [architecture/PROTOCOL-LAYERING-MATRIX.md](architecture/PROTOCOL-LAYERING-MATRIX.md)，再改代码。
- **顶层引用**：`CLAUDE.md` / `README.md` / `CHANGELOG.md` 引用文档时使用本结构下的完整路径。

# Ozon Delivery Retrospective

## 1. 目标回顾

本轮 Ozon 交付的目标，不是单点补功能，而是把 Ozon 从“若干能力已落地但分散、默认写链路关闭、缺少交付闭环”的状态，推进到“核心链路完整、前后端融合、可观测、可验证、可灰度发布”的状态。

最终落点包括：

- 核心业务链路完整
- 前端工作台融合完成
- 运维观测补齐
- 页面级回归落地
- 本地验证脚本成套
- CI 最小验证链路落地
- 发布交接文档成套

## 2. 交付路径回顾

### 2.1 业务能力补齐

先补了 Ozon 的主工作台链路：

- Auth / Warehouse / Delivery Method / Init Task
- Product / Preview / Publish / Publish Task History
- Price / Stock
- Posting / Shipment / AfterSale
- Finance / Chat / Ads
- Task / Error / Ops

这一步解决的是“能不能完成业务闭环”。

### 2.2 前后端融合

随后把散点能力收回到原有页面体系：

- `Task / Error` 作为统一运维入口
- `Product / Posting / Finance / Chat / Ads` 做成工作台，不再开平行页面
- 页面统一感知 feature gate
- 深链、焦点恢复、筛选状态恢复全部补齐

这一步解决的是“用户是否能在同一套 UI 中稳定使用”。

### 2.3 运维观测

之后进入 support layer：

- `ops summary`
- `api log`
- `operation audit`

这一步解决的是“上线后是否可维护、可定位、可重试”。

### 2.4 自动化验证

再往后补验证闭环：

- 后端 Ozon 定向单测
- 前端 Playwright 页面级回归
- 只读 / 网关 / 写链路 smoke
- `npm audit`
- CI workflow

这一步解决的是“是否能稳定重复验证，而不是靠手工演示”。

### 2.5 发布交接

最后补的是交付资产：

- gray rollout runbook
- release checklist
- CI pipeline doc
- handoff / summary / release note / index / portal

这一步解决的是“是否能交给别人执行，而不是只能由当前实施者口头说明”。

## 3. 关键决策

### 3.1 不重建第二套 Ozon UI

坚持复用现有：

- `HeaderPlatform`
- `ozon.js` 路由
- `Task / Error`
- 现有 Element Plus / shared components

这避免了后续出现“双入口、双工作流、双运维系统”的维护灾难。

### 3.2 默认不开写链路

没有为了“演示完整”而把 write gate 全部打开。保持：

- `product-write`
- `stock-write`
- `price-write`
- `posting-write`

按顺序灰度，是保证上线风险可控的前提。

### 3.3 不强行接未验证的远端合同能力

`chat-send` 和 `ads-sync` 没有被硬上正式能力，而是先做成同页双模预留位。这样既保留未来演进路径，也避免把未验证合同接口变成生产风险。

### 3.4 优先做验证与交付链，而不是无限堆功能

后期工作重点明显从“补更多页面”转向了：

- 页面级回归
- smoke 脚本
- CI
- 交接模板
- 索引/portal

这是正确的收口顺序。否则功能再多，也不能稳定上线。

## 4. 最终交付物

### 4.1 代码侧

- Ozon 业务域实现
- Ozon ops 观测域
- 前端 Ozon 工作台与双模页面
- Playwright Ozon 页面级回归
- 前端高风险依赖清理

### 4.2 文档侧

- [Ozon Delivery Status](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-status.md)
- [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
- [Ozon Gray Rollout Runbook](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)
- [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
- [Ozon CI Pipeline](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)
- handoff / summary / release note / verification index templates

### 4.3 脚本侧

- [90-full-check.sh](/mnt/d/project/wimoor/tools/ozon/local/90-full-check.sh)
- [95-release-handoff.sh](/mnt/d/project/wimoor/tools/ozon/local/95-release-handoff.sh)
- [96-verify-summary.sh](/mnt/d/project/wimoor/tools/ozon/local/96-verify-summary.sh)
- [97-release-note.sh](/mnt/d/project/wimoor/tools/ozon/local/97-release-note.sh)
- [98-verify-index.sh](/mnt/d/project/wimoor/tools/ozon/local/98-verify-index.sh)

## 5. 当前剩余问题

### 5.1 仍需真实环境验证的部分

- GitHub Actions 远端实际执行结果
- 灰度环境 write gate 顺序演练
- `chat-send / ads-sync` 的正式接入验证

### 5.2 非阻断工程问题

- 本地 `90-full-check.sh` 执行时间偏长
- CI 当前主要是最小链路，还可以继续做 artifact 汇总与门禁增强

## 6. 本轮交付最重要的价值

这轮最大的价值不是“又多了几个接口”，而是：

1. Ozon 已经不是一堆分散页面，而是一套完整工作流。
2. Ozon 已经不是只能本机演示，而是具备可重复验证和发布交接能力。
3. 后续工作不再需要回头重建基础设施，而可以直接进入真实灰度与环境演练。

## 7. 建议后续动作

### 7.1 如果目标是上线

- 先跑一轮真实灰度演练
- 验证 `ozon-verify` workflow
- 使用 `90 -> 95 -> 96 -> 97 -> 98` 生成完整交付材料

### 7.2 如果目标是继续扩展

- 单独立项推进 `chat-send`
- 单独立项推进 `ads-sync`
- 在真实合同和远端接口 fresh verification 完成后再打开

### 7.3 如果目标是团队交接

- 先看 portal
- 再看 delivery status
- 最后按 checklist 和 handoff 执行

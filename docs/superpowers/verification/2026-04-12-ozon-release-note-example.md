# Ozon Release Note Example

> 本文件是发布说明的示例填法，用于说明一份可交付的 Ozon release note 应该覆盖哪些信息。

## Release Snapshot

- Commit: `6324491`
- Environment: `gray`
- Related handoff: `ozon-release-handoff-20260412-0850.md`
- Verification Summary: `ozon-verify-summary-20260412-0855.md`

## Included Changes

- `ed97801` 运维观测接线与页面融合
- `fef808c` 财务聊天广告双模融合
- `2a137b2` Ozon 页面级回归测试
- `16e28f3` Ozon CI 验证工作流
- `e27a912` 高风险前端依赖移除

## Validation Coverage

- Backend verification:
  - Ozon 定向 Maven 测试通过
- Frontend Playwright:
  - `npm run test:e2e:ozon` 通过
- Frontend build:
  - `build_in_linux_fs.sh` 通过
- Frontend audit:
  - `npm audit --omit=dev` 通过
- Local smoke:
  - `60 / 65 / 70 / 90` 已通过一轮

## Release Notes

- User-facing changes:
  - Ozon 工作台已覆盖 Auth、Product、Posting、Finance、Chat、Ads 主链路
  - Task / Error 已支持深链回跳与运维关联查看
- Operational changes:
  - `ops summary / api log / operation audit` 已接入
  - 本地 smoke、Playwright、CI 与发布交接文档已成套
- Feature gates involved:
  - `auth / product / task / error / finance / chat / ads`
  - 灰度阶段按顺序控制 `product-write -> stock-write/price-write -> posting-write`
- Risks / caveats:
  - `chat-send / ads-sync` 仍保持关闭
  - 真实环境灰度顺序不可并发跳步

## Rollback

- Rollback gates:
  - `product-write`
  - `stock-write`
  - `price-write`
  - `posting-write`
- Rollback owner:
  - release-operator
- Trigger conditions:
  - 页面级回归失败
  - 只读 smoke 失败
  - `Task / Error` 深链恢复失效
  - 远端写链路连续失败

## Links

- Gray rollout runbook: [2026-04-11-ozon-gray-rollout-runbook.md](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)
- Release checklist: [2026-04-11-ozon-release-checklist.md](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
- CI pipeline: [2026-04-11-ozon-ci-pipeline.md](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)
- CI post-run checklist: [2026-04-12-ozon-ci-postrun-checklist.md](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md)

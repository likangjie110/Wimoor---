# Ozon CI 验证流水线说明

## 1. 目标

本文件说明仓库中的 Ozon 最小 CI 验证链路，确保后端定向测试、前端页面级回归、构建和安全审计可以在统一流水线上执行。

## 2. 工作流文件

GitHub Actions 工作流位置：

```text
.github/workflows/ozon-verify.yml
```

统一导航页：

- [Ozon Verification README](/mnt/d/project/wimoor/docs/superpowers/verification/README.md)
- [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
- [Ozon CI Post-Run Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md)

## 3. 覆盖内容

### Backend Job

- JDK 8
- Ozon 模块定向 Maven 测试
- 覆盖：
  - auth
  - seller
  - product
  - price / stock
  - posting / shipment / aftersale
  - finance / chat / ads
  - ops
  - feature gate
  - smoke wiring

### Frontend Job

- Node 20
- `npm install`
- Playwright Chromium cache
- `npx playwright install --with-deps chromium`
- `node scripts/check_ozon_route_smoke.mjs`
- `npm run test:e2e:ozon`
- `./scripts/build_in_linux_fs.sh`
- `npm audit --omit=dev`

## 4. 触发条件

当前工作流会在以下情况触发：

- push 到涉及 Ozon、前端、验证脚本或 workflow 本身的改动
- pull request 涉及上述路径
- 手工 `workflow_dispatch`

## 5. 与本地验证的关系

CI 覆盖的是“不依赖本地基础设施”的稳定链路：

- 后端定向单测
- 前端 Playwright 回归
- 前端构建
- 前端安全审计

本地 `60 / 65 / 70 / 90` smoke 仍然负责：

- 网关连通
- 本地环境写链路
- 带 Nacos / MySQL / Redis / Gateway 的真实联调

## 6. 推荐使用方式

开发阶段：

```bash
cd wimoorui && npm run test:e2e:ozon
```

发布前：

```bash
bash tools/ozon/local/90-full-check.sh
bash tools/ozon/local/95-release-handoff.sh
bash tools/ozon/local/96-verify-summary.sh
bash tools/ozon/local/97-release-note.sh
bash tools/ozon/local/98-verify-index.sh
```

PR 合并前：

- 观察 `ozon-verify` workflow 是否双 job 都为绿色
- workflow 完成后按 `CI Post-Run Checklist` 复核 artifact 与 summary

## 7. 产物留痕

当前 workflow 会在执行后上传以下 artifact：

- `ozon-backend-surefire-reports`
  - Ozon 后端定向测试报告
- `ozon-frontend-artifacts`
  - Playwright `playwright-report`
  - Playwright `test-results`
  - `audit-report.txt`
- `ozon-verification-docs`
  - Ozon 验证静态文档集合
  - 建议从 portal/checklist/runbook 开始阅读

同时会把 backend / frontend 的验证摘要写入 `GITHUB_STEP_SUMMARY`。

本地建议额外保留：

- `ozon-local-report-*.md`
- `ozon-release-handoff-*.md`
- `ozon-verify-summary-*.md`
- `ozon-release-note-*.md`
- `ozon-verification-index.md`

建议优先阅读：

1. [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
2. `ozon-verification-index.md`
3. `ozon-release-handoff-*.md`

## 8. 后续可扩展项

- 将 `90-full-check.sh` 的报告产物作为 workflow artifact 上传
- 增加 `Auth / Finance / Chat / Ads` 之外的更深层业务回归
- 将 Ozon 灰度 gate 状态输出到 CI 报告摘要

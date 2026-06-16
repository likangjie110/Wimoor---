# Ozon Verification Portal

## 1. 目标

本页作为 Ozon 验证与发布交付的单点导航入口，用于统一索引：

- 本地脚本
- 静态流程文档
- 交接模板
- 动态验证产物
- CI 工作流

README 风格总入口：

- [Ozon Verification README](/mnt/d/project/wimoor/docs/superpowers/verification/README.md)

## 2. 快速入口

### 本地验证主入口

```bash
bash tools/ozon/local/90-full-check.sh
```

### 发布交接产物

```bash
bash tools/ozon/local/95-release-handoff.sh
bash tools/ozon/local/96-verify-summary.sh
bash tools/ozon/local/97-release-note.sh
bash tools/ozon/local/98-verify-index.sh
```

### 前端页面级回归

```bash
cd wimoorui && npm run test:e2e:ozon
```

### CI 工作流

```text
.github/workflows/ozon-verify.yml
```

## 3. 本地脚本导航

- [00-env.example](/mnt/d/project/wimoor/tools/ozon/local/00-env.example)
- [60-smoke-readonly.sh](/mnt/d/project/wimoor/tools/ozon/local/60-smoke-readonly.sh)
- [65-smoke-gateway.sh](/mnt/d/project/wimoor/tools/ozon/local/65-smoke-gateway.sh)
- [70-smoke-write.sh](/mnt/d/project/wimoor/tools/ozon/local/70-smoke-write.sh)
- [90-full-check.sh](/mnt/d/project/wimoor/tools/ozon/local/90-full-check.sh)
- [95-release-handoff.sh](/mnt/d/project/wimoor/tools/ozon/local/95-release-handoff.sh)
- [96-verify-summary.sh](/mnt/d/project/wimoor/tools/ozon/local/96-verify-summary.sh)
- [97-release-note.sh](/mnt/d/project/wimoor/tools/ozon/local/97-release-note.sh)
- [98-verify-index.sh](/mnt/d/project/wimoor/tools/ozon/local/98-verify-index.sh)

## 4. 静态文档导航

- [Ozon Delivery Status](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-status.md)
- [Ozon Delivery Retrospective](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-retrospective.md)
- [Ozon Gray Rollout Runbook](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)
- [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
- [Ozon CI Pipeline](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)
- [Ozon CI Post-Run Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md)

## 5. 模板导航

- [Deployment Handoff Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-deployment-handoff-template.md)
- [Release Note Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-note-template.md)
- [Release Note Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-release-note-example.md)
- [Verification Index Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-index-template.md)
- [Gray Rehearsal Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-template.md)
- [Gray Rehearsal Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-example.md)

## 6. 动态产物约定

运行完整验证链后，目录中通常会出现以下文件：

- `ozon-local-report-*.md`
- `ozon-release-handoff-*.md`
- `ozon-verify-summary-*.md`
- `ozon-release-note-*.md`
- `ozon-verification-index.md`

推荐先看：

1. `ozon-verification-index.md`
2. `ozon-verify-summary-*.md`
3. `ozon-release-handoff-*.md`
4. `ozon-release-note-*.md`

## 7. 建议阅读顺序

1. 先看本页，明确入口。
2. 再看 [Ozon Delivery Status](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-status.md)。
3. 再看 [Ozon Delivery Retrospective](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-retrospective.md)。
4. 然后看 [Ozon Gray Rollout Runbook](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)。
5. 发布前核对 [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)。
6. 若走自动化链路，先看 [Ozon CI Pipeline](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)。
7. CI 执行完成后，再按 [Ozon CI Post-Run Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md) 验收。

# Ozon Verification README

## 1. 这是什么

这是 Ozon 交付链路的总入口页。

如果你是第一次接手 Ozon，或者只想快速找到：

- 怎么跑本地验证
- 怎么看 CI
- 怎么做灰度
- 怎么生成交接文档
- 当前交付到了什么程度

优先看这一页。

## 2. 推荐阅读顺序

1. [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
2. [Ozon Delivery Status](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-status.md)
3. [Ozon Delivery Retrospective](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-retrospective.md)
4. [Ozon Gray Rollout Runbook](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)
5. [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
6. [Ozon CI Pipeline](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)
7. [Ozon CI Post-Run Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md)
8. [Ozon Gray Rehearsal Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-template.md)
9. [Ozon Gray Rehearsal Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-example.md)
10. [Ozon Release Note Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-release-note-example.md)

## 3. 本地执行入口

### 3.1 完整验证

```bash
bash tools/ozon/local/90-full-check.sh
```

### 3.2 交付文档链

```bash
bash tools/ozon/local/95-release-handoff.sh
bash tools/ozon/local/96-verify-summary.sh
bash tools/ozon/local/97-release-note.sh
bash tools/ozon/local/98-verify-index.sh
```

### 3.3 前端页面级回归

```bash
cd wimoorui && npm run test:e2e:ozon
```

## 4. CI 入口

工作流文件：

- [.github/workflows/ozon-verify.yml](/mnt/d/project/wimoor/.github/workflows/ozon-verify.yml)

远端跑完后验收：

- [Ozon CI Post-Run Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-ci-postrun-checklist.md)

## 5. 交付产物

静态文档：

- [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
- [Ozon Delivery Status](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-status.md)
- [Ozon Delivery Retrospective](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-delivery-retrospective.md)
- [Ozon Gray Rollout Runbook](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md)
- [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
- [Ozon CI Pipeline](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md)

模板：

- [Deployment Handoff Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-deployment-handoff-template.md)
- [Release Note Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-note-template.md)
- [Release Note Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-release-note-example.md)
- [Verification Index Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-index-template.md)
- [Gray Rehearsal Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-template.md)
- [Gray Rehearsal Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-example.md)

动态产物：

- `ozon-local-report-*.md`
- `ozon-release-handoff-*.md`
- `ozon-verify-summary-*.md`
- `ozon-release-note-*.md`
- `ozon-verification-index.md`

## 6. 什么时候继续开发，什么时候该上线

继续开发：

- 要接 `chat-send`
- 要接 `ads-sync`
- 要扩更深的业务对象或真实远端合同能力

进入上线/灰度：

- 当前实现范围已经满足业务闭环
- 本地验证脚本和 Playwright 已通过
- CI 工作流和 post-run 检查路径已明确
- 发布交接模板和说明链已成套

# Ozon CI Post-Run Checklist

## 1. 适用范围

本清单用于 `ozon-verify` GitHub Actions 工作流执行完成后的验收动作。

目标不是“看到绿色就结束”，而是确认：

- job 结果正确
- artifact 可下载
- 文档入口一致
- 后续 gray / release 仍有据可依

## 2. 先看哪里

按这个顺序：

1. workflow 总体结果
2. `GITHUB_STEP_SUMMARY`
3. `ozon-backend-surefire-reports`
4. `ozon-frontend-artifacts`
5. `ozon-verification-docs`

## 3. Job 级检查

### 3.1 Backend Job

- [ ] `ozon-backend` job 为绿色
- [ ] Maven 定向测试没有被 `failIfNoTests=false` 掩盖真实失败
- [ ] `ozon-backend-surefire-reports` artifact 存在
- [ ] 关键测试类在报告中可见：
  - [ ] `OzonSmokeWorkflowTests`
  - [ ] `OzonOpsServiceTests`
  - [ ] `OzonFinanceImportTests`
  - [ ] `OzonChatSyncTests`
  - [ ] `OzonAdsReportTests`

### 3.2 Frontend Job

- [ ] `ozon-frontend` job 为绿色
- [ ] `npm run test:e2e:ozon` 成功
- [ ] `build_in_linux_fs.sh` 成功
- [ ] `npm audit --omit=dev` 成功
- [ ] `ozon-frontend-artifacts` artifact 存在

## 4. Artifact 级检查

### 4.1 `ozon-backend-surefire-reports`

- [ ] 能下载
- [ ] 至少包含 Ozon 模块 `surefire-reports`
- [ ] 可定位失败或通过信息

### 4.2 `ozon-frontend-artifacts`

- [ ] `playwright-report/` 存在
- [ ] `test-results/` 存在
- [ ] `audit-report.txt` 存在
- [ ] 若 Playwright 失败，能在 artifact 中看到 trace / screenshot / video

### 4.3 `ozon-verification-docs`

- [ ] 包含静态 Ozon verification 文档
- [ ] 至少能看到：
  - [ ] portal
  - [ ] delivery status
  - [ ] delivery retrospective
  - [ ] gray rollout runbook
  - [ ] release checklist
  - [ ] CI pipeline

## 5. Summary 检查

在 workflow 的 `Summary` 页面确认：

- [ ] backend summary 存在
- [ ] frontend summary 存在
- [ ] summary 中提到了 artifact 名称
- [ ] summary 与 portal 中的命名一致

## 6. 失败时怎么处理

### 6.1 Backend 失败

- 优先下载 `ozon-backend-surefire-reports`
- 找失败测试类
- 判断是：
  - 真实功能回退
  - 环境偶发问题
  - 依赖/构建层问题

### 6.2 Frontend 失败

- 优先查看 `audit-report.txt`
- 再看 Playwright `trace / screenshot / video`
- 若 build 失败，先检查依赖锁文件和最近前端包升级

### 6.3 文档 artifact 缺失

- 检查 workflow `upload-artifact` path
- 检查 portal / checklist / CI pipeline 文档路径是否变化

## 7. 通过后下一步

CI 通过并不等于可直接上线。通过后仍需：

1. 对照 [Ozon Release Checklist](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-release-checklist.md)
2. 结合 [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)
3. 在真实环境执行 gray / release 演练

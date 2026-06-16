# Ozon 最终发布 Checklist

## 1. 适用范围

本清单用于 Ozon 相关功能从开发完成进入灰度和正式上线前的最终核对。

统一导航页：

- [Ozon Verification Portal](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-11-ozon-verification-portal.md)

## 2. 提交前要求

- 当前分支工作区必须干净。
- Ozon 相关改动必须已经拆分为可回溯 commit。
- 若涉及前端依赖升级，`npm audit --omit=dev` 结果必须附在发布说明中。

## 3. 自动化验证矩阵

必须全部通过：

```bash
mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am test
```

```bash
cd wimoorui && npm run test:e2e:ozon
```

```bash
bash wimoorui/scripts/build_in_linux_fs.sh
```

```bash
bash tools/ozon/local/60-smoke-readonly.sh
bash tools/ozon/local/65-smoke-gateway.sh
bash tools/ozon/local/70-smoke-write.sh
```

全量汇总入口：

```bash
bash tools/ozon/local/90-full-check.sh
```

发布交接文档生成：

```bash
bash tools/ozon/local/95-release-handoff.sh
bash tools/ozon/local/96-verify-summary.sh
bash tools/ozon/local/97-release-note.sh
bash tools/ozon/local/98-verify-index.sh
```

## 4. 页面级回归要求

当前 Ozon Playwright 回归最少覆盖以下 8 条：

- Product 深链恢复与发布区焦点恢复
- Posting 路由状态恢复与详情自动打开
- Task 筛选恢复与运维摘要
- Error 载荷抽屉与关联日志
- Auth 授权工作台与 3 个子标签页
- Finance 最近任务结果与原文 drawer
- Chat 会话、消息、回复审计
- Ads 账号级联、报表汇总、同步意图

## 5. 灰度顺序

固定顺序：

1. `auth / product / task / error / finance / chat / ads`
2. `product-write`
3. `stock-write / price-write`
4. `posting-write`

默认不纳入：

- `chat-send`
- `ads-sync`

## 6. 上线前人工核对

- 权限 SQL 已同步到环境。
- `HeaderPlatform`、菜单、路由入口无回退。
- `Task / Error` 深链跳转到源页面可用。
- Ozon `ops` 摘要、接口日志、操作审计可读。
- 本地冒烟报告已生成并归档到 `docs/superpowers/verification/`。
- release handoff 文档已生成并填写关键发布信息。
- verification summary 文档已生成并完成最终复核。
- release note 文档已生成并补齐用户侧与运维侧说明。
- verification index 文档已更新到最新产物。
- 如执行真实灰度，gray rehearsal 文档已生成并填写。
- 如需对照写法，release note 示例已可直接参考。

## 7. 回滚条件

出现以下任一情况应停止灰度并回滚对应 gate：

- 只读 smoke 失败。
- 页面级回归失败。
- `Task` 或 `Error` 无法恢复定位到源页面。
- `ops` 日志无法记录写链路行为。
- 新开启的 write gate 在灰度环境出现连续失败。

## 8. 建议发布说明模板

```text
版本:
提交:
模块:
灰度 gate:
自动化验证:
- backend:
- e2e:
- build:
- smoke:
剩余风险:
- 
回滚开关:
- 
```

# Ozon 灰度发布与验证 Runbook

## 1. 目标

本文件用于复用 Ozon 的本地 smoke、灰度发布和上线前验证流程，避免每次上线都依赖一次性手工演示。

## 2. 固定灰度顺序

1. `auth / product / task / error / finance / chat / ads`
2. `product-write`
3. `stock-write / price-write`
4. `posting-write`

本轮不纳入默认灰度：

- `chat-send`
- `ads-sync`

这两项仅允许在完成 fresh verification 和专项计划后单独启用。

灰度演练记录模板：

- [Ozon Gray Rehearsal Template](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-template.md)
- [Ozon Gray Rehearsal Example](/mnt/d/project/wimoor/docs/superpowers/verification/2026-04-12-ozon-gray-rehearsal-example.md)

## 3. 本地验证入口

只读冒烟：

```bash
bash tools/ozon/local/60-smoke-readonly.sh
```

网关冒烟：

```bash
bash tools/ozon/local/65-smoke-gateway.sh
```

写链路冒烟：

```bash
bash tools/ozon/local/70-smoke-write.sh
```

全量检查：

```bash
bash tools/ozon/local/90-full-check.sh
```

## 4. 覆盖范围

`60-smoke-readonly.sh` 当前覆盖：

- feature meta
- auth list / ping
- seller warehouse / delivery method
- product draft list / draft detail / category tree / preview / publish task list / publish task detail
- stock snapshot / stock task
- price snapshot / price task
- posting list / posting detail
- shipment history
- aftersale detail
- task list
- error list
- finance task list / transaction list / task raw
- chat session list / message list / reply audit list
- ads account / campaign / report / summary
- ops summary / api log / operation audit

`70-smoke-write.sh` 当前覆盖：

- auth bind
- stock push
- price push
- posting sync
- shipment pushTracking
- product publish
- finance local import
- chat local import + reply audit
- ads local import

## 5. 前端页面级回归

当前复用以下静态 smoke：

```bash
node wimoorui/scripts/check_ozon_entry.mjs
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
node wimoorui/scripts/check_ozon_route_smoke.mjs
```

其中 `check_ozon_route_smoke.mjs` 固定校验 4 条主回路：

- Product 深链与焦点恢复
- Posting 路由状态恢复与 Shipment 跳转
- Task 筛选状态与运维摘要
- Error 筛选状态与关联日志加载

## 6. 上线前必须确认

- `mvn` Ozon 定向测试通过
- `wimoorui` 构建通过
- 本地 `60 / 65 / 70 / 90` 脚本至少通过一次
- 默认配置下 `chat-send` 与 `ads-sync` 保持关闭
- 灰度环境只逐步开放一层 write gate，不并发放开全部写能力

## 7. 异常处理

- 若 `60-smoke-readonly.sh` 失败，先检查 `AUTH_ID`、后端端口和本地会话头。
- 若 `65-smoke-gateway.sh` 失败，优先检查 `jsessionid`、gateway 端口和 admin 登录。
- 若 `70-smoke-write.sh` 失败，先确认 Nacos 中的 write gate 是否已开启，再检查样例物料和映射。
- 若 `90-full-check.sh` 失败，优先查看 `docs/superpowers/verification/ozon-local-report-*.md` 中最后一个失败步骤。

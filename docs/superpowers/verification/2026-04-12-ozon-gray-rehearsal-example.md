# Ozon Gray Rehearsal Example

> 本文件是灰度演练记录的示例填法，用于说明一轮“按 gate 顺序推进、逐段验证、逐段决策”的记录方式。

## 1. 基本信息

- Date: 2026-04-12
- Environment: gray
- Operator: release-operator
- Observer: biz-owner

## 2. Scope

- Version: `2026.04-gray.1`
- Commit: `6324491`
- Related release note: `ozon-release-note-20260412-0900.md`
- Related handoff: `ozon-release-handoff-20260412-0850.md`

## 3. Planned Gate Order

1. `auth / product / task / error / finance / chat / ads`
2. `product-write`
3. `stock-write / price-write`
4. `posting-write`

## 4. Actual Execution

### Phase 1

- Gate: `auth / product / task / error / finance / chat / ads`
- Start time: `2026-04-12 09:00 +0800`
- End time: `2026-04-12 09:20 +0800`
- Validation:
  - Ozon 页面入口可见
  - `Task / Error` 深链恢复正常
  - `Finance / Chat / Ads` 工作台可读
- Result: pass
- Issues: none

### Phase 2

- Gate: `product-write`
- Start time: `2026-04-12 09:25 +0800`
- End time: `2026-04-12 09:40 +0800`
- Validation:
  - Product 发布按钮可用
  - 发布任务详情正常回写
  - publish task history 正常显示
- Result: pass
- Issues:
  - 一次发布任务 polling 停留时间偏长，但未失败

### Phase 3

- Gate: `stock-write / price-write`
- Start time: `2026-04-12 09:45 +0800`
- End time: `2026-04-12 10:05 +0800`
- Validation:
  - Price push 成功
  - Stock push 成功
  - `Task` 中 `PRICE_SYNC / STOCK_SYNC` 记录生成
  - `ops summary` 与 operation audit 有新增
- Result: pass
- Issues: none

### Phase 4

- Gate: `posting-write`
- Start time: `2026-04-12 10:10 +0800`
- End time: `2026-04-12 10:40 +0800`
- Validation:
  - Posting sync 成功
  - Shipment pushTracking 成功
  - `Error` 无新增 OPEN 记录
  - `Posting / Shipment / AfterSale` 联动正常
- Result: pass
- Issues:
  - 远端接口响应较慢，需要继续观察

## 5. Stop / Continue Decision

- Can continue: yes
- Blocking issues: none
- Rollback needed: no

## 6. Final Summary

- User-facing impact:
  - Ozon 核心工作台已可在灰度环境完整使用
- Operational impact:
  - `ops` 日志和审计已能覆盖灰度写链路
- Follow-up actions:
  - 继续观察 `posting-write` 的远端响应时间
  - 暂不开放 `chat-send / ads-sync`

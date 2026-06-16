# Ozon Gray Rehearsal Template

## 1. 使用场景

本模板用于真实灰度环境演练记录。

目标不是替代 release note，而是专门记录：

- 这次灰度到底开了哪些 gate
- 每一阶段验证了什么
- 什么时候停下、什么时候继续
- 是否满足进入下一阶段的条件

## 2. 推荐使用时机

在以下场景使用：

- 首次灰度前
- 新增 write gate 灰度前
- 灰度过程中发生异常，需要留痕与复盘

## 3. 模板

```text
# Ozon Gray Rehearsal

Date:
Environment:
Operator:
Observer:

## Scope
- Version:
- Commit:
- Related release note:
- Related handoff:

## Planned Gate Order
1. auth / product / task / error / finance / chat / ads
2. product-write
3. stock-write / price-write
4. posting-write

## Actual Execution

### Phase 1
- Gate:
- Start time:
- End time:
- Validation:
- Result:
- Issues:

### Phase 2
- Gate:
- Start time:
- End time:
- Validation:
- Result:
- Issues:

### Phase 3
- Gate:
- Start time:
- End time:
- Validation:
- Result:
- Issues:

### Phase 4
- Gate:
- Start time:
- End time:
- Validation:
- Result:
- Issues:

## Stop / Continue Decision
- Can continue:
- Blocking issues:
- Rollback needed:

## Final Summary
- User-facing impact:
- Operational impact:
- Follow-up actions:
```

## 4. 填写建议

- `Validation` 不要写“看起来正常”，应写具体页面、脚本或任务类型。
- `Issues` 要写绝对时间和 gate 状态，方便回放。
- `Stop / Continue Decision` 必须清楚说明责任人。

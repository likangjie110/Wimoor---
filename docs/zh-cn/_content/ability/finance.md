# 财务

财务能力由 `wimoor-modules/wimoor-finance` 承载，网关路径为 `/finance/**`。它提供会计期间、科目、辅助核算、凭证、账簿、报表模板和编码规则等基础财务能力。

## 功能范围

- 会计期间。
- 会计科目。
- 辅助核算类型和项目。
- 凭证、凭证明细和凭证修改日志。
- 总账、明细账。
- 报表模板和报表项目。
- 编码规则、编码缓存和编码生成日志。

## 模块边界

| 层级 | 位置 | 职责 |
| --- | --- | --- |
| 前端 API | `wimoorui/src/api/finance` | 会计期间、科目、凭证、账簿、报表请求 |
| 前端路由 | `wimoorui/src/router/modules/finance.js` | 财务相关工具页面入口 |
| 后端服务 | `wimoor-modules/wimoor-finance` | 财务 Controller、Service、Mapper |
| 数据库 | `db_finance` | 科目、辅助核算、凭证、分录、账簿、模板 |
| 编码能力 | code rule/cache/log | 生成凭证号、单据号等编号 |

## 财务数据流图

```mermaid
flowchart TB
  period["会计期间"]
  subject["会计科目"]
  auxType["辅助核算类型"]
  auxItem["辅助核算项目"]
  codeRule["编码规则"]
  voucher["凭证"]
  entry["凭证明细/分录"]
  log["凭证修改日志"]
  ledger["总账/明细账"]
  template["报表模板"]
  reportItem["报表项目"]
  report["报表输出"]
  db["db_finance"]

  period --> voucher
  subject --> voucher
  auxType --> auxItem --> entry
  codeRule --> voucher
  voucher --> entry --> ledger
  voucher --> log
  ledger --> template --> reportItem --> report
  voucher --> db
  ledger --> db
```

## 制证和账簿时序图

```mermaid
sequenceDiagram
  autonumber
  actor Accountant as 财务人员
  participant UI as 财务前端
  participant Gateway as wimoor-gateway
  participant Finance as wimoor-finance
  participant DB as db_finance

  Accountant->>UI: 维护会计期间、科目、辅助核算
  UI->>Gateway: POST /finance/api/** 基础资料
  Gateway->>Finance: 转发基础资料请求
  Finance->>DB: 保存期间、科目、辅助核算
  Accountant->>UI: 新增凭证和分录
  UI->>Gateway: POST /finance/api/** voucher
  Gateway->>Finance: 转发凭证请求
  Finance->>DB: 校验期间、科目、借贷平衡
  Finance->>DB: 写入凭证、分录、编码日志
  Accountant->>UI: 查询总账/明细账
  UI->>Gateway: GET /finance/api/** ledger
  Gateway->>Finance: 查询账簿
  Finance->>DB: 按期间、科目、辅助核算汇总
  Finance-->>UI: 返回账簿结果
```

## 报表生成流程图

```mermaid
flowchart LR
  template["选择报表模板"]
  item["加载报表项目"]
  period["选择会计期间"]
  ledger["读取账簿数据"]
  calculate["按模板规则计算"]
  output["生成报表结果"]
  review["财务复核"]

  template --> item --> period --> ledger --> calculate --> output --> review
```

## 关键数据和接口

| 类型 | 说明 |
| --- | --- |
| 网关路径 | `/finance/**` |
| API 前缀 | `/finance/api/**` |
| 主要 API 家族 | periods、subjects、auxiliary items/types、vouchers、entries、ledgers、report templates/items、code cache/rules |
| 主要数据库 | `db_finance` |
| 端口注意 | 当前配置中 `wimoor-finance` 与 `wimoor-ozon` 同为 `8106`，本地同时启动前必须调整端口 |

## 排错关注点

- 凭证保存失败：检查会计期间是否开启、科目是否启用、借贷是否平衡。
- 明细账为空：检查凭证状态、期间条件、科目和辅助核算过滤条件。
- 报表结果不正确：检查模板项目、取数规则、期间和账簿汇总口径。
- 编码重复：检查编码规则、编码缓存和并发生成日志。

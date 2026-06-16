# 报价

报价能力由 `wimoor-modules/wimoor-quote` 承载，网关路径为 `/quote/**`。它用于供应商报价、采购报价单、报价订单和运输渠道管理，并为 ERP 采购提供供应商价格参考。

## 功能范围

- 供应商报价。
- 采购报价单。
- 报价订单。
- 运输渠道。
- 供应商公开报价入口。
- 与 ERP 采购资料、物料和供应商选择协作。

## 模块边界

| 层级 | 位置 | 职责 |
| --- | --- | --- |
| 前端 API | `wimoorui/src/api/quote` | 报价单、供应商报价、运输渠道请求 |
| 后端服务 | `wimoor-modules/wimoor-quote` | Quote Controller、Service、Mapper |
| Feign | `wimoor-api/wimoor-api-quote` | 保存报价等跨服务契约 |
| 数据库 | `db_quote` | 报价单、报价明细、供应商报价、运输渠道 |
| 关联模块 | `wimoor-erp` | 物料、采购资料、供应商选择 |

## 报价业务流程图

```mermaid
flowchart TB
  buyer["采购人员创建询价/报价需求"]
  material["选择物料和采购资料"]
  supplier["选择供应商"]
  publicLink["生成供应商公开报价入口"]
  supplierQuote["供应商提交报价"]
  quoteOrder["形成报价订单/明细"]
  compare["价格、交期、运输渠道比较"]
  approve["采购确认"]
  erp["同步或引用到 ERP 采购"]
  db["db_quote"]

  buyer --> material --> supplier --> publicLink --> supplierQuote
  supplierQuote --> quoteOrder --> compare --> approve --> erp
  quoteOrder --> db
```

## 供应商报价时序图

```mermaid
sequenceDiagram
  autonumber
  actor Buyer as 采购人员
  actor Supplier as 供应商
  participant UI as 前端页面
  participant Gateway as wimoor-gateway
  participant Quote as wimoor-quote
  participant ERP as wimoor-erp
  participant DB as db_quote

  Buyer->>UI: 创建报价需求并选择物料
  UI->>Gateway: POST /quote/api/** 报价单
  Gateway->>Quote: 保存报价需求
  Quote->>ERP: 查询物料、采购资料或供应商信息
  ERP-->>Quote: 返回基础资料
  Quote->>DB: 写入报价单和公开入口
  Quote-->>UI: 返回供应商报价链接
  Supplier->>Gateway: 通过公开入口提交报价
  Gateway->>Quote: 保存供应商报价
  Quote->>DB: 写入报价明细、价格、交期、渠道
  Buyer->>UI: 查看报价对比并确认
  UI->>Gateway: 提交确认结果
  Gateway->>Quote: 更新报价状态
```

## 报价到采购关系图

```mermaid
flowchart LR
  quote["报价单"]
  supplierPrice["供应商价格"]
  leadTime["交期"]
  channel["运输渠道"]
  decision["采购选择"]
  purchase["ERP 采购计划/采购单"]

  quote --> supplierPrice --> decision
  quote --> leadTime --> decision
  quote --> channel --> decision
  decision --> purchase
```

## 关键数据和接口

| 类型 | 说明 |
| --- | --- |
| 网关路径 | `/quote/**` |
| API 前缀 | `/quote/api/**` |
| 主要 API 家族 | supplier quote、purchase quote、shipment quote |
| 主要数据库 | `db_quote` |
| 公开路径 | 网关配置中存在 quote supplier public path 放行，用于供应商提交报价 |

## 排错关注点

- 供应商公开链接打不开：检查网关白名单、公开路径和报价单状态。
- 报价保存失败：检查供应商、物料、币种、价格和交期字段完整性。
- ERP 无法引用报价：检查报价状态、物料编码和 ERP 采购资料关联。
- 报价对比不准确：检查币种、税率、运费、最小起订量和运输渠道。

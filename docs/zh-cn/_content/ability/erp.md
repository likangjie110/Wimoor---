# ERP 核心

ERP 核心能力由 `wimoor-erp/erp-boot` 承载，网关路径为 `/erp/**`。它负责跨境电商内部的物料、采购、仓库、库存、发货和采购财务，是 Amazon、Quote、Finance 等模块的业务基础。

## 功能范围

- 物料、品牌、分类、海关、采购资料。
- 采购计划、采购单、采购变更、采购收货。
- 1688 采购订单、物流跟踪和结算信息。
- 仓库、货架、入库、出库、盘点、调拨。
- 发货计划、FBA 发货、运输方式、箱规和报关信息。
- 库存报表、周转率、滞销分析。
- 与 Amazon 发货、Quote 报价、Admin 基础资料的服务协作。

## 模块边界

| 层级 | 位置 | 职责 |
| --- | --- | --- |
| 前端 API | `wimoorui/src/api/erp` | 物料、采购、库存、仓库、发货页面请求 |
| 前端路由 | `wimoorui/src/router/modules/erp.js` | ERP 详情页、采购仓库、发货步骤页 |
| 后端服务 | `wimoor-erp/erp-boot` | ERP Controller、Service、Mapper |
| Feign | `wimoor-erp/erp-api`、`AmazonClientOneFeignApi` | ERP 内部契约、ERP 调 Amazon |
| 数据库 | `db_erp` | 物料、采购、库存、仓库、发货数据 |

## 核心业务流程图

```mermaid
flowchart TB
  material["物料资料维护"]
  supplier["供应商和采购资料"]
  plan["补货/采购计划"]
  purchase["采购单"]
  alibaba["1688 订单和物流跟踪"]
  receive["采购收货"]
  inbound["入库上架"]
  inventory["库存管理"]
  stocktaking["盘点/调拨/库存报表"]
  shipPlan["发货计划"]
  shipment["FBA/物流发货"]
  outbound["出库扣减"]
  finance["采购财务和成本结算"]
  amazon["Amazon 发货/Listing 协作"]
  quote["Quote 报价协作"]

  material --> supplier --> plan --> purchase
  quote --> supplier
  purchase --> alibaba --> receive --> inbound --> inventory
  inventory --> stocktaking
  inventory --> shipPlan --> shipment --> outbound --> finance
  shipment --> amazon
  purchase --> finance
  outbound --> inventory
```

## 采购入库时序图

```mermaid
sequenceDiagram
  autonumber
  actor Buyer as 采购人员
  participant UI as ERP 前端
  participant Gateway as wimoor-gateway
  participant ERP as wimoor-erp
  participant DB as db_erp
  participant Alibaba as 1688/物流信息

  Buyer->>UI: 创建采购计划或采购单
  UI->>Gateway: POST /erp/api/** 采购单
  Gateway->>ERP: 转发采购请求
  ERP->>DB: 保存采购单、明细、供应商信息
  ERP-->>UI: 返回采购单号和状态
  Buyer->>UI: 刷新 1688 订单或物流
  UI->>Gateway: 调用采购跟踪接口
  Gateway->>ERP: 转发跟踪请求
  ERP->>Alibaba: 查询订单/物流状态
  ERP->>DB: 更新外部单号、物流、到货状态
  Buyer->>UI: 确认收货入库
  UI->>Gateway: POST /erp/api/** 收货入库
  Gateway->>ERP: 转发入库请求
  ERP->>DB: 写入入库、库存流水、库存余额
  ERP-->>UI: 返回入库结果
```

## 发货出库时序图

```mermaid
sequenceDiagram
  autonumber
  actor Operator as 发货人员
  participant UI as ERP 发货页面
  participant Gateway as wimoor-gateway
  participant ERP as wimoor-erp
  participant Amazon as wimoor-amazon
  participant DB as db_erp

  Operator->>UI: 创建发货计划
  UI->>Gateway: POST /erp/api/** 发货计划
  Gateway->>ERP: 保存计划和箱规信息
  ERP->>DB: 锁定待发库存
  Operator->>UI: 生成 FBA/物流发货单
  UI->>Gateway: 提交发货单
  Gateway->>ERP: 转发发货请求
  ERP->>Amazon: 查询站点、货件或 Listing 相关信息
  Amazon-->>ERP: 返回 Amazon 侧数据
  ERP->>DB: 写入发货单、箱子、出库记录
  ERP-->>UI: 返回发货状态和后续操作入口
```

## 关键数据和接口

| 类型 | 说明 |
| --- | --- |
| 网关路径 | `/erp/**` |
| API 前缀 | `/erp/api/v1/**`、`/erp/api/v2/**` |
| 主要 API 家族 | material、category、brand、purchase plan/form、inventory、warehouse、stocktaking、shipment plan/form、1688 |
| 主要数据库 | `db_erp` |
| 关联服务 | `wimoor-amazon`、`wimoor-quote`、`wimoor-admin` |

## 排错关注点

- 采购单保存失败：检查物料、供应商、仓库等基础资料是否完整。
- 入库后库存不变：检查入库状态、库存流水和库存余额表是否同步写入。
- 发货计划无法生成：检查库存可用量、仓库货架、Amazon 站点或货件数据。
- 1688 跟踪失败：检查外部订单号、授权、网络和 Quartz 手动触发记录。
- 库存报表不更新：检查 ERP 月度库存汇总任务是否执行。

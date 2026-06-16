# Ozon

Ozon 能力由 `wimoor-ozon/ozon-boot` 承载，网关路径为 `/ozon/**`。它负责 Ozon 店铺授权、商品发布、库存价格同步、订单发货、聊天、财务、广告、错误中心和任务同步。

## 功能范围

- 店铺授权和连通性检测。
- 商品草稿、属性、图片、变体和发布任务。
- 库存推送、库存快照和库存任务。
- 价格同步、价格任务和价格快照。
- 订单、售后、发货和包裹。
- 买家聊天、回复审核和会话同步。
- 财务交易、广告、错误中心、元数据和运维操作。

## 模块边界

| 层级 | 位置 | 职责 |
| --- | --- | --- |
| 前端 API | `wimoorui/src/api/ozon` | Ozon 授权、商品、库存、价格、聊天、订单、任务请求 |
| 前端路由 | `wimoorui/src/router/modules/ozon.js` | auth、product、stock、price、chat、ads、finance、posting、shipment、task、error |
| 后端服务 | `wimoor-ozon/ozon-boot` | Ozon Controller、Service、任务入口 |
| Feign | `wimoor-api/wimoor-api-ozon` | Ozon 远程服务契约 |
| 数据库 | `db_ozon` | 授权、商品、库存、价格、订单、聊天、财务、任务数据 |
| 外部 API | Ozon Seller API | 商品、库存、订单、聊天、财务等接口 |

## 总体流程图

```mermaid
flowchart TB
  auth["授权和连通性检测"]
  meta["类目/属性/元数据"]
  draft["商品草稿"]
  publish["商品发布任务"]
  stock["库存同步"]
  price["价格同步"]
  posting["订单/售后"]
  shipment["发货/包裹"]
  chat["买家聊天"]
  finance["财务交易"]
  ads["广告数据"]
  error["错误中心"]
  task["任务中心"]
  api["Ozon Seller API"]
  db["db_ozon"]

  auth --> api
  meta --> draft --> publish --> task --> api --> db
  stock --> task
  price --> task
  posting --> shipment --> api
  chat --> api
  finance --> api
  ads --> api
  api --> error
  error --> db
```

## 商品发布时序图

```mermaid
sequenceDiagram
  autonumber
  actor Operator as 运营人员
  participant UI as Ozon 前端
  participant Gateway as wimoor-gateway
  participant OzonSvc as wimoor-ozon
  participant API as Ozon Seller API
  participant DB as db_ozon

  Operator->>UI: 编辑商品草稿、属性、图片、变体
  UI->>Gateway: POST /ozon/api/v1/product/**
  Gateway->>OzonSvc: 转发商品保存请求
  OzonSvc->>DB: 保存草稿和本地任务
  Operator->>UI: 提交发布任务
  UI->>Gateway: POST /ozon/api/v1/task/**
  Gateway->>OzonSvc: 触发发布任务
  OzonSvc->>API: 上传商品、图片、价格或库存
  API-->>OzonSvc: 返回 taskId、错误或处理状态
  OzonSvc->>DB: 更新任务状态和错误信息
  UI->>Gateway: 查询任务结果
  Gateway->>OzonSvc: 查询本地任务和错误中心
  OzonSvc-->>UI: 返回发布结果
```

## 订单发货和聊天时序图

```mermaid
sequenceDiagram
  autonumber
  participant Quartz as 定时/人工触发
  participant Gateway as wimoor-gateway
  participant OzonSvc as wimoor-ozon
  participant API as Ozon Seller API
  participant DB as db_ozon
  participant UI as Ozon 页面

  Quartz->>Gateway: 调用订单/聊天同步接口
  Gateway->>OzonSvc: 转发同步请求
  OzonSvc->>API: 拉取 posting、shipment、chat、finance 数据
  API-->>OzonSvc: 返回订单、包裹、消息、交易
  OzonSvc->>DB: 写入订单、发货、聊天、财务数据
  UI->>Gateway: 查看订单或聊天
  Gateway->>OzonSvc: 查询本地数据
  OzonSvc-->>UI: 返回业务列表
  UI->>Gateway: 回复聊天或处理发货
  Gateway->>OzonSvc: 转发操作请求
  OzonSvc->>API: 提交回复/发货操作
  OzonSvc->>DB: 更新操作状态
```

## 关键数据和接口

| 类型 | 说明 |
| --- | --- |
| 网关路径 | `/ozon/**` |
| API 前缀 | `/ozon/api/v1/**` |
| 主要 API 家族 | auth、product、stock、price、posting、shipment、chat、finance、ads、meta、ops、error、task |
| 主要数据库 | `db_ozon` |
| 外部依赖 | Ozon Seller API |
| 端口注意 | 当前配置中 `wimoor-ozon` 与 `wimoor-finance` 同为 `8106`，本地同时启动前必须调整端口 |

## 排错关注点

- Ozon 授权失败：检查 client id、api key、店铺状态和连通性检测接口。
- 商品发布失败：优先看错误中心，确认类目属性、图片、价格、库存是否满足 Ozon 要求。
- 库存价格不同步：检查任务中心状态、Ozon API 返回错误和本地库存来源。
- 订单发货缺数据：检查订单同步时间范围、店铺授权和 Ozon 返回分页。
- 聊天无法回复：检查会话状态、回复审核规则和 Ozon API 错误码。

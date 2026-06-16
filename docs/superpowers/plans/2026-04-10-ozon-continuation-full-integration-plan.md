# Wimoor Ozon 后续完整实施与前端融合总计划

> **For agentic workers:** REQUIRED: Follow this plan as the single source of truth for continued Ozon delivery. Steps use checkbox (`- [ ]`) syntax for tracking. Do not fork a second Ozon UI or a second Ozon workflow outside the routes and pages already present in `wimoorui/src/views/ozon`.

**Goal:** 将当前 Ozon 从“核心链路已落地、部分能力为本地导入版、多个写链路默认关闭”的状态，推进到“业务边界完整、前后端联动、与现有导航/任务/错误/ERP 桥接融合、可灰度上线”的交付状态。

**Architecture:** 继续保持 `wimoor-ozon` 为独立 bounded context，Ozon 原生对象只保存在 `db_ozon`；ERP 只接收标准化事实（订单、库存、价格、履约结果等）。前端继续沿用现有 `wimoorui` 路由、菜单、权限和共享组件体系，不建立第二套入口，不绕开已有 `task` / `error` / `auth` / `product` 页面。

**Tech Stack:** Maven multi-module Java 8, Spring Boot 2.6, Spring Cloud Alibaba/Nacos/Feign, MyBatis-Plus, MySQL, Redis, Vue 3 + Vite + Element Plus.

---

## 1. 当前基线快照

### 1.1 已经具备的能力

- 已有独立服务启动入口、Mapper 扫描、Nacos 配置与 Ozon 路由接入。
- 已有授权管理、仓库同步、商品映射、刊登草稿、类目模板、预检、真实发布、发布任务详情。
- 已有价格推送、库存推送、Posting 同步、ERP 订单桥接、追踪号推送。
- 已有任务中心、错误中心、财务导入、聊天导入与回复审计、广告导入与汇总。
- 已有 11 个 Ozon 前端页面、菜单与权限 SQL、Header 平台入口。
- 已有服务级单测、Spring Bean Wiring 测试、Feature Gate 测试、基础 smoke wiring 测试。

### 1.2 当前仍然存在的缺口

- 商品、库存、价格、Posting 写链路默认关闭，前端没有同步感知开关状态，用户点击后只能收到后端报错。
- 商品域缺少发布任务历史、任务追踪强化、草稿生命周期操作、跨页面深链。
- Posting/Shipment 只覆盖主链路，没有覆盖 `delivery method`、`package`、`return`、`cancellation`。
- 财务、聊天、广告目前是“本地导入可验证版”，不是“官方在线拉取/发送版”。
- 任务系统只有 `sync_job` 记录，没有 `sync_cursor`、`api_log`、`operation_audit` 等支撑设施。
- 错误中心和任务中心已有列表，但没有做到和源页面之间的双向跳转闭环。
- 前端 Ozon 页面已经存在，但还偏分散，缺少围绕 Auth / Product / Posting 三个核心工作台的联动。

### 1.3 当前缺口形成原因

- `chat-send` 和 `ads-sync` 在配置中明确保持关闭，原因是官方合同没有完成 fresh verification。
- `product-write`、`stock-write`、`price-write`、`posting-write` 默认关闭，原因是当前代码具备写能力但仍处于灰度控制阶段。
- 退货、取消、包裹、配送方式、同步游标、操作审计等对象在总设计中存在，但未进入当前落库与实现批次。
- 现有前端主要先满足“可进入页面并驱动接口”，还没有围绕运营链路做融合和降噪。

---

## 2. 融合原则

### 2.1 导航与入口原则

- [ ] 不新增第二个 Ozon 顶级入口，继续使用 [HeaderPlatform.vue](/mnt/d/project/wimoor/wimoorui/src/layout/components/Header/HeaderPlatform.vue) 和 [ozon.js](/mnt/d/project/wimoor/wimoorui/src/router/modules/ozon.js)。
- [ ] 不把每个补充能力都做成新的一级菜单；优先将缺失能力融入现有页面的 tab、drawer、panel。
- [ ] 菜单与权限继续沿用 `t_sys_menu.sql`、`t_sys_permission.sql` 体系，新增能力只在确有独立工作台时才新增菜单。

### 2.2 页面融合原则

- [ ] `Auth` 页面承载“授权、仓库、配送方式、初始化状态”。
- [ ] `Product` 页面承载“映射、草稿、预检、发布、发布任务历史”。
- [ ] `Posting` 页面承载“订单同步、桥接结果、履约记录、售后对象（return/cancellation/package）”。
- [ ] `Shipment` 页面只保留“直接按 posting 操作”的补充入口，不能与 `Posting` 页面形成重复工作流。
- [ ] `Task` 和 `Error` 页面作为统一运维入口，必须能跳回源页面。
- [ ] `Finance`、`Chat`、`Ads` 页面要支持“当前本地导入模式”与“未来官方同步模式”的双模演进，而不是推翻重写。

### 2.3 共享组件原则

- [ ] 继续复用 `GlobalTable`、`Pagination`、`RightToolbar`、`ImageUpload`、`ImagePreview`、`UploadDialog`、`Editor`、现有 header selectors。
- [ ] 新增 Ozon 前端通用能力时，优先抽到 `wimoorui/src/views/ozon/components` 或 `wimoorui/src/views/ozon/composables`，不要在每页重复写。
- [ ] 所有页面统一支持 `authId` 路由上下文，避免用户反复切店。

### 2.4 后端边界原则

- [ ] Ozon 原生对象继续保存在 `db_ozon`。
- [ ] ERP 只接受标准化事实，不直接保存 Ozon 原生复杂结构。
- [ ] 新增 support table 时，优先补齐 `mapper/entity/service/controller/test/sql` 一整套，不接受只建表不接服务。

---

## 3. 前端融合蓝图

## 3.1 Ozon Auth 工作台

现有页面：`wimoorui/src/views/ozon/auth/index.vue`

目标演进：

- [ ] 保留当前“绑定、列表、ping、停用、轮换密钥”主流。
- [ ] 新增二级 tab：
  - [ ] `授权列表`
  - [ ] `仓库同步`
  - [ ] `配送方式`
  - [ ] `初始化任务`
- [ ] 在授权行上显示：
  - [ ] 仓库数量
  - [ ] 默认仓
  - [ ] 最近初始化任务状态
  - [ ] 当前可写开关摘要
- [ ] 从 Auth 页面可跳转到 Product / Posting / Finance，并带上 `authId`。

## 3.2 Ozon Product 工作台

现有页面：`wimoorui/src/views/ozon/product/index.vue`

目标演进：

- [ ] 页面结构调整为“左侧草稿导航 + 右侧四段式工作台”。
- [ ] 右侧统一使用 tab 或分段锚点：
  - [ ] `SKU 映射`
  - [ ] `草稿编辑`
  - [ ] `预检结果`
  - [ ] `发布任务`
- [ ] 支持从 Error / Task / Posting 页面通过 query 参数直接打开指定 `draftId`。
- [ ] 支持从 Product 页面直接跳转到 Price / Stock 页面，并自动带入 `authId + materialSku`。
- [ ] 图片上传与预览继续复用现有上传组件，不另起一套素材体系。

## 3.3 Ozon Posting / Shipment 融合工作台

现有页面：

- `wimoorui/src/views/ozon/posting/index.vue`
- `wimoorui/src/views/ozon/shipment/index.vue`

目标演进：

- [ ] `Posting` 页面作为订单与履约主页面。
- [ ] `Shipment` 页面保留为从 posting 深入后的轻量操作页。
- [ ] `Posting` 页面增加三类二级视图：
  - [ ] `订单列表`
  - [ ] `履约记录`
  - [ ] `售后记录（return/cancellation/package）`
- [ ] `Shipment` 页面必须支持 query 参数驱动：
  - [ ] `authId`
  - [ ] `postingId`
  - [ ] `postingNumber`
- [ ] 从 `Posting`、`Error`、`Task` 三处都能打开 `Shipment` 页。

## 3.4 Ozon Task / Error 融合工作台

现有页面：

- `wimoorui/src/views/ozon/task/index.vue`
- `wimoorui/src/views/ozon/error/index.vue`

目标演进：

- [ ] `Task` 页增加“来源页面跳转”列。
- [ ] `Error` 页根据 `sourceType` 输出跳转按钮：
  - [ ] `PRODUCT` -> Product
  - [ ] `POSTING` -> Posting
  - [ ] `SHIPMENT` -> Shipment
  - [ ] `FINANCE` -> Finance
  - [ ] `CHAT` -> Chat
  - [ ] `ADS` -> Ads
- [ ] `Task` 页显示更可读的业务摘要，而不是原始 payload 字符串。
- [ ] `Error` 页支持查看 request / response / retry payload，并在成功重试后同步刷新源页面状态。

## 3.5 Ozon Finance / Chat / Ads 双模工作台

现有页面：

- `wimoorui/src/views/ozon/finance/index.vue`
- `wimoorui/src/views/ozon/chat/index.vue`
- `wimoorui/src/views/ozon/ads/index.vue`

目标演进：

- [ ] 保留当前“本地导入模式”。
- [ ] 新增明显的模式切换区：
  - [ ] `本地导入`
  - [ ] `官方同步`
- [ ] 当官方模式未开启时：
  - [ ] 显示禁用态
  - [ ] 显示关闭原因
  - [ ] 保留当前导入功能
- [ ] 当官方模式开启后：
  - [ ] 在原页面内展开同步表单与任务结果，不新建重复页面。

---

## 4. 继续实施执行图

### 4.1 串行主链

- `Workstream 1` 前端特性发现与统一融合底座
- `Workstream 2` Auth / Warehouse / Delivery Method 完整化
- `Workstream 3` Product 工作台生产化
- `Workstream 4` Stock / Price 工作台生产化
- `Workstream 5` Posting / Shipment / Return / Cancellation 完整化
- `Workstream 6` Finance / Chat / Ads 双模演进
- `Workstream 7` 运维基础设施与观测
- `Workstream 8` E2E、灰度与上线验证

### 4.2 可并行部分

- [ ] `Workstream 3` 与 `Workstream 6` 可以在 `Workstream 1` 完成后并行推进。
- [ ] `Workstream 4` 与 `Workstream 7` 可以在 `Workstream 3` 启动后并行推进。
- [ ] `Workstream 5` 依赖 `Workstream 2` 和 `Workstream 4` 的部分输出，应放在后半段。

---

## 5. Workstream 1: 前端特性发现与统一融合底座

### Context Brief

当前后端有 feature gate，但前端没有统一读取能力。页面上的按钮与后端默认状态脱节，导致“用户能点、接口拒绝”的低质量体验。继续做任何前端融合前，必须先补齐“服务端配置 -> 前端显式感知 -> 页面统一行为”的链路。

### Files

#### Backend

- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/controller/OzonMetaController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/pojo/vo/OzonFeatureView.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureProperties.java`

#### Frontend

- Create: `wimoorui/src/api/ozon/meta/metaApi.js`
- Create: `wimoorui/src/views/ozon/composables/useOzonFeatures.js`
- Modify: all `wimoorui/src/views/ozon/*/index.vue`

### Tasks

- [ ] 新增 `GET /ozon/api/v1/meta/features`，返回所有 Ozon feature flags 与说明文案。
- [ ] 后端 VO 明确区分：
  - [ ] `auth`
  - [ ] `product`
  - [ ] `productWrite`
  - [ ] `stockWrite`
  - [ ] `priceWrite`
  - [ ] `postingWrite`
  - [ ] `chatSend`
  - [ ] `adsSync`
- [ ] 前端新增统一 composable，页面 mount 时只拉一次特性并缓存到当前 auth 作用域。
- [ ] 所有 Ozon 页统一行为：
  - [ ] 功能关闭时按钮为 disabled
  - [ ] hover 提示关闭原因
  - [ ] 不再让用户点了之后才看见后端报错
- [ ] `Task` / `Error` 页在页面头部显示当前 auth 的开关摘要。

### Verification

- [ ] `OzonControllerFeatureGateTests` 扩展为包含 `meta/features`。
- [ ] 新增前端单页 smoke 脚本或页面级 snapshot，验证禁用态可见。
- [ ] 手工验证 `product-write=false` 时 Product 页面发布按钮不可用。

### Exit Criteria

- [ ] 任意 Ozon 页面都能感知后端开关。
- [ ] 不再出现“默认关闭但前端看起来可用”的写按钮。

---

## 6. Workstream 2: Auth / Warehouse / Delivery Method 完整化

### Context Brief

Auth 目前已经能绑定、连通性检测、同步仓库，但仍缺 `delivery method` 与初始化状态可视化。按照总设计，`Auth` 页面应该是 Ozon 店铺级基础设置工作台，而不是只放密钥表单。

### Files

#### Backend

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_delivery_method.sql`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/pojo/entity/OzonDeliveryMethod.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/mapper/OzonDeliveryMethodMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/controller/OzonSellerSettingsController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/service/IOzonDeliveryMethodService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/service/impl/OzonDeliveryMethodServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/service/impl/OzonAuthServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/service/impl/OzonTaskServiceImpl.java`

#### Frontend

- Modify: `wimoorui/src/views/ozon/auth/index.vue`
- Create: `wimoorui/src/views/ozon/auth/components/WarehousePanel.vue`
- Create: `wimoorui/src/views/ozon/auth/components/DeliveryMethodPanel.vue`
- Create: `wimoorui/src/views/ozon/auth/components/InitTaskPanel.vue`
- Modify: `wimoorui/src/api/ozon/auth/authApi.js`

### Tasks

- [ ] 为 `delivery method` 增加表、Mapper、Service、查询与保存接口。
- [ ] `Auth` 页面改为 tab 式工作台。
- [ ] 显示当前授权对应的仓库列表和默认仓。
- [ ] 在 `初始化任务` tab 中显示 `INIT_SELLER`、`INIT_WAREHOUSE` 最近状态。
- [ ] 若未来引入初始化 worker，本页继续作为初始化观测面板，不另开页面。
- [ ] 菜单不新增一级项，继续沿用 `Ozon授权` 页面承载这些内容。

### Verification

- [ ] 新增 `OzonWarehouseSyncServiceTests` 扩展用例验证重复同步与默认仓更新。
- [ ] 新增 `OzonTaskServiceTests` 用例验证初始化任务展示。
- [ ] 手工验证 Auth 页面可以完成：
  - [ ] 绑定授权
  - [ ] 连接测试
  - [ ] 查看仓库
  - [ ] 保存配送方式

### Exit Criteria

- [ ] `Auth` 页面成为完整店铺基础配置页。
- [ ] 仓库和配送方式不再散落到其他页面。

---

## 7. Workstream 3: Product 工作台生产化

### Context Brief

当前 Product 已经是实现最深的 Ozon 子域，但仍偏“接口集合页”。继续实施要把它变成真正的运营工作台，并与 Price、Stock、Error、Task 融合。

### Files

#### Backend

- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/controller/OzonProductController.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPublishServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftCloneCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftArchiveCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublishTaskListView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPublishTaskQueryService.java`
- Optional Create: background task poller under `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/task/...`

#### Frontend

- Modify: `wimoorui/src/views/ozon/product/index.vue`
- Modify: `wimoorui/src/views/ozon/product/components/DraftSidebar.vue`
- Modify: `wimoorui/src/views/ozon/product/components/PublishTaskPanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/MappingPanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/TaskHistoryDrawer.vue`
- Create: `wimoorui/src/views/ozon/product/components/ActionHeader.vue`
- Modify: `wimoorui/src/api/ozon/product/productApi.js`

### Tasks

- [ ] 增加草稿 `clone / archive / delete / list tasks` 能力。
- [ ] 增加发布任务历史查询接口，不再只显示最后一次任务。
- [ ] 若保留 on-demand polling，则前端必须明确显示：
  - [ ] `RUNNING`
  - [ ] `SUCCESS`
  - [ ] `FAILED`
  - [ ] `PARTIAL`
- [ ] Product 页面顶部增加统一 ActionHeader，包含：
  - [ ] 当前 auth
  - [ ] 当前草稿
  - [ ] 最近预检状态
  - [ ] 最近发布状态
  - [ ] 跳转 Price
  - [ ] 跳转 Stock
- [ ] `DraftSidebar` 支持状态筛选和关键字。
- [ ] `PublishTaskPanel` 支持历史列表和查看 remote result 原文。
- [ ] Product 页面支持 query 参数：
  - [ ] `authId`
  - [ ] `draftId`
  - [ ] `materialSku`
  - [ ] `focus=preview|publish|mapping`

### Verification

- [ ] 扩展 `OzonListingDraftServiceTests` 覆盖 clone/archive。
- [ ] 扩展 `OzonProductPublishServiceTests` 覆盖 task history 与 partial retry 结果持久化。
- [ ] 补充前端 smoke：通过 query 直达某草稿并打开预检区。

### Exit Criteria

- [ ] Product 页面能独立承载日常刊登运维。
- [ ] Task / Error 页面能通过 deep link 直接把用户带到正确草稿。

---

## 8. Workstream 4: Stock / Price 工作台生产化

### Context Brief

库存和价格已经能真实调用 Ozon，但页面目前仍偏“单次提交 + 看快照”。继续实施时要让这两个页面成为 Product 工作台的自然延伸，而不是独立孤岛。

### Files

#### Backend

- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/stock/service/impl/OzonStockServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/price/service/impl/OzonPriceServiceImpl.java`
- Optional Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/stock/pojo/vo/OzonStockTaskView.java`
- Optional Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/price/pojo/vo/OzonPriceTaskView.java`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_operation_audit.sql`

#### Frontend

- Modify: `wimoorui/src/views/ozon/stock/index.vue`
- Modify: `wimoorui/src/views/ozon/price/index.vue`
- Create: `wimoorui/src/views/ozon/shared/components/OzonSkuPicker.vue`
- Create: `wimoorui/src/views/ozon/shared/components/OzonWarehouseSelector.vue`
- Modify: `wimoorui/src/api/ozon/stock/stockApi.js`
- Modify: `wimoorui/src/api/ozon/price/priceApi.js`

### Tasks

- [ ] 后端解析 Ozon 写接口返回值，不能只把提交成功视为最终成功。
- [ ] 对 price / stock 任务增加更细的状态与错误摘要。
- [ ] 页面支持从 Product 页面带入 `materialSku`。
- [ ] 页面支持：
  - [ ] 单 SKU 快速推送
  - [ ] 批量 SKU 推送
  - [ ] 最近快照
  - [ ] 最近任务摘要
- [ ] 所有写操作记录到 `operation_audit`。
- [ ] UI 上将“当前 feature gate 关闭”展示为页面级 banner，而不是按钮旁零散提示。

### Verification

- [ ] 扩展 `OzonStockSyncServiceTests` 验证关闭开关、任务状态与 payload。
- [ ] 扩展 `OzonPriceSyncServiceTests` 验证关闭开关、任务状态与 payload。
- [ ] 手工验证从 Product 页面跳转后，Price / Stock 页面能自动带入 SKU。

### Exit Criteria

- [ ] Stock / Price 成为 Product 的下游操作页。
- [ ] 所有写操作都能被 task/error/audit 三方追溯。

---

## 9. Workstream 5: Posting / Shipment / Return / Cancellation 完整化

### Context Brief

当前 Ozon 主业务链里最重要的剩余缺口在订单后半段。Posting 已能同步与桥接，Shipment 已能推 tracking，但缺少 return、cancellation、package、delivery method 等对象，前端也没有把这些信息融合到现有订单履约页面。

### Files

#### Backend

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_package.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_return.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_cancellation.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_sync_cursor.sql`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/service/impl/OzonPostingServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/shipment/service/impl/OzonShipmentServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/controller/OzonAfterSaleController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/service/IOzonAfterSaleService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/service/impl/OzonAfterSaleServiceImpl.java`
- Optional Create: scheduled sync job consumer under `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/job/...`

#### Frontend

- Modify: `wimoorui/src/views/ozon/posting/index.vue`
- Modify: `wimoorui/src/views/ozon/shipment/index.vue`
- Create: `wimoorui/src/views/ozon/posting/components/PostingDetailDrawer.vue`
- Create: `wimoorui/src/views/ozon/posting/components/AfterSalePanel.vue`
- Create: `wimoorui/src/views/ozon/posting/components/ShipmentHistoryPanel.vue`
- Modify: `wimoorui/src/api/ozon/posting/postingApi.js`
- Modify: `wimoorui/src/api/ozon/shipment/shipmentApi.js`

### Tasks

- [ ] 引入 `sync_cursor`，让 posting 增量同步具备可持续运行能力。
- [ ] 增加 return / cancellation / package 模型与列表查询接口。
- [ ] `Posting` 页面增加详情 drawer，展示：
  - [ ] posting 原始字段
  - [ ] items
  - [ ] bridge status
  - [ ] ERP 订单号
  - [ ] shipment history
  - [ ] after-sale records
- [ ] `Shipment` 页面接受路由 query 并自动加载 posting 概览。
- [ ] `Error` 页对 `POSTING` / `SHIPMENT` 错误提供深链。
- [ ] 若仓库中已有 ERP 订单事实列表页，在后续 PR 中补“从 Ozon posting 跳到 ERP 订单详情”的入口；若无现成页面，则先保留 ERP 订单号文本展示。

### Verification

- [ ] 扩展 `OzonPostingSyncServiceTests` 覆盖 cursor、bridge diagnostics、error retry。
- [ ] 扩展 `OzonShipmentServiceTests` 覆盖 route-driven 查询与错误回补。
- [ ] 新增 after-sale service tests。

### Exit Criteria

- [ ] Posting 页面成为完整订单履约主页面。
- [ ] Shipment 页面不再独立承担完整工作流，只承担 posting 的补充视图。

---

## 10. Workstream 6: Finance / Chat / Ads 双模演进

### Context Brief

这三个子域当前都是“本地导入可验证版”。继续实施不能粗暴推翻，应在原页面内保留本地导入模式，同时为未来官方接口模式留出明确演进路径。

### Files

#### Backend

- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/finance/service/impl/OzonFinanceServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/service/impl/OzonChatServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/service/impl/OzonAdsServiceImpl.java`
- Optional Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/client/OzonChatApiClient.java`
- Optional Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/client/OzonPerformanceApiClient.java`
- Optional Create: finance report pull client after contract verification

#### Frontend

- Modify: `wimoorui/src/views/ozon/finance/index.vue`
- Modify: `wimoorui/src/views/ozon/chat/index.vue`
- Modify: `wimoorui/src/views/ozon/ads/index.vue`
- Create: `wimoorui/src/views/ozon/shared/components/ModeSwitchBanner.vue`
- Create: `wimoorui/src/views/ozon/chat/components/ReplyComposer.vue`
- Create: `wimoorui/src/views/ozon/ads/components/SyncPanel.vue`
- Create: `wimoorui/src/views/ozon/finance/components/TaskResultPanel.vue`

### Tasks

- [ ] 三个页面统一引入模式切换 banner。
- [ ] `Finance` 页面保留导入，同时新增：
  - [ ] task timeline
  - [ ] 原文查看 drawer
  - [ ] 后续官方同步入口占位
- [ ] `Chat` 页面保留导入与 reply audit，同时新增：
  - [ ] send 模式禁用态
  - [ ] 开启后在原页面内显示回复编辑器
  - [ ] 会话状态、未读筛选、最近 reply audit
- [ ] `Ads` 页面保留导入与 summary，同时新增：
  - [ ] sync 模式禁用态
  - [ ] 开启后在原页面内显示同步表单和任务结果
  - [ ] account/campaign/report 三层筛选联动
- [ ] 在官方合同完成 fresh verification 前，不得把 `chat-send` 或 `ads-sync` 改成默认开启。

### Verification

- [ ] 扩展 `OzonFinanceImportTests` 覆盖 task result 展示所需字段。
- [ ] 扩展 `OzonChatSyncTests` 覆盖 reply audit 列表与禁用态 reason。
- [ ] 扩展 `OzonAdsReportTests` 覆盖 sync mode banner 所需字段。

### Exit Criteria

- [ ] 本地导入模式继续可用。
- [ ] 未来官方模式在同一页面可平滑开启，不需要重建 UI。

---

## 11. Workstream 7: 运维基础设施与观测

### Context Brief

继续实施如果没有游标、API 日志、操作审计、统一追踪，后续的真实同步、重试与定位会很快失控。该工作流不是附属项，而是 Ozon 从“能用”走向“可维护”的关键。

### Files

#### Backend

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_api_log.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_operation_audit.sql`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/log/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/audit/...`
- Modify: `DefaultOzonSellerApiClient.java`
- Modify: `DefaultOzonProductApiClient.java`
- Modify: task / error / posting / shipment / product publish services

#### Frontend

- Modify: `wimoorui/src/views/ozon/task/index.vue`
- Modify: `wimoorui/src/views/ozon/error/index.vue`
- Optional Create: `wimoorui/src/views/ozon/ops/index.vue` only if task+error 已无法承载；否则不新增菜单

### Tasks

- [ ] 所有真实远端调用写入 `api_log`。
- [ ] 所有人工写操作写入 `operation_audit`。
- [ ] `Task` 页支持查看关联 API 摘要。
- [ ] `Error` 页支持查看错误发生时的 request / response / retry payload。
- [ ] 若不新增 `ops` 页面，则把这些能力全部融合到 `Task` / `Error` 页中。

### Verification

- [ ] 新增日志/audit service tests。
- [ ] 手工验证一次 price/stock/posting/publish 调用后，task 与 error 页面都能找到对应痕迹。

### Exit Criteria

- [ ] Ozon 任何真实写链路都可追踪。
- [ ] 不需要翻业务表才能判断某次调用为何失败。

---

## 12. Workstream 8: E2E、灰度与上线验证

### Context Brief

Ozon 当前已有不少单测，但还缺“页面级回归 + 灰度开关上线流程”。最终交付必须覆盖单测、构建、本地 smoke、前端页面级回归和灰度开关策略。

### Files

#### Backend / Scripts

- Modify: `tools/ozon/local/60-smoke-readonly.sh`
- Modify: `tools/ozon/local/70-smoke-write.sh`
- Modify: `tools/ozon/local/90-full-check.sh`
- Modify: `docs/superpowers/verification/*.md` generation path if needed

#### Frontend

- Create or Modify: `wimoorui/tests/e2e/ozon/*.spec.*` if Playwright is introduced
- Optional Create: route query smoke scripts for posting/shipment/error/task deep links

### Tasks

- [ ] 为 Product / Posting / Error / Task 至少补 4 条页面级回归。
- [ ] 本地 smoke 脚本覆盖：
  - [ ] feature meta
  - [ ] auth
  - [ ] product preview
  - [ ] publish task detail
  - [ ] posting list
  - [ ] shipment history
  - [ ] finance list
  - [ ] chat session list
  - [ ] ads summary
- [ ] 灰度发布顺序固定为：
  - [ ] `auth/product/task/error/finance/chat/ads` 先上线
  - [ ] `product-write` 再灰度
  - [ ] `stock-write` / `price-write` 再灰度
  - [ ] `posting-write` 最后灰度
- [ ] `chat-send` 与 `ads-sync` 不纳入本轮默认灰度，除非单独完成 fresh verification 与专项计划。

### Verification

- [ ] `mvn test` 覆盖 Ozon 模块目标用例通过。
- [ ] `wimoorui` 构建通过。
- [ ] 本地网关与只读冒烟通过。
- [ ] 写链路只在灰度环境中验证，不在默认配置中常开。

### Exit Criteria

- [ ] 形成可复用的发布流程，而不是一次性手工演示。

---

## 13. 与现有功能融合的具体约束

### 13.1 必须融合的已有能力

- [ ] 继续复用 [HeaderPlatform.vue](/mnt/d/project/wimoor/wimoorui/src/layout/components/Header/HeaderPlatform.vue) 作为平台入口。
- [ ] 继续复用 [router/index.js](/mnt/d/project/wimoor/wimoorui/src/router/index.js) 与 [ozon.js](/mnt/d/project/wimoor/wimoorui/src/router/modules/ozon.js#L1) 作为路由注册点。
- [ ] 继续复用现有菜单与权限 SQL，而不是硬编码前端菜单。
- [ ] 继续复用 `UserInfoContext + jsessionid + Redis Session` 认证上下文链路。
- [ ] 继续复用当前 `Task` 和 `Error` 页面，不做平行运维系统。

### 13.2 不允许出现的反模式

- [ ] 不新增第二套 Ozon 左侧菜单。
- [ ] 不新增 “Ozon V2” 或 “Ozon New” 页面目录。
- [ ] 不把 return/cancellation/package 各自做成新的一级菜单。
- [ ] 不让前端写死 feature 开关。
- [ ] 不让官方同步模式覆盖掉当前本地导入模式。

### 13.3 统一深链约定

- [ ] Product 页面接受：`authId`, `draftId`, `materialSku`, `focus`
- [ ] Posting 页面接受：`authId`, `postingId`, `postingNumber`, `status`
- [ ] Shipment 页面接受：`authId`, `postingId`, `postingNumber`
- [ ] Error 页面接受：`authId`, `sourceType`, `status`, `keyword`
- [ ] Task 页面接受：`authId`, `jobType`, `status`, `relatedId`

---

## 14. 推荐 PR 切分

- [ ] PR1: `feat: 增加 Ozon feature meta 与前端统一禁用态`
- [ ] PR2: `feat: 完整化 Ozon auth 工作台并补配送方式`
- [ ] PR3: `feat: 增强 Ozon product 工作台与发布任务历史`
- [ ] PR4: `feat: 完整化 Ozon 库存价格操作中心`
- [ ] PR5: `feat: 完整化 Ozon posting shipment 与售后对象`
- [ ] PR6: `feat: 扩展 Ozon finance chat ads 双模页面`
- [ ] PR7: `feat: 补齐 Ozon api_log audit cursor 运维基础设施`
- [ ] PR8: `test: 增加 Ozon e2e 与灰度验收脚本`

---

## 15. 最终交付出口

只有同时满足以下条件，才允许把 Ozon 标记为“继续实施完成”：

- [ ] 所有现有 Ozon 页面都已接入 feature meta，禁用态清晰。
- [ ] `Auth`、`Product`、`Posting` 三个工作台成为主作业入口。
- [ ] `Task` / `Error` 与源页面形成双向跳转。
- [ ] `delivery method`、`package`、`return`、`cancellation` 至少达到列表与查询可用。
- [ ] `sync_cursor`、`api_log`、`operation_audit` 落地。
- [ ] 现有本地导入模式继续可用。
- [ ] 官方同步模式的未完成项明确保留开关关闭，不伪装成已上线。
- [ ] 所有新增后端能力都具备对应前端入口或被明确融合到现有页面。
- [ ] 本地自动化、后端测试、前端构建与 smoke 全部通过。

---

## 16. 本计划与旧计划的关系

- `2026-03-23-ozon-platform-implementation.md` 是平台初始建设总计划。
- `2026-03-28-ozon-delivery-automation.md` 与 `2026-03-28-ozon-full-local-automation.md` 是本地交付与验收 runbook。
- **本文件** 是“下一阶段继续实施计划”，专门解决“现有已实现能力如何补齐、如何把前端融合到原有功能里、如何把缺失业务对象与运维基础设施补完”。

执行顺序上：

- [ ] 先以本文件为开发计划推进代码。
- [ ] 再以本地自动化 runbook 做最终验收。


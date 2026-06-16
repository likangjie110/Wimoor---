# Ozon 平台接入设计方案

## 1. 目标

在当前 Wimoor ERP 体系内新增俄罗斯平台 `Ozon` 的完整销售渠道接入能力，首期目标是让 Ozon 店铺可以在 ERP 内形成可运营闭环，覆盖：

- 店铺授权与安全管理
- 商品、类目、属性、刊登
- 价格、库存、仓库
- 订单、履约、发货、退货取消
- 财务流水、报表、对账
- 客服聊天
- 广告与经营分析
- 任务调度、异常补偿、审计与监控

## 2. 已验证事实

以下内容来自本地代码审查与 Ozon 官方文档核对结果：

### 2.1 现有仓库接入模式

- 当前仓库销售平台实际已形成独立微服务模式：
  - `wimoor-amazon`
  - `wimoor-amazon-adv`
- 网关按平台前缀独立路由：
  - `/amazon/**`
  - `/amazonadv/**`
- 前端 API 目录按平台能力分域组织，Amazon 已形成相对完整的独立目录结构。
- Amazon 授权成功后，系统会立即初始化站点并触发首批同步任务，这说明现有平台接入是“授权即初始化”的运行模式。

### 2.2 Ozon 官方能力边界

- Ozon Seller API 的授权形态为 `Client ID + API Key`，不是 Amazon 的 OAuth 回跳授权。
- Ozon 官方能力面覆盖商品、库存、价格、订单、报表、聊天。
- Ozon 存在独立的 Performance API 能力域，说明经营/广告/表现数据后续可能与主 Seller API 具备不同主机、节流或权限边界。

## 3. 基于证据的设计推断

以下为设计推断，不是官方强制结论：

- Ozon 一期如果覆盖商品、订单、财务、聊天、广告，必须独立成平台业务域，不能缝进 Amazon 现有页面。
- 如果先做 Amazon/Ozon 的统一重构再接 Ozon，会显著拉长交付周期并放大 Amazon 回归风险。
- 最稳妥方案是：`Ozon 独立微服务 + 共享平台契约层`。
- Ozon 原生数据不应直接进入 `db_erp` 核心表，而应先进入专属平台库，再标准化投递给 ERP。

## 4. 总体架构

### 4.1 推荐方案

采用 `方案 C：Ozon 独立微服务 + 共享平台契约层`。

目标平衡点：

- 最小扰动 Amazon 现网链路
- 满足 Ozon 一期全链路业务需求
- 为后续多平台扩展预留统一边界

### 4.2 模块拆分

新增模块建议：

- `wimoor-ozon`
- `wimoor-ozon/ozon-api`
- `wimoor-ozon/ozon-boot`
- 可选 `wimoor-ozon/ozon-performance-api`

首期默认建议先合并到 `ozon-boot`，仅在以下条件同时满足时再拆分 `ozon-performance-api`：

- Ozon Performance API 与 Seller API 在域名或鉴权上差异显著
- 广告/表现任务与主链路任务节流策略明显不同
- 广告报表量级足以影响订单和库存同步

### 4.3 网关设计

新增路由建议：

- `/ozon/**` -> `wimoor-ozon`

如后续拆分表现/广告服务，再新增：

- `/ozonperf/**` -> `wimoor-ozon-performance`

### 4.4 共享平台契约层

首期只抽取少量稳定公共契约，不做大规模重构。

建议放入共享层的内容：

- 平台枚举与平台店铺标识
- 渠道商品映射 DTO
- 渠道订单标准化 DTO
- 渠道库存回写 DTO
- 渠道任务状态模型
- 通用错误码包装
- 凭证加密与审计规范

明确不放入共享层的内容：

- Ozon 专属字段模型
- Ozon 商品属性结构
- Ozon 订单状态机细节
- Ozon 广告与聊天原生对象
- Amazon 既有复杂业务实现

## 5. 数据库与数据域边界

### 5.1 库级边界

建议新增 `db_ozon`。

职责划分：

- `db_ozon`
  - 保存 Ozon 原生对象
  - 保存 Ozon 同步任务与游标
  - 保存 Ozon 报表、错误、审计、聊天、广告
- `db_erp`
  - 继续保存 ERP 主数据与核心业务事实
  - 接收 Ozon 标准化后的订单、库存、财务事实

### 5.2 不建议的做法

不建议：

- 将 Ozon 原生订单表直接写进 ERP 核心订单表
- 将 Ozon offer_id 直接塞进 ERP 物料主表
- 将 Ozon 报表原文混存到 Amazon 数据库

### 5.3 核心表建议

#### 授权与店铺

- `t_ozon_auth`
- `t_ozon_shop_config`
- `t_ozon_warehouse`
- `t_ozon_delivery_method`

#### 商品与映射

- `t_ozon_product`
- `t_ozon_product_sku`
- `t_ozon_product_media`
- `t_ozon_product_attribute`
- `t_ozon_category`
- `t_ozon_attribute_dictionary`
- `t_ozon_product_map`

#### 价格与库存

- `t_ozon_price_task`
- `t_ozon_price_snapshot`
- `t_ozon_stock_task`
- `t_ozon_stock_snapshot`
- `t_ozon_stock_reservation_rule`

#### 订单与履约

- `t_ozon_posting`
- `t_ozon_posting_item`
- `t_ozon_shipment`
- `t_ozon_package`
- `t_ozon_return`
- `t_ozon_cancellation`

#### 财务与报表

- `t_ozon_fin_transaction`
- `t_ozon_settlement_summary`
- `t_ozon_report_task`
- `t_ozon_report_file`

#### 聊天与广告

- `t_ozon_chat_session`
- `t_ozon_chat_message`
- `t_ozon_ads_account`
- `t_ozon_ads_campaign`
- `t_ozon_ads_report`
- `t_ozon_performance_snapshot`

#### 同步、审计与异常

- `t_ozon_sync_job`
- `t_ozon_sync_cursor`
- `t_ozon_api_log`
- `t_ozon_error_event`
- `t_ozon_operation_audit`

### 5.4 关键表字段建议

#### `t_ozon_auth`

- `id`
- `shop_id`
- `group_id`
- `client_id`
- `api_key_ciphertext`
- `api_key_fingerprint`
- `seller_name`
- `status`
- `last_validated_at`
- `last_rotate_at`
- `last_sync_at`
- `created_by`
- `updated_by`

#### `t_ozon_product_map`

- `id`
- `auth_id`
- `erp_sku`
- `ozon_offer_id`
- `ozon_sku`
- `ozon_product_id`
- `mapping_status`
- `last_sync_at`

#### `t_ozon_posting`

- `id`
- `auth_id`
- `posting_number`
- `fulfillment_type`
- `posting_status`
- `substatus`
- `warehouse_id`
- `order_created_at`
- `shipment_deadline_at`
- `customer_payload_json`
- `erp_order_id`
- `sync_version`

#### `t_ozon_sync_job`

- `id`
- `auth_id`
- `job_type`
- `scope_key`
- `cursor_value`
- `status`
- `retry_count`
- `next_run_at`
- `started_at`
- `finished_at`
- `error_code`
- `error_message`

### 5.5 关键数据原则

- API Key 不明文落库
- 商品映射独立建表
- 订单原单与 ERP 标准单双轨保存

## 6. 业务流设计

### 6.1 授权链路

流程：

1. 运营在 Ozon 授权页录入 `Client ID + API Key`
2. 系统调用连通校验接口
3. 成功后保存加密凭证
4. 触发初始化任务：
   - 店铺基本信息
   - 仓库
   - 配送方式
   - 商品基线
   - 最近订单
   - 基础财务报表
5. 建立增量同步 cursor

### 6.2 商品链路

目标：

- ERP 继续作为商品主数据源
- Ozon 作为渠道商品发布与销售载体

流程：

1. ERP 物料进入 Ozon 商品工作台
2. 进行 SKU 映射、类目选择、属性补全、图片绑定
3. 形成刊登草稿
4. 提交 Ozon 商品/Offer
5. 回收审核状态、失败原因、最终 Ozon 标识

### 6.3 价格与库存链路

流程：

1. ERP 计算可售量与渠道价格
2. 应用渠道规则：
   - 安全库存
   - 锁库存
   - 仓库映射
   - 最低价/最高价
   - 活动价覆盖规则
3. 生成批量同步任务
4. 提交 Ozon
5. 回读当前快照
6. 对账差异并生成告警或补偿任务

### 6.4 订单与履约链路

支持范围：

- `FBO`
- `FBS`
- 其他后续变体统一收敛到 `fulfillment_type`

流程：

1. 增量拉取 Ozon posting
2. 落地 `db_ozon` 原始单
3. 标准化生成 ERP 订单事实
4. ERP 执行仓储或平台仓识别逻辑
5. 回写发货状态、追踪号、履约节点
6. 失败对象进入补偿队列

### 6.5 财务链路

采用两段式：

- 渠道原始财务层
- ERP 标准财务层

原始层保存：

- 交易流水
- 结算报表
- 费用项
- 退款
- 广告费用

标准层输出：

- 订单级收益事实
- 费用事实
- 对账结果
- 经营分析口径

### 6.6 聊天与广告链路

首期策略：

- 聊天与广告先作为 Ozon 原生运营域独立实现
- 统一驾驶舱只读聚合结果
- 不强行抽象为与 Amazon 广告完全一致的数据模型

## 7. 同步机制设计

采用四段式同步模型：

### 7.1 初始化同步

新店铺绑定后拉取：

- 仓库
- 配送配置
- 商品清单
- 近 30-90 天订单
- 财务基线
- 当前聊天会话
- 广告/表现基线

### 7.2 增量轮询

每类数据独立维护 cursor：

- 订单 cursor
- 库存 cursor
- 价格 cursor
- 财务报表 cursor
- 聊天 cursor
- 广告报表 cursor

### 7.3 事件回补

对关键对象设置补抓：

- 订单状态
- 发货状态
- 退货取消
- 库存结果

### 7.4 人工重放

所有关键任务必须支持：

- 按店铺重跑
- 按时间窗重跑
- 按对象 ID 重跑

## 8. 前后端设计

### 8.1 前端目录建议

新增 API 目录：

- `wimoorui/src/api/ozon/auth`
- `wimoorui/src/api/ozon/product`
- `wimoorui/src/api/ozon/stock`
- `wimoorui/src/api/ozon/price`
- `wimoorui/src/api/ozon/posting`
- `wimoorui/src/api/ozon/finance`
- `wimoorui/src/api/ozon/chat`
- `wimoorui/src/api/ozon/ads`
- `wimoorui/src/api/ozon/task`

新增页面目录：

- `wimoorui/src/views/ozon/auth`
- `wimoorui/src/views/ozon/product`
- `wimoorui/src/views/ozon/stock`
- `wimoorui/src/views/ozon/price`
- `wimoorui/src/views/ozon/posting`
- `wimoorui/src/views/ozon/finance`
- `wimoorui/src/views/ozon/chat`
- `wimoorui/src/views/ozon/ads`
- `wimoorui/src/views/ozon/task`

### 8.2 页面建议

一期至少提供：

- 店铺授权页
- 商品工作台
- 库存与价格页
- 订单与履约页
- 财务页
- 聊天页
- 广告页
- 任务中心
- 异常中心

### 8.3 复用策略

允许复用：

- 表格
- 筛选器
- 分页
- 状态标签
- 异常抽屉
- 任务弹窗

不建议复用：

- Amazon 业务页面
- Amazon 业务查询模型
- Amazon 状态机逻辑

## 9. 安全与可靠性设计

### 9.1 安全设计

必须满足：

- API Key 仅密文保存
- 日志不打印敏感凭证
- 修改凭证与人工补偿全量审计
- 导出与聊天数据脱敏
- 平台操作走角色权限校验

### 9.2 限流设计

采用双维度限流：

- 按店铺
- 按 API 组

API 组建议：

- 订单组
- 商品组
- 价格库存组
- 财务报表组
- 聊天组
- 广告组

### 9.3 任务调度设计

继续复用 Quartz，但重新定义任务粒度。

分层：

- `L1 初始化任务`
- `L2 高频增量任务`
- `L3 中频任务`
- `L4 低频任务`
- `L5 补偿任务`

### 9.4 异常中心

异常中心必须记录：

- 店铺
- 对象类型
- 对象 ID
- 任务 ID
- 错误码
- 错误摘要
- 首次出现时间
- 最近重试时间
- 当前状态
- 可补偿动作

### 9.5 可观测性

需要三个视角：

- 店铺视角
- 任务视角
- 对象视角

## 10. 实施路线图

### Phase 0：底座与安全基建

- 新建 `wimoor-ozon`
- 新建 `db_ozon`
- 打通网关、配置、权限
- 建立 API Client、加密、限流器、任务骨架、异常中心骨架

### Phase 1：店铺 + 商品 + 价格库存

- 店铺授权
- 仓库同步
- 商品映射
- 商品刊登基础能力
- 价格库存同步
- 快照回读与差异告警

### Phase 2：订单 + 履约

- 拉单
- 标准订单入 ERP
- 分拣/出库/包裹/发货回传
- 退货取消
- 补偿与异常处理

### Phase 3：财务 + 聊天 + 广告

- 财务流水与报表
- 对账
- 聊天收发
- 广告账户与报表

### Phase 4：统一运营与优化

- 统一经营分析
- 利润与库存健康
- 平台共享契约沉淀

## 10.1 首期落地与灰度说明

### 10.1.1 首期默认策略

首期采用“读开写关”：

- 先开放授权、商品映射、任务中心、异常中心
- 财务、聊天、广告先开放只读查询和本地 JSON 导入
- 库存、价格、posting 写回默认关闭
- 聊天主动发送与广告外部同步默认关闭
- 当前明确延期项：`chat.send` 因官方 Ozon 契约未完成核实，暂按未完成能力处理，不纳入本期交付验收

对应部署默认值：

```yaml
ozon:
  feature:
    auth: true
    product: true
    task: true
    error: true
    finance: true
    chat: true
    ads: true
    "stock.write": false
    "price.write": false
    "posting.write": false
    "chat.send": false
    "ads.sync": false
```

### 10.1.2 灰度顺序

建议按以下顺序灰度：

1. `auth`、`product`、`task`、`error`
2. `finance`、`chat`、`ads` 的只读/本地导入能力
3. posting 读取同步与 ERP 桥接验证
4. `stock.write`、`price.write`、`posting.write` 按店铺逐步开启
5. `chat.send`、`ads.sync` 在真实 API 契约与运营 SOP 固化后再开放

### 10.1.3 必需凭证与运行时参数

生产或联调至少需要：

- Ozon 店铺 `Client ID`
- Ozon 店铺 `API Key`
- `OZON_SECURITY_AES_KEY`

本地单机调试额外建议显式覆盖：

- `spring.cloud.nacos.discovery.enabled=false`
- `spring.cloud.nacos.config.enabled=false`
- `spring.mvc.pathmatch.matching-strategy=ant_path_matcher`
- 本地 MySQL、Redis 连接参数
- `config.photo-server`
- `config.photo-server-url`

### 10.1.4 本地运行与操作入口

本地构建/验证建议：

- 后端：`timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -DskipTests -pl wimoor-ozon/ozon-boot -am clean package`
- 前端：`cd wimoorui && timeout 600s ./scripts/build_in_linux_fs.sh`
- 烟雾测试：`timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonSmokeWorkflowTests test`

运营入口页：

- `/ozon/auth`
- `/ozon/product`
- `/ozon/stock`
- `/ozon/price`
- `/ozon/posting`
- `/ozon/task`
- `/ozon/error`
- `/ozon/finance`
- `/ozon/chat`
- `/ozon/ads`

## 11. 测试策略

### 11.1 单元测试

覆盖：

- DTO 转换
- 状态映射
- 请求头构造
- 错误码转换
- 价格与库存规则

### 11.2 集成测试

覆盖：

- Ozon API Client
- 任务调度与 cursor
- `db_ozon` 持久化
- ERP 标准事实写入
- 补偿流程

### 11.3 端到端流程测试

至少覆盖：

- 授权
- 初始化同步
- 商品映射
- 价格库存回写
- 拉单
- 发货回推
- 财务报表入账
- 聊天消息收发
- 广告报表同步

### 11.4 回归测试

必须确认：

- Amazon 不回归
- ERP 主流程不回归
- Quartz 现有任务不受 Ozon 影响

## 12. 风险清单

### 12.1 高风险项

- Ozon 字段模型与状态机复杂
- FBO/FBS/其他模式履约差异大
- 价格库存一致性与限流冲突
- 财务报表口径与订单口径不一致
- 聊天与广告需求易膨胀

### 12.2 风险控制策略

- 按阶段交付，不大爆炸上线
- 先灰度少量店铺
- 先开读链路，再开写链路
- 所有写操作具备独立开关
- 以对象级补偿优先于全量重刷

## 13. 最终结论

推荐路径：

1. 先保底链路
   - 授权
   - 安全
   - 任务框架
   - 商品映射
   - 订单拉取
   - 库存价格回写
   - 异常中心
2. 再做运营链路
   - 财务
   - 聊天
   - 广告
   - 驾驶舱
3. 最后沉淀共享抽象
   - 从 Ozon 项目反提炼平台契约
   - 不先重构 Amazon 再上线

## 14. 与真实凭证相关的处理要求

本设计文档不写入任何真实 `Client ID` 或 `API Key`。

真实凭证仅允许：

- 在授权页面录入
- 在后端加密存储
- 在运行时解密调用

不得：

- 写入源码
- 写入设计文档
- 写入测试快照
- 打印到日志

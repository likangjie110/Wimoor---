# Wimoor Ozon 自动化交付与验收 Runbook

## 1. 目标

把 `wimoor-ozon` 的“开发完整、功能可用”从口头判断改成可执行、可复核、可阻塞的交付流程。

本文件用于约束 Codex 或人工执行者在以下范围内完成 Ozon 交付：

- 范围锁定
- 自动化检查
- 手工冒烟
- 阻塞判定
- 交付出口

只有通过本文件定义的全部关卡，才能把 `wimoor-ozon` 标记为 `PASS`。

## 2. 输入真值

执行本 Runbook 时，以以下文件为范围与实现真值：

- `docs/superpowers/specs/2026-03-23-ozon-design.md`
- `docs/superpowers/specs/2026-03-27-ozon-product-publish-full-design.md`
- `docs/superpowers/plans/2026-03-23-ozon-platform-implementation.md`
- `docs/superpowers/plans/2026-03-27-ozon-product-publish-full-implementation.md`
- `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon`
- `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon`
- `wimoorui/src/router/modules/ozon.js`
- `wimoorui/src/views/ozon`
- `wimoorui/src/api/ozon`
- `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`

如果设计稿、计划、代码、测试之间出现冲突，以“当前本地代码和 fresh verification 结果”为最终真值，并将冲突记录为 `BLOCKED`。

## 3. 状态定义

- `NOT_STARTED`: 尚未执行
- `IN_PROGRESS`: 执行中
- `BLOCKED`: 缺代码、缺环境、缺依赖、缺权限，无法继续
- `FAILED`: 已执行且未通过
- `PASSED`: 自动化检查和手工冒烟都通过

禁止使用 `基本完成`、`差不多`、`应该可用` 这类模糊状态。

## 4. 当前 fresh verification 约束

以下事实来自当前代码和 `docs/superpowers/verification/ozon-local-report-20260328-2311.md`，后续若有新的 fresh report，以新的报告覆盖本段：

- 当前前端已有 11 个 Ozon 页面入口：`auth`、`product`、`stock`、`price`、`chat`、`ads`、`finance`、`posting`、`shipment`、`task`、`error`。
- `OzonProductController` 已存在 `preview`、`publish`、`publish task detail` HTTP 面，`wimoorui/scripts/check_ozon_product_publish_entry.mjs` 也已存在并纳入一键门禁。
- 本地启动必须同时注入 `LOG_HOME`、`JM.LOG.PATH`、`JM.SNAPSHOT.PATH`，否则日志初始化会阻塞启动。
- 默认 Nacos 写开关仍为关闭，且键名使用连字符形式：
  - `ozon.feature.product-write=false`
  - `ozon.feature.stock-write=false`
  - `ozon.feature.price-write=false`
  - `ozon.feature.posting-write=false`
  - `ozon.feature.chat-send=false`
  - `ozon.feature.ads-sync=false`
- 本地网关冒烟必须先调用 `POST /admin/api/v1/auth/login` 获取 `data.session`，再以 `jsessionid` 访问 `/ozon/api/...`；`X-USERINFO` 仅用于直连 `8106/ozon` 的服务级冒烟。
- 本地空库下，写冒烟必须先准备一条 ERP 物料和一条 Ozon product map；否则价格、刊登、发货链路会因基础数据缺失而误判失败。
- 本地 backend reactor 安装不仅包含 `ozon-boot`，还需要安装 ERP 私有 jar，并对 `admin/gateway/erp/ozon` 依赖链执行 `clean install`。

## 5. 完成矩阵

每个子域都必须同时满足“代码存在 + 测试存在 + 自动化通过 + 手工冒烟通过”。缺一项即记为 `FAILED` 或 `BLOCKED`。

| 子域 | 后端入口 | 前端入口 | 最低自动化证据 | 手工冒烟要求 |
| --- | --- | --- | --- | --- |
| Auth | `OzonAuthController` | `views/ozon/auth/index.vue` | `OzonAuthServiceTests` + `OzonAuthServiceSpringContextTests` | 绑定、列表、连通性检测、停用、轮换密钥 |
| Product Mapping | `OzonProductController` | `views/ozon/product/index.vue` | `OzonProductMapServiceTests` + `OzonListingDraftServiceTests` + `OzonProductMetadataServiceTests` | 列表、映射保存、草稿导入 |
| Product Publish | `OzonProductController` | `views/ozon/product/index.vue` | `OzonProductPreviewServiceTests` + `OzonProductPublishServiceTests` + `OzonProductControllerFeatureTests` + `check_ozon_product_publish_entry.mjs` | 草稿详情、预览、发布、任务详情回写 |
| Stock | `OzonStockController` | `views/ozon/stock/index.vue` | `OzonStockSyncServiceTests` | 快照查询、写入开关开启后推送 |
| Price | `OzonPriceController` | `views/ozon/price/index.vue` | `OzonPriceSyncServiceTests` | 快照查询、写入开关开启后推送 |
| Posting | `OzonPostingController` | `views/ozon/posting/index.vue` | `OzonPostingSyncServiceTests` | 拉单、重试、列表、ERP 桥接 |
| Shipment | `OzonShipmentController` | `views/ozon/shipment/index.vue` | `OzonShipmentServiceTests` | 列表、运单推送、按 posting 查询 |
| Task | `OzonTaskController` | `views/ozon/task/index.vue` | `OzonTaskServiceTests` + `OzonTaskServiceSpringBeanTests` | 任务列表、状态过滤 |
| Error | `OzonErrorCenterController` | `views/ozon/error/index.vue` | `OzonErrorCenterServiceTests` + `OzonErrorCenterServiceSpringBeanTests` | 列表、单条重试、忽略 |
| Finance | `OzonFinanceController` | `views/ozon/finance/index.vue` | `OzonFinanceImportTests` + `OzonFinanceServiceSpringBeanTests` | 导入、任务列表、流水列表、原始报表查看 |
| Chat | `OzonChatController` | `views/ozon/chat/index.vue` | `OzonChatSyncTests` + `OzonChatServiceSpringBeanTests` | 导入、会话列表、消息列表、回复记录 |
| Ads | `OzonAdsController` | `views/ozon/ads/index.vue` | `OzonAdsReportTests` + `OzonAdsServiceSpringBeanTests` | 导入、活动列表、报表列表、汇总 |
| Platform Entry | `OzonApplication` + 路由模块 | `router/modules/ozon.js` | `OzonApplicationTests` + `OzonApplicationMapperScanTests` + `OzonSmokeWorkflowTests` + `check_ozon_entry.mjs` | Header 入口、菜单跳转、页面可达 |

## 6. 自动化执行顺序

推荐先执行一键门禁：

```bash
bash tools/ozon/local/90-full-check.sh
```

若需要定位失败，再按以下分步路径执行。

### Step 1: 冻结当前现场

先记录工作区状态，禁止在未理解现有改动的情况下覆盖用户工作。

```bash
git status --short
```

如果 Ozon 相关文件已有未提交改动，后续验证必须基于当前现场继续，不得 reset。

### Step 2: 盘点范围是否齐全

```bash
find wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon -path '*/controller/*.java' | sort
find wimoorui/src/views/ozon -maxdepth 2 -type f | sort
find wimoorui/src/api/ozon -maxdepth 2 -type f | sort
find wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon -type f | sort
node wimoorui/scripts/check_ozon_entry.mjs
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
```

判定规则：

- 缺少控制器、页面、API 或测试文件时，不进入 `PASS`，直接标记对应子域 `BLOCKED`。
- `check_ozon_entry.mjs` 失败时，平台入口直接记为 `FAILED`。
- `check_ozon_product_publish_entry.mjs` 失败时，商品发布子域直接记为 `FAILED`。

### Step 3: 跑 Ozon 后端已验证回归套件

```bash
timeout 900s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests,OzonProductMetadataServiceTests,OzonProductPreviewServiceTests,OzonProductPublishServiceTests,OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test
```

判定规则：

- 任意一个测试失败，整体状态为 `FAILED`。
- 测试全部通过，只能说明当前自动化门禁覆盖的模块级回归通过，不能替代手工冒烟。

### Step 4: 跑前端入口与构建验证

```bash
node wimoorui/scripts/check_ozon_entry.mjs
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
cd wimoorui
timeout 900s ./scripts/build_in_linux_fs.sh
```

判定规则：

- 路由检查失败，平台入口为 `FAILED`。
- 商品发布入口检查失败，商品发布子域为 `FAILED`。
- 构建失败，所有用户可见子域均不得标记 `PASSED`。

### Step 5: 跑跨模块回归

```bash
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon/amazon-boot -am -DskipTests package
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon-adv/amazon-adv-boot -am -DskipTests package
```

判定规则：

- 任一 Amazon 回归构建失败，Ozon 交付整体记为 `FAILED`。
- 原因是 Ozon 不允许以破坏既有 Amazon 流程为代价上线。

### Step 6: 本地服务启动与只读/网关冒烟

环境就绪后，按脚本化顺序启动本地 Ozon 栈：

```bash
bash tools/ozon/local/10-prepare-mysql.sh
bash tools/ozon/local/15-bootstrap-infra.sh
bash tools/ozon/local/25-start-infra.sh
bash tools/ozon/local/30-install-reactor-artifacts.sh
bash tools/ozon/local/40-start-backend.sh
bash tools/ozon/local/50-start-frontend.sh
bash tools/ozon/local/65-smoke-gateway.sh
bash tools/ozon/local/60-smoke-readonly.sh
```

最低只读冒烟项：

- `/api/v1/auth/list`
- `/api/v1/auth/ping`
- `/api/v1/product/draft/list`
- `/api/v1/product/category/tree`
- `/api/v1/stock/snapshot/list`
- `/api/v1/price/snapshot/list`
- `/api/v1/posting/list`
- `/api/v1/shipment/list`
- `/api/v1/task/list`
- `/api/v1/error/list`
- `/api/v1/finance/task/list`
- `/api/v1/chat/session/list`
- `/api/v1/ads/summary`

可选增强项：

- 当 `DRAFT_ID` 已知时，再追加 `/api/v1/product/draft/detail` 与 `/api/v1/product/preview`

判定规则：

- `65-smoke-gateway.sh` 失败时，不允许宣称“前端本地可用”。
- 任何只读接口启动失败、报 5xx 或依赖不可达，整体记为 `BLOCKED` 或 `FAILED`。
- 只读冒烟通过后，仍不能代表写链路可用。

### Step 7: 写操作灰度冒烟

只有在明确接受灰度写入后，才允许临时开启以下开关：

- `ozon.feature.product-write=true`
- `ozon.feature.stock-write=true`
- `ozon.feature.price-write=true`
- `ozon.feature.posting-write=true`

默认不打开 `ozon.feature.chat-send` 和 `ozon.feature.ads-sync`。若要宣称这两个能力“真实可用”，必须先补充官方合同核验和新的 fresh verification。

执行入口：

```bash
bash tools/ozon/local/70-smoke-write.sh
```

写操作最低冒烟项：

- Product Publish: `/api/v1/product/publish`、`/api/v1/product/publish/task/detail`
- Stock: `/api/v1/stock/push`
- Price: `/api/v1/price/push`
- Posting: `/api/v1/posting/sync`、`/api/v1/posting/retryOne`
- Shipment: `/api/v1/shipment/pushTracking`
- Chat: `reply/record` 只验证记录链路；若真实发送合同未核实，不得声称“已具备真实发送能力”
- Ads: `import` 只验证本地导入链路；若真实远程同步合同未核实，不得声称“已具备真实同步能力”

判定规则：

- 如果功能开关关闭时接口仍执行真实写操作，记为严重缺陷。
- 如果空库未准备样例 ERP 物料 / Ozon product map 就直接跑写链路，结果只能记为 `BLOCKED`，不能记为“代码失败”。
- 如果开关开启后写链路失败，相关子域记为 `FAILED`。

## 7. 交付出口

只有满足以下全部条件，才能把 `wimoor-ozon` 标记为“功能完整可用”：

- 完成矩阵所有必需子域均为 `PASSED`
- 后端 Ozon 已验证回归套件通过
- 前端 Ozon 入口检查通过
- 前端构建通过
- Amazon 双模块回归构建通过
- 网关冒烟通过
- 本地只读冒烟通过
- 所有声明已开放的写操作冒烟通过
- 没有未解释的红旗

只要存在以下任一情况，结论必须降级：

- 使用旧的点号键名修改 Nacos，导致写开关实际上没有生效
- 未通过 `jsessionid` 网关冒烟，却宣称前端本地可用
- 本地空库未准备样例数据，却把基础数据缺失误报为代码不可用
- 环境未启动成功却宣称“本地可用”
- 只跑了 Bean smoke，却宣称“业务完整可用”

## 8. 结果记录模板

每次执行完成后，用以下模板记录，不允许只报一句“通过了”：

```md
## Ozon Delivery Report

- 日期: 2026-03-28
- 执行人: <name>
- 代码基线: <commit or working tree note>

### Automated Checks
- Ozon backend tests: PASSED | FAILED | BLOCKED
- Ozon entry check: PASSED | FAILED | BLOCKED
- Frontend build: PASSED | FAILED | BLOCKED
- Amazon regression package: PASSED | FAILED | BLOCKED

### Domain Matrix
- Auth:
- Product Mapping:
- Product Publish:
- Stock:
- Price:
- Posting:
- Shipment:
- Task:
- Error:
- Finance:
- Chat:
- Ads:
- Platform Entry:

### Manual Smoke
- Gateway smoke:
- Read-only smoke:
- Write smoke:

### Red Flags
- <none or list>

### Final Decision
- PASS | FAIL | BLOCKED
```

## 9. 执行原则

- 证据优先于判断
- Fresh verification 优先于历史结论
- 最小变更优先于重写
- 发现缺口先记录为 `BLOCKED`，不要伪造成“已完成”

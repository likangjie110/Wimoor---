# Wimoor Project Map

本目录是 Wimoor 仓库的项目初始化梳理入口，覆盖后端 Maven 多模块、前端 Vue/Vite、运行配置、数据库、定时任务和关键业务流程。文档目标是建立可维护索引，不替代源码、SQL 或 Nacos 配置本身。

中文手册在 [../zh-cn/README.md](../zh-cn/README.md) 维护，`docs/zh-cn` 同时作为 Obsidian vault 和 Docsify 内容源。项目地图作为事实索引，中文手册作为面向阅读和交接的说明层。

## Reading Order

1. [architecture.md](architecture.md) - 系统总览、服务边界和整体架构图。
2. [module-map.md](module-map.md) - Maven 模块、启动类、服务名、端口和职责。
3. [runtime-config.md](runtime-config.md) - Bootstrap、Nacos、Gateway、MySQL、Redis、Seata 配置关系。
4. [frontend-map.md](frontend-map.md) - Vite、路由、权限守卫和前端 API 分组。
5. [backend-api-index.md](backend-api-index.md) - 后端 Controller、Feign 和接口前缀索引。
6. [database-index.md](database-index.md) - 数据库结构、种子数据和业务归属。
7. [scheduled-jobs.md](scheduled-jobs.md) - Quartz 任务来源、分组和典型调用链。

## Diagrams

流程图和时序图在 [flows](flows) 下维护：

| 文件 | 内容 |
| --- | --- |
| [system-context.md](flows/system-context.md) | 系统上下文和外部依赖 |
| [request-sequence.md](flows/request-sequence.md) | 前端请求到网关和后端服务时序 |
| [login-auth-sequence.md](flows/login-auth-sequence.md) | 登录、SSO、权限加载和前端路由守卫 |
| [scheduled-task-sequence.md](flows/scheduled-task-sequence.md) | Quartz 任务调度和服务调用 |
| [amazon-report-flow.md](flows/amazon-report-flow.md) | Amazon 报表、Feed、库存任务主流程 |
| [erp-purchase-shipment-flow.md](flows/erp-purchase-shipment-flow.md) | ERP 采购、仓库、发货流程 |
| [ozon-flow.md](flows/ozon-flow.md) | Ozon 授权、商品、库存、聊天和任务同步 |
| [finance-flow.md](flows/finance-flow.md) | Finance 凭证、科目、账簿、报表流程 |

## Indexes

可维护索引在 [indexes](indexes) 下维护：

| 文件 | 内容 |
| --- | --- |
| [services.md](indexes/services.md) | 服务、端口、启动类、网关路径 |
| [controllers.md](indexes/controllers.md) | Controller 分布、关键基础路径、再生成命令 |
| [frontend-routes.md](indexes/frontend-routes.md) | 前端静态路由和动态路由来源 |
| [frontend-apis.md](indexes/frontend-apis.md) | 前端 API 文件分组和代理路径 |
| [databases.md](indexes/databases.md) | 数据库结构表数、种子数据表数 |
| [nacos-configs.md](indexes/nacos-configs.md) | Nacos 配置文件、行数、敏感键处理 |
| [quartz-jobs.md](indexes/quartz-jobs.md) | Quartz 任务服务分布和业务分组 |

## Source Boundaries

- 后端源码：`wimoor-admin`、`wimoor-erp`、`wimoor-amazon`、`wimoor-amazon-adv`、`wimoor-ozon`、`wimoor-modules`、`wimoor-common`、`wimoor-api`、`wimoor-gateway`。
- 前端源码：`wimoorui/src`。
- 初始化配置：`init-config/mysql`、`init-config/nacos`、`init-config/seata`。
- 排除目录：`.git`、`node_modules`、`target`、`logs`。

## Verified Baseline

- 根 `pom.xml` 聚合 9 个顶层后端模块：`wimoor-common`、`wimoor-admin`、`wimoor-gateway`、`wimoor-erp`、`wimoor-amazon`、`wimoor-amazon-adv`、`wimoor-ozon`、`wimoor-api`、`wimoor-modules`。
- 前端为 `wimoorui`，使用 Vue 3、Vite、Element Plus、Vue Router、Vuex、Axios。
- 网关端口为 `8099`，前端开发服务端口为 `8084`，前端代理到 `http://localhost:8099`。
- 后端扫描到约 260 个 `@RestController`、8 个 `@FeignClient`、502 个 Mapper 文件。
- 前端扫描到 78 个路由 path、190 个 API 文件。
- `init-config/mysql/数据库结构` 下有 764 个结构 SQL，`init-config/mysql/数据` 下有 64 个种子 SQL。

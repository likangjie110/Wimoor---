# Wimoor 中文说明文档

> 面向部署、二次开发和维护交接的 Wimoor ERP 中文说明手册。

## 概述

Wimoor 是跨境电商 ERP 系统，后端采用 Maven 多模块与 Spring Cloud 微服务，前端采用 Vue 3 + Vite。系统通过 `wimoor-gateway` 统一接入后端服务，使用 Nacos 承载配置与服务发现，业务数据按模块拆分到 MySQL 多个库中。

本手册参考 docsify 风格组织，目标是把项目结构、运行方式、功能边界、关键流程和排错方法讲清楚。更细的技术索引保留在 [项目地图](../project-map/README.md) 中。

## 在线访问

GitHub Pages 发布源设置为 `GitHub Actions` 后，可通过 Docsify 在线访问：

- [Wimoor 文档站首页](https://likangjie110.github.io/Wimoor---/)
- [Wimoor 中文文档直达](https://likangjie110.github.io/Wimoor---/zh-cn/#/)

如果访问 404，先到 GitHub 仓库 `Settings -> Pages` 确认：

| 配置项 | 值 |
| --- | --- |
| Source | GitHub Actions |

本仓库已提供 `.github/workflows/pages.yml`。保存 Pages 设置后，`pages` workflow 会把 `docs` 目录作为 GitHub Pages artifact 发布。等待部署完成后，再刷新上述地址。

## 核心特性

- 前后端分离：前端在 `wimoorui`，后端在多个 `wimoor-*` Maven 模块。
- 微服务路由：`wimoor-gateway` 按 `/admin`、`/erp`、`/amazon`、`/ozon` 等路径转发。
- 多业务域：覆盖系统管理、ERP、Amazon、Amazon Ads、Ozon、财务、报价、数据迁移和代码生成。
- 多数据源初始化：`init-config/mysql` 提供业务库结构和种子数据。
- 配置中心：`init-config/nacos/DEFAULT_GROUP` 提供服务配置、网关路由和外部集成配置模板。
- 定时任务：`db_admin.t_sys_quartz_task` 定义 Amazon、Amazon Ads、ERP 等任务调用链。

## 系统能力清单

**作为跨境 ERP 平台**

- [X] 登录、SSO、用户、角色、菜单、权限、字典、文件和系统工具
- [X] 商品资料、品牌、分类、采购资料、组合品和耗材管理
- [X] 采购计划、采购单、1688 订单跟踪、采购财务
- [X] 仓库、货架、入库、出库、盘点、调拨和库存报表
- [X] Amazon 授权、订单、报表、Feed、Listing、FBA 发货、结算和利润分析
- [X] Amazon Ads 授权、广告活动、关键词、预算、报表和发票
- [X] Ozon 授权、商品、库存、价格、订单、发货、聊天、财务和广告
- [X] 财务会计期间、科目、辅助核算、凭证、账簿和报表模板
- [X] 供应商报价、数据迁移、代码生成和运维辅助工具

## 阅读入口

- 新人先读：[项目概览](_content/introduction/overview.md)、[环境要求](_content/introduction/environment.md)、[编译](_content/introduction/compile.md)。
- 部署先读：[配置](_content/introduction/config.md)、[部署](_content/introduction/deployment.md)、[启动顺序](_content/introduction/startup.md)。
- 功能梳理看：[功能与使用](_content/ability/login-auth.md)。
- 架构和流程看：[流程与原理](_content/theory/architecture.md) 与 [流程图目录](_content/flows/system-context.md)。
- 排错看：[常见问答](_content/qa/start-error.md)。
- 本地维护看：[Obsidian 维护](_content/skill/obsidian-workflow.md) 与 [GitHub Pages 发布](_content/skill/github-pages.md)。

## 维护模式

本仓库的中文说明文档采用 `Obsidian + GitHub Pages + Docsify`：

- `docs/zh-cn` 是 Obsidian 本地知识库根目录。
- `docs/index.html` 和 `docs/zh-cn/index.html` 负责 Docsify 渲染。
- `tools/docs` 下的脚本负责预览、检查和发布。
- GitHub Pages 通过 `.github/workflows/pages.yml` 发布 `docs` 目录。

## 文档边界

本手册不复制敏感配置值，不替代源码、SQL 或 Nacos 配置本身。涉及 `password`、`secret`、`token`、`key` 等内容时只保留键名或 `<redacted>`。

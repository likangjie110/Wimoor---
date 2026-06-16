# 配置

## 配置来源

| 来源 | 说明 |
| --- | --- |
| `*/src/main/resources/bootstrap.yml` | 指定 active profile |
| `bootstrap-dev.yml` / `bootstrap-prod.yml` | 服务名、端口、Nacos discovery/config 基础配置 |
| `init-config/nacos/DEFAULT_GROUP` | 业务数据源、网关路由、外部平台、Feign、Seata 等配置模板 |
| `init-config/mysql` | 数据库结构和基础数据 |
| `wimoorui/vite.config.js` | 前端 dev server 和代理配置 |

## 网关配置

网关路由在 Nacos 的 `wimoor-gateway` 配置中维护，核心路由如下：

| 路径 | 服务 |
| --- | --- |
| `/admin/**` | `wimoor-admin` |
| `/erp/**` | `wimoor-erp` |
| `/amazon/**` | `wimoor-amazon` |
| `/amazonadv/**` | `wimoor-amazon-adv` |
| `/ozon/**` | `wimoor-ozon` |
| `/quote/**` | `wimoor-quote` |
| `/mdata/**` | `wimoor-data` |
| `/finance/**` | `wimoor-finance` |
| `/code/**` | `wimoor-gen` |

## 敏感配置

文档只记录键名，不记录真实值。涉及 `password`、`secret`、`token`、`key`、第三方 app 凭据时统一写为 `<redacted>`。


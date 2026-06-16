# 网关路由

`wimoor-gateway` 使用 Spring Cloud Gateway。实际路由配置来自 Nacos `wimoor-gateway` 配置。

## 路由规则

| 路径 | 目标服务 |
| --- | --- |
| `/admin/**` | `lb://wimoor-admin` |
| `/erp/**` | `lb://wimoor-erp` |
| `/amazon/**` | `lb://wimoor-amazon` |
| `/amazonadv/**` | `lb://wimoor-amazon-adv` |
| `/ozon/**` | `lb://wimoor-ozon` |
| `/quote/**` | `lb://wimoor-quote` |
| `/mdata/**` | `lb://wimoor-data` |
| `/finance/**` | `lb://wimoor-finance` |
| `/code/**` | `lb://wimoor-gen` |

## 放行路径

登录、注册、短信、授权回调、Webhook、供应商报价提交等路径通过 `security.ignoreUrls` 放行。具体路径以 Nacos 配置为准。


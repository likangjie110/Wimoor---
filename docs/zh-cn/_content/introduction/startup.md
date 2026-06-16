# 启动顺序

## 后端服务顺序

1. 启动 MySQL。
2. 启动 Redis。
3. 启动 Nacos，并确认服务配置已导入。
4. 按需启动 Seata。
5. 启动 `wimoor-admin`。
6. 启动 `wimoor-gateway`。
7. 启动业务服务：`wimoor-erp`、`wimoor-amazon`、`wimoor-amazon-adv`、`wimoor-quote`、`wimoor-data`、`wimoor-finance`、`wimoor-ozon`、`wimoor-gen`。
8. 启动前端 `wimoorui`。

## 端口基线

| 服务 | 端口 |
| --- | ---: |
| `wimoor-gateway` | 8099 |
| `wimoor-admin` | 8100 |
| `wimoor-erp` | 8101 |
| `wimoor-amazon` | 8102 |
| `wimoor-amazon-adv` | 8103 |
| `wimoor-data` | 8104 |
| `wimoor-quote` | 8105 |
| `wimoor-finance` | 8106 |
| `wimoor-ozon` | 8106 |
| `wimoor-gen` | 8107 |
| `wimoorui` | 8084 |

`wimoor-finance` 与 `wimoor-ozon` 当前存在 `8106` 端口冲突，启动前必须调整其中一个服务端口或错开运行。


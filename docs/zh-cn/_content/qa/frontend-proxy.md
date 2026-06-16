# 前端代理

## 开发代理

`wimoorui/vite.config.js` 将 `/admin/api`、`/erp/api`、`/amazon/api`、`/ozon/api` 等路径代理到 `http://localhost:8099`。

## 常见问题

| 现象 | 检查 |
| --- | --- |
| 请求打到前端 404 | proxy path 是否匹配 |
| 网关 404 | Nacos gateway route 是否存在 |
| 服务 404 | Controller path 是否正确 |
| 跨域异常 | 是否绕过网关直接访问服务 |

## 建议

开发环境也尽量走网关路径，避免前后端路径在生产环境漂移。


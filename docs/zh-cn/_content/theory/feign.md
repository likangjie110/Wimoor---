# Feign 调用

跨服务调用通过各 `*-api` 模块和 `wimoor-api` 中的 Feign 接口表达。

## 主要契约

| 契约 | 目标 |
| --- | --- |
| `AdminClientOneFeign` | `wimoor-admin` |
| `ErpClientOneFeign` | `wimoor-erp` |
| `AmazonClientOneFeign` | `wimoor-amazon` |
| `AmazonAdvFeignClient` | `wimoor-amazon-adv` |
| `RemoteAmazonService` | Amazon 服务常量 |
| `RemoteOzonService` | Ozon 服务常量 |
| `QuoteClientOneFeign` | `wimoor-quote` |

## 设计原则

- Feign 接口应与目标服务 Controller 路径保持一致。
- 公共契约变更会影响调用方，修改前应检查引用。
- 新增跨服务调用时优先放到对应 API 模块，避免业务模块直接拼接远程 URL。


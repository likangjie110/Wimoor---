# Ozon 当前状态清单

更新时间：2026-07-11

| 模块 | 代码完成 | 合同验证 | 生产启用 | 说明 |
| --- | --- | --- | --- | --- |
| Auth / Warehouse | 已完成 | 待真实账号验证 | 可读功能默认可用 | 本地授权、仓库与配送设置保留兼容接口 |
| Product | 已完成 | 待真实账号验证 | 写入默认关闭 | 商品发布由 `product-write` 控制 |
| Stock | 已完成 | 待真实账号验证 | 写入默认关闭 | 任务详情已从 `t_ozon_stock_snapshot.task_id` 返回 SKU 结果 |
| Price | 已完成 | 待真实账号验证 | 写入默认关闭 | 价格写入由 `price-write` 控制 |
| Posting / After-sale | 已完成 | 待真实账号验证 | 写入默认关闭 | 取消订单核心请求要求 `cancelReasonId` |
| Finance | 已完成 | 待真实账号验证 | 远程同步默认关闭 | 本地页面 `finance` 与远程同步 `finance-sync` 已拆分 |
| Chat | 已完成 | 待真实账号验证 | 同步/发送默认关闭 | 本地导入可用，远程同步 `chat-sync`、发送 `chat-send` 独立控制 |
| Ads | 已完成 | 待真实账号验证 | 远程同步默认关闭 | Performance API 独立 Client；无账号 ID 时返回诊断错误 |
| Ops / Monitoring | 已完成 | 不适用 | 可读功能默认可用 | `/api/v1/ops/dashboard` 提供真实日志、审计与错误聚合 |
| CI / Contract | 已完成 | 手工触发 | 不自动发布 | `ozon-contract` job 仅在 `workflow_dispatch` 且凭据显式存在时运行 |

生产配置要求：所有写入与远程同步门禁保持默认关闭，本次不执行正式灰度发布。

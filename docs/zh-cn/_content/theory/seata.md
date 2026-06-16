# Seata 事务

Seata 用于需要跨库或跨服务一致性的业务场景。初始化配置在 `init-config/seata`，数据库结构在 `init-config/mysql/数据库结构/seata`。

## 组成

| 组件 | 说明 |
| --- | --- |
| Seata Server | 管理全局事务和分支事务 |
| `seata` 数据库 | `global_table`、`branch_table`、`lock_table` 等事务元数据 |
| 业务服务 | 通过 Spring Cloud Alibaba Seata starter 参与事务 |

## 使用注意

- 只有确实需要跨服务一致性时才应使用分布式事务。
- 事务边界应尽量小，避免长事务。
- 本地调试前确认 Seata 配置、Nacos 配置和数据库连接一致。


# 数据库分库

系统按业务域拆分数据库，初始化结构在 `init-config/mysql/数据库结构`，基础数据在 `init-config/mysql/数据`。

| 数据库 | 归属服务 | 说明 |
| --- | --- | --- |
| `db_admin` | Admin | 用户、角色、菜单、权限、任务、字典 |
| `db_quartz` | Admin/Quartz | Quartz 调度表 |
| `db_erp` | ERP | 物料、采购、仓库、库存、发货 |
| `db_amazon` | Amazon | 授权、订单、报表、商品、FBA、结算 |
| `db_amazon_adv` | Amazon Ads | 广告、报表、发票 |
| `db_ozon` | Ozon | 商品、库存、订单、聊天、财务 |
| `db_finance` | Finance | 科目、凭证、账簿、报表 |
| `db_quote` | Quote | 报价、供应商、运输 |
| `db_datamove` | Data | 数据迁移 |
| `seata` | Seata | 分布式事务元数据 |

详细统计见 [数据库索引](../reference/databases.md)。


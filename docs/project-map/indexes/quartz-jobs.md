# Quartz Jobs Index

Source: `init-config/mysql/数据/db_admin/t_sys_quartz_task.sql`.

## Target Services

| Target service | Seed tasks |
| --- | ---: |
| `wimoor-amazon` | 53 |
| `wimoor-amazon-adv` | 9 |
| `wimoor-erp` | 2 |

## Business Groups

| Group | Seed tasks |
| --- | ---: |
| `亚马逊报表` | 29 |
| `亚马逊广告` | 9 |
| `亚马逊商品` | 5 |
| `亚马逊财务` | 4 |
| `亚马逊订单` | 4 |
| `亚马逊汇总` | 3 |
| `亚马逊消息` | 2 |
| `阿里巴巴` | 1 |
| `进销存` | 1 |
| `亚马逊产品` | 1 |
| `亚马逊模块` | 1 |
| `亚马逊权限` | 1 |
| `亚马逊授权` | 1 |
| `亚马逊FBA` | 1 |
| `亚马逊Feed` | 1 |

## Refresh Command

```powershell
rg -n "REPLACE INTO `t_sys_quartz_task`" "init-config/mysql/数据/db_admin/t_sys_quartz_task.sql"
```


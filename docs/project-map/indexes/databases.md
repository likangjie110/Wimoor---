# Databases Index

## Structure SQL

| Database | Files |
| --- | ---: |
| `db_admin` | 79 |
| `db_amazon` | 238 |
| `db_amazon_adv` | 159 |
| `db_datamove` | 4 |
| `db_erp` | 189 |
| `db_finance` | 24 |
| `db_ozon` | 34 |
| `db_quartz` | 11 |
| `db_quote` | 22 |
| `seata` | 4 |

## Seed SQL

| Database | Files |
| --- | ---: |
| `db_admin` | 15 |
| `db_amazon` | 37 |
| `db_amazon_adv` | 3 |
| `db_erp` | 9 |

## Refresh Commands

```powershell
rg --files "init-config/mysql/数据库结构" -g "*.sql"
rg --files "init-config/mysql/数据" -g "*.sql"
```


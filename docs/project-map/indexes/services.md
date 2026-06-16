# Services Index

| Service | Module | Main class | Port | Gateway route | Database |
| --- | --- | --- | ---: | --- | --- |
| `wimoor-gateway` | `wimoor-gateway` | `GatewayApplication` | 8099 | entry | none |
| `wimoor-admin` | `wimoor-admin/admin-boot` | `AdminApplication` | 8100 | `/admin/**` | `db_admin`, `db_quartz` |
| `wimoor-erp` | `wimoor-erp/erp-boot` | `ERPApplication` | 8101 | `/erp/**` | `db_erp` |
| `wimoor-amazon` | `wimoor-amazon/amazon-boot` | `AmazonApplication` | 8102 | `/amazon/**` | `db_amazon` |
| `wimoor-amazon-adv` | `wimoor-amazon-adv/amazon-adv-boot` | `AmazonAdvApplication` | 8103 | `/amazonadv/**` | `db_amazon_adv` |
| `wimoor-data` | `wimoor-modules/wimoor-data` | `DataApplication` | 8104 | `/mdata/**` | `db_datamove` |
| `wimoor-quote` | `wimoor-modules/wimoor-quote` | `QuoteApplication` | 8105 | `/quote/**` | `db_quote` |
| `wimoor-finance` | `wimoor-modules/wimoor-finance` | `WimoorFinanceApplication` | 8106 | `/finance/**` | `db_finance` |
| `wimoor-ozon` | `wimoor-ozon/ozon-boot` | `OzonApplication` | 8106 | `/ozon/**` | `db_ozon` |
| `wimoor-gen` | `wimoor-modules/wimoor-gen` | `WimoorGenApplication` | 8107 | `/code/**` | service-specific generated metadata |
| `proxy` | `wimoor-erp/wimoor-erp-proxy` | `ProxyApplication` | 8080 | not in gateway route | proxy/open integration |

`wimoor-finance` and `wimoor-ozon` share port `8106` in current config and require environment-level resolution before running both locally.


# Backend API Index

## Controller Distribution

| Root | RestController count | RequestMapping count | Primary API prefix |
| --- | ---: | ---: | --- |
| `wimoor-admin` | 30 | 54 | `/admin/api/v1/**` through gateway |
| `wimoor-erp` | 68 | 77 | `/erp/api/v1/**`, `/erp/api/v2/**` through gateway |
| `wimoor-amazon` | 94 | 99 | `/amazon/api/v0/**`, `/amazon/api/v1/**`, `/amazon/api/v2/**` through gateway |
| `wimoor-amazon-adv` | 19 | 46 | `/amazonadv/api/v1/**` through gateway |
| `wimoor-ozon` | 15 | 15 | `/ozon/api/v1/**` through gateway |
| `wimoor-modules` | 30 | 30 | `/quote/**`, `/mdata/**`, `/finance/**`, `/code/**` through gateway |
| `wimoor-gateway` | 0 | 0 | route-only service |

## Key API Families

| Service | Families |
| --- | --- |
| Admin | auth, users, roles, menus, permissions, dicts, files, SMS, email, tags, query fields, progress, task/quartz |
| ERP | material, category, brand, customer, purchase plan/form, inventory manager, warehouse, stocktaking, shipment plan/form, 1688, thirdparty |
| Amazon | authority, marketplace, reports, orders, product/listing, feed, inbound shipment, settlement, profit, transparency |
| Amazon Adv | ads, campaign, budget rules, keywords, product targeting, stores, report, invoices, schedule |
| Ozon | auth, product, stock, price, posting, shipment, chat, finance, ads, meta, ops, error, task |
| Finance | periods, subjects, auxiliary items/types, vouchers, entries, ledgers, report templates/items, code cache/rules |
| Quote/Data/Gen | supplier quote, purchase quote, shipment quote, data move, code generation |

## Feign Clients

| File | Target | Purpose |
| --- | --- | --- |
| `wimoor-admin/admin-api/.../AdminClientOneFeign.java` | `wimoor-admin` | user roles, tags, dicts, mail, openid binding |
| `wimoor-erp/erp-api/.../ErpClientOneFeign.java` | `wimoor-erp` | material, inventory, shipment, purchase and quote-related ERP calls |
| `wimoor-amazon/amazon-api/.../AmazonClientOneFeign.java` | `wimoor-amazon` | shipment, authority, marketplace, listing, product, order and settlement calls |
| `wimoor-amazon-adv/amazon-adv-api/.../AmazonAdvFeignClient.java` | `wimoor-amazon-adv` | ads invoice summary |
| `wimoor-api/wimoor-api-amazon/.../RemoteAmazonService.java` | Amazon service constant | Amazon group list with fallback factory |
| `wimoor-api/wimoor-api-ozon/.../RemoteOzonService.java` | Ozon service constant | Ozon auth ping |
| `wimoor-api/wimoor-api-quote/.../QuoteClientOneFeign.java` | `wimoor-quote` | quote save |
| `wimoor-erp/erp-boot/.../AmazonClientOneFeignApi.java` | `wimoor-amazon` | ERP-to-Amazon local contract |

## Regeneration Commands

Use these from the repository root to refresh the index:

```powershell
rg -n "@RestController|@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" -g "*.java"
rg -n "@FeignClient|@(Get|Post|Put|Delete|Request)Mapping" wimoor-api wimoor-admin/admin-api wimoor-erp/erp-api wimoor-amazon/amazon-api wimoor-amazon-adv/amazon-adv-api -g "*.java"
```


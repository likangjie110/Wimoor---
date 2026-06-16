# Nacos Configs Index

Source root: `init-config/nacos/DEFAULT_GROUP`.

| Config | Lines | Main content |
| --- | ---: | --- |
| `seataServer.properties` | 102 | Seata server registry/store settings |
| `wimoor-admin` | 59 | admin datasource, Quartz, mail, SMS, CAS, WeChat, Feishu, DeepSeek |
| `wimoor-amazon` | 63 | Amazon datasource and integration settings |
| `wimoor-amazon-adv` | 14 | Amazon Ads datasource and settings |
| `wimoor-commom-ext` | 28 | common extension config |
| `wimoor-common` | 14 | shared config |
| `wimoor-data` | 34 | data move config |
| `wimoor-erp` | 36 | ERP datasource, 1688, multipart, Feign |
| `wimoor-finance` | 52 | finance datasource and code/rule config |
| `wimoor-gateway` | 68 | gateway routes and security ignore URLs |
| `wimoor-gen` | 56 | code generation datasource/config |
| `wimoor-ozon` | 35 | Ozon datasource/config |
| `wimoor-quote` | 28 | quote datasource/config |

## Redaction Policy

When copying config details into docs, replace values for keys matching `password`, `secret`, `token`, `key`, `app-id`, `appSecret`, `access_key`, `username` when credential-like, and third-party private values with `<redacted>`.

## Refresh Command

```powershell
$rows=foreach($f in rg --files "init-config/nacos/DEFAULT_GROUP"){
  [pscustomobject]@{Name=(Split-Path $f -Leaf); Lines=((Get-Content $f | Measure-Object -Line).Lines)}
}
$rows | Sort-Object Name
```


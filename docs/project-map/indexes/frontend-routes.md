# Frontend Routes Index

## Static Route Files

| File | Path entries | Notes |
| --- | ---: | --- |
| `wimoorui/src/router/index.js` | 4 | layout root, home, blank, user center, global guard |
| `wimoorui/src/router/modules/sysRouter.js` | 8 | SSO/login/register/reset/public callbacks |
| `wimoorui/src/router/modules/erp.js` | 41 | ERP workflow pages and detail routes |
| `wimoorui/src/router/modules/amazon.js` | 12 | Amazon shipment/listing/ad/finance helper pages |
| `wimoorui/src/router/modules/ozon.js` | 11 | Ozon auth/product/stock/price/chat/ads/finance/posting/shipment/task/error |
| `wimoorui/src/router/modules/finance.js` | 2 | code generation edit route |

## White Paths

Defined in `wimoorui/src/router/index.js`:

| Path | Purpose |
| --- | --- |
| `/ssologin` | SSO login entry |
| `/authresult` | Amazon auth callback result |
| `/login` | local login |
| `/register` | registration |
| `/resetPassword` | password reset |
| `/quote` | supplier quote public route |

## Dynamic Route Source

After session/permission checks, backend menu metadata is transformed into dynamic routes and committed to Vue Router. Permission strings are stored in `permissionStore.permission` and consumed by permission directives.

## Refresh Command

```powershell
rg -n "path:" wimoorui/src/router/index.js wimoorui/src/router/modules
```


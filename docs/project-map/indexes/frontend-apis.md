# Frontend APIs Index

Source root: `wimoorui/src/api`.

| Domain | File count | Gateway prefix | Main topics |
| --- | ---: | --- | --- |
| `amazon` | 73 | `/amazon/api`, `/amazonadv/api` | auth, ads, feed, finance, inbound, listing, order, product, profit, summary, transparency |
| `erp` | 58 | `/erp/api` | assembly, common download, finance, inventory, material, order, purchase, ship, shipv2, thirdparty, warehouse |
| `finance` | 14 | `/finance/api` | periods, subjects, auxiliary, vouchers, entries, ledgers, reports, cache, category, log |
| `ozon` | 13 | `/ozon/api` | auth, product, stock, price, posting, shipment, chat, ads, finance, meta, ops, task, error |
| `quote` | 4 | `/quote/api` | order, purchase, supplier, transchannel |
| `sys` | 28 | `/admin/api`, `/code/gen` | login, admin users/roles/menus/permissions, files, notify, task, tool APIs |

## Refresh Command

```powershell
$files=rg --files wimoorui/src/api -g "*.js"
$files | ForEach-Object {
  $p=($_ -replace "\\","/")
  $domain=(($p -replace "^wimoorui/src/api/","") -split "/")[0]
  [pscustomobject]@{Domain=$domain; File=($p -replace "^wimoorui/src/api/","")}
} | Sort-Object Domain,File
```


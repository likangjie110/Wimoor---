# Controllers Index

## Distribution

| Root | Controllers | Main prefixes |
| --- | ---: | --- |
| `wimoor-admin` | 30 | `/api/v1/auth`, `/api/v1/users`, `/api/v1/menus`, `/api/v1/roles`, `/api/v1/task` |
| `wimoor-erp` | 68 | `/api/v1/material`, `/api/v1/purchase_*`, `/api/v1/inventory`, `/api/v1/warehouse`, `/api/v1/ship*`, `/api/v2/shipForm` |
| `wimoor-amazon` | 94 | `/api/v0/orders`, `/api/v0/feed`, `/api/v1/report`, `/api/v1/product`, `/api/v1/ship*`, `/api/v1/settlement`, `/api/v2/shipInboundPlan` |
| `wimoor-amazon-adv` | 19 | `/api/v1/ads`, `/api/v1/advert`, `/api/v1/adv*`, `/api/v1/advschedule` |
| `wimoor-ozon` | 15 | `/api/v1/auth`, `/api/v1/product`, `/api/v1/stock`, `/api/v1/price`, `/api/v1/posting`, `/api/v1/chat`, `/api/v1/task` |
| `wimoor-modules` | 30 | `/periods`, `/subjects`, `/vouchers`, `/ledger`, `/api/v1/quote`, `/api/v1/data`, `/api/code` |

## Representative Controller Families

| Service | Controllers |
| --- | --- |
| Admin | `LoginController`, `UserController`, `MenuController`, `RoleController`, `PermissionController`, `DictController`, `SysTaskController`, `SysQueryFieldController` |
| ERP | `MaterialController`, `PlanController`, `PurchaseFormController`, `InventoryManagerController`, `WarehouseController`, `ShipPlanController`, `ShipFormController`, `AlibabaController` |
| Amazon | `AmazonAuthorityController`, `ReportController`, `OrdersController`, `FeedController`, `ProductInfoController`, `AmzProductRefreshController`, `ShipFormController`, `AmzSettlementReportController` |
| Amazon Adv | `AdvertAdsController`, `AdvertCampaignManagerController`, `AdvertKeywordManagerController`, `AdvertReportController`, `AdvertInvoicesController`, `SchedulingConfigController` |
| Ozon | `OzonAuthController`, `OzonProductController`, `OzonStockController`, `OzonPriceController`, `OzonPostingController`, `OzonChatController`, `OzonTaskController` |
| Finance/Quote/Data/Gen | `FinVouchersController`, `FinAccountingSubjectsController`, `FinGeneralLedgerController`, `QuoteManagerController`, `DataController`, `CodeGenerateController` |

## Full Refresh Command

```powershell
$rows=foreach($f in rg --files -g "*.java" wimoor-admin wimoor-erp wimoor-amazon wimoor-amazon-adv wimoor-ozon wimoor-modules){
  $text=Get-Content -Raw $f
  if($text -match "@RestController"){
    $class=[IO.Path]::GetFileNameWithoutExtension($f)
    $map=""
    if($text -match "@RequestMapping\((?:value\s*=\s*)?`"([^`"]+)`"") { $map=$Matches[1] }
    [pscustomobject]@{Service=($f -split "\\")[0]; Class=$class; Mapping=$map; File=($f -replace "\\","/")}
  }
}
$rows | Sort-Object Service,Class | Format-Table -AutoSize
```


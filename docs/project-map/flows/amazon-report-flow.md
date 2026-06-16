# Amazon Report Flow

```mermaid
flowchart TB
  task["Quartz / manual trigger"]
  reportReq["ReportController requestReport"]
  spApi["Amazon SP-API report request"]
  requestDb["Report request tables"]
  refresh["refreshReport / processReport"]
  file["Report document download"]
  parser["Report parser by type"]
  domainDb["Orders / inventory / finance / product tables"]
  ui["wimoorui Amazon pages"]

  task --> reportReq
  ui --> reportReq
  reportReq --> spApi
  spApi --> requestDb
  task --> refresh
  refresh --> requestDb
  refresh --> file
  file --> parser
  parser --> domainDb
  domainDb --> ui
```

Related controller families include `ReportController`, `AmzProductRefreshController`, `OrdersController`, `FeedController`, `AmzFinAccountController`, inbound shipment controllers and settlement controllers.


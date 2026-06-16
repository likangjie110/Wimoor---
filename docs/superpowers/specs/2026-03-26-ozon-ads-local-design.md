# Ozon Ads Local Flow Design

**Goal:** Build a locally verifiable Ozon ads workflow that can be exercised without relying on the real Ozon Performance API.

**Scope:** This slice covers local JSON import of ads account, campaign, and daily report data; campaign browsing; report browsing; summary card aggregation; and raw row inspection. It does not create campaigns, change bids, or call the real Performance API.

## Design Summary

The ads slice follows the same bounded-context approach already used by Ozon auth, posting, error center, finance, and chat: Ozon-native ads data stays inside `db_ozon`, and the UI operates directly on Ozon-native campaign/report models. The import source is local JSON so the feature can be deployed and verified in the current WSL environment without external API credentials.

This slice still reserves a clean path for future real integration. The backend will keep ads logic behind Ozon-specific service boundaries, and later real Performance API pull can replace the local import path without rewriting the tables or page structure.

## Data Model

### `t_ozon_ads_account`

Stores one ads account summary per authorized Ozon store.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `account_id`
- `account_name`
- `status`
- `currency_code`
- `create_time`
- `update_time`

Rules:

- `auth_id + account_id` acts as the business key.

### `t_ozon_ads_campaign`

Stores campaign master data.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `account_id`
- `campaign_id`
- `campaign_name`
- `campaign_type`
- `campaign_status`
- `budget`
- `create_time`
- `update_time`

Rules:

- `auth_id + campaign_id` is the campaign idempotency key.

### `t_ozon_ads_report`

Stores daily campaign metrics.

Fields:

- `id`
- `auth_id`
- `shop_id`
- `account_id`
- `campaign_id`
- `report_date`
- `impressions`
- `clicks`
- `spend`
- `orders`
- `sales`
- `ctr`
- `cpc`
- `acos`
- `roas`
- `raw_line_json`
- `create_time`

Rules:

- `auth_id + campaign_id + report_date` is the report idempotency key.
- This slice saves provided metrics directly and does not re-derive all indicators server-side unless needed for a summary aggregate.

## Import Payload Shape

The local import endpoint accepts:

- `authId`
- `rawContent`

`rawContent` must support the following minimal shape:

```json
{
  "account": {
    "accountId": "acc-1",
    "accountName": "Ozon Ads Account",
    "status": "ACTIVE",
    "currencyCode": "RUB"
  },
  "campaigns": [
    {
      "campaignId": "camp-1",
      "campaignName": "Spring Campaign",
      "campaignType": "SEARCH_PROMO",
      "campaignStatus": "ACTIVE",
      "budget": 1000
    }
  ],
  "reports": [
    {
      "campaignId": "camp-1",
      "reportDate": "2026-03-26",
      "impressions": 1000,
      "clicks": 50,
      "spend": 120.5,
      "orders": 5,
      "sales": 800,
      "ctr": 5,
      "cpc": 2.41,
      "acos": 15.06,
      "roas": 6.64
    }
  ]
}
```

Import behavior:

- Invalid JSON fails the whole import.
- Missing `campaignId` fails the import.
- Missing `reportDate` fails report import.
- Account, campaigns, and reports are upserted.

## API Surface

The backend exposes:

- `POST /ozon/api/v1/ads/import`
- `GET /ozon/api/v1/ads/campaign/list`
- `GET /ozon/api/v1/ads/report/list`
- `GET /ozon/api/v1/ads/summary`

All endpoints continue to use `UserInfoContext` + `OzonAuthAccessService` for shop scoping.

## UI Structure

Page path: `/ozon/ads`

The page is split into three areas:

1. Import area
2. Campaign list area
3. Report + summary area

### Import Area

Inputs:

- auth selector
- raw JSON textarea
- import button

### Campaign List

Columns:

- campaign name
- campaign type
- campaign status
- budget

Filters:

- keyword

### Summary + Report Area

Summary cards:

- impressions
- clicks
- spend
- orders
- sales
- acos
- roas

Daily report table:

- report date
- impressions
- clicks
- spend
- orders
- sales
- ctr
- cpc
- acos
- roas

Raw row JSON must be viewable for a selected report record.

## Error Handling

- Empty import payload: reject
- Unauthorized auth scope: reject via existing auth access service
- Empty campaign list is allowed only if reports are also empty; otherwise reject inconsistent payload
- Summary for empty report set returns zero-like values or an empty summary object

## Future Real API Boundary

Future real integration should sit behind an Ozon-specific ads client such as:

- `com.wimoor.ozon.ads.client.OzonPerformanceApiClient`

This slice does not call the real API, but table design and service boundaries are chosen so the local import path can later be swapped with a real pull from `api-performance.ozon.ru`.

## Verification Strategy

Verification must include:

- targeted backend tests for ads import/list/summary
- frontend route/build verification
- local deployment verification in WSL using seeded auth data and a locally injected session token

## Out of Scope

- Real Ozon Performance API pull
- Campaign creation/update
- Bid optimization
- Ad group / keyword / creative hierarchy
- Multi-account orchestration

# Ozon Ads Local Flow Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a locally verifiable Ozon ads flow with JSON import, campaign browsing, daily report browsing, and summary card aggregation.

**Architecture:** Keep ads data fully inside `db_ozon` and expose Ozon-native endpoints for import, campaign list, report list, and summary. Reuse the existing Ozon auth scoping, controller wrappers, route patterns, and local deployment strategy already validated for finance and chat.

**Tech Stack:** Spring Boot 2.6, MyBatis-Plus, MySQL, Vue 3, Vite, Element Plus.

---

## File Structure Map

### Backend

- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/controller/OzonAdsController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/mapper/OzonAdsAccountMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/mapper/OzonAdsCampaignMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/mapper/OzonAdsReportMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/dto/OzonAdsImportCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/dto/OzonAdsReportQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/entity/OzonAdsAccount.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/entity/OzonAdsCampaign.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/entity/OzonAdsReport.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/vo/OzonAdsImportResult.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/pojo/vo/OzonAdsSummary.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/service/IOzonAdsService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/service/impl/OzonAdsServiceImpl.java`

### SQL

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_account.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_campaign.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_report.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`

### Frontend

- Create: `wimoorui/src/api/ozon/ads/adsApi.js`
- Create: `wimoorui/src/views/ozon/ads/index.vue`
- Modify: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/scripts/check_ozon_entry.mjs`

### Tests

- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/ads/OzonAdsReportTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/ads/OzonAdsServiceSpringBeanTests.java`

## Chunk 1: Backend Ads Flow

### Task 1: Implement ads import, list, and summary service

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/ads/OzonAdsReportTests.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/...`

- [ ] **Step 1: Write the failing ads behavior test**

```java
class OzonAdsReportTests {
    @Test
    void importCreatesCampaignsReportsAndSummary() {
        OzonAdsImportResult result = service.importAds(user, command);
        assertEquals(1, result.getCampaignCount());
        assertEquals(1, result.getReportCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonAdsReportTests test
```

Expected: compilation failure because ads package does not exist yet.

- [ ] **Step 3: Add DTOs, entities, and mappers**

Implement:

- `OzonAdsImportCommand`
- `OzonAdsReportQuery`
- `OzonAdsAccount`
- `OzonAdsCampaign`
- `OzonAdsReport`
- `OzonAdsImportResult`
- `OzonAdsSummary`
- mappers for the three tables

- [ ] **Step 4: Implement service**

Service must support:

- `importAds`
- `listCampaigns`
- `listReports`
- `summary`

Rules:

- use `OzonAuthAccessService`
- import fails on invalid JSON
- upsert account/campaign/report
- summary aggregates report rows by current filters
- do not reuse Amazon ads models

- [ ] **Step 5: Implement controller**

Add endpoints:

- `POST /ozon/api/v1/ads/import`
- `GET /ozon/api/v1/ads/campaign/list`
- `GET /ozon/api/v1/ads/report/list`
- `GET /ozon/api/v1/ads/summary`

- [ ] **Step 6: Run test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonAdsReportTests test
```

Expected: PASS

## Chunk 2: SQL and Frontend

### Task 2: Add ads tables and page

**Files:**
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_account.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_campaign.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_report.sql`
- Create: `wimoorui/src/api/ozon/ads/adsApi.js`
- Create: `wimoorui/src/views/ozon/ads/index.vue`
- Modify: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/scripts/check_ozon_entry.mjs`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`

- [ ] **Step 1: Write the failing route/build presence check**

Add expected route `ozon/ads` to the existing Ozon entry check and run it before implementing the page.

- [ ] **Step 2: Run the check to verify it fails**

Run:

```bash
cd wimoorui && node scripts/check_ozon_entry.mjs
```

Expected: FAIL because `ozon/ads` is missing.

- [ ] **Step 3: Add SQL files**

Tables should include the fields confirmed in the spec and business keys on:

- `auth_id + account_id`
- `auth_id + campaign_id`
- `auth_id + campaign_id + report_date`

- [ ] **Step 4: Add frontend API**

Implement:

- `importAds`
- `listCampaigns`
- `listReports`
- `getSummary`

- [ ] **Step 5: Add frontend page**

Page should contain:

- auth selector
- raw JSON textarea
- import button
- campaign list
- summary cards
- report table with raw row drawer

- [ ] **Step 6: Add route and menu seeds**

Add:

- `/ozon/ads`
- menu row
- permission rows

- [ ] **Step 7: Re-run the route check**

Run:

```bash
cd wimoorui && node scripts/check_ozon_entry.mjs
```

Expected: PASS

## Chunk 3: Verification

### Task 3: Verify backend, frontend, and local deployment

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/ads/OzonAdsServiceSpringBeanTests.java`

- [ ] **Step 1: Add spring bean construction test**

Create a minimal `ApplicationContextRunner` test to ensure `OzonAdsServiceImpl` can be instantiated by Spring without constructor ambiguity.

- [ ] **Step 2: Run backend ads tests**

Run:

```bash
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonAdsReportTests,OzonAdsServiceSpringBeanTests test
```

Expected: PASS

- [ ] **Step 3: Run frontend checks and build**

Run:

```bash
cd wimoorui && node scripts/check_linux_path_case.mjs
cd wimoorui && node scripts/check_ozon_entry.mjs
cd wimoorui && timeout 600s ./scripts/build_in_linux_fs.sh
```

Expected: PASS

- [ ] **Step 4: Local deployment verification**

Use the same local deployment strategy already validated for finance and chat:

- build `ozon-boot.jar`
- start with local overrides
- seed `db_ozon`
- seed Redis `login_tokens:<token>`
- call:
  - `GET /api/v1/auth/list`
  - `POST /api/v1/ads/import`
  - `GET /api/v1/ads/campaign/list`
  - `GET /api/v1/ads/report/list`
  - `GET /api/v1/ads/summary`

Expected:

- campaign list returns imported campaign
- report list returns imported daily report
- summary returns aggregated impressions/clicks/spend/orders/sales/acos/roas

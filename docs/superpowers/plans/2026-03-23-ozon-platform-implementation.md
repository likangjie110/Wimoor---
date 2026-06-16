# Ozon Platform Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-ready Ozon sales-channel integration for Wimoor ERP that covers auth, product/listing, price/stock, posting/order fulfillment, finance, chat, ads, and operational controls without destabilizing existing Amazon flows.

**Architecture:** Introduce a new `wimoor-ozon` microservice family and `db_ozon` schema, keep Ozon-native data in its own bounded context, and bridge only standardized business facts into ERP. Reuse existing gateway, Quartz, Nacos, Feign, and Vue routing conventions while extracting only a thin shared contract layer into `wimoor-common` and `wimoor-api`.

**Tech Stack:** Maven multi-module Java 8, Spring Boot 2.6, Spring Cloud Alibaba/Nacos/Feign, MyBatis-Plus, MySQL, Quartz, Vue 3 + Vite + Element Plus.

---

## Scope Split

This spec spans multiple large subsystems. To keep execution safe, this plan is organized as one master rollout with 4 independently shippable chunks:

- `Chunk 1`: platform skeleton, shared contracts, auth/security foundation
- `Chunk 2`: product, mapping, warehouse, price, stock
- `Chunk 3`: posting/order bridge, fulfillment, tasks, exception center
- `Chunk 4`: finance, chat, ads, frontend completion, rollout verification

Each chunk should be completed and reviewed before the next one begins.

## File Structure Map

### New backend modules

- Create: `wimoor-ozon/pom.xml`
- Create: `wimoor-ozon/ozon-api/pom.xml`
- Create: `wimoor-ozon/ozon-boot/pom.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/OzonApplication.java`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap.yml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap-dev.yml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap-prod.yml`

### Shared contracts and constants

- Modify: `pom.xml`
- Modify: `wimoor-api/pom.xml`
- Create: `wimoor-api/wimoor-api-ozon/pom.xml`
- Create: `wimoor-api/wimoor-api-ozon/src/main/java/com/wimoor/ozon/api/RemoteOzonService.java`
- Modify: `wimoor-common/common-core/src/main/java/com/wimoor/common/ServiceNameConstants.java`
- Create: `wimoor-common/common-core/src/main/java/com/wimoor/common/PlatformType.java`
- Create: `wimoor-common/common-core/src/main/java/com/wimoor/common/security/ChannelCredentialCipher.java`
- Create: `wimoor-common/common-core/src/test/java/com/wimoor/common/security/ChannelCredentialCipherTests.java`

### Ozon service backend packages

- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/price/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/stock/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/shipment/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/finance/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/...`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/...`

### ERP / gateway / admin bridge points

- Modify: `init-config/nacos/DEFAULT_GROUP/wimoor-gateway`
- Create: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`
- Modify: `wimoor-erp/erp-api/src/main/java/com/wimoor/erp/api/ErpClientOneFeign.java`
- Modify: `wimoor-admin/admin-boot/src/main/java/com/wimoor/sys/tool/controller/SysTaskController.java`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_quartz_task.sql`

### Database scripts

- Create: `init-config/mysql/数据库结构/db_ozon/undo_log.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_auth.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_shop_config.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_warehouse.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product_sku.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product_map.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_price_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_price_snapshot.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_stock_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_stock_snapshot.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_posting.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_posting_item.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_shipment.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_return.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_fin_transaction.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_report_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_report_file.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_session.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_message.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_campaign.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_report.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_sync_job.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_sync_cursor.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_api_log.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_error_event.sql`

### Frontend

- Create: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/src/router/index.js`
- Create: `wimoorui/src/api/ozon/auth/authApi.js`
- Create: `wimoorui/src/api/ozon/product/productApi.js`
- Create: `wimoorui/src/api/ozon/stock/stockApi.js`
- Create: `wimoorui/src/api/ozon/price/priceApi.js`
- Create: `wimoorui/src/api/ozon/posting/postingApi.js`
- Create: `wimoorui/src/api/ozon/finance/financeApi.js`
- Create: `wimoorui/src/api/ozon/chat/chatApi.js`
- Create: `wimoorui/src/api/ozon/ads/adsApi.js`
- Create: `wimoorui/src/api/ozon/task/taskApi.js`
- Create: `wimoorui/src/views/ozon/auth/index.vue`
- Create: `wimoorui/src/views/ozon/product/index.vue`
- Create: `wimoorui/src/views/ozon/stock/index.vue`
- Create: `wimoorui/src/views/ozon/price/index.vue`
- Create: `wimoorui/src/views/ozon/posting/index.vue`
- Create: `wimoorui/src/views/ozon/finance/index.vue`
- Create: `wimoorui/src/views/ozon/chat/index.vue`
- Create: `wimoorui/src/views/ozon/ads/index.vue`
- Create: `wimoorui/src/views/ozon/task/index.vue`

## Chunk 1: Foundation and Shared Contracts

### Task 1: Create the Ozon module skeleton and service registration

**Files:**
- Modify: `pom.xml`
- Create: `wimoor-ozon/pom.xml`
- Create: `wimoor-ozon/ozon-api/pom.xml`
- Create: `wimoor-ozon/ozon-boot/pom.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/OzonApplication.java`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap.yml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap-dev.yml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap-prod.yml`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonApplicationTests.java`
- Modify: `init-config/nacos/DEFAULT_GROUP/wimoor-gateway`
- Create: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`

- [ ] **Step 1: Write the failing smoke test**

```java
package com.wimoor.ozon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OzonApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonApplicationTests test
```

Expected: Maven fails because `wimoor-ozon` does not exist in the reactor yet.

- [ ] **Step 3: Add the new reactor modules**

Add `wimoor-ozon` to root `pom.xml`, then create:

```xml
<!-- /wimoor-ozon/pom.xml -->
<modules>
  <module>ozon-api</module>
  <module>ozon-boot</module>
</modules>
```

and a boot app:

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wimoor")
@MapperScan("com.wimoor.ozon.**.mapper")
public class OzonApplication {
    public static void main(String[] args) {
        SpringApplication.run(OzonApplication.class, args);
    }
}
```

- [ ] **Step 4: Wire gateway and nacos config**

Add `wimoor-ozon` route to `init-config/nacos/DEFAULT_GROUP/wimoor-gateway` and create `init-config/nacos/DEFAULT_GROUP/wimoor-ozon` with:

```properties
spring.application.name=wimoor-ozon
server.port=0
spring.datasource.jdbc-url=jdbc:mysql://${mysql.host}:${mysql.port}/db_ozon?allowMultiQueries=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
```

- [ ] **Step 5: Run test to verify the module boots**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonApplicationTests test
```

Expected: `OzonApplicationTests` passes and the Spring context starts.

- [ ] **Step 6: Commit**

```bash
git add pom.xml wimoor-ozon init-config/nacos/DEFAULT_GROUP/wimoor-gateway init-config/nacos/DEFAULT_GROUP/wimoor-ozon
git commit -m "feat: scaffold ozon service modules"
```

### Task 2: Add shared contracts, service constants, and credential crypto

**Files:**
- Modify: `wimoor-api/pom.xml`
- Create: `wimoor-api/wimoor-api-ozon/pom.xml`
- Create: `wimoor-api/wimoor-api-ozon/src/main/java/com/wimoor/ozon/api/RemoteOzonService.java`
- Modify: `wimoor-common/common-core/src/main/java/com/wimoor/common/ServiceNameConstants.java`
- Create: `wimoor-common/common-core/src/main/java/com/wimoor/common/PlatformType.java`
- Create: `wimoor-common/common-core/src/main/java/com/wimoor/common/security/ChannelCredentialCipher.java`
- Create: `wimoor-common/common-core/src/test/java/com/wimoor/common/security/ChannelCredentialCipherTests.java`

- [ ] **Step 1: Write the failing cipher test**

```java
class ChannelCredentialCipherTests {
    @Test
    void encryptAndDecryptRoundTrip() {
        ChannelCredentialCipher cipher = new ChannelCredentialCipher("0123456789abcdef");
        String ciphertext = cipher.encrypt("a89f7d00-xxxx");
        assertNotEquals("a89f7d00-xxxx", ciphertext);
        assertEquals("a89f7d00-xxxx", cipher.decrypt(ciphertext));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 60s mvn -pl wimoor-common/common-core -Dtest=ChannelCredentialCipherTests test
```

Expected: test fails because `ChannelCredentialCipher` does not exist yet.

- [ ] **Step 3: Implement the shared constants and cipher**

Add:

```java
public final class ServiceNameConstants {
    public static final String WIMOOR_OZON = "wimoor-ozon";
}
```

```java
public enum PlatformType {
    AMAZON,
    OZON
}
```

Implement `ChannelCredentialCipher` with explicit `encrypt`, `decrypt`, and `fingerprint` methods. Do not hardcode secrets; read the AES key from config or env-backed properties.

- [ ] **Step 4: Create the API module**

Create `wimoor-api/wimoor-api-ozon` mirroring `wimoor-api-amazon`, with a Feign interface shaped like:

```java
@FeignClient(value = ServiceNameConstants.WIMOOR_OZON)
public interface RemoteOzonService {
    @GetMapping("/ozon/api/v1/auth/ping")
    Result<?> ping(@RequestParam String authId);
}
```

- [ ] **Step 5: Run tests**

Run:

```bash
timeout 60s mvn -pl wimoor-common/common-core,wimoor-api/wimoor-api-ozon -am test -Dtest=ChannelCredentialCipherTests
```

Expected: common-core tests pass and `wimoor-api-ozon` compiles cleanly.

- [ ] **Step 6: Commit**

```bash
git add wimoor-api/pom.xml wimoor-api/wimoor-api-ozon wimoor-common/common-core/src/main/java wimoor-common/common-core/src/test/java
git commit -m "feat: add ozon shared contracts and credential cipher"
```

## Chunk 2: Auth, Seller, Warehouse, Product, Price, Stock

### Task 3: Implement auth, seller config, warehouse sync, and admin menus

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/controller/OzonAuthController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/service/IOzonAuthService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/service/impl/OzonAuthServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/mapper/OzonAuthMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/auth/OzonAuthMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/auth/pojo/entity/OzonAuth.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/seller/...`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_auth.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_shop_config.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_warehouse.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/auth/OzonAuthServiceTests.java`
- Create: `wimoorui/src/api/ozon/auth/authApi.js`
- Create: `wimoorui/src/views/ozon/auth/index.vue`

- [ ] **Step 1: Write the failing auth service test**

```java
class OzonAuthServiceTests {
    @Test
    void bindAuthEncryptsApiKeyAndCreatesWarehouseInitJob() {
        OzonAuth saved = service.bindAuth(user, new OzonAuthBindCommand("test-client-id", "test-key"));
        assertEquals("test-client-id", saved.getClientId());
        assertNotNull(saved.getApiKeyCiphertext());
        assertNull(saved.getApiKeyPlaintext());
        assertEquals(JobType.INIT_WAREHOUSE, syncJobMapper.selectLatest(saved.getId()).getJobType());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonAuthServiceTests test
```

Expected: missing auth entity/service/mapper and SQL tables.

- [ ] **Step 3: Implement the auth domain**

Build `OzonAuthController` endpoints:

```java
@PostMapping("/ozon/api/v1/auth/bind")
@GetMapping("/ozon/api/v1/auth/list")
@GetMapping("/ozon/api/v1/auth/ping")
@PostMapping("/ozon/api/v1/auth/disable")
@PostMapping("/ozon/api/v1/auth/rotateKey")
```

`bind` must:

- validate `Client ID + API Key`
- encrypt API key
- store only ciphertext + fingerprint
- create init jobs for seller/warehouse sync
- write audit log

- [ ] **Step 4: Add initial UI and menu seeds**

Create an Ozon auth page with:

- credential form
- connection test button
- auth list table
- last sync / status column
- rotate-key action

Seed menu and permission entries in `t_sys_menu.sql` and `t_sys_permission.sql`.

- [ ] **Step 5: Run tests and frontend build**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonAuthServiceTests test
cd wimoorui && npm run build
```

Expected: auth tests pass, frontend build succeeds.

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot init-config/mysql/数据库结构/db_ozon init-config/mysql/数据/db_admin/t_sys_menu.sql init-config/mysql/数据/db_admin/t_sys_permission.sql wimoorui/src/api/ozon/auth wimoorui/src/views/ozon/auth
git commit -m "feat: add ozon auth and seller setup"
```

### Task 4: Implement product mapping, warehouse sync, price sync, and stock sync

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/controller/OzonProductController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/price/controller/OzonPriceController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/stock/controller/OzonStockController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/...`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/...`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product_sku.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_product_map.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_price_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_price_snapshot.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_stock_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_stock_snapshot.sql`
- Modify: `wimoor-erp/erp-api/src/main/java/com/wimoor/erp/api/ErpClientOneFeign.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductMapServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/stock/OzonStockSyncServiceTests.java`
- Create: `wimoorui/src/api/ozon/product/productApi.js`
- Create: `wimoorui/src/api/ozon/price/priceApi.js`
- Create: `wimoorui/src/api/ozon/stock/stockApi.js`
- Create: `wimoorui/src/views/ozon/product/index.vue`
- Create: `wimoorui/src/views/ozon/price/index.vue`
- Create: `wimoorui/src/views/ozon/stock/index.vue`

- [ ] **Step 1: Write failing mapping and stock tests**

```java
class OzonProductMapServiceTests {
    @Test
    void saveMappingPersistsOfferAndErpSku() {
        ProductMap map = service.saveMapping(authId, "ERP-SKU-1", "offer-1", "ozonSku1");
        assertEquals("ERP-SKU-1", map.getErpSku());
        assertEquals("offer-1", map.getOzonOfferId());
    }
}
```

```java
class OzonStockSyncServiceTests {
    @Test
    void stockPushWritesTaskAndSnapshot() {
        StockPushResult result = service.push(authId, List.of(new StockUpdate("ERP-SKU-1", 11)));
        assertEquals(1, result.getAccepted());
        assertEquals(1, snapshotMapper.countByTaskId(result.getTaskId()));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonProductMapServiceTests,OzonStockSyncServiceTests test
```

Expected: missing product/stock services and schema.

- [ ] **Step 3: Implement product mapping and ERP material bridge**

Use `ErpClientOneFeign` to resolve ERP material data. Add new bridge methods if needed:

```java
@PostMapping("/erp/api/v1/material/getMaterialInfos")
Result<Map<String, Object>> findMaterialMapBySku(@RequestBody List<String> skus);
```

Build Ozon product endpoints:

- `/ozon/api/v1/product/list`
- `/ozon/api/v1/product/map/save`
- `/ozon/api/v1/product/importDraft`
- `/ozon/api/v1/product/syncStatus`

- [ ] **Step 4: Implement price/stock tasks and snapshots**

Build services that:

- translate ERP stock to Ozon warehouse stock
- translate ERP pricing rules to Ozon price payloads
- store every push in `t_ozon_price_task` / `t_ozon_stock_task`
- store current read-back values in snapshots
- emit diff events when platform values diverge

- [ ] **Step 5: Build product, price, and stock pages**

Pages must include:

- SKU mapping table
- product sync status
- batch stock push action
- batch price push action
- latest snapshot/diff drawer

- [ ] **Step 6: Run tests and frontend build**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductMapServiceTests,OzonStockSyncServiceTests test
cd wimoorui && npm run build
```

Expected: mapping and stock tests pass, UI builds.

- [ ] **Step 7: Commit**

```bash
git add wimoor-ozon/ozon-boot wimoor-erp/erp-api/src/main/java/com/wimoor/erp/api/ErpClientOneFeign.java init-config/mysql/数据库结构/db_ozon wimoorui/src/api/ozon/product wimoorui/src/api/ozon/price wimoorui/src/api/ozon/stock wimoorui/src/views/ozon/product wimoorui/src/views/ozon/price wimoorui/src/views/ozon/stock
git commit -m "feat: add ozon product mapping and stock price sync"
```

## Chunk 3: Posting, Fulfillment, Tasks, and Exception Center

### Task 5: Implement posting ingestion, ERP order normalization, and fulfillment callbacks

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/controller/OzonPostingController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/posting/service/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/shipment/controller/OzonShipmentController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/shipment/service/...`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_posting.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_posting_item.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_shipment.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_return.sql`
- Modify: `wimoor-erp/erp-api/src/main/java/com/wimoor/erp/api/ErpClientOneFeign.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/posting/OzonPostingSyncServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/shipment/OzonShipmentCallbackServiceTests.java`
- Create: `wimoorui/src/api/ozon/posting/postingApi.js`
- Create: `wimoorui/src/views/ozon/posting/index.vue`

- [ ] **Step 1: Write the failing posting sync test**

```java
class OzonPostingSyncServiceTests {
    @Test
    void importPostingCreatesRawPostingAndErpOrderFact() {
        PostingSyncResult result = service.syncIncremental(authId, cursor);
        assertTrue(result.getImported() > 0);
        assertNotNull(postingMapper.selectLatest(authId));
        assertNotNull(result.getErpOrderIds().get(0));
    }
}
```

- [ ] **Step 2: Write the failing shipment callback test**

```java
class OzonShipmentCallbackServiceTests {
    @Test
    void callbackPushesTrackingNumberAndPersistsAudit() {
        service.pushShipment(authId, postingNumber, "TRACK-1");
        assertEquals("TRACK-1", shipmentMapper.selectLatest(postingNumber).getTrackingNumber());
        assertEquals(1, apiLogMapper.countSuccessByObject("posting", postingNumber));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonPostingSyncServiceTests,OzonShipmentCallbackServiceTests test
```

Expected: missing posting/shipment implementations.

- [ ] **Step 4: Implement raw-posting and normalized-order pipeline**

Create a two-stage pipeline:

```java
PostingRaw raw = postingGateway.fetch(...);
postingRepository.save(raw);
ErpOrderCommand cmd = postingNormalizer.toErpOrder(raw);
erpBridge.upsertChannelOrder(cmd);
```

If `ErpClientOneFeign` lacks a suitable endpoint, add a dedicated Ozon channel bridge method rather than reusing unrelated shipment endpoints.

- [ ] **Step 5: Implement fulfillment callbacks**

Support:

- tracking number push
- shipment status push
- cancel/return sync
- object-level retry

Each callback must create:

- API log row
- audit row
- retryable error event on failure

- [ ] **Step 6: Build posting page**

The page must support:

- FBO/FBS filtering
- status filtering
- raw payload drawer
- ERP bridge status
- manual retry for a single posting

- [ ] **Step 7: Run tests and frontend build**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonPostingSyncServiceTests,OzonShipmentCallbackServiceTests test
cd wimoorui && npm run build
```

Expected: posting and shipment tests pass, UI builds.

- [ ] **Step 8: Commit**

```bash
git add wimoor-ozon/ozon-boot wimoor-erp/erp-api/src/main/java/com/wimoor/erp/api/ErpClientOneFeign.java init-config/mysql/数据库结构/db_ozon wimoorui/src/api/ozon/posting wimoorui/src/views/ozon/posting
git commit -m "feat: add ozon posting sync and shipment callbacks"
```

### Task 6: Implement Quartz tasks, cursor management, and exception center

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/controller/OzonTaskController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/service/...`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/error/controller/OzonErrorCenterController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/error/service/...`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_sync_job.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_sync_cursor.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_api_log.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_error_event.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_quartz_task.sql`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/task/OzonCursorResumeTests.java`
- Create: `wimoorui/src/api/ozon/task/taskApi.js`
- Create: `wimoorui/src/views/ozon/task/index.vue`

- [ ] **Step 1: Write the failing cursor-resume test**

```java
class OzonCursorResumeTests {
    @Test
    void interruptedJobResumesFromSavedCursor() {
        syncJobService.markRunning(jobId, "cursor-10");
        syncJobService.resume(jobId);
        assertEquals("cursor-10", gateway.getResumeCursor());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonCursorResumeTests test
```

Expected: cursor/task services do not exist yet.

- [ ] **Step 3: Implement task model and Quartz entries**

Create task endpoints for:

- auth init sync
- posting incremental sync
- stock reconcile
- price reconcile
- finance report pull
- chat pull
- ads pull
- object retry

Seed `t_sys_quartz_task.sql` entries so operations can manage Ozon jobs from existing task UI.

- [ ] **Step 4: Implement exception center**

Create error center endpoints:

- `/ozon/api/v1/error/list`
- `/ozon/api/v1/error/retryOne`
- `/ozon/api/v1/error/ignore`
- `/ozon/api/v1/error/replayWindow`

The retry path must operate at object granularity, not all-store granularity.

- [ ] **Step 5: Build task and exception pages**

Task page must show:

- last run time
- backlog
- average latency
- failure count
- shop filter
- job-type filter

Exception page must show:

- object type/id
- latest error
- retry count
- retry action
- raw response drawer

- [ ] **Step 6: Run tests and frontend build**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonCursorResumeTests test
cd wimoorui && npm run build
```

Expected: cursor/task tests pass, UI builds, seeded Quartz SQL remains syntactically valid.

- [ ] **Step 7: Commit**

```bash
git add wimoor-ozon/ozon-boot init-config/mysql/数据库结构/db_ozon init-config/mysql/数据/db_admin/t_sys_quartz_task.sql wimoorui/src/api/ozon/task wimoorui/src/views/ozon/task
git commit -m "feat: add ozon task orchestration and error center"
```

## Chunk 4: Finance, Chat, Ads, Frontend Completion, and Rollout

### Task 7: Implement finance reports, chat, and ads/performance

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/finance/controller/OzonFinanceController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/chat/controller/OzonChatController.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/ads/controller/OzonAdsController.java`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_fin_transaction.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_report_task.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_report_file.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_session.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_chat_message.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_campaign.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_ads_report.sql`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/finance/OzonFinanceImportTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/chat/OzonChatSyncTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/ads/OzonAdsReportTests.java`
- Create: `wimoorui/src/api/ozon/finance/financeApi.js`
- Create: `wimoorui/src/api/ozon/chat/chatApi.js`
- Create: `wimoorui/src/api/ozon/ads/adsApi.js`
- Create: `wimoorui/src/views/ozon/finance/index.vue`
- Create: `wimoorui/src/views/ozon/chat/index.vue`
- Create: `wimoorui/src/views/ozon/ads/index.vue`

- [ ] **Step 1: Write the failing finance, chat, and ads tests**

```java
class OzonFinanceImportTests {
    @Test
    void reportImportCreatesChannelTransactions() {
        service.importReport(authId, reportId);
        assertTrue(financeMapper.countByAuthId(authId) > 0);
    }
}
```

```java
class OzonChatSyncTests {
    @Test
    void syncCreatesSessionsAndMessages() {
        service.sync(authId);
        assertTrue(sessionMapper.countByAuthId(authId) > 0);
        assertTrue(messageMapper.countUnread(authId) >= 0);
    }
}
```

```java
class OzonAdsReportTests {
    @Test
    void reportPullPersistsCampaignMetrics() {
        service.pullDaily(authId, LocalDate.now().minusDays(1));
        assertTrue(reportMapper.countByAuthId(authId) > 0);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonFinanceImportTests,OzonChatSyncTests,OzonAdsReportTests test
```

Expected: finance/chat/ads layers are not present yet.

- [ ] **Step 3: Implement finance/report import**

Finance import must:

- keep raw channel transaction rows in `db_ozon`
- map standardized facts into ERP only after transformation
- support rerun by report id and date window

- [ ] **Step 4: Implement chat**

Chat flow must support:

- session list
- message list
- unread count
- send reply
- reply audit

Do not store raw credentials in message logs.

- [ ] **Step 5: Implement ads/performance**

Ads flow must support:

- campaign/account sync
- daily report pull
- metrics persistence
- backend summary endpoint for dashboard cards

Keep ads logic in Ozon-native structures; do not force-fit Amazon ad models.

- [ ] **Step 6: Build finance, chat, and ads pages**

Each page must show:

- shop filter
- date range
- sync status
- failure entry points
- raw/report detail drawer where appropriate

- [ ] **Step 7: Run tests and frontend build**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am -Dtest=OzonFinanceImportTests,OzonChatSyncTests,OzonAdsReportTests test
cd wimoorui && npm run build
```

Expected: all three backend test groups pass, frontend build succeeds.

- [ ] **Step 8: Commit**

```bash
git add wimoor-ozon/ozon-boot init-config/mysql/数据库结构/db_ozon wimoorui/src/api/ozon/finance wimoorui/src/api/ozon/chat wimoorui/src/api/ozon/ads wimoorui/src/views/ozon/finance wimoorui/src/views/ozon/chat wimoorui/src/views/ozon/ads
git commit -m "feat: add ozon finance chat and ads flows"
```

### Task 8: Complete frontend routing, rollout switches, and final verification

**Files:**
- Create: `wimoorui/src/router/modules/ozon.js`
- Modify: `wimoorui/src/router/index.js`
- Modify: `wimoorui/src/layout/components/Header/HeaderPlatform.vue`
- Modify: `init-config/mysql/数据/db_admin/t_sys_menu.sql`
- Modify: `init-config/mysql/数据/db_admin/t_sys_permission.sql`
- Modify: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonSmokeWorkflowTests.java`
- Modify: `docs/superpowers/specs/2026-03-23-ozon-design.md`

- [ ] **Step 1: Write the failing smoke workflow test**

```java
class OzonSmokeWorkflowTests {
    @Test
    void auth_product_stock_posting_finance_flow_is_wired() {
        assertTrue(applicationContext.containsBean("ozonAuthController"));
        assertTrue(applicationContext.containsBean("ozonProductController"));
        assertTrue(applicationContext.containsBean("ozonPostingController"));
        assertTrue(applicationContext.containsBean("ozonFinanceController"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -Dtest=OzonSmokeWorkflowTests test
```

Expected: fail until the full bean graph and route wiring are complete.

- [ ] **Step 3: Add Ozon routing and platform entry**

Create `wimoorui/src/router/modules/ozon.js`, import it in `wimoorui/src/router/index.js`, and add an Ozon entry in `HeaderPlatform.vue`. Keep it visually separate from Amazon and avoid hardcoding Ozon into Amazon page logic.

- [ ] **Step 4: Add rollout switches to nacos config**

In `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`, define feature switches such as:

```properties
ozon.feature.auth=true
ozon.feature.product=true
ozon.feature.stock.write=false
ozon.feature.price.write=false
ozon.feature.posting.write=false
ozon.feature.chat.send=false
ozon.feature.ads.sync=false
```

Start with read-heavy gray release defaults.

- [ ] **Step 5: Run end-to-end verification commands**

Run:

```bash
timeout 60s mvn -pl wimoor-ozon/ozon-boot -am test
cd wimoorui && npm run build
```

Expected:

- backend Ozon tests pass within 60 seconds per invocation batch
- frontend bundle builds
- no compile break in existing Amazon modules when running:

```bash
timeout 60s mvn -pl wimoor-amazon/amazon-boot,wimoor-amazon-adv/amazon-adv-boot -am -DskipTests package
```

- [ ] **Step 6: Update docs**

Amend the spec or add an implementation note describing:

- initial rollout switches
- gray-release order
- required Ozon secrets handling
- operator runbook entry points

- [ ] **Step 7: Commit**

```bash
git add wimoorui/src/router/modules/ozon.js wimoorui/src/router/index.js wimoorui/src/layout/components/Header/HeaderPlatform.vue init-config/mysql/数据/db_admin/t_sys_menu.sql init-config/mysql/数据/db_admin/t_sys_permission.sql init-config/nacos/DEFAULT_GROUP/wimoor-ozon docs/superpowers/specs/2026-03-23-ozon-design.md
git commit -m "feat: finalize ozon routing rollout and verification"
```

## Execution Notes

- Execute chunks in order. Do not start `Chunk 3` before `Chunk 2` is fully passing.
- Keep Ozon-native data in `db_ozon`; do not bypass this plan by writing raw Ozon payloads directly into ERP tables.
- Prefer adding Ozon-specific bridge endpoints over overloading existing Amazon or shipment endpoints with platform conditionals.
- When in doubt, duplicate a narrow Ozon entry point rather than expanding a generic abstraction too early.

## Verification Checklist

- [ ] `timeout 60s mvn -pl wimoor-ozon/ozon-boot -am test`
- [ ] `cd wimoorui && npm run build`
- [ ] `timeout 60s mvn -pl wimoor-amazon/amazon-boot,wimoor-amazon-adv/amazon-adv-boot -am -DskipTests package`
- [ ] Manual gray-release check with one Ozon shop using read-only feature flags first
- [ ] Manual credential-rotation test with masked logging verification

## Review Note

This environment exposes subagents, but the current session did not include explicit user permission for delegation. Review this plan locally before execution, then execute chunk by chunk.

# Ozon Product Publish Full Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first production-ready Ozon product publish workflow with listing drafts, category/type metadata, preview validation, real publish tasks, and the new workbench UI.

**Architecture:** Keep `t_ozon_product_map` as the SKU identity layer and introduce a separate listing draft domain inside `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product`. Backend owns all Ozon payload assembly, metadata normalization, and async task polling; frontend only edits draft state, requests preview, and triggers publish.

**Tech Stack:** Maven multi-module Java 8, Spring Boot 2.6, MyBatis-Plus, MySQL, Feign-style Ozon clients, Vue 3 + Element Plus + existing Vite build scripts.

---

## File Structure Map

### Database and mapper layer

- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_draft.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_variant.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_attribute.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_image.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_publish_task.sql`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingDraftMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingVariantMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingAttributeMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingImageMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingPublishTaskMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingDraftMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingVariantMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingAttributeMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingImageMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingPublishTaskMapper.xml`

### Product domain backend

- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/controller/OzonProductController.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftImportCommand.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductMapService.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductMapServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/client/OzonProductApiClient.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/client/DefaultOzonProductApiClient.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingDraft.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingVariant.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingAttribute.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingImage.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingPublishTask.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftSaveCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftListQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftDetailQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductCategoryTemplateQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPreviewCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPublishCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPublishTaskQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftListView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftDetailView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductCategoryTreeView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductCategoryTemplateView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPreviewView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublishView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublishTaskView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductMetadataService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPreviewService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPublishService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductMetadataServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPreviewServiceImpl.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPublishServiceImpl.java`

### Shared config and task/error integration

- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureProperties.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureGate.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/pojo/entity/OzonSyncJobType.java`
- Modify: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`

### Frontend product workbench

- Modify: `wimoorui/src/api/ozon/product/productApi.js`
- Modify: `wimoorui/src/views/ozon/product/index.vue`
- Create: `wimoorui/src/views/ozon/product/components/DraftSidebar.vue`
- Create: `wimoorui/src/views/ozon/product/components/DraftBaseForm.vue`
- Create: `wimoorui/src/views/ozon/product/components/CommonAttributePanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/CommonImagePanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/VariantMatrix.vue`
- Create: `wimoorui/src/views/ozon/product/components/PreviewPanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/PublishTaskPanel.vue`
- Create: `wimoorui/scripts/check_ozon_product_publish_entry.mjs`

### Tests

- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductMetadataServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPreviewServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPublishServiceTests.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductControllerFeatureTests.java`
- Modify: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonSmokeWorkflowTests.java`

## Chunk 1: Backend Product Draft Domain

### Task 1: Introduce listing draft schema, entities, and persistence layer

**Files:**
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_draft.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_variant.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_attribute.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_image.sql`
- Create: `init-config/mysql/数据库结构/db_ozon/t_ozon_listing_publish_task.sql`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftSaveCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftDetailView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingDraft.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingVariant.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingAttribute.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingImage.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/entity/OzonListingPublishTask.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingDraftMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingVariantMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingAttributeMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingImageMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/mapper/OzonListingPublishTaskMapper.java`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingDraftMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingVariantMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingAttributeMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingImageMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingPublishTaskMapper.xml`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java`

- [ ] **Step 1: Write the failing persistence/service test**

```java
@Test
void saveDraftReplacesNestedCollectionsAndPreservesSourceSnapshots() {
    OzonProductDraftSaveCommand command = Fixtures.draftSaveCommand();
    Fixtures.seedExistingDraftWithTwoVariants(draftMapper, variantMapper, attributeMapper, imageMapper);

    OzonProductDraftDetailView saved = service.saveDraft(buildUser(), command);

    assertEquals("draft-1", saved.getDraftId());
    assertEquals(1, saved.getCommonAttributes().size());
    assertEquals(1, saved.getVariants().size());
    assertEquals("ERP-SKU-1", saved.getVariants().get(0).getMaterialSku());
    assertEquals("ERP title", saved.getTitleSourceValue());
    assertEquals("Override title", saved.getTitleOverrideValue());
    assertFalse(saved.getVariants().stream().anyMatch(item -> "ERP-SKU-OLD".equals(item.getMaterialSku())));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: FAIL because draft entities, mappers, and service do not exist yet.

- [ ] **Step 3: Add schema and persistence objects**

Create SQL files and Java entities/mappers for the exact fields defined in the spec:

```sql
CREATE TABLE t_ozon_listing_draft (
  id bigint unsigned primary key,
  auth_id bigint unsigned not null,
  shop_id bigint unsigned not null,
  draft_name varchar(128) not null,
  description_category_id bigint unsigned,
  description_category_name varchar(255),
  type_id bigint unsigned,
  type_name varchar(255),
  title_source_value varchar(255),
  title_override_value varchar(255),
  brand_source_value varchar(255),
  brand_override_value varchar(255),
  description_source_value text,
  description_override_value text,
  status varchar(20) not null,
  last_preview_status varchar(20),
  last_preview_message varchar(255),
  last_publish_task_id bigint unsigned,
  create_time datetime,
  update_time datetime,
  key idx_auth_status (auth_id, status)
);
```

Repeat the same explicit-column treatment for:

- `t_ozon_listing_variant`

```sql
CREATE TABLE t_ozon_listing_variant (
  id bigint unsigned primary key,
  draft_id bigint unsigned not null,
  auth_id bigint unsigned not null,
  shop_id bigint unsigned not null,
  material_sku varchar(64) not null,
  material_name varchar(255),
  offer_id_override varchar(128),
  barcode_override varchar(128),
  price_source_value decimal(18,2),
  price_override decimal(18,2),
  weight_source_value decimal(18,3),
  weight_override_value decimal(18,3),
  length_source_value decimal(18,2),
  length_override_value decimal(18,2),
  width_source_value decimal(18,2),
  width_override_value decimal(18,2),
  height_source_value decimal(18,2),
  height_override_value decimal(18,2),
  variant_label varchar(128),
  status varchar(20) not null,
  last_sync_status varchar(20),
  last_sync_message varchar(255),
  create_time datetime,
  update_time datetime,
  unique key uk_draft_material_sku (draft_id, material_sku),
  key idx_auth_draft (auth_id, draft_id)
);
```

- `t_ozon_listing_attribute`

```sql
CREATE TABLE t_ozon_listing_attribute (
  id bigint unsigned primary key,
  draft_id bigint unsigned not null,
  variant_id bigint unsigned null,
  auth_id bigint unsigned not null,
  shop_id bigint unsigned not null,
  scope varchar(20) not null,
  attribute_id bigint unsigned not null,
  attribute_name varchar(255),
  attribute_value_json text not null,
  required_flag tinyint(1) not null default 0,
  create_time datetime,
  update_time datetime,
  key idx_draft_scope (draft_id, scope),
  key idx_variant_scope (variant_id, scope)
);
```

- `t_ozon_listing_image`

```sql
CREATE TABLE t_ozon_listing_image (
  id bigint unsigned primary key,
  draft_id bigint unsigned not null,
  variant_id bigint unsigned null,
  auth_id bigint unsigned not null,
  shop_id bigint unsigned not null,
  scope varchar(20) not null,
  source varchar(20) not null,
  image_url varchar(1024) not null,
  sort_order int not null default 0,
  is_primary tinyint(1) not null default 0,
  create_time datetime,
  update_time datetime,
  key idx_draft_scope_sort (draft_id, scope, sort_order),
  key idx_variant_scope_sort (variant_id, scope, sort_order)
);
```

- `t_ozon_listing_publish_task`

```sql
CREATE TABLE t_ozon_listing_publish_task (
  id bigint unsigned primary key,
  draft_id bigint unsigned not null,
  auth_id bigint unsigned not null,
  shop_id bigint unsigned not null,
  task_status varchar(20) not null,
  remote_task_id bigint unsigned null,
  request_payload_json longtext,
  response_payload_json longtext,
  error_message varchar(1024),
  operator varchar(64),
  create_time datetime,
  update_time datetime,
  key idx_draft_status (draft_id, task_status),
  key idx_remote_task (remote_task_id)
);
```

- [ ] **Step 4: Implement minimal draft persistence service**

Create `OzonListingDraftServiceImpl` with full-replace semantics inside one draft:

```java
public OzonProductDraftDetailView saveDraft(UserInfo user, OzonProductDraftSaveCommand command) {
    OzonAuth auth = authAccessService.requireOwnedAuth(user, command.getAuthId());
    validateDraft(command);
    // upsert draft header
    // replace common attributes/images
    // replace variants and nested attributes/images
    // compute draft status READY vs DRAFT
    return loadDetail(auth.getId(), draftId);
}
```

`validateDraft(command)` must explicitly reject:

- duplicate `materialSku` in `variants[]`
- variant without `materialSku`
- malformed nested attribute/image payload

`descriptionCategoryId` and `typeId` are not save blockers. Missing values must still persist the draft and leave it in `DRAFT`.

Concrete persistence order:

```java
@Transactional
saveDraft(...) {
  upsertDraftHeader();
  replaceCommonAttributes();
  replaceCommonImages();
  loadExistingVariantsByDraftId();
  deleteRemovedVariantChildren();
  upsertVariants();
  replaceVariantAttributes();
  replaceVariantImages();
  recalculateVariantStatuses();
  recalculateDraftStatus();
  return loadDetail(...);
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add init-config/mysql/数据库结构/db_ozon/t_ozon_listing_*.sql wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java
git commit -m "ozon: 新增刊登草稿持久化模型"
```

### Task 2: Add Product API metadata client and normalized template service

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/client/OzonProductApiClient.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/client/DefaultOzonProductApiClient.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductCategoryTreeView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductCategoryTemplateView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductMetadataService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductMetadataServiceImpl.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductMetadataServiceTests.java`

- [ ] **Step 1: Write the failing metadata normalization test**

```java
@Test
void categoryTemplateSplitsAspectAttributesIntoVariantAndCommonGroups() {
    when(productApiClient.listAttributes("cid", "key", 200001483L, 971445087L))
        .thenReturn(Fixtures.attributePayload());
    when(productApiClient.listCategoryTree("cid", "key"))
        .thenReturn(Fixtures.categoryTreePayload());

    OzonProductCategoryTemplateView template = service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L);
    OzonProductCategoryTreeView tree = service.getCategoryTree(buildUser(), "auth-1", null);

    assertEquals(1, template.getVariantAttributes().size());
    assertEquals(1, template.getCommonAttributes().size());
    assertEquals(3, template.getRequiredImageCount());
    assertTrue(template.isRequiresBarcode());
    assertFalse(tree.getCategories().isEmpty());
    assertEquals(Long.valueOf(971445087L), tree.getCategories().get(0).getTypes().get(0).getTypeId());
}

@Test
void templateCacheUsesStaleValueWhenRemoteFetchFails() {
    when(productApiClient.listAttributes("cid", "key", 200001483L, 971445087L))
        .thenReturn(Fixtures.attributePayload())
        .thenThrow(new IllegalStateException("remote fail"));

    service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L);
    OzonProductCategoryTemplateView stale = service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L);

    assertEquals(1, stale.getCommonAttributes().size());
}

@Test
void templateMissWithoutStaleCacheFails() {
    when(productApiClient.listAttributes("cid", "key", 200001483L, 971445087L))
        .thenThrow(new IllegalStateException("remote fail"));

    assertThrows(IllegalStateException.class,
        () -> service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L));
}

@Test
void categoryTreeSupportsKeywordAndCacheExpiry() {
    fakeClock.set("2026-03-27T10:00:00Z");
    when(productApiClient.listCategoryTree("cid", "key"))
        .thenReturn(Fixtures.categoryTreePayload());
    when(productApiClient.listAttributes("cid", "key", 200001483L, 971445087L))
        .thenReturn(Fixtures.attributePayload());

    OzonProductCategoryTreeView filtered = service.getCategoryTree(buildUser(), "auth-1", "Book");
    service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L);
    fakeClock.set("2026-03-27T16:01:00Z");
    service.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L);

    assertFalse(filtered.getCategories().isEmpty());
    verify(productApiClient, times(2)).listAttributes("cid", "key", 200001483L, 971445087L);
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductMetadataServiceTests test
```

Expected: FAIL because the metadata client/service does not exist yet.

- [ ] **Step 3: Implement the product metadata client**

Expose the verified endpoints:

```java
List<OzonCategoryNode> listCategoryTree(String clientId, String apiKey);
List<OzonAttributeTemplateItem> listAttributes(String clientId, String apiKey, Long descriptionCategoryId, Long typeId);
```

Use:

```java
POST /v1/description-category/tree
POST /v1/description-category/attribute
```

- [ ] **Step 4: Implement normalized template service with cache**

`OzonProductMetadataServiceImpl` must:

```java
// cache key: authId + descriptionCategoryId + typeId
// variantAttributes = is_aspect == true
// commonAttributes = is_aspect == false
// ttl = 6 hours
// on miss: synchronous remote fetch
// on remote failure: return stale cache if present, else raise error
```

Expected outputs:

- tree nodes normalized to `descriptionCategoryId / descriptionCategoryName / types[]`
- template normalized to `descriptionCategoryId / descriptionCategoryName / typeId / typeName / commonAttributes[] / variantAttributes[] / requiredImageCount / requiresBarcode`
- template attribute DTOs normalized to `mode = TEXT | DICT | MULTI_TEXT | MULTI_DICT` plus structured `values[]`
- 6-hour TTL cache
- synchronous fetch on miss
- stale-cache fallback on remote metadata failure
- `category/tree` supports optional keyword filtering

- [ ] **Step 5: Run the test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductMetadataServiceTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/client wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductMetadataService.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductMetadataServiceImpl.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductCategory*.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductMetadataServiceTests.java
git commit -m "ozon: 新增商品类目与属性模板服务"
```

### Task 3: Implement additive `importDraft` and source snapshot refresh

**Files:**
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftImportCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftImportResult.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java`

- [ ] **Step 1: Extend the failing test for `importDraft` additive semantics**

```java
@Test
void importDraftUpdatesSourceSnapshotsAndReturnsSkippedSkus() {
    OzonProductDraftImportCommand command = new OzonProductDraftImportCommand("auth-1", "draft-1", "Books", Arrays.asList("ERP-SKU-1", "MISSING"));

    OzonProductDraftImportResult result = service.importDraft(buildUser(), command);

    assertEquals("draft-1", result.getDraftId());
    assertEquals(1, result.getImportedCount());
    assertEquals(1, result.getCreatedVariantCount() + result.getUpdatedVariantCount());
    assertEquals(Collections.singletonList("MISSING"), result.getSkippedSkus());
    assertEquals("ERP title v2", draftMapper.selectById("draft-1").getTitleSourceValue());
    assertNotNull(variantMapper.selectByDraftIdAndMaterialSku("draft-1", "ERP-SKU-OLD"));
}

@Test
void importDraftCreatesNewDraftWhenDraftIdIsMissingAndRejectsEmptySkus() {
    assertThrows(IllegalArgumentException.class,
        () -> service.importDraft(buildUser(), new OzonProductDraftImportCommand("auth-1", null, "Books", Collections.emptyList())));

    OzonProductDraftImportResult created = service.importDraft(
        buildUser(),
        new OzonProductDraftImportCommand("auth-1", null, "Books", Arrays.asList("ERP-SKU-1"))
    );

    assertNotNull(created.getDraftId());
}

@Test
void importDraftFailsWhenErpLookupFails() {
    when(erpClient.findMaterialMapBySku(any())).thenThrow(new IllegalStateException("erp down"));

    assertThrows(IllegalStateException.class,
        () -> service.importDraft(buildUser(), new OzonProductDraftImportCommand("auth-1", "draft-1", "Books", Arrays.asList("ERP-SKU-1"))));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: FAIL because `importDraft` still uses the old mapping-only behavior.

- [ ] **Step 3: Implement additive `importDraft` against ERP snapshots**

Use `ErpClientOneFeign.findMaterialMapBySku` and persist source snapshot fields only:

```java
// title_source_value <- name
// price_source_value <- price
// group image source snapshot <- image
// brand/description/dimensions remain null until operator fills them
```

Bootstrap rule:

- when `draftId` is absent, create a new draft first
- `draftName` uses the incoming value when present, otherwise falls back to the first imported SKU

This task must not move draft list/detail into `IOzonProductMapService`; all draft behavior stays under `IOzonListingDraftService`.


- [ ] **Step 4: Implement additive/update snapshot semantics only**

Implement:

```java
// same draftId + materialSku refreshes source snapshot fields
// omitted existing variants remain untouched
// missing ERP rows append into skippedSkus[]
```

- [ ] **Step 5: Run the tests to verify they pass**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftImportCommand.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftImportResult.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java
git commit -m "ozon: 完善刊登草稿导入与源快照刷新"
```

### Task 4: Implement draft list/detail queries

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftListQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftDetailQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftListView.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftDetailView.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingDraftMapper.xml`
- Modify: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingVariantMapper.xml`
- Modify: `wimoor-ozon/ozon-boot/src/main/resources/mapper/ozon/product/OzonListingPublishTaskMapper.xml`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java`

- [ ] **Step 1: Extend the failing test for list/detail contracts**

```java
@Test
void listDraftsAndDetailExposeWorkbenchFields() {
    Fixtures.seedPublishTask(publishTaskMapper, "draft-1", "SUCCESS");
    List<OzonProductDraftListView> drafts = service.listDrafts(buildUser(), new OzonProductDraftListQuery("auth-1", null, null));
    OzonProductDraftDetailView detail = service.getDraftDetail(buildUser(), new OzonProductDraftDetailQuery("auth-1", "draft-1"));

    assertEquals(1, drafts.get(0).getVariantCount());
    assertNotNull(drafts.get(0).getLastPublishAt());
    assertNotNull(detail.getLatestPublishTaskSummary());
    assertNotNull(detail.getLatestPreviewStatus());
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: FAIL because list/detail DTOs and service methods are missing.

- [ ] **Step 3: Implement list/detail under `IOzonListingDraftService`**

Expected outputs:

- draft list rows include `draftId / draftName / descriptionCategoryName / typeName / status / variantCount / lastPublishAt`
- draft detail includes common attributes/images, variant rows, preview badge fields, and latest publish task summary

- [ ] **Step 4: Run the test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests test
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftListQuery.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductDraftDetailQuery.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftListView.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductDraftDetailView.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonListingDraftService.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonListingDraftServiceImpl.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java
git commit -m "ozon: 增加刊登草稿列表与详情"
```

### Task 5: Implement preview assembly and local validation

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPreviewCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPreviewView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPreviewService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPreviewServiceImpl.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPreviewServiceTests.java`

- [ ] **Step 1: Write the failing preview validation test**

```java
@Test
void previewFlagsMissingDimensionsAndRequiredAttributes() {
    OzonProductPreviewView preview = service.preview(buildUser(), new OzonProductPreviewCommand("auth-1", "draft-1"));

    assertFalse(preview.isCanPublish());
    assertTrue(preview.getValidationErrors().stream().anyMatch(msg -> msg.contains("weight")));
    assertFalse(preview.getVariantIssues().isEmpty());
    assertNotNull(preview.getEffectivePayloadSummary());
    assertFalse(preview.getEffectivePayloadSummary().getVariants().isEmpty());
}

@Test
void previewFailsWhenMetadataUnavailableAndNoStaleCacheExists() {
    when(metadataService.getTemplate(buildUser(), "auth-1", 200001483L, 971445087L))
        .thenThrow(new IllegalStateException("template unavailable"));

    assertThrows(IllegalStateException.class,
        () -> service.preview(buildUser(), new OzonProductPreviewCommand("auth-1", "draft-1")));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductPreviewServiceTests test
```

Expected: FAIL because preview service does not exist.

- [ ] **Step 3: Implement payload assembly**

Build one normalized preview payload per variant:

```java
effectiveOfferId -> offer_id
titleOverride/source -> name
descriptionCategoryId -> description_category_id
typeId -> type_id
effectiveBarcode -> barcode
effectivePrice -> price
effectiveWeight -> weight
effectiveLength -> depth
effectiveWidth -> width
effectiveHeight -> height
effectiveImages -> images[]
brand/description/category-specific values -> attributes[]
```

Precedence rules to implement explicitly:

- `effectiveOfferId`: `offerIdOverride` -> existing `ozon_offer_id` from `t_ozon_product_map` -> `materialSku`
- effective images: operator variant images -> effective group images -> ERP group image snapshot

- [ ] **Step 4: Implement local validation rules**

Validate:

- auth / draft / type presence
- `descriptionCategoryId`
- ERP-SKU binding per variant
- `effectiveOfferId`
- dimensions and weight
- price
- required common/variant attributes
- metadata-driven `requiresBarcode`
- metadata-driven `requiredImageCount`
- effective image set
- metadata unavailable with no stale cache
- persist `last_preview_status` / `last_preview_message` on the draft after every preview call

- [ ] **Step 5: Run the test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductPreviewServiceTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPreviewCommand.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPreviewView.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPreviewService.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPreviewServiceImpl.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPreviewServiceTests.java
git commit -m "ozon: 新增刊登预览与本地校验"
```

## Chunk 2: Publish Flow, HTTP Surface, and Workbench UI

### Task 5: Implement real publish client, polling, and partial-result writeback

**Files:**
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPublishCommand.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPublishTaskQuery.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublishView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublishTaskView.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPublishService.java`
- Create: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPublishServiceImpl.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/pojo/entity/OzonSyncJobType.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPublishServiceTests.java`

- [ ] **Step 1: Write the failing publish test**

```java
@Test
void publishMarksTaskPartialAndWritesBackSuccessfulVariantsOnly() {
    OzonProductPublishView result = service.publish(buildUser(), new OzonProductPublishCommand("auth-1", "draft-1"));

    assertEquals("PARTIAL", result.getTaskStatus());
    verify(productMapMapper).updatePublishResult("ERP-SKU-1", "BOOK-001", "3911142260");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductPublishServiceTests test
```

Expected: FAIL because publish service does not exist.

- [ ] **Step 3: Implement real publish and polling**

Use the verified Product API contract:

```java
POST /v3/product/import
POST /v1/product/import/info
```

Persist `remote_task_id`, poll every 2 seconds up to 30 seconds, and normalize:

```java
SUCCESS | FAILED | PARTIAL | RUNNING
```

- [ ] **Step 4: Implement partial-result writeback and product error events**

For `PARTIAL`:

- write back `offer_id` + `product_id` for successful variants only
- mark failed variants `FAILED`
- open `OzonErrorEvent` with source `PRODUCT`

- [ ] **Step 5: Run the test to verify it passes**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductPublishServiceTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/dto/OzonProductPublish*.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/pojo/vo/OzonProductPublish*.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/IOzonProductPublishService.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/service/impl/OzonProductPublishServiceImpl.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/task/pojo/entity/OzonSyncJobType.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPublishServiceTests.java
git commit -m "ozon: 新增商品真实发布与任务轮询"
```

### Task 6: Extend feature gates and controller endpoints

**Files:**
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureProperties.java`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureGate.java`
- Modify: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`
- Modify: `wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/controller/OzonProductController.java`
- Create: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductControllerFeatureTests.java`
- Modify: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonSmokeWorkflowTests.java`

- [ ] **Step 1: Write the failing controller feature-gate test**

```java
@Test
void publishEndpointRejectsWhenProductWriteFeatureDisabled() {
    Result<?> result = controller.publish(new OzonProductPublishCommand("auth-1", "draft-1"));
    assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
    assertEquals("Ozon商品发布写操作未开启", result.getMsg());
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductControllerFeatureTests test
```

Expected: FAIL because `product.write` gate and new endpoints do not exist.

- [ ] **Step 3: Add the product write gate and HTTP endpoints**

Add:

```java
private boolean productWrite = false;
public void assertProductWriteEnabled() { ... }
```

and expose:

- `POST /api/v1/product/importDraft` -> `IOzonListingDraftService#importDraft` returning `OzonProductDraftImportResult`
- `GET /api/v1/product/draft/list`
- `POST /api/v1/product/draft/save`
- `GET /api/v1/product/draft/detail`
- `GET /api/v1/product/category/tree`
- `GET /api/v1/product/category/template`
- `POST /api/v1/product/preview`
- `POST /api/v1/product/publish`
- `GET /api/v1/product/publish/task/detail`

- [ ] **Step 4: Update smoke coverage**

Extend `OzonSmokeWorkflowTests` to assert the product controller still wires cleanly with the new services.

- [ ] **Step 5: Run tests to verify they pass**

Run:

```bash
timeout 300s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test
```

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureProperties.java wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/config/OzonFeatureGate.java init-config/nacos/DEFAULT_GROUP/wimoor-ozon wimoor-ozon/ozon-boot/src/main/java/com/wimoor/ozon/product/controller/OzonProductController.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductControllerFeatureTests.java wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonSmokeWorkflowTests.java
git commit -m "ozon: 接入商品发布接口与开关"
```

### Task 7: Build the workbench UI and frontend API surface

**Files:**
- Modify: `wimoorui/src/api/ozon/product/productApi.js`
- Modify: `wimoorui/src/views/ozon/product/index.vue`
- Create: `wimoorui/src/views/ozon/product/components/DraftSidebar.vue`
- Create: `wimoorui/src/views/ozon/product/components/DraftBaseForm.vue`
- Create: `wimoorui/src/views/ozon/product/components/CommonAttributePanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/CommonImagePanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/VariantMatrix.vue`
- Create: `wimoorui/src/views/ozon/product/components/PreviewPanel.vue`
- Create: `wimoorui/src/views/ozon/product/components/PublishTaskPanel.vue`
- Create: `wimoorui/scripts/check_ozon_product_publish_entry.mjs`

- [ ] **Step 1: Write the failing frontend structure check**

Create `wimoorui/scripts/check_ozon_product_publish_entry.mjs` with assertions for:

```js
// productApi exports listDrafts/saveDraft/detail/preview/publish/publishTaskDetail
// product workbench imports all 6 child components
```

- [ ] **Step 2: Run the script to verify it fails**

Run:

```bash
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
```

Expected: FAIL because the new API methods and components do not exist yet.

- [ ] **Step 3: Implement frontend API calls**

Expose:

```js
request.get("/ozon/api/v1/product/draft/list", ...)
request.post("/ozon/api/v1/product/draft/save", ...)
request.get("/ozon/api/v1/product/draft/detail", ...)
request.get("/ozon/api/v1/product/category/tree", ...)
request.get("/ozon/api/v1/product/category/template", ...)
request.post("/ozon/api/v1/product/preview", ...)
request.post("/ozon/api/v1/product/publish", ...)
request.get("/ozon/api/v1/product/publish/task/detail", ...)
```

- [ ] **Step 4: Replace the current simple mapping page with the workbench layout**

Compose the page with:

- `DraftSidebar`
- `DraftBaseForm`
- `CommonAttributePanel`
- `CommonImagePanel`
- `VariantMatrix`
- `PreviewPanel`
- `PublishTaskPanel`

and keep the approved A-layout structure.

- [ ] **Step 5: Run the script and frontend build**

Run:

```bash
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
cd wimoorui && timeout 600s ./scripts/build_in_linux_fs.sh
```

Expected: script PASS, frontend build PASS

- [ ] **Step 6: Commit**

```bash
git add wimoorui/src/api/ozon/product/productApi.js wimoorui/src/views/ozon/product wimoorui/scripts/check_ozon_product_publish_entry.mjs
git commit -m "ozon: 搭建商品刊登工作台"
```

### Task 8: Run final verification and local deployment checks

**Files:**
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonListingDraftServiceTests.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductMetadataServiceTests.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPreviewServiceTests.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductPublishServiceTests.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/product/OzonProductControllerFeatureTests.java`
- Test: `wimoor-ozon/ozon-boot/src/test/java/com/wimoor/ozon/OzonSmokeWorkflowTests.java`

- [ ] **Step 1: Run the focused backend suite**

Run:

```bash
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests,OzonProductMetadataServiceTests,OzonProductPreviewServiceTests,OzonProductPublishServiceTests,OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test
```

Expected: PASS, zero failures.

- [ ] **Step 2: Run Amazon regression package commands**

Run:

```bash
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon/amazon-boot -am -DskipTests package
timeout 600s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon-adv/amazon-adv-boot -am -DskipTests package
```

Expected: both builds PASS.

- [ ] **Step 3: Run frontend verification**

Run:

```bash
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
cd wimoorui && timeout 600s ./scripts/build_in_linux_fs.sh
```

Expected: script PASS, frontend build PASS.

- [ ] **Step 4: Perform local deployment smoke**

Start `wimoor-ozon` with local overrides, then verify:

- draft list endpoint returns data
- category tree endpoint returns metadata
- preview returns validation results
- publish creates a local task and returns `localTaskId`

Document the exact local command/output in the execution thread before claiming completion.

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "ozon: 完成商品真实刊登发布链路"
```

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-03-27-ozon-product-publish-full-implementation.md`. Ready to execute?

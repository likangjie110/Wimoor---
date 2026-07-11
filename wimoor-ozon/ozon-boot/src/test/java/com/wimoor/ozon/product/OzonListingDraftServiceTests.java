package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftDetailQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftImportCommand;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftListQuery;
import com.wimoor.ozon.product.pojo.dto.OzonProductDraftSaveCommand;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;
import com.wimoor.ozon.product.pojo.entity.OzonListingPublishTask;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftDetailView;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftImportResult;
import com.wimoor.ozon.product.pojo.vo.OzonProductDraftListView;
import com.wimoor.ozon.product.service.impl.OzonListingDraftServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OzonListingDraftServiceTests {

    private static final String AUTH_ID = "auth-1";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonListingDraftMapper draftMapper;

    @Mock
    private OzonListingVariantMapper variantMapper;

    @Mock
    private OzonListingAttributeMapper attributeMapper;

    @Mock
    private OzonListingImageMapper imageMapper;

    @Mock
    private OzonListingPublishTaskMapper publishTaskMapper;

    @Mock
    private ErpClientOneFeign erpClientOneFeign;

    private OzonListingDraftServiceImpl service;
    private DraftStore store;

    @BeforeEach
    void setUp() {
        service = new OzonListingDraftServiceImpl(authMapper, draftMapper, variantMapper, attributeMapper, imageMapper, publishTaskMapper, erpClientOneFeign);
        when(authMapper.selectById(AUTH_ID)).thenReturn(Fixtures.auth());
        store = new DraftStore();
        Fixtures.bindDraftMapper(draftMapper, store);
        Fixtures.bindVariantMapper(variantMapper, store);
        Fixtures.bindAttributeMapper(attributeMapper, store);
        Fixtures.bindImageMapper(imageMapper, store);
        Fixtures.bindPublishTaskMapper(publishTaskMapper, store);
    }

    @Test
    void saveDraftReplacesNestedCollectionsAndPreservesSourceSnapshots() {
        OzonProductDraftSaveCommand command = Fixtures.draftSaveCommand();
        Fixtures.seedExistingDraftWithTwoVariants(store);

        OzonProductDraftDetailView saved = service.saveDraft(buildUser(), command);

        assertEquals("draft-1", saved.getDraftId());
        assertEquals(1, saved.getCommonAttributes().size());
        assertEquals(1, saved.getVariants().size());
        assertEquals("ERP-SKU-1", saved.getVariants().get(0).getMaterialSku());
        assertEquals("ERP title", saved.getTitleSourceValue());
        assertEquals("Override title", saved.getTitleOverrideValue());
        assertFalse(saved.getVariants().stream().anyMatch(item -> "ERP-SKU-OLD".equals(item.getMaterialSku())));
    }

    @Test
    void importDraftUpdatesSourceSnapshotsAndReturnsSkippedSkus() {
        Fixtures.seedExistingDraftWithTwoVariants(store);
        store.variants.get("variant-old-1").setMaterialSku("ERP-SKU-1");
        when(erpClientOneFeign.findMaterialMapBySku(any())).thenReturn(Result.success(Fixtures.materialMap()));

        OzonProductDraftImportResult result = service.importDraft(
                buildUser(),
                new OzonProductDraftImportCommand(AUTH_ID, "draft-1", "Books", java.util.Arrays.asList("ERP-SKU-1", "MISSING"))
        );

        assertEquals("draft-1", result.getDraftId());
        assertEquals(Integer.valueOf(1), result.getImportedCount());
        assertEquals(Integer.valueOf(1), result.getUpdatedVariantCount());
        assertEquals(java.util.Collections.singletonList("MISSING"), result.getSkippedSkus());
        assertEquals("ERP title v2", store.drafts.get("draft-1").getTitleSourceValue());
        assertNotNull(findVariantBySku("ERP-SKU-OLD-2"));
    }

    @Test
    void importDraftCreatesNewDraftWhenDraftIdMissingAndRejectsEmptySkus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.importDraft(buildUser(), new OzonProductDraftImportCommand(AUTH_ID, null, "Books", java.util.Collections.emptyList())));

        when(erpClientOneFeign.findMaterialMapBySku(any())).thenReturn(Result.success(Fixtures.materialMapSingle()));
        OzonProductDraftImportResult created = service.importDraft(
                buildUser(),
                new OzonProductDraftImportCommand(AUTH_ID, null, "Books", java.util.Collections.singletonList("ERP-SKU-1"))
        );

        assertNotNull(created.getDraftId());
    }

    @Test
    void importDraftFailsWhenErpLookupFails() {
        when(erpClientOneFeign.findMaterialMapBySku(any())).thenThrow(new IllegalStateException("erp down"));

        assertThrows(IllegalStateException.class,
                () -> service.importDraft(buildUser(), new OzonProductDraftImportCommand(AUTH_ID, "draft-1", "Books", java.util.Collections.singletonList("ERP-SKU-1"))));
    }

    @Test
    void listDraftsAndDetailExposeWorkbenchFields() {
        Fixtures.seedExistingDraftWithOneVariant(store);
        Fixtures.seedPublishTask(store, "draft-1", "SUCCESS");

        List<OzonProductDraftListView> drafts = service.listDrafts(buildUser(), new OzonProductDraftListQuery(AUTH_ID, null, null));
        OzonProductDraftDetailView detail = service.getDraftDetail(buildUser(), new OzonProductDraftDetailQuery(AUTH_ID, "draft-1"));

        assertEquals(1, drafts.get(0).getVariantCount());
        assertNotNull(drafts.get(0).getLastPublishAt());
        assertEquals(Long.valueOf(17028994L), drafts.get(0).getDescriptionCategoryId());
        assertEquals(Long.valueOf(970801724L), drafts.get(0).getTypeId());
        assertNotNull(detail.getLastPublishTaskId());
        assertNotNull(detail.getLastPreviewStatus());
    }

    private OzonListingVariant findVariantBySku(String sku) {
        return store.variants.values().stream().filter(item -> sku.equals(item.getMaterialSku())).findFirst().orElse(null);
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }

    private static final class Fixtures {

        private Fixtures() {
        }

        private static OzonAuth auth() {
            OzonAuth auth = new OzonAuth();
            auth.setId(AUTH_ID);
            auth.setShopId("company-1");
            return auth;
        }

        private static OzonProductDraftSaveCommand draftSaveCommand() {
            OzonProductDraftSaveCommand command = new OzonProductDraftSaveCommand();
            command.setAuthId(AUTH_ID);
            command.setDraftId("draft-1");
            command.setDraftName("Test draft");
            command.setTitleOverrideValue("Override title");
            command.setBrandOverrideValue("Brand override");
            command.setDescriptionOverrideValue("Override description");
            command.setCommonAttributes(new ArrayList<>());
            command.getCommonAttributes().add(attribute(1001L, "Color", "Red"));
            command.setCommonImages(new ArrayList<>());
            command.getCommonImages().add(image("MAIN", "https://img.test/common-1.png", 0, true));
            command.setVariants(new ArrayList<>());
            command.getVariants().add(variant());
            return command;
        }

        private static OzonProductDraftSaveCommand.AttributeItem attribute(Long attributeId, String name, String value) {
            OzonProductDraftSaveCommand.AttributeItem item = new OzonProductDraftSaveCommand.AttributeItem();
            item.setAttributeId(attributeId);
            item.setAttributeName(name);
            item.setMode("TEXT");
            OzonProductDraftSaveCommand.AttributeValue row = new OzonProductDraftSaveCommand.AttributeValue();
            row.setText(value);
            item.setValues(java.util.Collections.singletonList(row));
            return item;
        }

        private static OzonProductDraftSaveCommand.ImageItem image(
                String source,
                String imageUrl,
                Integer sortOrder,
                Boolean primary
        ) {
            OzonProductDraftSaveCommand.ImageItem item = new OzonProductDraftSaveCommand.ImageItem();
            item.setSource(source);
            item.setImageUrl(imageUrl);
            item.setSortOrder(sortOrder);
            item.setPrimary(primary);
            return item;
        }

        private static OzonProductDraftSaveCommand.VariantItem variant() {
            OzonProductDraftSaveCommand.VariantItem item = new OzonProductDraftSaveCommand.VariantItem();
            item.setMaterialSku("ERP-SKU-1");
            item.setMaterialName("ERP item 1");
            item.setPriceOverride(new BigDecimal("13.50"));
            item.setWeightOverrideValue(new BigDecimal("0.900"));
            item.setVariantLabel("Variant A");
            item.setAttributes(new ArrayList<>());
            item.getAttributes().add(attribute(2001L, "Size", "M"));
            item.setImages(new ArrayList<>());
            item.getImages().add(image("VARIANT", "https://img.test/variant-1.png", 0, true));
            return item;
        }

        private static void seedExistingDraftWithTwoVariants(DraftStore store) {
            Date now = new Date();
            OzonListingDraft draft = new OzonListingDraft();
            draft.setId("draft-1");
            draft.setAuthId(AUTH_ID);
            draft.setShopId("company-1");
            draft.setDraftName("Old draft");
            draft.setDescriptionCategoryId(17028994L);
            draft.setDescriptionCategoryName("Коллекционирование");
            draft.setTypeId(970801724L);
            draft.setTypeName("Коллекционные минералы и кристаллы");
            draft.setTitleSourceValue("ERP title");
            draft.setTitleOverrideValue("Old override");
            draft.setStatus("DRAFT");
            draft.setCreateTime(now);
            draft.setUpdateTime(now);
            store.drafts.put(draft.getId(), draft);

            OzonListingVariant oldOne = new OzonListingVariant();
            oldOne.setId("variant-old-1");
            oldOne.setDraftId("draft-1");
            oldOne.setAuthId(AUTH_ID);
            oldOne.setShopId("company-1");
            oldOne.setMaterialSku("ERP-SKU-OLD");
            oldOne.setStatus("DRAFT");
            oldOne.setCreateTime(now);
            oldOne.setUpdateTime(now);
            store.variants.put(oldOne.getId(), oldOne);

            OzonListingVariant oldTwo = new OzonListingVariant();
            oldTwo.setId("variant-old-2");
            oldTwo.setDraftId("draft-1");
            oldTwo.setAuthId(AUTH_ID);
            oldTwo.setShopId("company-1");
            oldTwo.setMaterialSku("ERP-SKU-OLD-2");
            oldTwo.setStatus("DRAFT");
            oldTwo.setCreateTime(now);
            oldTwo.setUpdateTime(now);
            store.variants.put(oldTwo.getId(), oldTwo);

            store.attributes.put("attr-common-old", entityAttribute("attr-common-old", "draft-1", null, "COMMON", 88L, "Old", "[\"Old\"]"));
            store.attributes.put("attr-variant-old", entityAttribute("attr-variant-old", "draft-1", "variant-old-1", "VARIANT", 99L, "OldVariant", "[\"Legacy\"]"));
            store.images.put("img-common-old", entityImage("img-common-old", "draft-1", null, "COMMON", "MAIN", "https://img.test/old-common.png"));
            store.images.put("img-variant-old", entityImage("img-variant-old", "draft-1", "variant-old-2", "VARIANT", "VARIANT", "https://img.test/old-variant.png"));
        }

        private static void seedExistingDraftWithOneVariant(DraftStore store) {
            seedExistingDraftWithTwoVariants(store);
            store.variants.remove("variant-old-2");
        }

        private static void seedPublishTask(DraftStore store, String draftId, String status) {
            OzonListingPublishTask task = new OzonListingPublishTask();
            task.setId("task-1");
            task.setDraftId(draftId);
            task.setAuthId(AUTH_ID);
            task.setShopId("company-1");
            task.setTaskStatus(status);
            task.setCreateTime(new Date());
            task.setUpdateTime(new Date());
            store.tasks.put(task.getId(), task);
            store.drafts.get(draftId).setLastPublishTaskId(task.getId());
            store.drafts.get(draftId).setLastPreviewStatus("READY");
        }

        private static Map<String, Object> materialMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ERP-SKU-1", material("ERP-SKU-1", "ERP title v2", "https://img.test/source-1.png", "12.30"));
            return result;
        }

        private static Map<String, Object> materialMapSingle() {
            return materialMap();
        }

        private static Map<String, Object> material(String sku, String name, String image, String price) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("msku", sku);
            row.put("name", name);
            row.put("ownername", "owner");
            row.put("image", image);
            row.put("price", price);
            return row;
        }

        private static OzonListingAttribute entityAttribute(
                String id,
                String draftId,
                String variantId,
                String scope,
                Long attributeId,
                String name,
                String valueJson
        ) {
            OzonListingAttribute item = new OzonListingAttribute();
            item.setId(id);
            item.setDraftId(draftId);
            item.setVariantId(variantId);
            item.setAuthId(AUTH_ID);
            item.setShopId("company-1");
            item.setScope(scope);
            item.setAttributeId(attributeId);
            item.setAttributeName(name);
            item.setAttributeValueJson(valueJson);
            item.setRequiredFlag(Boolean.TRUE);
            return item;
        }

        private static OzonListingImage entityImage(
                String id,
                String draftId,
                String variantId,
                String scope,
                String source,
                String imageUrl
        ) {
            OzonListingImage item = new OzonListingImage();
            item.setId(id);
            item.setDraftId(draftId);
            item.setVariantId(variantId);
            item.setAuthId(AUTH_ID);
            item.setShopId("company-1");
            item.setScope(scope);
            item.setSource(source);
            item.setImageUrl(imageUrl);
            item.setSortOrder(0);
            item.setPrimary(Boolean.TRUE);
            return item;
        }

        private static void bindDraftMapper(OzonListingDraftMapper draftMapper, DraftStore store) {
            when(draftMapper.selectByAuthIdAndDraftId(eq(AUTH_ID), any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(1);
                return store.drafts.get(draftId);
            });
            when(draftMapper.selectList(any(QueryWrapper.class))).thenAnswer(invocation -> new ArrayList<>(store.drafts.values()));
            lenient().when(draftMapper.insert(any(OzonListingDraft.class))).thenAnswer(invocation -> {
                OzonListingDraft entity = invocation.getArgument(0);
                store.drafts.put(entity.getId(), entity);
                return 1;
            });
            when(draftMapper.updateById(any(OzonListingDraft.class))).thenAnswer(invocation -> {
                OzonListingDraft entity = invocation.getArgument(0);
                store.drafts.put(entity.getId(), entity);
                return 1;
            });
        }

        private static void bindVariantMapper(OzonListingVariantMapper variantMapper, DraftStore store) {
            when(variantMapper.listByDraftId(any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(0);
                return store.variants.values().stream()
                        .filter(item -> draftId.equals(item.getDraftId()))
                        .sorted(Comparator.comparing(OzonListingVariant::getId))
                        .collect(Collectors.toList());
            });
            when(variantMapper.insert(any(OzonListingVariant.class))).thenAnswer(invocation -> {
                OzonListingVariant entity = invocation.getArgument(0);
                store.variants.put(entity.getId(), entity);
                return 1;
            });
            lenient().when(variantMapper.updateById(any(OzonListingVariant.class))).thenAnswer(invocation -> {
                OzonListingVariant entity = invocation.getArgument(0);
                store.variants.put(entity.getId(), entity);
                return 1;
            });
            when(variantMapper.deleteById(any(Serializable.class))).thenAnswer(invocation -> {
                Object id = invocation.getArgument(0);
                store.variants.remove(String.valueOf(id));
                return 1;
            });
        }

        private static void bindAttributeMapper(OzonListingAttributeMapper attributeMapper, DraftStore store) {
            when(attributeMapper.listByDraftIdAndVariantId(any(), any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(0);
                String variantId = invocation.getArgument(1);
                return store.attributes.values().stream()
                        .filter(item -> draftId.equals(item.getDraftId()))
                        .filter(item -> variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId()))
                        .sorted(Comparator.comparing(OzonListingAttribute::getId))
                        .collect(Collectors.toList());
            });
            when(attributeMapper.deleteByDraftIdAndVariantId(any(), any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(0);
                String variantId = invocation.getArgument(1);
                List<String> ids = store.attributes.values().stream()
                        .filter(item -> draftId.equals(item.getDraftId()))
                        .filter(item -> variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId()))
                        .map(OzonListingAttribute::getId)
                        .collect(Collectors.toList());
                ids.forEach(store.attributes::remove);
                return ids.size();
            });
            when(attributeMapper.insert(any(OzonListingAttribute.class))).thenAnswer(invocation -> {
                OzonListingAttribute entity = invocation.getArgument(0);
                store.attributes.put(entity.getId(), entity);
                return 1;
            });
        }

        private static void bindImageMapper(OzonListingImageMapper imageMapper, DraftStore store) {
            when(imageMapper.listByDraftIdAndVariantId(any(), any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(0);
                String variantId = invocation.getArgument(1);
                return store.images.values().stream()
                        .filter(item -> draftId.equals(item.getDraftId()))
                        .filter(item -> variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId()))
                        .sorted(Comparator.comparing(OzonListingImage::getId))
                        .collect(Collectors.toList());
            });
            when(imageMapper.deleteByDraftIdAndVariantId(any(), any())).thenAnswer(invocation -> {
                String draftId = invocation.getArgument(0);
                String variantId = invocation.getArgument(1);
                List<String> ids = store.images.values().stream()
                        .filter(item -> draftId.equals(item.getDraftId()))
                        .filter(item -> variantId == null ? item.getVariantId() == null : variantId.equals(item.getVariantId()))
                        .map(OzonListingImage::getId)
                        .collect(Collectors.toList());
                ids.forEach(store.images::remove);
                return ids.size();
            });
            when(imageMapper.insert(any(OzonListingImage.class))).thenAnswer(invocation -> {
                OzonListingImage entity = invocation.getArgument(0);
                store.images.put(entity.getId(), entity);
                return 1;
            });
        }

        private static void bindPublishTaskMapper(OzonListingPublishTaskMapper publishTaskMapper, DraftStore store) {
            when(publishTaskMapper.selectById(any())).thenAnswer(invocation -> store.tasks.get(String.valueOf(invocation.getArgument(0))));
            when(publishTaskMapper.selectOne(any(QueryWrapper.class))).thenAnswer(invocation ->
                    store.tasks.values().stream().reduce((first, second) -> second).orElse(null));
        }
    }

    private static final class DraftStore {

        private final Map<String, OzonListingDraft> drafts = new LinkedHashMap<>();
        private final Map<String, OzonListingVariant> variants = new LinkedHashMap<>();
        private final Map<String, OzonListingAttribute> attributes = new LinkedHashMap<>();
        private final Map<String, OzonListingImage> images = new LinkedHashMap<>();
        private final Map<String, OzonListingPublishTask> tasks = new LinkedHashMap<>();
    }
}

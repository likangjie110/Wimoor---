package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.product.client.OzonProductApiClient;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView;
import com.wimoor.ozon.product.service.impl.OzonProductMetadataServiceImpl;
import com.wimoor.ozon.security.OzonCredentialService;

@ExtendWith(MockitoExtension.class)
class OzonProductMetadataServiceTests {

    private static final String AUTH_ID = "auth-1";
    private static final Long CATEGORY_ID = 200001483L;
    private static final Long TYPE_ID = 971445087L;
    private static final String LANGUAGE_ZH = "ZH_HANS";
    private static final String LANGUAGE_EN = "EN";

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonProductApiClient productApiClient;

    private MutableClock fakeClock;
    private OzonProductMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        fakeClock = new MutableClock("2026-03-27T10:00:00Z");
        OzonCredentialService credentialService = new OzonCredentialService("0123456789abcdef");
        service = new OzonProductMetadataServiceImpl(authMapper, productApiClient, credentialService, fakeClock);
        when(authMapper.selectById(AUTH_ID)).thenReturn(Fixtures.auth(credentialService));
    }

    @Test
    void categoryTemplateSplitsAspectAttributesIntoVariantAndCommonGroups() {
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_ZH))
                .thenReturn(Fixtures.attributePayload("品牌", "作者", "Acme", "条码", "图片"));
        when(productApiClient.listCategoryTree("cid", "key", LANGUAGE_ZH))
                .thenReturn(Fixtures.categoryTreePayload("图书", "纸质书"));

        OzonProductCategoryTemplateView template = service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_ZH);
        OzonProductCategoryTreeView tree = service.getCategoryTree(buildUser(), AUTH_ID, null, LANGUAGE_ZH);

        assertEquals(1, template.getVariantAttributes().size());
        assertEquals(1, template.getCommonAttributes().size());
        assertEquals("DICT", template.getCommonAttributes().get(0).getMode());
        assertEquals("MULTI_TEXT", template.getVariantAttributes().get(0).getMode());
        assertEquals(3, template.getRequiredImageCount());
        assertTrue(template.isRequiresBarcode());
        assertFalse(tree.getCategories().isEmpty());
        assertEquals("图书", tree.getCategories().get(0).getDescriptionCategoryName());
        assertEquals(TYPE_ID, tree.getCategories().get(0).getTypes().get(0).getTypeId());
    }

    @Test
    void templateCacheUsesStaleValueWhenRemoteFetchFails() {
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_ZH))
                .thenReturn(Fixtures.attributePayload("品牌", "作者", "Acme", "条码", "图片"))
                .thenThrow(new IllegalStateException("remote fail"));

        service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_ZH);
        fakeClock.set("2026-03-27T16:01:00Z");

        OzonProductCategoryTemplateView stale = service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_ZH);

        assertEquals(1, stale.getCommonAttributes().size());
    }

    @Test
    void templateMissWithoutStaleCacheFails() {
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_ZH))
                .thenThrow(new IllegalStateException("remote fail"));

        assertThrows(IllegalStateException.class,
                () -> service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_ZH));
    }

    @Test
    void categoryTreeSupportsKeywordAndCacheExpiry() {
        when(productApiClient.listCategoryTree("cid", "key", LANGUAGE_EN))
                .thenReturn(Fixtures.categoryTreePayload("Books", "Printed Book"));
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_EN))
                .thenReturn(Fixtures.attributePayload("Brand", "Author", "Acme", "Barcode", "Images"));

        OzonProductCategoryTreeView filtered = service.getCategoryTree(buildUser(), AUTH_ID, "Book", LANGUAGE_EN);
        service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_EN);
        fakeClock.set("2026-03-27T16:01:00Z");
        service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_EN);

        assertEquals(1, filtered.getCategories().size());
        assertEquals("Books", filtered.getCategories().get(0).getDescriptionCategoryName());
        verify(productApiClient, times(2)).listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_EN);
    }

    @Test
    void categoryTreePrefersInheritedLocalizedCategoryNameForTypeLeafNodes() {
        when(productApiClient.listCategoryTree("cid", "key", LANGUAGE_ZH))
                .thenReturn(Fixtures.categoryTreePayloadWithLocalizedParentAndRawLeaf("图书", "Книги", "纸质书"));

        OzonProductCategoryTreeView tree = service.getCategoryTree(buildUser(), AUTH_ID, null, LANGUAGE_ZH);

        assertEquals(1, tree.getCategories().size());
        assertEquals("图书", tree.getCategories().get(0).getDescriptionCategoryName());
        assertEquals("纸质书", tree.getCategories().get(0).getTypes().get(0).getTypeName());
    }

    @Test
    void categoryTreeAndTemplateCachesAreScopedByLanguage() {
        when(productApiClient.listCategoryTree("cid", "key", LANGUAGE_ZH))
                .thenReturn(Fixtures.categoryTreePayload("图书", "纸质书"));
        when(productApiClient.listCategoryTree("cid", "key", LANGUAGE_EN))
                .thenReturn(Fixtures.categoryTreePayload("Books", "Printed Book"));
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_ZH))
                .thenReturn(Fixtures.attributePayload("品牌", "作者", "阿克米", "条码", "图片"));
        when(productApiClient.listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_EN))
                .thenReturn(Fixtures.attributePayload("Brand", "Author", "Acme", "Barcode", "Images"));

        OzonProductCategoryTreeView zhTree = service.getCategoryTree(buildUser(), AUTH_ID, null, LANGUAGE_ZH);
        OzonProductCategoryTreeView enTree = service.getCategoryTree(buildUser(), AUTH_ID, null, LANGUAGE_EN);
        OzonProductCategoryTemplateView zhTemplate = service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_ZH);
        OzonProductCategoryTemplateView enTemplate = service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_EN);
        service.getCategoryTree(buildUser(), AUTH_ID, null, LANGUAGE_ZH);
        service.getTemplate(buildUser(), AUTH_ID, CATEGORY_ID, TYPE_ID, LANGUAGE_EN);

        assertEquals("图书", zhTree.getCategories().get(0).getDescriptionCategoryName());
        assertEquals("Books", enTree.getCategories().get(0).getDescriptionCategoryName());
        assertEquals("品牌", zhTemplate.getCommonAttributes().get(0).getAttributeName());
        assertEquals("Brand", enTemplate.getCommonAttributes().get(0).getAttributeName());
        verify(productApiClient, times(1)).listCategoryTree("cid", "key", LANGUAGE_ZH);
        verify(productApiClient, times(1)).listCategoryTree("cid", "key", LANGUAGE_EN);
        verify(productApiClient, times(1)).listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_ZH);
        verify(productApiClient, times(1)).listAttributes("cid", "key", CATEGORY_ID, TYPE_ID, LANGUAGE_EN);
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

        private static OzonAuth auth(OzonCredentialService credentialService) {
            OzonAuth auth = new OzonAuth();
            auth.setId(AUTH_ID);
            auth.setShopId("company-1");
            auth.setClientId("cid");
            auth.setApiKeyCiphertext(credentialService.encrypt("key"));
            return auth;
        }

        private static List<OzonProductApiClient.AttributeTemplateItem> attributePayload(
                String brandName,
                String authorName,
                String dictionaryText,
                String barcodeName,
                String imageName
        ) {
            return Arrays.asList(
                    OzonProductApiClient.AttributeTemplateItem.builder()
                            .id(85L)
                            .name(brandName)
                            .type("String")
                            .isAspect(Boolean.FALSE)
                            .isCollection(Boolean.FALSE)
                            .isRequired(Boolean.TRUE)
                            .dictionaryId(77L)
                            .dictionaryValues(Collections.singletonList(
                                    OzonProductApiClient.DictionaryValue.builder()
                                            .dictionaryValueId(123456L)
                                            .text(dictionaryText)
                                            .build()
                            ))
                            .build(),
                    OzonProductApiClient.AttributeTemplateItem.builder()
                            .id(4182L)
                            .name(authorName)
                            .type("String")
                            .isAspect(Boolean.TRUE)
                            .isCollection(Boolean.TRUE)
                            .isRequired(Boolean.TRUE)
                            .sampleTexts(Arrays.asList("Author A", "Author B"))
                            .build(),
                    OzonProductApiClient.AttributeTemplateItem.builder()
                            .id(99901L)
                            .name(barcodeName)
                            .type("String")
                            .isAspect(Boolean.FALSE)
                            .isCollection(Boolean.FALSE)
                            .isRequired(Boolean.TRUE)
                            .build(),
                    OzonProductApiClient.AttributeTemplateItem.builder()
                            .id(99902L)
                            .name(imageName)
                            .type("Image")
                            .isAspect(Boolean.FALSE)
                            .isCollection(Boolean.TRUE)
                            .isRequired(Boolean.TRUE)
                            .maxValueCount(3)
                            .build()
            );
        }

        private static List<OzonProductApiClient.CategoryNode> categoryTreePayload(String categoryName, String typeName) {
            return Collections.singletonList(
                    OzonProductApiClient.CategoryNode.builder()
                            .descriptionCategoryId(CATEGORY_ID)
                            .categoryName(categoryName)
                            .children(Collections.singletonList(
                                    OzonProductApiClient.CategoryNode.builder()
                                            .descriptionCategoryId(CATEGORY_ID)
                                            .categoryName(categoryName)
                                            .typeId(TYPE_ID)
                                            .typeName(typeName)
                                            .children(Collections.emptyList())
                                            .build()
                            ))
                            .build()
            );
        }

        private static List<OzonProductApiClient.CategoryNode> categoryTreePayloadWithLocalizedParentAndRawLeaf(
                String parentCategoryName,
                String leafCategoryName,
                String typeName
        ) {
            return Collections.singletonList(
                    OzonProductApiClient.CategoryNode.builder()
                            .descriptionCategoryId(CATEGORY_ID)
                            .categoryName(parentCategoryName)
                            .children(Collections.singletonList(
                                    OzonProductApiClient.CategoryNode.builder()
                                            .descriptionCategoryId(CATEGORY_ID)
                                            .categoryName(leafCategoryName)
                                            .typeId(TYPE_ID)
                                            .typeName(typeName)
                                            .children(Collections.emptyList())
                                            .build()
                            ))
                            .build()
            );
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(String instant) {
            this.instant = Instant.parse(instant);
        }

        private void set(String nextInstant) {
            this.instant = Instant.parse(nextInstant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

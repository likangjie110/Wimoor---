package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.pojo.dto.OzonProductPreviewCommand;
import com.wimoor.ozon.product.pojo.entity.OzonListingAttribute;
import com.wimoor.ozon.product.pojo.entity.OzonListingDraft;
import com.wimoor.ozon.product.pojo.entity.OzonListingImage;
import com.wimoor.ozon.product.pojo.entity.OzonListingVariant;
import com.wimoor.ozon.product.pojo.entity.OzonProductMap;
import com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTemplateView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPreviewView;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.impl.OzonProductPreviewServiceImpl;

@ExtendWith(MockitoExtension.class)
class OzonProductPreviewServiceTests {

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
    private OzonProductMapMapper productMapMapper;

    @Mock
    private IOzonProductMetadataService metadataService;

    @Captor
    private ArgumentCaptor<OzonListingDraft> draftCaptor;

    private OzonProductPreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonProductPreviewServiceImpl(
                authMapper,
                draftMapper,
                variantMapper,
                attributeMapper,
                imageMapper,
                productMapMapper,
                metadataService
        );
        when(authMapper.selectById("auth-1")).thenReturn(Fixtures.auth());
        when(draftMapper.selectByAuthIdAndDraftId("auth-1", "draft-1")).thenReturn(Fixtures.draft());
        when(variantMapper.listByDraftId("draft-1")).thenReturn(Collections.singletonList(Fixtures.variantMissingDimensions()));
        when(attributeMapper.listByDraftIdAndVariantId("draft-1", null)).thenReturn(Collections.emptyList());
        when(attributeMapper.listByDraftIdAndVariantId("draft-1", "variant-1")).thenReturn(Collections.emptyList());
        when(imageMapper.listByDraftIdAndVariantId("draft-1", null)).thenReturn(Collections.singletonList(Fixtures.commonImage()));
        when(imageMapper.listByDraftIdAndVariantId("draft-1", "variant-1")).thenReturn(Collections.emptyList());
        when(productMapMapper.listByMaterialSkus("auth-1", Collections.singletonList("ERP-SKU-1")))
                .thenReturn(Collections.singletonList(Fixtures.productMap()));
    }

    @Test
    void previewFlagsMissingDimensionsAndRequiredAttributes() {
        when(metadataService.getTemplate(any(), eq("auth-1"), eq(200001483L), eq(971445087L)))
                .thenReturn(Fixtures.template());

        OzonProductPreviewView preview = service.preview(buildUser(), new OzonProductPreviewCommand("auth-1", "draft-1"));

        assertFalse(preview.isCanPublish());
        assertTrue(preview.getValidationErrors().stream().anyMatch(msg -> msg.contains("weight")));
        assertTrue(preview.getValidationErrors().stream().anyMatch(msg -> msg.contains("Brand")));
        assertFalse(preview.getVariantIssues().isEmpty());
        assertTrue(preview.getEffectivePayloadSummary().getVariants().stream()
                .anyMatch(item -> "ERP-SKU-1".equals(item.getMaterialSku()) && "BOOK-001".equals(item.getEffectiveOfferId())));
        verify(draftMapper).updateById(draftCaptor.capture());
        assertTrue("FAILED".equals(draftCaptor.getValue().getLastPreviewStatus()));
    }

    @Test
    void previewFailsWhenMetadataUnavailableAndNoStaleCacheExists() {
        when(metadataService.getTemplate(any(), eq("auth-1"), eq(200001483L), eq(971445087L)))
                .thenThrow(new IllegalStateException("template unavailable"));

        assertThrows(IllegalStateException.class,
                () -> service.preview(buildUser(), new OzonProductPreviewCommand("auth-1", "draft-1")));
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
            auth.setId("auth-1");
            auth.setShopId("company-1");
            return auth;
        }

        private static OzonListingDraft draft() {
            OzonListingDraft draft = new OzonListingDraft();
            draft.setId("draft-1");
            draft.setAuthId("auth-1");
            draft.setShopId("company-1");
            draft.setDraftName("Books draft");
            draft.setDescriptionCategoryId(200001483L);
            draft.setDescriptionCategoryName("Books");
            draft.setTypeId(971445087L);
            draft.setTypeName("Printed Book");
            draft.setTitleOverrideValue("Book title");
            draft.setBrandOverrideValue("Acme");
            draft.setStatus("READY");
            draft.setCreateTime(new Date());
            draft.setUpdateTime(new Date());
            return draft;
        }

        private static OzonListingVariant variantMissingDimensions() {
            OzonListingVariant variant = new OzonListingVariant();
            variant.setId("variant-1");
            variant.setDraftId("draft-1");
            variant.setAuthId("auth-1");
            variant.setShopId("company-1");
            variant.setMaterialSku("ERP-SKU-1");
            variant.setMaterialName("ERP Book");
            variant.setOfferIdOverride("BOOK-001");
            variant.setPriceSourceValue(new BigDecimal("99.00"));
            variant.setWeightSourceValue(null);
            variant.setLengthSourceValue(new BigDecimal("22.00"));
            variant.setWidthSourceValue(new BigDecimal("15.00"));
            variant.setHeightSourceValue(new BigDecimal("3.00"));
            variant.setStatus("READY");
            return variant;
        }

        private static OzonListingImage commonImage() {
            OzonListingImage image = new OzonListingImage();
            image.setId("image-1");
            image.setDraftId("draft-1");
            image.setAuthId("auth-1");
            image.setShopId("company-1");
            image.setScope("COMMON");
            image.setImageUrl("https://img.test/book-1.png");
            image.setSource("ERP");
            image.setSortOrder(0);
            image.setPrimary(Boolean.TRUE);
            return image;
        }

        private static OzonProductMap productMap() {
            OzonProductMap map = new OzonProductMap();
            map.setId("map-1");
            map.setAuthId("auth-1");
            map.setShopId("company-1");
            map.setMaterialSku("ERP-SKU-1");
            map.setOzonOfferId("BOOK-001");
            map.setImage("https://img.test/book-erp.png");
            return map;
        }

        private static OzonProductCategoryTemplateView template() {
            OzonProductCategoryTemplateView template = new OzonProductCategoryTemplateView();
            template.setDescriptionCategoryId(200001483L);
            template.setDescriptionCategoryName("Books");
            template.setTypeId(971445087L);
            template.setTypeName("Printed Book");
            template.setRequiredImageCount(1);
            template.setRequiresBarcode(false);
            template.setCommonAttributes(Collections.singletonList(attribute(85L, "Brand", "DICT")));
            template.setVariantAttributes(Collections.singletonList(attribute(4182L, "Author", "TEXT")));
            return template;
        }

        private static OzonProductCategoryTemplateView.AttributeItem attribute(Long id, String name, String mode) {
            OzonProductCategoryTemplateView.AttributeItem item = new OzonProductCategoryTemplateView.AttributeItem();
            item.setAttributeId(id);
            item.setAttributeName(name);
            item.setMode(mode);
            item.setValues(Collections.emptyList());
            return item;
        }
    }
}

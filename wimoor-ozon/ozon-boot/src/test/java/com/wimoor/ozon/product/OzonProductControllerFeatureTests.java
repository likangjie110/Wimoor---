package com.wimoor.ozon.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.result.Result;
import com.wimoor.common.result.ResultCode;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.product.controller.OzonProductController;
import com.wimoor.ozon.product.pojo.dto.OzonProductPublishCommand;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishTaskHistoryView;
import com.wimoor.ozon.product.pojo.vo.OzonProductPublishView;
import com.wimoor.ozon.product.service.IOzonListingDraftService;
import com.wimoor.ozon.product.service.IOzonProductMapService;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.IOzonProductPublishService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class OzonProductControllerFeatureTests {

    @Mock
    private IOzonProductMapService productMapService;

    @Mock
    private IOzonListingDraftService listingDraftService;

    @Mock
    private IOzonProductMetadataService metadataService;

    @Mock
    private IOzonProductPreviewService previewService;

    @Mock
    private IOzonProductPublishService publishService;

    @BeforeEach
    void setUp() {
        UserInfoContext.set(buildUser());
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    @Test
    void publishEndpointRejectsWhenProductWriteFeatureDisabled() {
        OzonProductController controller = new OzonProductController(
                productMapService,
                listingDraftService,
                metadataService,
                previewService,
                publishService,
                gate(properties -> properties.setProductWrite(false))
        );

        Result<OzonProductPublishView> result = controller.publish(new OzonProductPublishCommand("auth-1", "draft-1"));

        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals("Ozon商品发布写操作未开启", result.getMsg());
        verifyNoInteractions(publishService);
    }

    @Test
    void categoryTreeEndpointUsesProductFeatureGateOnly() {
        OzonProductController controller = new OzonProductController(
                productMapService,
                listingDraftService,
                metadataService,
                previewService,
                publishService,
                gate(properties -> properties.setProduct(true))
        );
        when(metadataService.getCategoryTree(any(), eq("auth-1"), eq("book"), eq("EN")))
                .thenReturn(new com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView());

        Result<com.wimoor.ozon.product.pojo.vo.OzonProductCategoryTreeView> result = controller.categoryTree("auth-1", "book", "EN");

        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
    }

    @Test
    void publishTaskListRejectsWhenProductFeatureDisabled() {
        OzonProductController controller = new OzonProductController(
                productMapService,
                listingDraftService,
                metadataService,
                previewService,
                publishService,
                gate(properties -> properties.setProduct(false))
        );

        Result<java.util.List<OzonProductPublishTaskHistoryView>> result = controller.publishTaskList("auth-1", "draft-1");

        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals("Ozon商品功能未开启", result.getMsg());
        verifyNoInteractions(publishService);
    }

    private OzonFeatureGate gate(java.util.function.Consumer<OzonFeatureProperties> customizer) {
        OzonFeatureProperties properties = new OzonFeatureProperties();
        properties.setAuth(true);
        properties.setProduct(true);
        properties.setProductWrite(true);
        properties.setTask(true);
        properties.setError(true);
        properties.setFinance(true);
        properties.setChat(true);
        properties.setAds(true);
        properties.setStockWrite(true);
        properties.setPriceWrite(true);
        properties.setPostingWrite(true);
        properties.setChatSend(false);
        properties.setAdsSync(false);
        customizer.accept(properties);
        return new OzonFeatureGate(properties);
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}

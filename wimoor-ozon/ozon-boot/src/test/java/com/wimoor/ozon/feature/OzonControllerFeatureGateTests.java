package com.wimoor.ozon.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collections;
import java.util.List;

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
import com.wimoor.ozon.ads.controller.OzonAdsController;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;
import com.wimoor.ozon.ads.service.IOzonAdsService;
import com.wimoor.ozon.auth.controller.OzonAuthController;
import com.wimoor.ozon.auth.pojo.vo.OzonAuthView;
import com.wimoor.ozon.auth.service.IOzonAuthService;
import com.wimoor.ozon.chat.controller.OzonChatController;
import com.wimoor.ozon.chat.pojo.entity.OzonChatSession;
import com.wimoor.ozon.chat.service.IOzonChatService;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.config.OzonFeatureProperties;
import com.wimoor.ozon.config.controller.OzonMetaController;
import com.wimoor.ozon.config.pojo.vo.OzonFeatureView;
import com.wimoor.ozon.error.controller.OzonErrorCenterController;
import com.wimoor.ozon.error.pojo.dto.OzonErrorQuery;
import com.wimoor.ozon.error.pojo.vo.OzonErrorView;
import com.wimoor.ozon.error.service.IOzonErrorCenterService;
import com.wimoor.ozon.finance.controller.OzonFinanceController;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;
import com.wimoor.ozon.finance.service.IOzonFinanceService;
import com.wimoor.ozon.product.controller.OzonProductController;
import com.wimoor.ozon.product.pojo.vo.OzonProductMapView;
import com.wimoor.ozon.product.service.IOzonListingDraftService;
import com.wimoor.ozon.product.service.IOzonProductMapService;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.IOzonProductPublishService;
import com.wimoor.ozon.product.service.IOzonProductPublishTaskQueryService;
import com.wimoor.ozon.product.service.IOzonProductPublishTaskQueryService;
import com.wimoor.ozon.seller.controller.OzonSellerSettingsController;
import com.wimoor.ozon.seller.pojo.entity.OzonDeliveryMethod;
import com.wimoor.ozon.seller.pojo.vo.OzonWarehouseView;
import com.wimoor.ozon.seller.service.IOzonSellerSettingsService;
import com.wimoor.ozon.task.controller.OzonTaskController;
import com.wimoor.ozon.task.pojo.dto.OzonTaskQuery;
import com.wimoor.ozon.task.pojo.vo.OzonTaskView;
import com.wimoor.ozon.task.service.IOzonTaskService;

@ExtendWith(MockitoExtension.class)
class OzonControllerFeatureGateTests {

    @Mock
    private IOzonAuthService authService;

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

    @Mock
    private IOzonProductPublishTaskQueryService publishTaskQueryService;

    @Mock
    private IOzonTaskService taskService;

    @Mock
    private IOzonErrorCenterService errorCenterService;

    @Mock
    private IOzonFinanceService financeService;

    @Mock
    private IOzonChatService chatService;

    @Mock
    private IOzonAdsService adsService;

    @Mock
    private IOzonSellerSettingsService sellerSettingsService;

    @BeforeEach
    void setUp() {
        UserInfoContext.set(buildUser());
    }

    @AfterEach
    void tearDown() {
        UserInfoContext.set(null);
    }

    @Test
    void authControllerRejectsRequestsWhenFeatureDisabled() {
        OzonAuthController controller = new OzonAuthController(authService, gate(properties -> properties.setAuth(false)));

        Result<List<OzonAuthView>> result = controller.list();

        assertFailure(result, "Ozon授权功能未开启");
        verifyNoInteractions(authService);
    }

    @Test
    void productControllerRejectsRequestsWhenFeatureDisabled() {
        OzonProductController controller = new OzonProductController(
                productMapService,
                listingDraftService,
                metadataService,
                previewService,
                publishService,
                gate(properties -> properties.setProduct(false)),
                publishTaskQueryService
        );

        Result<List<OzonProductMapView>> result = controller.list("auth-1", null);

        assertFailure(result, "Ozon商品功能未开启");
        verifyNoInteractions(productMapService);
    }

    @Test
    void taskControllerRejectsRequestsWhenFeatureDisabled() {
        OzonTaskController controller = new OzonTaskController(taskService, gate(properties -> properties.setTask(false)));

        Result<List<OzonTaskView>> result = controller.list(new OzonTaskQuery("auth-1", null, null));

        assertFailure(result, "Ozon任务中心功能未开启");
        verifyNoInteractions(taskService);
    }

    @Test
    void errorControllerRejectsRequestsWhenFeatureDisabled() {
        OzonErrorCenterController controller = new OzonErrorCenterController(errorCenterService, gate(properties -> properties.setError(false)));

        Result<List<OzonErrorView>> result = controller.list(new OzonErrorQuery("auth-1", null, null, null));

        assertFailure(result, "Ozon异常中心功能未开启");
        verifyNoInteractions(errorCenterService);
    }

    @Test
    void financeControllerRejectsRequestsWhenFeatureDisabled() {
        OzonFinanceController controller = new OzonFinanceController(financeService, gate(properties -> properties.setFinance(false)));

        Result<List<OzonFinanceTaskView>> result = controller.listTasks("auth-1");

        assertFailure(result, "Ozon财务功能未开启");
        verifyNoInteractions(financeService);
    }

    @Test
    void chatControllerRejectsRequestsWhenFeatureDisabled() {
        OzonChatController controller = new OzonChatController(chatService, gate(properties -> properties.setChat(false)));

        Result<List<OzonChatSession>> result = controller.listSessions("auth-1", null, null, null);

        assertFailure(result, "Ozon聊天功能未开启");
        verifyNoInteractions(chatService);
    }

    @Test
    void adsControllerRejectsRequestsWhenFeatureDisabled() {
        OzonAdsController controller = new OzonAdsController(adsService, gate(properties -> properties.setAds(false)));

        Result<List<OzonAdsCampaign>> result = controller.listCampaigns("auth-1", null, null);

        assertFailure(result, "Ozon广告功能未开启");
        verifyNoInteractions(adsService);
    }

    @Test
    void sellerSettingsControllerRejectsRequestsWhenAuthFeatureDisabled() {
        OzonSellerSettingsController controller = new OzonSellerSettingsController(sellerSettingsService, gate(properties -> properties.setAuth(false)));

        Result<List<OzonWarehouseView>> warehouseResult = controller.listWarehouses("auth-1");
        Result<List<OzonDeliveryMethod>> methodResult = controller.listDeliveryMethods("auth-1");

        assertFailure(warehouseResult, "Ozon授权功能未开启");
        assertFailure(methodResult, "Ozon授权功能未开启");
        verifyNoInteractions(sellerSettingsService);
    }

    @Test
    void metaControllerExposesFeatureFlagsAndReasons() {
        OzonMetaController controller = new OzonMetaController(gate(properties -> {
            properties.setProductWrite(false);
            properties.setStockWrite(false);
            properties.setPriceWrite(false);
            properties.setPostingWrite(false);
            properties.setChatSend(false);
            properties.setAdsSync(false);
        }));

        Result<OzonFeatureView> result = controller.features();

        assertEquals(true, Result.isSuccess(result));
        assertEquals(false, result.getData().getProductWrite().isEnabled());
        assertEquals(false, result.getData().getStockWrite().isEnabled());
        assertEquals(false, result.getData().getChatSend().isEnabled());
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

    private void assertFailure(Result<?> result, String message) {
        assertEquals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode(), result.getCode());
        assertEquals(message, result.getMsg());
        assertEquals(null, result.getData());
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}

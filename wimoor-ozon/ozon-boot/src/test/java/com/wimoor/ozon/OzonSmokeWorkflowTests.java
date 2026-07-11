package com.wimoor.ozon;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import com.wimoor.common.service.IOperationLogService;
import com.wimoor.common.service.IPictureService;
import com.wimoor.common.service.impl.StorageService;
import com.wimoor.ozon.aftersale.service.IOzonAfterSaleService;
import com.wimoor.ozon.ads.service.IOzonAdsService;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.IOzonAuthService;
import com.wimoor.ozon.chat.service.IOzonChatService;
import com.wimoor.ozon.error.service.IOzonErrorCenterService;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.ops.mapper.OzonApiLogMapper;
import com.wimoor.ozon.ops.mapper.OzonOperationAuditMapper;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.finance.service.IOzonFinanceService;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.price.service.IOzonPriceService;
import com.wimoor.ozon.price.service.IOzonPriceTaskQueryService;
import com.wimoor.ozon.product.mapper.OzonListingAttributeMapper;
import com.wimoor.ozon.product.mapper.OzonListingDraftMapper;
import com.wimoor.ozon.product.mapper.OzonListingImageMapper;
import com.wimoor.ozon.product.mapper.OzonListingPublishTaskMapper;
import com.wimoor.ozon.product.mapper.OzonListingVariantMapper;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.service.IOzonListingDraftService;
import com.wimoor.ozon.product.service.IOzonProductMapService;
import com.wimoor.ozon.product.service.IOzonProductMetadataService;
import com.wimoor.ozon.product.service.IOzonProductPreviewService;
import com.wimoor.ozon.product.service.IOzonProductPublishService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.mapper.OzonDeliveryMethodMapper;
import com.wimoor.ozon.seller.service.IOzonSellerSettingsService;
import com.wimoor.ozon.seller.service.IOzonWarehouseSyncService;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;
import com.wimoor.ozon.stock.service.IOzonStockService;
import com.wimoor.ozon.stock.service.IOzonStockTaskQueryService;
import com.wimoor.ozon.task.mapper.OzonSyncCursorMapper;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.service.IOzonTaskService;

@SpringBootTest(classes = OzonTestApplication.class, properties = {
        "logging.config=classpath:logback-test.xml",
        "spring.mvc.pathmatch.matching-strategy=ant_path_matcher",
        "spring.sql.init.mode=never",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.autoconfigure.exclude="
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                + "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure,"
                + "io.seata.spring.boot.autoconfigure.SeataAutoConfiguration,"
                + "io.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration"
})
class OzonSmokeWorkflowTests {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private IOzonAuthService ozonAuthService;

    @MockBean
    private IOzonWarehouseSyncService ozonWarehouseSyncService;

    @MockBean
    private IOzonSellerSettingsService ozonSellerSettingsService;

    @MockBean
    private IOzonAfterSaleService ozonAfterSaleService;

    @MockBean
    private IOzonProductMapService ozonProductMapService;

    @MockBean
    private IOzonListingDraftService ozonListingDraftService;

    @MockBean
    private IOzonProductMetadataService ozonProductMetadataService;

    @MockBean
    private IOzonProductPreviewService ozonProductPreviewService;

    @MockBean
    private IOzonProductPublishService ozonProductPublishService;

    @MockBean
    private IOzonStockService ozonStockService;

    @MockBean
    private IOzonStockTaskQueryService ozonStockTaskQueryService;

    @MockBean
    private IOzonPriceService ozonPriceService;

    @MockBean
    private IOzonPriceTaskQueryService ozonPriceTaskQueryService;

    @MockBean
    private IOzonPostingService ozonPostingService;

    @MockBean
    private IOzonShipmentService ozonShipmentService;

    @MockBean
    private IOzonTaskService ozonTaskService;

    @MockBean
    private IOzonErrorCenterService ozonErrorCenterService;

    @MockBean
    private OzonErrorRecorder ozonErrorRecorder;

    @MockBean
    private IOzonFinanceService ozonFinanceService;

    @MockBean
    private IOzonChatService ozonChatService;

    @MockBean
    private IOzonAdsService ozonAdsService;

    @MockBean
    private IOzonOpsService ozonOpsService;

    @MockBean
    private IOperationLogService operationLogService;

    @MockBean
    private IPictureService pictureService;

    @MockBean
    private StorageService storageService;

    @MockBean
    private OzonAuthMapper ozonAuthMapper;

    @MockBean
    private OzonShopConfigMapper ozonShopConfigMapper;

    @MockBean
    private OzonWarehouseMapper ozonWarehouseMapper;

    @MockBean
    private OzonDeliveryMethodMapper ozonDeliveryMethodMapper;

    @MockBean
    private OzonSyncJobMapper ozonSyncJobMapper;

    @MockBean
    private OzonSyncCursorMapper ozonSyncCursorMapper;

    @MockBean
    private OzonListingDraftMapper ozonListingDraftMapper;

    @MockBean
    private OzonListingVariantMapper ozonListingVariantMapper;

    @MockBean
    private OzonListingAttributeMapper ozonListingAttributeMapper;

    @MockBean
    private OzonListingImageMapper ozonListingImageMapper;

    @MockBean
    private OzonListingPublishTaskMapper ozonListingPublishTaskMapper;

    @MockBean
    private OzonProductMapMapper ozonProductMapMapper;

    @MockBean
    private OzonApiLogMapper ozonApiLogMapper;

    @MockBean
    private OzonOperationAuditMapper ozonOperationAuditMapper;

    @Test
    void auth_product_stock_price_posting_task_error_finance_chat_ads_flow_is_wired() {
        assertTrue(applicationContext.containsBean("ozonAuthController"));
        assertTrue(applicationContext.containsBean("ozonProductController"));
        assertTrue(applicationContext.containsBean("ozonStockController"));
        assertTrue(applicationContext.containsBean("ozonPriceController"));
        assertTrue(applicationContext.containsBean("ozonPostingController"));
        assertTrue(applicationContext.containsBean("ozonTaskController"));
        assertTrue(applicationContext.containsBean("ozonErrorCenterController"));
        assertTrue(applicationContext.containsBean("ozonFinanceController"));
        assertTrue(applicationContext.containsBean("ozonChatController"));
        assertTrue(applicationContext.containsBean("ozonAdsController"));
        assertTrue(applicationContext.containsBean("ozonMetaController"));
        assertTrue(applicationContext.containsBean("ozonSellerSettingsController"));
        assertTrue(applicationContext.containsBean("ozonAfterSaleController"));
        assertTrue(applicationContext.containsBean("ozonOpsController"));
    }

}

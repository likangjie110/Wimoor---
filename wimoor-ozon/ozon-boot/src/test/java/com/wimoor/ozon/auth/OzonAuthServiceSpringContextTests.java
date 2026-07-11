package com.wimoor.ozon.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.wimoor.erp.api.ErpClientOneFeign;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.auth.service.impl.OzonAuthServiceImpl;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.error.service.OzonErrorRecorder;
import com.wimoor.ozon.posting.mapper.OzonPostingItemMapper;
import com.wimoor.ozon.posting.mapper.OzonPostingMapper;
import com.wimoor.ozon.posting.service.impl.OzonPostingServiceImpl;
import com.wimoor.ozon.price.mapper.OzonPriceSnapshotMapper;
import com.wimoor.ozon.price.mapper.OzonPriceTaskMapper;
import com.wimoor.ozon.price.service.impl.OzonPriceServiceImpl;
import com.wimoor.ozon.product.mapper.OzonProductMapMapper;
import com.wimoor.ozon.product.service.impl.OzonProductMapServiceImpl;
import com.wimoor.ozon.security.OzonCredentialService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.service.IOzonWarehouseSyncService;
import com.wimoor.ozon.stock.mapper.OzonStockSnapshotMapper;
import com.wimoor.ozon.stock.mapper.OzonStockTaskMapper;
import com.wimoor.ozon.stock.service.impl.OzonStockServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.mapper.OzonSyncCursorMapper;
import com.wimoor.ozon.shipment.mapper.OzonShipmentMapper;
import com.wimoor.ozon.config.OzonFeatureGate;

class OzonAuthServiceSpringContextTests {

    private static final String AES_KEY = "0123456789abcdef";

    @Test
    void springCanInstantiateOzonAuthServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerCommonBeans(context);
            context.registerBean(OzonShopConfigMapper.class, () -> mock(OzonShopConfigMapper.class));
            context.registerBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class));
            context.registerBean(IOzonWarehouseSyncService.class, () -> mock(IOzonWarehouseSyncService.class));
            context.registerBean(OzonAuthServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OzonAuthServiceImpl.class));
        }
    }

    @Test
    void springCanInstantiateOzonPostingServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerCommonBeans(context);
            context.registerBean(OzonAuthAccessService.class);
            context.registerBean(OzonPostingMapper.class, () -> mock(OzonPostingMapper.class));
            context.registerBean(OzonPostingItemMapper.class, () -> mock(OzonPostingItemMapper.class));
            context.registerBean(OzonProductMapMapper.class, () -> mock(OzonProductMapMapper.class));
            context.registerBean(ErpClientOneFeign.class, () -> mock(ErpClientOneFeign.class));
            context.registerBean(OzonShipmentMapper.class, () -> mock(OzonShipmentMapper.class));
            context.registerBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class));
            context.registerBean(OzonSyncCursorMapper.class, () -> mock(OzonSyncCursorMapper.class));
            context.registerBean(OzonErrorRecorder.class, () -> mock(OzonErrorRecorder.class));
            context.registerBean(OzonFeatureGate.class, OzonFeatureGate::allEnabled);
            context.registerBean(OzonPostingServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OzonPostingServiceImpl.class));
        }
    }

    @Test
    void springCanInstantiateOzonPriceServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerCommonBeans(context);
            context.registerBean(OzonAuthAccessService.class);
            context.registerBean(OzonProductMapMapper.class, () -> mock(OzonProductMapMapper.class));
            context.registerBean(OzonPriceTaskMapper.class, () -> mock(OzonPriceTaskMapper.class));
            context.registerBean(OzonPriceSnapshotMapper.class, () -> mock(OzonPriceSnapshotMapper.class));
            context.registerBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class));
            context.registerBean(OzonFeatureGate.class, OzonFeatureGate::allEnabled);
            context.registerBean(OzonPriceServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OzonPriceServiceImpl.class));
        }
    }

    @Test
    void springCanInstantiateOzonProductMapServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerCommonBeans(context);
            context.registerBean(OzonAuthAccessService.class);
            context.registerBean(OzonProductMapMapper.class, () -> mock(OzonProductMapMapper.class));
            context.registerBean(ErpClientOneFeign.class, () -> mock(ErpClientOneFeign.class));
            context.registerBean(OzonProductMapServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OzonProductMapServiceImpl.class));
        }
    }

    @Test
    void springCanInstantiateOzonStockServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            registerCommonBeans(context);
            context.registerBean(OzonAuthAccessService.class);
            context.registerBean(OzonProductMapMapper.class, () -> mock(OzonProductMapMapper.class));
            context.registerBean(OzonStockTaskMapper.class, () -> mock(OzonStockTaskMapper.class));
            context.registerBean(OzonStockSnapshotMapper.class, () -> mock(OzonStockSnapshotMapper.class));
            context.registerBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class));
            context.registerBean(OzonFeatureGate.class, OzonFeatureGate::allEnabled);
            context.registerBean(OzonStockServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(OzonStockServiceImpl.class));
        }
    }

    private void registerCommonBeans(AnnotationConfigApplicationContext context) {
        context.registerBean(OzonAuthMapper.class, () -> mock(OzonAuthMapper.class));
        context.registerBean(OzonSellerApiClient.class, () -> mock(OzonSellerApiClient.class));
        context.registerBean(OzonCredentialService.class, () -> new OzonCredentialService(AES_KEY));
    }
}

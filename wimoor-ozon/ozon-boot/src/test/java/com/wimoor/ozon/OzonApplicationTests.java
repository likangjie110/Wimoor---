package com.wimoor.ozon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.wimoor.common.service.IOperationLogService;
import com.wimoor.common.service.IPictureService;
import com.wimoor.common.service.impl.StorageService;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.service.IOzonAuthService;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.price.service.IOzonPriceService;
import com.wimoor.ozon.product.service.IOzonProductMapService;
import com.wimoor.ozon.seller.mapper.OzonShopConfigMapper;
import com.wimoor.ozon.seller.mapper.OzonWarehouseMapper;
import com.wimoor.ozon.seller.service.IOzonWarehouseSyncService;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;
import com.wimoor.ozon.stock.service.IOzonStockService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;

@SpringBootTest(classes = OzonApplicationTests.TestApplication.class, properties = {
        "logging.config=classpath:logback-test.xml",
        "spring.mvc.pathmatch.matching-strategy=ant_path_matcher",
        "spring.cloud.bootstrap.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.autoconfigure.exclude="
                + "com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure,"
                + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
                + "com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class OzonApplicationTests {

    @MockBean
    private IOzonAuthService ozonAuthService;

    @MockBean
    private IOzonWarehouseSyncService ozonWarehouseSyncService;

    @MockBean
    private IOzonProductMapService ozonProductMapService;

    @MockBean
    private IOzonStockService ozonStockService;

    @MockBean
    private IOzonPriceService ozonPriceService;

    @MockBean
    private IOzonPostingService ozonPostingService;

    @MockBean
    private IOzonShipmentService ozonShipmentService;

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
    private OzonSyncJobMapper ozonSyncJobMapper;

    @Test
    void contextLoads() {
    }

    @SpringBootApplication(scanBasePackages = "com.wimoor.ozon")
    @EnableFeignClients(basePackages = "com.wimoor")
    static class TestApplication {
    }
}

package com.wimoor.ozon.error;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.error.mapper.OzonErrorEventMapper;
import com.wimoor.ozon.error.service.impl.OzonErrorCenterServiceImpl;
import com.wimoor.ozon.posting.service.IOzonPostingService;
import com.wimoor.ozon.shipment.service.IOzonShipmentService;

class OzonErrorCenterServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonErrorEventMapper.class, () -> mock(OzonErrorEventMapper.class))
            .withBean(IOzonPostingService.class, () -> mock(IOzonPostingService.class))
            .withBean(IOzonShipmentService.class, () -> mock(IOzonShipmentService.class))
            .withBean(OzonErrorCenterServiceImpl.class);

    @Test
    void springCanConstructErrorCenterServiceBean() {
        contextRunner.run(context -> {
            OzonErrorCenterServiceImpl bean = context.getBean(OzonErrorCenterServiceImpl.class);
            assertNotNull(bean);
        });
    }
}

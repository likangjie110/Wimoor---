package com.wimoor.ozon.task;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;
import com.wimoor.ozon.task.service.impl.OzonTaskServiceImpl;

class OzonTaskServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class))
            .withBean(OzonTaskServiceImpl.class);

    @Test
    void springCanConstructTaskServiceBean() {
        contextRunner.run(context -> {
            OzonTaskServiceImpl bean = context.getBean(OzonTaskServiceImpl.class);
            assertNotNull(bean);
        });
    }
}

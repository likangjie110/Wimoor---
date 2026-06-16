package com.wimoor.ozon.finance;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.finance.mapper.OzonFinTransactionMapper;
import com.wimoor.ozon.finance.mapper.OzonReportFileMapper;
import com.wimoor.ozon.finance.mapper.OzonReportTaskMapper;
import com.wimoor.ozon.finance.service.impl.OzonFinanceServiceImpl;
import com.wimoor.ozon.task.mapper.OzonSyncJobMapper;

class OzonFinanceServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonReportTaskMapper.class, () -> mock(OzonReportTaskMapper.class))
            .withBean(OzonReportFileMapper.class, () -> mock(OzonReportFileMapper.class))
            .withBean(OzonFinTransactionMapper.class, () -> mock(OzonFinTransactionMapper.class))
            .withBean(OzonSyncJobMapper.class, () -> mock(OzonSyncJobMapper.class))
            .withBean(OzonFinanceServiceImpl.class);

    @Test
    void springCanConstructFinanceServiceBean() {
        contextRunner.run(context -> {
            OzonFinanceServiceImpl bean = context.getBean(OzonFinanceServiceImpl.class);
            assertNotNull(bean);
        });
    }
}

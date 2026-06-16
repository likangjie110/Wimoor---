package com.wimoor.ozon.ads;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.ads.mapper.OzonAdsAccountMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsCampaignMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsReportMapper;
import com.wimoor.ozon.ads.service.impl.OzonAdsServiceImpl;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;

class OzonAdsServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonAdsAccountMapper.class, () -> mock(OzonAdsAccountMapper.class))
            .withBean(OzonAdsCampaignMapper.class, () -> mock(OzonAdsCampaignMapper.class))
            .withBean(OzonAdsReportMapper.class, () -> mock(OzonAdsReportMapper.class))
            .withBean(OzonAdsServiceImpl.class);

    @Test
    void springCanConstructAdsServiceBean() {
        contextRunner.run(context -> {
            OzonAdsServiceImpl bean = context.getBean(OzonAdsServiceImpl.class);
            assertNotNull(bean);
        });
    }
}

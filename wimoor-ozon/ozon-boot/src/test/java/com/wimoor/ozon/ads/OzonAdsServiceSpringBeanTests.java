package com.wimoor.ozon.ads;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.wimoor.ozon.ads.mapper.OzonAdsAccountMapper;
import com.wimoor.ozon.ads.client.OzonPerformanceApiClient;
import com.wimoor.ozon.ads.mapper.OzonAdsCampaignMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsReportMapper;
import com.wimoor.ozon.ads.service.impl.OzonAdsServiceImpl;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.security.OzonCredentialService;

class OzonAdsServiceSpringBeanTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OzonAuthAccessService.class, () -> mock(OzonAuthAccessService.class))
            .withBean(OzonAdsAccountMapper.class, () -> mock(OzonAdsAccountMapper.class))
            .withBean(OzonAdsCampaignMapper.class, () -> mock(OzonAdsCampaignMapper.class))
            .withBean(OzonAdsReportMapper.class, () -> mock(OzonAdsReportMapper.class))
            .withBean(OzonSellerApiClient.class, () -> mock(OzonSellerApiClient.class))
            .withBean(OzonPerformanceApiClient.class, () -> mock(OzonPerformanceApiClient.class))
            .withBean(OzonCredentialService.class, () -> mock(OzonCredentialService.class))
            .withBean(OzonAdsServiceImpl.class);

    @Test
    void springCanConstructAdsServiceBean() {
        contextRunner.run(context -> {
            OzonAdsServiceImpl bean = context.getBean(OzonAdsServiceImpl.class);
            assertNotNull(bean);
        });
    }
}

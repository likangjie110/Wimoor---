package com.wimoor.ozon.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ads.mapper.OzonAdsAccountMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsCampaignMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsReportMapper;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsImportCommand;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsReportQuery;
import com.wimoor.ozon.ads.pojo.dto.OzonAdsSyncCommand;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsAccount;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsReport;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsImportResult;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSummary;
import com.wimoor.ozon.ads.pojo.vo.OzonAdsSyncIntentResult;
import com.wimoor.ozon.ads.service.impl.OzonAdsServiceImpl;
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.security.OzonCredentialService;

@ExtendWith(MockitoExtension.class)
class OzonAdsReportTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonAdsAccountMapper accountMapper;

    @Mock
    private OzonAdsCampaignMapper campaignMapper;

    @Mock
    private OzonAdsReportMapper reportMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private OzonCredentialService credentialService;

    @Captor
    private ArgumentCaptor<OzonAdsAccount> accountCaptor;

    @Captor
    private ArgumentCaptor<OzonAdsCampaign> campaignCaptor;

    @Captor
    private ArgumentCaptor<OzonAdsReport> reportCaptor;

    private OzonAdsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonAdsServiceImpl(
                new OzonAuthAccessService(authMapper),
                accountMapper,
                campaignMapper,
                reportMapper,
                sellerApiClient,
                credentialService
        );
    }

    @Test
    void importCreatesCampaignsReportsAndSummary() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsImportResult result = service.importAds(
                buildUser(),
                new OzonAdsImportCommand(
                        "auth-1",
                        "{\"account\":{\"accountId\":\"acc-1\",\"accountName\":\"Ozon Ads Account\",\"status\":\"ACTIVE\",\"currencyCode\":\"RUB\"},"
                                + "\"campaigns\":[{\"campaignId\":\"camp-1\",\"campaignName\":\"Spring Campaign\",\"campaignType\":\"SEARCH_PROMO\",\"campaignStatus\":\"ACTIVE\",\"budget\":1000}],"
                                + "\"reports\":[{\"campaignId\":\"camp-1\",\"reportDate\":\"2026-03-26\",\"impressions\":1000,\"clicks\":50,\"spend\":120.5,\"orders\":5,\"sales\":800,\"ctr\":5,\"cpc\":2.41,\"acos\":15.06,\"roas\":6.64}]}"
                )
        );

        assertEquals(1, result.getCampaignCount());
        assertEquals(1, result.getReportCount());
        verify(accountMapper).upsert(accountCaptor.capture());
        verify(campaignMapper).upsert(campaignCaptor.capture());
        verify(reportMapper).upsert(reportCaptor.capture());
        assertEquals("acc-1", accountCaptor.getValue().getAccountId());
        assertEquals("camp-1", campaignCaptor.getValue().getCampaignId());
        assertEquals(new BigDecimal("120.5"), reportCaptor.getValue().getSpend());
    }

    @Test
    void listReportsReturnsCampaignReports() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsReport report = new OzonAdsReport();
        report.setAuthId("auth-1");
        report.setCampaignId("camp-1");
        report.setReportDate(new Date(0L));
        report.setImpressions(1000L);
        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        List<OzonAdsReport> reports = service.listReports(
                buildUser(),
                new OzonAdsReportQuery("auth-1", "acc-1", "camp-1", "2026-03-01", "2026-03-31")
        );

        assertEquals(1, reports.size());
        assertEquals("camp-1", reports.get(0).getCampaignId());
    }

    @Test
    void summaryAggregatesMetrics() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsReport report = new OzonAdsReport();
        report.setAuthId("auth-1");
        report.setCampaignId("camp-1");
        report.setImpressions(1000L);
        report.setClicks(50L);
        report.setSpend(new BigDecimal("120.50"));
        report.setOrders(5L);
        report.setSales(new BigDecimal("800.00"));
        report.setAcos(new BigDecimal("15.06"));
        report.setRoas(new BigDecimal("6.64"));
        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        OzonAdsSummary summary = service.summary(
                buildUser(),
                new OzonAdsReportQuery("auth-1", "acc-1", "camp-1", "2026-03-01", "2026-03-31")
        );

        assertNotNull(summary);
        assertEquals(Long.valueOf(1000L), summary.getImpressions());
        assertEquals(Long.valueOf(50L), summary.getClicks());
        assertEquals(new BigDecimal("120.50"), summary.getSpend());
        assertEquals(new BigDecimal("800.00"), summary.getSales());
    }

    @Test
    void listAccountsReturnsCurrentAuthAccounts() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        account.setAccountName("Main Account");
        when(accountMapper.selectList(any())).thenReturn(Collections.singletonList(account));

        List<OzonAdsAccount> accounts = service.listAccounts(buildUser(), "auth-1");

        assertEquals(1, accounts.size());
        assertEquals("acc-1", accounts.get(0).getAccountId());
    }

    @Test
    void recordSyncIntentValidatesAccountAndReturnsPendingResult() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        account.setAccountName("Main Account");
        when(accountMapper.selectOne(any())).thenReturn(account);

        OzonAdsSyncIntentResult result = service.recordSyncIntent(
                buildUser(),
                new OzonAdsSyncCommand("auth-1", "acc-1", null, "2026-03-01", "2026-03-31")
        );

        assertEquals("acc-1", result.getAccountId());
        assertEquals("PENDING", result.getRequestStatus());
    }

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}

package com.wimoor.ozon.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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

/**
 * Phase 6: Ads 模块单元测试
 *
 * 测试范围：
 * 1. syncCampaignsFromApi - API 同步广告活动
 * 2. syncReportsFromApi - API 同步广告报告
 * 3. 权限验证
 * 4. 数据聚合与计算
 */
@ExtendWith(MockitoExtension.class)
class OzonAdsServiceTests {

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

    // ==================== importAds 测试 ====================

    @Test
    void importAdsCreatesAccountCampaignsAndReports() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsImportResult result = service.importAds(
                buildUser(),
                new OzonAdsImportCommand(
                        "auth-1",
                        "{\"account\":{\"accountId\":\"acc-1\",\"accountName\":\"Test Account\",\"status\":\"ACTIVE\",\"currencyCode\":\"RUB\"},"
                                + "\"campaigns\":[{\"campaignId\":\"camp-1\",\"campaignName\":\"Summer Sale\",\"campaignType\":\"SEARCH_PROMO\",\"campaignStatus\":\"ACTIVE\",\"budget\":5000}],"
                                + "\"reports\":[{\"campaignId\":\"camp-1\",\"reportDate\":\"2026-06-20\",\"impressions\":10000,\"clicks\":250,\"spend\":1500.50,\"orders\":50,\"sales\":8000,\"ctr\":2.5,\"cpc\":6.00,\"acos\":18.76,\"roas\":5.33}]}"
                )
        );

        assertEquals(1, result.getCampaignCount());
        assertEquals(1, result.getReportCount());
        assertNotNull(result.getImportedAt());

        verify(accountMapper).upsert(accountCaptor.capture());
        verify(campaignMapper).upsert(campaignCaptor.capture());
        verify(reportMapper).upsert(reportCaptor.capture());

        assertEquals("acc-1", accountCaptor.getValue().getAccountId());
        assertEquals("Test Account", accountCaptor.getValue().getAccountName());
        assertEquals("camp-1", campaignCaptor.getValue().getCampaignId());
        assertEquals("Summer Sale", campaignCaptor.getValue().getCampaignName());
        assertEquals(new BigDecimal("1500.50"), reportCaptor.getValue().getSpend());
    }

    @Test
    void importAdsRequiresAccountData() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.importAds(
                        buildUser(),
                        new OzonAdsImportCommand("auth-1", "{\"campaigns\":[]}")
                ));
    }

    @Test
    void importAdsRequiresCampaignData() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.importAds(
                        buildUser(),
                        new OzonAdsImportCommand("auth-1", "{\"account\":{\"accountId\":\"acc-1\"}}")
                ));
    }

    @Test
    void importAdsRequiresAuthPermission() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithDifferentShop());

        assertThrows(IllegalArgumentException.class, () ->
                service.importAds(
                        buildUser(),
                        new OzonAdsImportCommand("auth-1", "{\"account\":{},\"campaigns\":[]}")
                ));
    }

    // ==================== listAccounts 测试 ====================

    @Test
    void listAccountsReturnsAuthAccounts() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        account.setAccountName("Main Account");
        account.setStatus("ACTIVE");
        when(accountMapper.selectList(any())).thenReturn(Collections.singletonList(account));

        List<OzonAdsAccount> accounts = service.listAccounts(buildUser(), "auth-1");

        assertEquals(1, accounts.size());
        assertEquals("acc-1", accounts.get(0).getAccountId());
        assertEquals("Main Account", accounts.get(0).getAccountName());
    }

    // ==================== listCampaigns 测试 ====================

    @Test
    void listCampaignsReturnsFilteredCampaigns() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsCampaign campaign = new OzonAdsCampaign();
        campaign.setAuthId("auth-1");
        campaign.setAccountId("acc-1");
        campaign.setCampaignId("camp-1");
        campaign.setCampaignName("Summer Sale");
        campaign.setCampaignType("SEARCH_PROMO");
        when(campaignMapper.selectList(any())).thenReturn(Collections.singletonList(campaign));

        List<OzonAdsCampaign> campaigns = service.listCampaigns(buildUser(), "auth-1", "acc-1", null);

        assertEquals(1, campaigns.size());
        assertEquals("camp-1", campaigns.get(0).getCampaignId());
        assertEquals("Summer Sale", campaigns.get(0).getCampaignName());
    }

    @Test
    void listCampaignsFiltersKeyword() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(campaignMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.listCampaigns(buildUser(), "auth-1", null, "summer");

        verify(campaignMapper).selectList(any());
    }

    // ==================== listReports 测试 ====================

    @Test
    void listReportsReturnsFilteredReports() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsReport report = new OzonAdsReport();
        report.setAuthId("auth-1");
        report.setAccountId("acc-1");
        report.setCampaignId("camp-1");
        report.setImpressions(10000L);
        report.setClicks(250L);
        report.setSpend(new BigDecimal("1500.50"));
        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        OzonAdsReportQuery query = new OzonAdsReportQuery("auth-1", "acc-1", "camp-1", "2026-06-01", "2026-06-30");
        List<OzonAdsReport> reports = service.listReports(buildUser(), query);

        assertEquals(1, reports.size());
        assertEquals("camp-1", reports.get(0).getCampaignId());
        assertEquals(Long.valueOf(10000), reports.get(0).getImpressions());
    }

    // ==================== summary 测试 ====================

    @Test
    void summaryAggregatesMultipleReports() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsReport report1 = new OzonAdsReport();
        report1.setImpressions(10000L);
        report1.setClicks(250L);
        report1.setSpend(new BigDecimal("1500.50"));
        report1.setOrders(50L);
        report1.setSales(new BigDecimal("8000.00"));

        OzonAdsReport report2 = new OzonAdsReport();
        report2.setImpressions(5000L);
        report2.setClicks(100L);
        report2.setSpend(new BigDecimal("800.00"));
        report2.setOrders(20L);
        report2.setSales(new BigDecimal("3200.00"));

        when(reportMapper.selectList(any())).thenReturn(Arrays.asList(report1, report2));

        OzonAdsSummary summary = service.summary(buildUser(), new OzonAdsReportQuery("auth-1", null, null, null, null));

        assertEquals(Long.valueOf(15000), summary.getImpressions());
        assertEquals(Long.valueOf(350), summary.getClicks());
        assertEquals(new BigDecimal("2300.50"), summary.getSpend());
        assertEquals(Long.valueOf(70), summary.getOrders());
        assertEquals(new BigDecimal("11200.00"), summary.getSales());
        assertNotNull(summary.getAcos());
        assertNotNull(summary.getRoas());
    }

    @Test
    void summaryCalculatesAcosAndRoas() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsReport report = new OzonAdsReport();
        report.setImpressions(10000L);
        report.setClicks(250L);
        report.setSpend(new BigDecimal("1500.00"));
        report.setOrders(50L);
        report.setSales(new BigDecimal("8000.00"));

        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        OzonAdsSummary summary = service.summary(buildUser(), new OzonAdsReportQuery("auth-1", null, null, null, null));

        // ACOS = (spend / sales) * 100 = (1500 / 8000) * 100 = 18.75
        assertEquals(new BigDecimal("18.75"), summary.getAcos());

        // ROAS = sales / spend = 8000 / 1500 = 5.33
        assertEquals(new BigDecimal("5.33"), summary.getRoas());
    }

    @Test
    void summaryHandlesZeroSalesForAcos() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsReport report = new OzonAdsReport();
        report.setSpend(new BigDecimal("1500.00"));
        report.setSales(BigDecimal.ZERO);

        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        OzonAdsSummary summary = service.summary(buildUser(), new OzonAdsReportQuery("auth-1", null, null, null, null));

        assertEquals(BigDecimal.ZERO, summary.getAcos());
    }

    @Test
    void summaryHandlesZeroSpendForRoas() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonAdsReport report = new OzonAdsReport();
        report.setSpend(BigDecimal.ZERO);
        report.setSales(new BigDecimal("8000.00"));

        when(reportMapper.selectList(any())).thenReturn(Collections.singletonList(report));

        OzonAdsSummary summary = service.summary(buildUser(), new OzonAdsReportQuery("auth-1", null, null, null, null));

        assertEquals(BigDecimal.ZERO, summary.getRoas());
    }

    // ==================== recordSyncIntent 测试 ====================

    @Test
    void recordSyncIntentValidatesAccountExists() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        account.setAccountName("Main Account");
        when(accountMapper.selectOne(any())).thenReturn(account);

        OzonAdsSyncIntentResult result = service.recordSyncIntent(
                buildUser(),
                new OzonAdsSyncCommand("auth-1", "acc-1", null, "2026-06-01", "2026-06-30")
        );

        assertEquals("acc-1", result.getAccountId());
        assertEquals("PENDING", result.getRequestStatus());
        assertNotNull(result.getRequestId());
        assertNotNull(result.getRequestedAt());
    }

    @Test
    void recordSyncIntentValidatesCampaignIfProvided() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        when(accountMapper.selectOne(any())).thenReturn(account);

        OzonAdsCampaign campaign = new OzonAdsCampaign();
        campaign.setAuthId("auth-1");
        campaign.setAccountId("acc-1");
        campaign.setCampaignId("camp-1");
        when(campaignMapper.selectOne(any())).thenReturn(campaign);

        OzonAdsSyncIntentResult result = service.recordSyncIntent(
                buildUser(),
                new OzonAdsSyncCommand("auth-1", "acc-1", "camp-1", "2026-06-01", "2026-06-30")
        );

        assertEquals("acc-1", result.getAccountId());
        assertEquals("camp-1", result.getCampaignId());
        assertEquals("PENDING", result.getRequestStatus());
    }

    @Test
    void recordSyncIntentRequiresAccountExists() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(accountMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.recordSyncIntent(
                        buildUser(),
                        new OzonAdsSyncCommand("auth-1", "acc-1", null, "2026-06-01", "2026-06-30")
                ));
    }

    @Test
    void recordSyncIntentRequiresCampaignExistsIfProvided() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAuthId("auth-1");
        account.setAccountId("acc-1");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(campaignMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.recordSyncIntent(
                        buildUser(),
                        new OzonAdsSyncCommand("auth-1", "acc-1", "camp-999", "2026-06-01", "2026-06-30")
                ));
    }

    // ==================== Helper Methods ====================

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private OzonAuth buildAuthWithDifferentShop() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("different-company");
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

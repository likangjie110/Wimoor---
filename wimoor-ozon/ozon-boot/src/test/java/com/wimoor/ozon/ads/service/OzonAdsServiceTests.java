package com.wimoor.ozon.ads.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.ads.mapper.OzonAdsAccountMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsCampaignMapper;
import com.wimoor.ozon.ads.mapper.OzonAdsReportMapper;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsAccount;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsCampaign;
import com.wimoor.ozon.ads.pojo.entity.OzonAdsReport;
import com.wimoor.ozon.ads.service.impl.OzonAdsServiceImpl;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.security.OzonCredentialService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Ozon 广告服务测试")
class OzonAdsServiceTests {

    @Mock
    private OzonAuthAccessService authAccessService;

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

    @Mock
    private IOzonOpsService opsService;

    @InjectMocks
    private OzonAdsServiceImpl adsService;

    private UserInfo testUser;
    private OzonAuth testAuth;

    @BeforeEach
    void setUp() {
        testUser = new UserInfo();
        testUser.setId("user-123");

        testAuth = new OzonAuth();
        testAuth.setId("auth-123");
        testAuth.setShopId("shop-456");
        testAuth.setClientId("client-789");
        testAuth.setApiKeyCiphertext("encrypted-key");

        adsService.setOpsService(opsService);
        OzonAdsAccount account = new OzonAdsAccount();
        account.setAccountId("acc-1");
        when(accountMapper.selectList(any())).thenReturn(Collections.singletonList(account));
    }

    @Test
    @DisplayName("同步广告活动 - 成功场景")
    void testSyncCampaignsFromApi_Success() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        JSONArray campaigns = new JSONArray();
        JSONObject campaign1 = new JSONObject();
        campaign1.put("campaign_id", "camp-1");
        campaign1.put("campaign_name", "Test Campaign");
        campaign1.put("campaign_type", "SEARCH_PROMO");
        campaign1.put("status", "ACTIVE");
        campaign1.put("budget", "1000");
        campaigns.add(campaign1);
        result.put("campaigns", campaigns);
        result.put("account_id", "acc-1");
        response.put("result", result);

        when(sellerApiClient.listAdsCampaigns(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());
        when(campaignMapper.selectOne(any())).thenReturn(null);
        when(campaignMapper.upsert(any())).thenReturn(1);

        // Act
        List<OzonAdsCampaign> actualResult = adsService.syncCampaignsFromApi(testUser, authId);

        // Assert
        assertNotNull(actualResult);
        assertEquals(1, actualResult.size());
        assertEquals("camp-1", actualResult.get(0).getCampaignId());
        assertEquals("Test Campaign", actualResult.get(0).getCampaignName());

        verify(sellerApiClient, times(1)).listAdsCampaigns(anyString(), anyString(), anyString());
        verify(campaignMapper, times(1)).upsert(any());
        verify(opsService, times(1)).recordOperationAudit(any());
    }

    @Test
    @DisplayName("同步广告活动 - 空结果")
    void testSyncCampaignsFromApi_EmptyResult() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        result.put("campaigns", new JSONArray());
        response.put("result", result);

        when(sellerApiClient.listAdsCampaigns(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());

        // Act
        List<OzonAdsCampaign> actualResult = adsService.syncCampaignsFromApi(testUser, authId);

        // Assert
        assertNotNull(actualResult);
        assertTrue(actualResult.isEmpty());

        verify(sellerApiClient, times(1)).listAdsCampaigns(anyString(), anyString(), anyString());
        verify(campaignMapper, never()).upsert(any());
        verify(opsService, times(1)).recordOperationAudit(any());
    }

    @Test
    @DisplayName("同步广告活动 - 权限验证失败")
    void testSyncCampaignsFromApi_AuthFailed() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId))
                .thenThrow(new IllegalArgumentException("Ozon授权不存在"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adsService.syncCampaignsFromApi(testUser, authId);
        });

        verify(sellerApiClient, never()).listAdsCampaigns(anyString(), anyString(), anyString());
        verify(campaignMapper, never()).upsert(any());
    }

    @Test
    @DisplayName("同步广告活动 - API 调用失败")
    void testSyncCampaignsFromApi_ApiFailed() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");
        when(sellerApiClient.listAdsCampaigns(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("API调用失败"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            adsService.syncCampaignsFromApi(testUser, authId);
        });

        verify(campaignMapper, never()).upsert(any());
        verify(opsService, times(1)).recordOperationAudit(any());
    }

    @Test
    @DisplayName("同步广告活动 - 跳过已存在的活动")
    void testSyncCampaignsFromApi_SkipExisting() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        JSONArray campaigns = new JSONArray();
        JSONObject campaign1 = new JSONObject();
        campaign1.put("campaign_id", "camp-1");
        campaign1.put("campaign_name", "Existing Campaign");
        campaigns.add(campaign1);
        result.put("campaigns", campaigns);
        response.put("result", result);

        when(sellerApiClient.listAdsCampaigns(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());

        OzonAdsCampaign existingCampaign = new OzonAdsCampaign();
        existingCampaign.setCampaignId("camp-1");
        when(campaignMapper.selectOne(any())).thenReturn(existingCampaign);

        // Act
        List<OzonAdsCampaign> actualResult = adsService.syncCampaignsFromApi(testUser, authId);

        // Assert
        assertNotNull(actualResult);
        assertTrue(actualResult.isEmpty());

        verify(campaignMapper, never()).upsert(any());
    }

    @Test
    @DisplayName("同步广告报告 - 成功场景")
    void testSyncReportsFromApi_Success() {
        // Arrange
        String authId = "auth-123";
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        JSONArray reports = new JSONArray();
        JSONObject report1 = new JSONObject();
        report1.put("campaign_id", "camp-1");
        report1.put("date", "2026-03-15");
        report1.put("impressions", "1000");
        report1.put("clicks", "50");
        report1.put("spend", "120.5");
        report1.put("orders", "5");
        report1.put("sales", "800");
        reports.add(report1);
        result.put("reports", reports);
        result.put("account_id", "acc-1");
        response.put("result", result);

        when(sellerApiClient.getAdsReport(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());
        when(reportMapper.selectOne(any())).thenReturn(null);
        when(reportMapper.upsert(any())).thenReturn(1);

        // Act
        List<OzonAdsReport> actualResult = adsService.syncReportsFromApi(testUser, authId, startDate, endDate);

        // Assert
        assertNotNull(actualResult);
        assertEquals(1, actualResult.size());
        assertEquals("camp-1", actualResult.get(0).getCampaignId());
        assertEquals(1000L, actualResult.get(0).getImpressions());

        verify(sellerApiClient, times(1)).getAdsReport(anyString(), anyString(), anyString());
        verify(reportMapper, times(1)).upsert(any());
        verify(opsService, times(1)).recordOperationAudit(any());
    }

    @Test
    @DisplayName("同步广告报告 - 日期参数为空")
    void testSyncReportsFromApi_NullDates() {
        // Arrange
        String authId = "auth-123";
        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            adsService.syncReportsFromApi(testUser, authId, null, null);
        });

        verify(sellerApiClient, never()).getAdsReport(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("同步广告报告 - 空结果")
    void testSyncReportsFromApi_EmptyResult() {
        // Arrange
        String authId = "auth-123";
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        result.put("reports", new JSONArray());
        response.put("result", result);

        when(sellerApiClient.getAdsReport(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());

        // Act
        List<OzonAdsReport> actualResult = adsService.syncReportsFromApi(testUser, authId, startDate, endDate);

        // Assert
        assertNotNull(actualResult);
        assertTrue(actualResult.isEmpty());

        verify(reportMapper, never()).upsert(any());
    }

    @Test
    @DisplayName("同步广告报告 - API 调用失败")
    void testSyncReportsFromApi_ApiFailed() {
        // Arrange
        String authId = "auth-123";
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");
        when(sellerApiClient.getAdsReport(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("API调用失败"));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            adsService.syncReportsFromApi(testUser, authId, startDate, endDate);
        });

        verify(reportMapper, never()).upsert(any());
        verify(opsService, times(1)).recordOperationAudit(any());
    }

    @Test
    @DisplayName("同步广告报告 - 跳过已存在的报告")
    void testSyncReportsFromApi_SkipExisting() {
        // Arrange
        String authId = "auth-123";
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        when(authAccessService.requireOwnedAuth(testUser, authId)).thenReturn(testAuth);
        when(credentialService.decrypt("encrypted-key")).thenReturn("decrypted-key");

        JSONObject response = new JSONObject();
        JSONObject result = new JSONObject();
        JSONArray reports = new JSONArray();
        JSONObject report1 = new JSONObject();
        report1.put("campaign_id", "camp-1");
        report1.put("date", "2026-03-15");
        reports.add(report1);
        result.put("reports", reports);
        response.put("result", result);

        when(sellerApiClient.getAdsReport(anyString(), anyString(), anyString()))
                .thenReturn(response.toJSONString());

        OzonAdsReport existingReport = new OzonAdsReport();
        existingReport.setCampaignId("camp-1");
        when(reportMapper.selectOne(any())).thenReturn(existingReport);

        // Act
        List<OzonAdsReport> actualResult = adsService.syncReportsFromApi(testUser, authId, startDate, endDate);

        // Assert
        assertNotNull(actualResult);
        assertTrue(actualResult.isEmpty());

        verify(reportMapper, never()).upsert(any());
    }
}

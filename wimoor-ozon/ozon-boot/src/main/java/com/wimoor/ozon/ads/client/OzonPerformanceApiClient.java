package com.wimoor.ozon.ads.client;

import com.wimoor.ozon.ops.annotation.OzonApiLog;

public interface OzonPerformanceApiClient {

    @OzonApiLog(apiGroup = "Ads", actionName = "listCampaigns", objectType = "Campaign")
    String listCampaigns(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Ads", actionName = "getReport", objectType = "AdsReport")
    String getReport(String clientId, String apiKey, String payload);
}

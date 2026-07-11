package com.wimoor.ozon.ads.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wimoor.common.HttpClientUtil;

import cn.hutool.core.util.StrUtil;

@Component
public class DefaultOzonPerformanceApiClient implements OzonPerformanceApiClient {

    private static final String CAMPAIGN_LIST_PATH = "/v1/performance/campaign/list";
    private static final String REPORT_PATH = "/v1/performance/report";

    private final String baseUrl;

    public DefaultOzonPerformanceApiClient(
            @Value("${ozon.api.performance-base-url:https://api-performance.ozon.ru}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String listCampaigns(String clientId, String apiKey, String payload) {
        return postJson(CAMPAIGN_LIST_PATH, payload, clientId, apiKey);
    }

    @Override
    public String getReport(String clientId, String apiKey, String payload) {
        return postJson(REPORT_PATH, payload, clientId, apiKey);
    }

    private String postJson(String path, String payload, String clientId, String apiKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Client-Id", clientId);
        headers.put("Api-Key", apiKey);
        headers.put("Content-Type", "application/json");
        try {
            return HttpClientUtil.postUrl(StrUtil.removeSuffix(baseUrl, "/") + path, payload, headers);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}

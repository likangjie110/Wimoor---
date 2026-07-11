package com.wimoor.ozon.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wimoor.common.HttpClientUtil;

import cn.hutool.core.util.StrUtil;

@Component
public class DefaultOzonSellerApiClient implements OzonSellerApiClient {

    private static final String WAREHOUSE_PATH = "/v1/warehouse/list";
    private static final String FBS_POSTING_LIST_PATH = "/v3/posting/fbs/list";
    private static final String PRICE_IMPORT_PATH = "/v1/product/import/prices";
    private static final String STOCK_IMPORT_PATH = "/v2/products/stocks";
    private static final String TRACKING_NUMBER_SET_PATH = "/v2/fbs/posting/tracking-number/set";
    private static final String CHAT_SEND_MESSAGE_PATH = "/v1/chat/send/message";
    private static final String POSTING_CANCEL_PATH = "/v2/posting/fbs/cancel";
    private static final String RETURNS_LIST_PATH = "/v3/returns/company/fbs";
    private static final String POSTING_PACKAGES_PATH = "/v1/posting/fbs/package";
    private static final String FINANCE_TRANSACTION_LIST_PATH = "/v2/finance/transaction/list";
    private static final String FINANCE_REALIZATION_LIST_PATH = "/v2/finance/realization/product/list";
    private static final String FINANCE_REPORT_INFO_PATH = "/v1/report/info";
    private static final String CHAT_LIST_PATH = "/v1/chat/list";
    private static final String CHAT_HISTORY_PATH = "/v1/chat/history";
    private static final String ADS_CAMPAIGN_LIST_PATH = "/v1/performance/campaign/list";
    private static final String ADS_REPORT_PATH = "/v1/performance/report";

    private final String baseUrl;

    public DefaultOzonSellerApiClient(@Value("${ozon.api.base-url:https://api-seller.ozon.ru}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public OzonConnectionStatus ping(String clientId, String apiKey) {
        try {
            listWarehouses(clientId, apiKey);
            return OzonConnectionStatus.success("connected");
        } catch (RuntimeException ex) {
            return OzonConnectionStatus.failure(ex.getMessage());
        }
    }

    @Override
    public List<OzonRemoteWarehouse> listWarehouses(String clientId, String apiKey) {
        try {
            String response = postJson(WAREHOUSE_PATH, "{}", clientId, apiKey);
            if (StrUtil.isBlank(response)) {
                return Collections.emptyList();
            }
            JSONObject payload = JSONObject.parseObject(response);
            JSONArray result = payload.getJSONArray("result");
            if (result == null) {
                return Collections.emptyList();
            }
            List<OzonRemoteWarehouse> warehouses = new ArrayList<>(result.size());
            for (int index = 0; index < result.size(); index++) {
                JSONObject item = result.getJSONObject(index);
                warehouses.add(new OzonRemoteWarehouse(
                        item.getLong("warehouse_id"),
                        item.getString("name"),
                        item.getString("status"),
                        item.getString("type")
                ));
            }
            return warehouses;
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String importPrices(String clientId, String apiKey, String payload) {
        try {
            return postJson(PRICE_IMPORT_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listFbsPostings(String clientId, String apiKey, String payload) {
        try {
            return postJson(FBS_POSTING_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String updateStocks(String clientId, String apiKey, String payload) {
        try {
            return postJson(STOCK_IMPORT_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String setTrackingNumber(String clientId, String apiKey, String payload) {
        try {
            return postJson(TRACKING_NUMBER_SET_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String sendChatMessage(String clientId, String apiKey, String payload) {
        try {
            return postJson(CHAT_SEND_MESSAGE_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String cancelPosting(String clientId, String apiKey, String payload) {
        try {
            return postJson(POSTING_CANCEL_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listReturns(String clientId, String apiKey, String payload) {
        try {
            return postJson(RETURNS_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getPostingPackages(String clientId, String apiKey, String payload) {
        try {
            return postJson(POSTING_PACKAGES_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listFinanceTransactions(String clientId, String apiKey, String payload) {
        try {
            return postJson(FINANCE_TRANSACTION_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listFinanceRealizations(String clientId, String apiKey, String payload) {
        try {
            return postJson(FINANCE_REALIZATION_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getFinanceReportInfo(String clientId, String apiKey, String payload) {
        try {
            return postJson(FINANCE_REPORT_INFO_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listChats(String clientId, String apiKey, String payload) {
        try {
            return postJson(CHAT_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getChatHistory(String clientId, String apiKey, String payload) {
        try {
            return postJson(CHAT_HISTORY_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String listAdsCampaigns(String clientId, String apiKey, String payload) {
        try {
            return postJson(ADS_CAMPAIGN_LIST_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getAdsReport(String clientId, String apiKey, String payload) {
        try {
            return postJson(ADS_REPORT_PATH, payload, clientId, apiKey);
        } catch (HttpException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private Map<String, String> buildHeaders(String clientId, String apiKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Client-Id", clientId);
        headers.put("Api-Key", apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    private String postJson(String path, String payload, String clientId, String apiKey) throws HttpException {
        return HttpClientUtil.postUrl(buildUrl(path), payload, buildHeaders(clientId, apiKey));
    }

    private String buildUrl(String path) {
        return StrUtil.removeSuffix(baseUrl, "/") + path;
    }
}

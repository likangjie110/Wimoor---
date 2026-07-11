package com.wimoor.ozon.client;

import java.util.List;

import com.wimoor.ozon.ops.annotation.OzonApiLog;

public interface OzonSellerApiClient {

    OzonConnectionStatus ping(String clientId, String apiKey);

    @OzonApiLog(apiGroup = "Warehouse", actionName = "listWarehouses", objectType = "Warehouse")
    List<OzonRemoteWarehouse> listWarehouses(String clientId, String apiKey);

    @OzonApiLog(apiGroup = "Posting", actionName = "listFbsPostings", objectType = "Posting")
    String listFbsPostings(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Price", actionName = "importPrices", objectType = "Price")
    String importPrices(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Stock", actionName = "updateStocks", objectType = "Stock")
    String updateStocks(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Posting", actionName = "setTrackingNumber", objectType = "Posting")
    String setTrackingNumber(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Chat", actionName = "sendChatMessage", objectType = "Chat")
    String sendChatMessage(String clientId, String apiKey, String payload);

    // AfterSale APIs
    @OzonApiLog(apiGroup = "Posting", actionName = "cancelPosting", objectType = "Posting")
    String cancelPosting(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "AfterSale", actionName = "listReturns", objectType = "Return")
    String listReturns(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Posting", actionName = "getPostingPackages", objectType = "Package")
    String getPostingPackages(String clientId, String apiKey, String payload);

    // Finance APIs
    @OzonApiLog(apiGroup = "Finance", actionName = "listTransactions", objectType = "Transaction")
    String listFinanceTransactions(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Finance", actionName = "listRealizations", objectType = "Realization")
    String listFinanceRealizations(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Finance", actionName = "getReportInfo", objectType = "Report")
    String getFinanceReportInfo(String clientId, String apiKey, String payload);

    // Chat APIs
    @OzonApiLog(apiGroup = "Chat", actionName = "listChats", objectType = "Chat")
    String listChats(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Chat", actionName = "getChatHistory", objectType = "Chat")
    String getChatHistory(String clientId, String apiKey, String payload);

    // Ads APIs
    @OzonApiLog(apiGroup = "Ads", actionName = "listCampaigns", objectType = "Campaign")
    String listAdsCampaigns(String clientId, String apiKey, String payload);

    @OzonApiLog(apiGroup = "Ads", actionName = "getAdsReport", objectType = "AdsReport")
    String getAdsReport(String clientId, String apiKey, String payload);
}

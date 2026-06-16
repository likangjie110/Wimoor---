package com.wimoor.ozon.client;

import java.util.List;

public interface OzonSellerApiClient {

    OzonConnectionStatus ping(String clientId, String apiKey);

    List<OzonRemoteWarehouse> listWarehouses(String clientId, String apiKey);

    String listFbsPostings(String clientId, String apiKey, String payload);

    String importPrices(String clientId, String apiKey, String payload);

    String updateStocks(String clientId, String apiKey, String payload);

    String setTrackingNumber(String clientId, String apiKey, String payload);

    String sendChatMessage(String clientId, String apiKey, String payload);
}

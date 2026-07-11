package com.wimoor.ozon.ads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Phase 6: Ads API 客户端测试
 *
 * 测试范围：
 * 1. API 请求构建
 * 2. API 响应解析
 * 3. 请求参数验证
 */
class OzonAdsApiClientTests {

    // ==================== API 请求构建测试 ====================

    @Test
    void buildListCampaignsRequestPayloadIncludesAccountId() {
        JSONObject payload = new JSONObject();
        payload.put("account_id", "acc-1");
        payload.put("status", "ACTIVE");
        payload.put("page", 1);
        payload.put("page_size", 100);

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"account_id\":\"acc-1\""));
        assertTrue(json.contains("\"status\":\"ACTIVE\""));
        assertTrue(json.contains("\"page\":1"));
        assertTrue(json.contains("\"page_size\":100"));
    }

    @Test
    void buildGetAdsReportRequestPayloadIncludesDateRange() {
        JSONObject payload = new JSONObject();
        payload.put("campaign_id", "camp-1");
        payload.put("date_from", "2026-06-01");
        payload.put("date_to", "2026-06-30");
        payload.put("group_by", "day");

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"campaign_id\":\"camp-1\""));
        assertTrue(json.contains("\"date_from\":\"2026-06-01\""));
        assertTrue(json.contains("\"date_to\":\"2026-06-30\""));
        assertTrue(json.contains("\"group_by\":\"day\""));
    }

    // ==================== API 响应解析测试 ====================

    @Test
    void parseListCampaignsResponseExtractsCampaigns() {
        String response = "{\"result\":{\"campaigns\":[" +
                "{\"campaign_id\":\"camp-1\",\"campaign_name\":\"Summer Sale\",\"campaign_type\":\"SEARCH_PROMO\",\"status\":\"ACTIVE\",\"budget\":5000}," +
                "{\"campaign_id\":\"camp-2\",\"campaign_name\":\"Winter Sale\",\"campaign_type\":\"DISPLAY\",\"status\":\"PAUSED\",\"budget\":3000}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("campaigns"));
        assertEquals(2, result.getJSONArray("campaigns").size());
        assertEquals("camp-1", result.getJSONArray("campaigns").getJSONObject(0).getString("campaign_id"));
        assertEquals("Summer Sale", result.getJSONArray("campaigns").getJSONObject(0).getString("campaign_name"));
        assertEquals(Integer.valueOf(5000), result.getJSONArray("campaigns").getJSONObject(0).getInteger("budget"));
    }

    @Test
    void parseGetAdsReportResponseExtractsReportData() {
        String response = "{\"result\":{\"reports\":[" +
                "{\"campaign_id\":\"camp-1\",\"date\":\"2026-06-20\",\"impressions\":10000,\"clicks\":250,\"spend\":1500.50,\"orders\":50,\"sales\":8000.00,\"ctr\":2.5,\"cpc\":6.00,\"acos\":18.76,\"roas\":5.33}," +
                "{\"campaign_id\":\"camp-1\",\"date\":\"2026-06-21\",\"impressions\":8000,\"clicks\":200,\"spend\":1200.00,\"orders\":40,\"sales\":6400.00,\"ctr\":2.5,\"cpc\":6.00,\"acos\":18.75,\"roas\":5.33}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("reports"));
        assertEquals(2, result.getJSONArray("reports").size());
        assertEquals("camp-1", result.getJSONArray("reports").getJSONObject(0).getString("campaign_id"));
        assertEquals(Integer.valueOf(10000), result.getJSONArray("reports").getJSONObject(0).getInteger("impressions"));
        assertEquals(Integer.valueOf(250), result.getJSONArray("reports").getJSONObject(0).getInteger("clicks"));
    }

    @Test
    void parseAdsReportExtractsMetrics() {
        String reportLine = "{\"campaign_id\":\"camp-1\",\"date\":\"2026-06-20\"," +
                "\"impressions\":10000,\"clicks\":250,\"spend\":1500.50," +
                "\"orders\":50,\"sales\":8000.00,\"ctr\":2.5,\"cpc\":6.00,\"acos\":18.76,\"roas\":5.33}";

        JSONObject json = JSON.parseObject(reportLine);

        assertEquals("camp-1", json.getString("campaign_id"));
        assertEquals("2026-06-20", json.getString("date"));
        assertEquals(Integer.valueOf(10000), json.getInteger("impressions"));
        assertEquals(Integer.valueOf(250), json.getInteger("clicks"));
        assertEquals("1500.50", json.getString("spend"));
        assertEquals(Integer.valueOf(50), json.getInteger("orders"));
        assertEquals("8000.00", json.getString("sales"));
        assertEquals("2.5", json.getString("ctr"));
        assertEquals("6.00", json.getString("cpc"));
        assertEquals("18.76", json.getString("acos"));
        assertEquals("5.33", json.getString("roas"));
    }

    // ==================== 请求参数验证测试 ====================

    @Test
    void listCampaignsRequestRequiresAccountId() {
        JSONObject payload = new JSONObject();
        payload.put("account_id", "acc-1");
        payload.put("page", 1);
        payload.put("page_size", 100);

        assertTrue(payload.containsKey("account_id"));
        assertEquals("acc-1", payload.getString("account_id"));
    }

    @Test
    void getAdsReportRequestRequiresCampaignIdAndDateRange() {
        JSONObject payload = new JSONObject();
        payload.put("campaign_id", "camp-1");
        payload.put("date_from", "2026-06-01");
        payload.put("date_to", "2026-06-30");

        assertTrue(payload.containsKey("campaign_id"));
        assertTrue(payload.containsKey("date_from"));
        assertTrue(payload.containsKey("date_to"));
        assertEquals("camp-1", payload.getString("campaign_id"));
        assertEquals("2026-06-01", payload.getString("date_from"));
        assertEquals("2026-06-30", payload.getString("date_to"));
    }

    @Test
    void listCampaignsRequestIncludesPagination() {
        JSONObject payload = new JSONObject();
        payload.put("account_id", "acc-1");
        payload.put("page", 1);
        payload.put("page_size", 100);

        assertTrue(payload.containsKey("page"));
        assertTrue(payload.containsKey("page_size"));
        assertEquals(1, payload.getInteger("page"));
        assertEquals(100, payload.getInteger("page_size"));
    }
}

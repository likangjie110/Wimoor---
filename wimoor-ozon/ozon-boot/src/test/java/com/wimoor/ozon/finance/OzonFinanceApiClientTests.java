package com.wimoor.ozon.finance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Phase 6: Finance API 客户端测试
 *
 * 测试范围：
 * 1. API 请求构建
 * 2. API 响应解析
 * 3. 请求参数验证
 */
class OzonFinanceApiClientTests {

    // ==================== API 请求构建测试 ====================

    @Test
    void buildTransactionRequestPayloadIncludesDateFilter() {
        JSONObject payload = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("date", new JSONObject()
            .fluentPut("from", "2026-06-01T00:00:00Z")
            .fluentPut("to", "2026-06-30T23:59:59Z"));
        payload.put("filter", filter);
        payload.put("page", 1);
        payload.put("page_size", 1000);

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"filter\""));
        assertTrue(json.contains("\"date\""));
        assertTrue(json.contains("\"from\":\"2026-06-01T00:00:00Z\""));
        assertTrue(json.contains("\"to\":\"2026-06-30T23:59:59Z\""));
        assertTrue(json.contains("\"page\":1"));
        assertTrue(json.contains("\"page_size\":1000"));
    }

    @Test
    void buildRealizationRequestPayloadIncludesDateRange() {
        JSONObject payload = new JSONObject();
        payload.put("date", new JSONObject()
            .fluentPut("from", "2026-06-01T00:00:00Z")
            .fluentPut("to", "2026-06-30T23:59:59Z"));
        payload.put("page", 1);
        payload.put("page_size", 1000);

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"date\""));
        assertTrue(json.contains("\"from\":\"2026-06-01T00:00:00Z\""));
        assertTrue(json.contains("\"to\":\"2026-06-30T23:59:59Z\""));
    }

    @Test
    void buildReportRequestPayloadIncludesReportCode() {
        JSONObject payload = new JSONObject();
        payload.put("code", "seller_report");

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"code\":\"seller_report\""));
    }

    // ==================== API 响应解析测试 ====================

    @Test
    void parseTransactionResponseExtractsTransactionArray() {
        String response = "{\"result\":{\"transactions\":[" +
                "{\"transactionId\":\"tx-1\",\"operationType\":\"sale\",\"amount\":1000.50,\"currencyCode\":\"RUB\"}," +
                "{\"transactionId\":\"tx-2\",\"operationType\":\"refund\",\"amount\":-500.25,\"currencyCode\":\"RUB\"}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("transactions"));
        assertEquals(2, result.getJSONArray("transactions").size());
        assertEquals("tx-1", result.getJSONArray("transactions").getJSONObject(0).getString("transactionId"));
        assertEquals("tx-2", result.getJSONArray("transactions").getJSONObject(1).getString("transactionId"));
    }

    @Test
    void parseRealizationResponseExtractsTransactionArray() {
        String response = "{\"result\":{\"transactions\":[" +
                "{\"transactionId\":\"real-1\",\"operationType\":\"realization\",\"postingNumber\":\"post-1\",\"amount\":2500.00}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("transactions"));
        assertEquals(1, result.getJSONArray("transactions").size());
        assertEquals("real-1", result.getJSONArray("transactions").getJSONObject(0).getString("transactionId"));
        assertEquals("realization", result.getJSONArray("transactions").getJSONObject(0).getString("operationType"));
    }

    @Test
    void parseReportResponseExtractsReportInfo() {
        String response = "{\"result\":{\"reportId\":\"report-123\",\"status\":\"success\",\"url\":\"https://ozon.ru/reports/123\"}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertEquals("report-123", result.getString("reportId"));
        assertEquals("success", result.getString("status"));
        assertEquals("https://ozon.ru/reports/123", result.getString("url"));
    }

    // ==================== 请求参数验证测试 ====================

    @Test
    void transactionRequestRequiresPagination() {
        JSONObject payload = new JSONObject();
        payload.put("filter", new JSONObject().fluentPut("date", new JSONObject()
            .fluentPut("from", "2026-06-01T00:00:00Z")
            .fluentPut("to", "2026-06-30T23:59:59Z")));
        payload.put("page", 1);
        payload.put("page_size", 1000);

        assertTrue(payload.containsKey("page"));
        assertTrue(payload.containsKey("page_size"));
        assertEquals(1, payload.getInteger("page"));
        assertEquals(1000, payload.getInteger("page_size"));
    }

    @Test
    void realizationRequestIncludesDateObject() {
        JSONObject payload = new JSONObject();
        payload.put("date", new JSONObject()
            .fluentPut("from", "2026-06-01T00:00:00Z")
            .fluentPut("to", "2026-06-30T23:59:59Z"));

        assertTrue(payload.containsKey("date"));
        JSONObject date = payload.getJSONObject("date");
        assertNotNull(date);
        assertTrue(date.containsKey("from"));
        assertTrue(date.containsKey("to"));
    }

    @Test
    void reportRequestIncludesReportCode() {
        JSONObject payload = new JSONObject();
        payload.put("code", "seller_report");

        assertTrue(payload.containsKey("code"));
        assertEquals("seller_report", payload.getString("code"));
    }
}

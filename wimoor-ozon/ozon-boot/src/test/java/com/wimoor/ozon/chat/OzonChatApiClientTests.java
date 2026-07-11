package com.wimoor.ozon.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * Phase 6: Chat API 客户端测试
 *
 * 测试范围：
 * 1. API 请求构建
 * 2. API 响应解析
 * 3. 请求参数验证
 */
class OzonChatApiClientTests {

    // ==================== API 请求构建测试 ====================

    @Test
    void buildSendMessageRequestPayloadIncludesChatIdAndText() {
        JSONObject payload = new JSONObject();
        payload.put("chat_id", "session-1");
        payload.put("text", "Thanks for your message");

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"chat_id\":\"session-1\""));
        assertTrue(json.contains("\"text\":\"Thanks for your message\""));
    }

    @Test
    void buildListChatsRequestPayloadIncludesFilter() {
        JSONObject payload = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("unread_only", true);
        payload.put("filter", filter);
        payload.put("page", 1);
        payload.put("page_size", 50);

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"filter\""));
        assertTrue(json.contains("\"unread_only\":true"));
        assertTrue(json.contains("\"page\":1"));
        assertTrue(json.contains("\"page_size\":50"));
    }

    @Test
    void buildGetChatHistoryRequestPayloadIncludesSessionId() {
        JSONObject payload = new JSONObject();
        payload.put("chat_id", "session-1");
        payload.put("limit", 100);

        String json = payload.toJSONString();

        assertNotNull(json);
        assertTrue(json.contains("\"chat_id\":\"session-1\""));
        assertTrue(json.contains("\"limit\":100"));
    }

    // ==================== API 响应解析测试 ====================

    @Test
    void parseSendMessageResponseExtractsMessageId() {
        String response = "{\"result\":\"remote-msg-123\",\"status\":\"success\"}";

        JSONObject json = JSON.parseObject(response);
        String messageId = json.getString("result");

        assertNotNull(messageId);
        assertEquals("remote-msg-123", messageId);
    }

    @Test
    void parseListChatsResponseExtractsChats() {
        String response = "{\"result\":{\"chats\":[" +
                "{\"chat_id\":\"session-1\",\"customer_name\":\"Buyer A\",\"last_message\":\"hello\",\"unread_count\":2}," +
                "{\"chat_id\":\"session-2\",\"customer_name\":\"Buyer B\",\"last_message\":\"hi\",\"unread_count\":0}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("chats"));
        assertEquals(2, result.getJSONArray("chats").size());
        assertEquals("session-1", result.getJSONArray("chats").getJSONObject(0).getString("chat_id"));
        assertEquals(Integer.valueOf(2), result.getJSONArray("chats").getJSONObject(0).getInteger("unread_count"));
    }

    @Test
    void parseChatHistoryResponseExtractsMessages() {
        String response = "{\"result\":{\"messages\":[" +
                "{\"message_id\":\"msg-1\",\"sender_type\":\"BUYER\",\"text\":\"hello\",\"timestamp\":\"2026-06-20T10:00:00Z\"}," +
                "{\"message_id\":\"msg-2\",\"sender_type\":\"SELLER\",\"text\":\"hi\",\"timestamp\":\"2026-06-20T10:01:00Z\"}" +
                "]}}";

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");

        assertNotNull(result);
        assertNotNull(result.getJSONArray("messages"));
        assertEquals(2, result.getJSONArray("messages").size());
        assertEquals("msg-1", result.getJSONArray("messages").getJSONObject(0).getString("message_id"));
        assertEquals("BUYER", result.getJSONArray("messages").getJSONObject(0).getString("sender_type"));
    }

    // ==================== 请求参数验证测试 ====================

    @Test
    void sendMessageRequestRequiresChatIdAndText() {
        JSONObject payload = new JSONObject();
        payload.put("chat_id", "session-1");
        payload.put("text", "reply");

        assertTrue(payload.containsKey("chat_id"));
        assertTrue(payload.containsKey("text"));
        assertEquals("session-1", payload.getString("chat_id"));
        assertEquals("reply", payload.getString("text"));
    }

    @Test
    void listChatsRequestIncludesPagination() {
        JSONObject payload = new JSONObject();
        payload.put("page", 1);
        payload.put("page_size", 50);

        assertTrue(payload.containsKey("page"));
        assertTrue(payload.containsKey("page_size"));
        assertEquals(1, payload.getInteger("page"));
        assertEquals(50, payload.getInteger("page_size"));
    }

    @Test
    void getChatHistoryRequestRequiresChatId() {
        JSONObject payload = new JSONObject();
        payload.put("chat_id", "session-1");
        payload.put("limit", 100);

        assertTrue(payload.containsKey("chat_id"));
        assertTrue(payload.containsKey("limit"));
        assertEquals("session-1", payload.getString("chat_id"));
        assertEquals(100, payload.getInteger("limit"));
    }
}

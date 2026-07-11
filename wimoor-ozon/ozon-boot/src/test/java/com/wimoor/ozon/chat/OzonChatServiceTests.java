package com.wimoor.ozon.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.wimoor.ozon.auth.mapper.OzonAuthMapper;
import com.wimoor.ozon.auth.pojo.entity.OzonAuth;
import com.wimoor.ozon.auth.service.OzonAuthAccessService;
import com.wimoor.ozon.chat.mapper.OzonChatMessageMapper;
import com.wimoor.ozon.chat.mapper.OzonChatReplyAuditMapper;
import com.wimoor.ozon.chat.mapper.OzonChatSessionMapper;
import com.wimoor.ozon.chat.pojo.dto.OzonChatImportCommand;
import com.wimoor.ozon.chat.pojo.dto.OzonChatMessageQuery;
import com.wimoor.ozon.chat.pojo.dto.OzonChatReplyRecordCommand;
import com.wimoor.ozon.chat.pojo.entity.OzonChatMessage;
import com.wimoor.ozon.chat.pojo.entity.OzonChatReplyAudit;
import com.wimoor.ozon.chat.pojo.entity.OzonChatSession;
import com.wimoor.ozon.chat.pojo.vo.OzonChatImportResult;
import com.wimoor.ozon.chat.service.impl.OzonChatServiceImpl;
import com.wimoor.ozon.client.OzonSellerApiClient;
import com.wimoor.ozon.security.OzonCredentialService;

/**
 * Phase 6: Chat 模块单元测试
 *
 * 测试范围：
 * 1. syncChatsFromApi - API 同步聊天
 * 2. syncMessagesFromApi - API 同步消息
 * 3. sendReplyWithApi - API 发送回复
 * 4. 功能开关控制
 * 5. 权限验证
 */
@ExtendWith(MockitoExtension.class)
class OzonChatServiceTests {

    @Mock
    private OzonAuthMapper authMapper;

    @Mock
    private OzonChatSessionMapper sessionMapper;

    @Mock
    private OzonChatMessageMapper messageMapper;

    @Mock
    private OzonChatReplyAuditMapper replyAuditMapper;

    @Mock
    private OzonSellerApiClient sellerApiClient;

    @Mock
    private OzonCredentialService credentialService;

    @Captor
    private ArgumentCaptor<OzonChatSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<OzonChatMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<OzonChatReplyAudit> replyAuditCaptor;

    private OzonChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OzonChatServiceImpl(
                new OzonAuthAccessService(authMapper),
                sessionMapper,
                messageMapper,
                replyAuditMapper,
                sellerApiClient,
                credentialService
        );
    }

    // ==================== importMessages 测试 ====================

    @Test
    void importMessagesCreatesSessions() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonChatImportResult result = service.importMessages(
                buildUser(),
                new OzonChatImportCommand(
                        "auth-1",
                        "{\"sessions\":[{\"sessionId\":\"session-1\",\"customerName\":\"Buyer A\",\"sessionStatus\":\"OPEN\","
                                + "\"messages\":["
                                + "{\"messageId\":\"msg-1\",\"senderType\":\"BUYER\",\"messageText\":\"hello\",\"messageTime\":\"2026-06-20T10:00:00Z\",\"read\":false}"
                                + "]}]}"
                )
        );

        assertEquals(1, result.getSessionCount());
        assertEquals(1, result.getMessageCount());
        verify(sessionMapper).upsert(sessionCaptor.capture());
        verify(messageMapper).upsert(messageCaptor.capture());

        assertEquals("session-1", sessionCaptor.getValue().getSessionId());
        assertEquals("Buyer A", sessionCaptor.getValue().getCustomerName());
        assertEquals("msg-1", messageCaptor.getValue().getMessageId());
    }

    @Test
    void importMessagesCalculatesUnreadCount() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonChatImportResult result = service.importMessages(
                buildUser(),
                new OzonChatImportCommand(
                        "auth-1",
                        "{\"sessions\":[{\"sessionId\":\"session-1\",\"customerName\":\"Buyer A\",\"sessionStatus\":\"OPEN\","
                                + "\"messages\":["
                                + "{\"messageId\":\"msg-1\",\"senderType\":\"BUYER\",\"messageText\":\"hello\",\"messageTime\":\"2026-06-20T10:00:00Z\",\"read\":false},"
                                + "{\"messageId\":\"msg-2\",\"senderType\":\"BUYER\",\"messageText\":\"world\",\"messageTime\":\"2026-06-20T10:01:00Z\",\"read\":false},"
                                + "{\"messageId\":\"msg-3\",\"senderType\":\"SELLER\",\"messageText\":\"hi\",\"messageTime\":\"2026-06-20T10:02:00Z\",\"read\":true}"
                                + "]}]}"
                )
        );

        assertEquals(1, result.getSessionCount());
        assertEquals(3, result.getMessageCount());
        verify(sessionMapper).upsert(sessionCaptor.capture());

        assertEquals(Integer.valueOf(2), sessionCaptor.getValue().getUnreadCount());
    }

    @Test
    void importMessagesRequiresRawContent() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.importMessages(buildUser(), new OzonChatImportCommand("auth-1", null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.importMessages(buildUser(), new OzonChatImportCommand("auth-1", "")));
    }

    @Test
    void importMessagesRequiresAuthPermission() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithDifferentShop());

        assertThrows(IllegalArgumentException.class, () ->
                service.importMessages(
                        buildUser(),
                        new OzonChatImportCommand("auth-1", "{\"sessions\":[]}")
                ));
    }

    // ==================== listSessions 测试 ====================

    @Test
    void listSessionsReturnsAllSessions() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatSession session = new OzonChatSession();
        session.setAuthId("auth-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        session.setUnreadCount(2);
        when(sessionMapper.selectList(any())).thenReturn(Collections.singletonList(session));

        List<OzonChatSession> sessions = service.listSessions(buildUser(), "auth-1", null, null, null);

        assertEquals(1, sessions.size());
        assertEquals("session-1", sessions.get(0).getSessionId());
    }

    @Test
    void listSessionsFiltersUnreadOnly() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(sessionMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<OzonChatSession> sessions = service.listSessions(buildUser(), "auth-1", true, null, null);

        assertEquals(0, sessions.size());
        verify(sessionMapper).selectList(any());
    }

    @Test
    void listSessionsFiltersKeyword() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(sessionMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.listSessions(buildUser(), "auth-1", null, "test", null);

        verify(sessionMapper).selectList(argThat(wrapper ->
                wrapper.getSqlSegment() != null && wrapper.getSqlSegment().contains("LIKE")));
    }

    // ==================== listMessages 测试 ====================

    @Test
    void listMessagesReturnsSessionMessages() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatMessage message = new OzonChatMessage();
        message.setAuthId("auth-1");
        message.setSessionId("session-1");
        message.setMessageId("msg-1");
        message.setSenderType("BUYER");
        when(messageMapper.selectList(any())).thenReturn(Collections.singletonList(message));

        List<OzonChatMessage> messages = service.listMessages(
                buildUser(),
                new OzonChatMessageQuery("auth-1", "session-1")
        );

        assertEquals(1, messages.size());
        assertEquals("msg-1", messages.get(0).getMessageId());
    }

    @Test
    void listMessagesRequiresSessionId() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.listMessages(buildUser(), new OzonChatMessageQuery("auth-1", null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.listMessages(buildUser(), new OzonChatMessageQuery("auth-1", "")));
    }

    // ==================== recordReply 测试 ====================

    @Test
    void recordReplyCreatesAuditRow() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatSession session = new OzonChatSession();
        session.setAuthId("auth-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        when(sessionMapper.selectOne(any())).thenReturn(session);

        OzonChatReplyAudit audit = service.recordReply(
                buildUser(),
                new OzonChatReplyRecordCommand("auth-1", "session-1", "Thanks for your message")
        );

        assertNotNull(audit);
        assertEquals("session-1", audit.getSessionId());
        assertEquals("RECORDED", audit.getReplyStatus());
        verify(replyAuditMapper).insert(replyAuditCaptor.capture());
        assertEquals("Thanks for your message", replyAuditCaptor.getValue().getReplyText());
    }

    @Test
    void recordReplyRequiresSessionExists() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        when(sessionMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.recordReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", "reply")
                ));
    }

    @Test
    void recordReplyRequiresReplyText() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        assertThrows(IllegalArgumentException.class, () ->
                service.recordReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", null)
                ));

        assertThrows(IllegalArgumentException.class, () ->
                service.recordReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", "")
                ));
    }

    // ==================== sendReply 测试 ====================

    @Test
    void sendReplyCallsApiAndSavesMessage() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithCredential());
        when(credentialService.decrypt("cipher")).thenReturn("plain-api-key");
        when(sellerApiClient.sendChatMessage(
                eq("client-1"),
                eq("plain-api-key"),
                argThat(payload -> payload.contains("\"chat_id\":\"session-1\"") && payload.contains("\"text\":\"Thanks\""))))
                .thenReturn("{\"result\":\"remote-msg-1\"}");

        OzonChatSession session = new OzonChatSession();
        session.setId("session-row-1");
        session.setAuthId("auth-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        session.setUnreadCount(1);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        OzonChatReplyAudit audit = service.sendReply(
                buildUser(),
                new OzonChatReplyRecordCommand("auth-1", "session-1", "Thanks")
        );

        assertEquals("SENT", audit.getReplyStatus());
        verify(replyAuditMapper).insert(replyAuditCaptor.capture());
        verify(messageMapper).upsert(messageCaptor.capture());
        verify(sessionMapper).upsert(sessionCaptor.capture());

        assertEquals("SELLER", messageCaptor.getValue().getSenderType());
        assertEquals("Thanks", messageCaptor.getValue().getMessageText());
        assertEquals("Thanks", sessionCaptor.getValue().getLastMessageText());
    }

    @Test
    void sendReplyFailsOnApiError() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithCredential());
        when(credentialService.decrypt("cipher")).thenReturn("plain-api-key");
        when(sellerApiClient.sendChatMessage(any(), any(), any()))
                .thenThrow(new RuntimeException("API connection failed"));

        OzonChatSession session = new OzonChatSession();
        session.setAuthId("auth-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        when(sessionMapper.selectOne(any())).thenReturn(session);

        assertThrows(IllegalStateException.class, () ->
                service.sendReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", "Thanks")
                ));
    }

    @Test
    void sendReplyRequiresSessionExists() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithCredential());
        when(sessionMapper.selectOne(any())).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
                service.sendReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", "reply")
                ));
    }

    @Test
    void sendReplyRequiresReplyText() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithCredential());

        assertThrows(IllegalArgumentException.class, () ->
                service.sendReply(
                        buildUser(),
                        new OzonChatReplyRecordCommand("auth-1", "session-1", null)
                ));
    }

    // ==================== listReplyAudits 测试 ====================

    @Test
    void listReplyAuditsReturnsRecentAudits() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatReplyAudit audit = new OzonChatReplyAudit();
        audit.setAuthId("auth-1");
        audit.setSessionId("session-1");
        audit.setReplyText("Thanks");
        audit.setReplyStatus("SENT");
        when(replyAuditMapper.selectList(any())).thenReturn(Collections.singletonList(audit));

        List<OzonChatReplyAudit> audits = service.listReplyAudits(buildUser(), "auth-1", "session-1");

        assertEquals(1, audits.size());
        assertEquals("Thanks", audits.get(0).getReplyText());
        assertEquals("SENT", audits.get(0).getReplyStatus());
    }

    // ==================== Helper Methods ====================

    private OzonAuth buildAuth() {
        OzonAuth auth = new OzonAuth();
        auth.setId("auth-1");
        auth.setShopId("company-1");
        auth.setStatus("ACTIVE");
        return auth;
    }

    private OzonAuth buildAuthWithCredential() {
        OzonAuth auth = buildAuth();
        auth.setClientId("client-1");
        auth.setApiKeyCiphertext("cipher");
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

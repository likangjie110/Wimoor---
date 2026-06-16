package com.wimoor.ozon.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.wimoor.ozon.client.OzonSellerApiClient;
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
import com.wimoor.ozon.security.OzonCredentialService;

@ExtendWith(MockitoExtension.class)
class OzonChatSyncTests {

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

    @Test
    void importCreatesSessionsMessagesAndUnreadCount() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());

        OzonChatImportResult result = service.importMessages(
                buildUser(),
                new OzonChatImportCommand(
                        "auth-1",
                        "{\"sessions\":[{\"sessionId\":\"session-1\",\"customerName\":\"Buyer A\",\"sessionStatus\":\"OPEN\","
                                + "\"messages\":["
                                + "{\"messageId\":\"msg-1\",\"senderType\":\"BUYER\",\"messageText\":\"hello\",\"messageTime\":\"2026-03-26T10:00:00Z\",\"read\":false},"
                                + "{\"messageId\":\"msg-2\",\"senderType\":\"SELLER\",\"messageText\":\"hi\",\"messageTime\":\"2026-03-26T10:01:00Z\",\"read\":true}"
                                + "]}]}"
                )
        );

        assertEquals(1, result.getSessionCount());
        assertEquals(2, result.getMessageCount());
        verify(sessionMapper).upsert(sessionCaptor.capture());
        verify(messageMapper, times(2)).upsert(messageCaptor.capture());
        assertEquals("session-1", sessionCaptor.getValue().getSessionId());
        assertEquals(Integer.valueOf(1), sessionCaptor.getValue().getUnreadCount());
        assertEquals("msg-2", messageCaptor.getAllValues().get(1).getMessageId());
    }

    @Test
    void listMessagesReturnsSessionMessages() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatMessage message = new OzonChatMessage();
        message.setAuthId("auth-1");
        message.setSessionId("session-1");
        message.setMessageId("msg-1");
        message.setSenderType("BUYER");
        message.setMessageText("hello");
        when(messageMapper.selectList(any())).thenReturn(Collections.singletonList(message));

        List<OzonChatMessage> messages = service.listMessages(
                buildUser(),
                new OzonChatMessageQuery("auth-1", "session-1")
        );

        assertEquals(1, messages.size());
        assertEquals("msg-1", messages.get(0).getMessageId());
    }

    @Test
    void listReplyAuditsReturnsRecentSessionRows() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatReplyAudit audit = new OzonChatReplyAudit();
        audit.setAuthId("auth-1");
        audit.setSessionId("session-1");
        audit.setReplyText("thanks");
        when(replyAuditMapper.selectList(any())).thenReturn(Collections.singletonList(audit));

        List<OzonChatReplyAudit> audits = service.listReplyAudits(buildUser(), "auth-1", "session-1");

        assertEquals(1, audits.size());
        assertEquals("thanks", audits.get(0).getReplyText());
    }

    @Test
    void recordReplyCreatesAuditRow() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuth());
        OzonChatSession session = new OzonChatSession();
        session.setAuthId("auth-1");
        session.setShopId("company-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        when(sessionMapper.selectOne(any())).thenReturn(session);

        OzonChatReplyAudit audit = service.recordReply(
                buildUser(),
                new OzonChatReplyRecordCommand("auth-1", "session-1", "thanks")
        );

        assertEquals("session-1", audit.getSessionId());
        assertEquals("RECORDED", audit.getReplyStatus());
        verify(replyAuditMapper).insert(replyAuditCaptor.capture());
        assertTrue(replyAuditCaptor.getValue().getReplyText().contains("thanks"));
    }

    @Test
    void sendReplyCallsRemoteApiAndPersistsSellerMessage() {
        when(authMapper.selectById("auth-1")).thenReturn(buildAuthWithCredential());
        when(credentialService.decrypt("cipher")).thenReturn("plain-api-key");
        when(sellerApiClient.sendChatMessage(
                org.mockito.ArgumentMatchers.eq("client-1"),
                org.mockito.ArgumentMatchers.eq("plain-api-key"),
                argThat(payload -> payload != null
                        && payload.contains("\"chat_id\":\"session-1\"")
                        && payload.contains("\"text\":\"thanks\""))))
                .thenReturn("{\"result\":\"remote-msg-1\"}");

        OzonChatSession session = new OzonChatSession();
        session.setId("session-row-1");
        session.setAuthId("auth-1");
        session.setShopId("company-1");
        session.setSessionId("session-1");
        session.setCustomerName("Buyer A");
        session.setSessionStatus("OPEN");
        session.setUnreadCount(1);
        when(sessionMapper.selectOne(any())).thenReturn(session);

        OzonChatReplyAudit audit = service.sendReply(
                buildUser(),
                new OzonChatReplyRecordCommand("auth-1", "session-1", "thanks")
        );

        assertEquals("SENT", audit.getReplyStatus());
        verify(replyAuditMapper).insert(replyAuditCaptor.capture());
        verify(messageMapper, times(1)).upsert(messageCaptor.capture());
        verify(sessionMapper, times(1)).upsert(sessionCaptor.capture());
        assertEquals("SELLER", messageCaptor.getValue().getSenderType());
        assertEquals("remote-msg-1", messageCaptor.getValue().getMessageId());
        assertEquals("thanks", sessionCaptor.getValue().getLastMessageText());
    }

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

    private UserInfo buildUser() {
        UserInfo user = new UserInfo();
        user.setId("tester");
        user.setCompanyid("company-1");
        return user;
    }
}

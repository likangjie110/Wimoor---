package com.wimoor.ozon.chat.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wimoor.common.user.UserInfo;
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
import com.wimoor.ozon.chat.service.IOzonChatService;
import com.wimoor.ozon.ops.pojo.dto.OzonApiLogRecordCommand;
import com.wimoor.ozon.ops.pojo.dto.OzonOperationAuditRecordCommand;
import com.wimoor.ozon.ops.service.IOzonOpsService;
import com.wimoor.ozon.security.OzonCredentialService;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class OzonChatServiceImpl implements IOzonChatService {

    private static final String BUYER = "BUYER";
    private static final String SELLER = "SELLER";
    private static final String RECORDED = "RECORDED";
    private static final String SENT = "SENT";
    private static final String FAILED = "FAILED";
    private static final String API_GROUP = "CHAT";
    private static final String CHAT_SEND_MESSAGE_ENDPOINT = "/v1/chat/send/message";

    private final OzonAuthAccessService authAccessService;
    private final OzonChatSessionMapper sessionMapper;
    private final OzonChatMessageMapper messageMapper;
    private final OzonChatReplyAuditMapper replyAuditMapper;
    private final OzonSellerApiClient sellerApiClient;
    private final OzonCredentialService credentialService;
    private IOzonOpsService opsService = new IOzonOpsService() {
    };

    public OzonChatServiceImpl(
            OzonAuthAccessService authAccessService,
            OzonChatSessionMapper sessionMapper,
            OzonChatMessageMapper messageMapper,
            OzonChatReplyAuditMapper replyAuditMapper,
            OzonSellerApiClient sellerApiClient,
            OzonCredentialService credentialService
    ) {
        this.authAccessService = authAccessService;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.replyAuditMapper = replyAuditMapper;
        this.sellerApiClient = sellerApiClient;
        this.credentialService = credentialService;
    }

    @Autowired(required = false)
    public void setOpsService(IOzonOpsService opsService) {
        if (opsService != null) {
            this.opsService = opsService;
        }
    }

    @Override
    public OzonChatImportResult importMessages(UserInfo user, OzonChatImportCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String rawContent = requireText(command == null ? null : command.getRawContent(), "rawContent不能为空");
        String auditPayload = JSON.toJSONString(command);
        try {
            JSONObject payload = JSON.parseObject(rawContent);
            JSONArray sessions = payload == null ? null : payload.getJSONArray("sessions");
            if (sessions == null || sessions.isEmpty()) {
                throw new IllegalArgumentException("未找到可导入的会话数据");
            }
            Date now = new Date();
            int sessionCount = 0;
            int messageCount = 0;
            for (int index = 0; index < sessions.size(); index++) {
                JSONObject sessionPayload = sessions.getJSONObject(index);
                String sessionId = requireText(firstText(sessionPayload, "sessionId", "session_id"), "sessionId不能为空");
                JSONArray messages = sessionPayload.getJSONArray("messages");
                List<OzonChatMessage> parsedMessages = parseMessages(auth, sessionId, messages, now);
                for (OzonChatMessage item : parsedMessages) {
                    messageMapper.upsert(item);
                    messageCount++;
                }
                sessionMapper.upsert(buildSession(auth, sessionPayload, sessionId, parsedMessages, now));
                sessionCount++;
            }
            OzonChatImportResult result = new OzonChatImportResult();
            result.setSessionCount(sessionCount);
            result.setMessageCount(messageCount);
            result.setImportedAt(now);
            recordOperationAudit(auth, user, "CHAT_IMPORT", auth.getId(), "IMPORT-" + sessionCount, auditPayload, "DONE", "sessions " + sessionCount + ", messages " + messageCount);
            return result;
        } catch (RuntimeException ex) {
            recordOperationAudit(auth, user, "CHAT_IMPORT", auth.getId(), "IMPORT", auditPayload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OzonChatSession> listSessions(UserInfo user, String authId, Boolean unreadOnly, String keyword, String sessionStatus) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonChatSession> wrapper = new QueryWrapper<OzonChatSession>().eq("auth_id", auth.getId());
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.gt("unread_count", 0);
        }
        if (StrUtil.isNotBlank(sessionStatus)) {
            wrapper.eq("session_status", sessionStatus.trim());
        }
        String cleanKeyword = trim(keyword);
        if (cleanKeyword != null) {
            wrapper.and(query -> query.like("customer_name", cleanKeyword)
                    .or().like("last_message_text", cleanKeyword)
                    .or().like("session_id", cleanKeyword));
        }
        wrapper.orderByDesc("last_message_at").orderByDesc("update_time").last("limit 100");
        List<OzonChatSession> sessions = sessionMapper.selectList(wrapper);
        return sessions == null ? Collections.emptyList() : sessions;
    }

    @Override
    public List<OzonChatMessage> listMessages(UserInfo user, OzonChatMessageQuery query) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, query == null ? null : query.getAuthId());
        String sessionId = requireText(query == null ? null : query.getSessionId(), "sessionId不能为空");
        List<OzonChatMessage> messages = messageMapper.selectList(new QueryWrapper<OzonChatMessage>()
                .eq("auth_id", auth.getId())
                .eq("session_id", sessionId)
                .orderByAsc("message_time")
                .orderByAsc("create_time")
                .last("limit 500"));
        return messages == null ? Collections.emptyList() : messages;
    }

    @Override
    public OzonChatReplyAudit recordReply(UserInfo user, OzonChatReplyRecordCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String sessionId = requireText(command == null ? null : command.getSessionId(), "sessionId不能为空");
        String replyText = requireText(command == null ? null : command.getReplyText(), "replyText不能为空");
        String auditPayload = JSON.toJSONString(command);
        try {
            OzonChatSession session = sessionMapper.selectOne(new QueryWrapper<OzonChatSession>()
                    .eq("auth_id", auth.getId())
                    .eq("session_id", sessionId)
                    .last("limit 1"));
            if (session == null) {
                throw new IllegalArgumentException("Ozon聊天会话不存在");
            }
            Date now = new Date();
            OzonChatReplyAudit audit = new OzonChatReplyAudit();
            audit.setId(nextId());
            audit.setAuthId(auth.getId());
            audit.setShopId(auth.getShopId());
            audit.setSessionId(sessionId);
            audit.setReplyText(replyText);
            audit.setReplyStatus(RECORDED);
            audit.setOperator(user == null ? null : user.getId());
            audit.setCreateTime(now);
            audit.setUpdateTime(now);
            replyAuditMapper.insert(audit);
            recordOperationAudit(auth, user, "CHAT_REPLY_RECORD", sessionId, session.getCustomerName(), auditPayload, RECORDED, "reply audit recorded");
            return audit;
        } catch (RuntimeException ex) {
            recordOperationAudit(auth, user, "CHAT_REPLY_RECORD", sessionId, sessionId, auditPayload, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public OzonChatReplyAudit sendReply(UserInfo user, OzonChatReplyRecordCommand command) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, command == null ? null : command.getAuthId());
        String sessionId = requireText(command == null ? null : command.getSessionId(), "sessionId不能为空");
        String replyText = requireText(command == null ? null : command.getReplyText(), "replyText不能为空");
        String auditPayload = JSON.toJSONString(command);
        OzonChatSession session = requireSession(auth, sessionId);
        JSONObject payload = new JSONObject();
        payload.put("chat_id", sessionId);
        payload.put("text", replyText);
        long startedAt = System.currentTimeMillis();
        try {
            String response = sellerApiClient.sendChatMessage(
                    auth.getClientId(),
                    credentialService.decrypt(auth.getApiKeyCiphertext()),
                    payload.toJSONString()
            );
            Date now = new Date();
            OzonChatReplyAudit audit = buildReplyAudit(auth, user, sessionId, replyText, SENT, now);
            replyAuditMapper.insert(audit);
            upsertSellerMessage(auth, session, replyText, response, now);
            refreshSessionAfterSellerReply(auth, session, replyText, now);
            recordApiLog(auth, user, sessionId, payload.toJSONString(), response, SENT, null, startedAt);
            recordOperationAudit(auth, user, "CHAT_REPLY_SEND", sessionId, session.getCustomerName(), auditPayload, SENT, "reply sent");
            return audit;
        } catch (RuntimeException ex) {
            String normalizedMessage = normalizeRemoteErrorMessage(ex.getMessage());
            recordApiLog(auth, user, sessionId, payload.toJSONString(), null, FAILED, normalizedMessage, startedAt);
            recordOperationAudit(auth, user, "CHAT_REPLY_SEND", sessionId, session.getCustomerName(), auditPayload, FAILED, normalizedMessage);
            throw new IllegalStateException(normalizedMessage, ex);
        }
    }

    @Override
    public List<OzonChatReplyAudit> listReplyAudits(UserInfo user, String authId, String sessionId) {
        OzonAuth auth = authAccessService.requireOwnedAuth(user, authId);
        QueryWrapper<OzonChatReplyAudit> wrapper = new QueryWrapper<OzonChatReplyAudit>().eq("auth_id", auth.getId());
        if (StrUtil.isNotBlank(sessionId)) {
            wrapper.eq("session_id", sessionId.trim());
        }
        wrapper.orderByDesc("create_time").last("limit 20");
        List<OzonChatReplyAudit> audits = replyAuditMapper.selectList(wrapper);
        return audits == null ? Collections.emptyList() : audits;
    }

    private List<OzonChatMessage> parseMessages(OzonAuth auth, String sessionId, JSONArray messages, Date now) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<OzonChatMessage> result = new ArrayList<>(messages.size());
        for (int index = 0; index < messages.size(); index++) {
            JSONObject item = messages.getJSONObject(index);
            String messageId = requireText(firstText(item, "messageId", "message_id"), "messageId不能为空");
            OzonChatMessage message = new OzonChatMessage();
            message.setId(nextId());
            message.setAuthId(auth.getId());
            message.setShopId(auth.getShopId());
            message.setSessionId(sessionId);
            message.setMessageId(messageId);
            message.setSenderType(StrUtil.blankToDefault(firstText(item, "senderType", "sender_type"), BUYER));
            message.setMessageText(firstText(item, "messageText", "message_text"));
            message.setMessageTime(parseDate(firstText(item, "messageTime", "message_time")));
            message.setReadFlag(resolveReadFlag(item));
            message.setRawLineJson(item.toJSONString());
            message.setCreateTime(now);
            result.add(message);
        }
        return result;
    }

    private OzonChatSession buildSession(
            OzonAuth auth,
            JSONObject sessionPayload,
            String sessionId,
            List<OzonChatMessage> messages,
            Date now
    ) {
        OzonChatSession session = new OzonChatSession();
        session.setId(nextId());
        session.setAuthId(auth.getId());
        session.setShopId(auth.getShopId());
        session.setSessionId(sessionId);
        session.setCustomerName(firstText(sessionPayload, "customerName", "customer_name"));
        session.setSessionStatus(firstText(sessionPayload, "sessionStatus", "session_status"));
        session.setLastMessageText(resolveLastMessageText(messages));
        session.setLastMessageAt(resolveLastMessageAt(messages));
        session.setUnreadCount(resolveUnreadCount(messages));
        session.setCreateTime(now);
        session.setUpdateTime(now);
        return session;
    }

    private Boolean resolveReadFlag(JSONObject item) {
        Boolean read = item.getBoolean("read");
        return read != null ? read : item.getBoolean("read_flag");
    }

    private String resolveLastMessageText(List<OzonChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        OzonChatMessage latest = messages.get(0);
        for (OzonChatMessage item : messages) {
            if (compareDate(item.getMessageTime(), latest.getMessageTime()) >= 0) {
                latest = item;
            }
        }
        return latest.getMessageText();
    }

    private Date resolveLastMessageAt(List<OzonChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        Date latest = messages.get(0).getMessageTime();
        for (OzonChatMessage item : messages) {
            if (compareDate(item.getMessageTime(), latest) >= 0) {
                latest = item.getMessageTime();
            }
        }
        return latest;
    }

    private Integer resolveUnreadCount(List<OzonChatMessage> messages) {
        int unread = 0;
        if (messages == null) {
            return unread;
        }
        for (OzonChatMessage item : messages) {
            if (BUYER.equalsIgnoreCase(item.getSenderType()) && !Boolean.TRUE.equals(item.getReadFlag())) {
                unread++;
            }
        }
        return unread;
    }

    private int compareDate(Date left, Date right) {
        long leftTime = left == null ? Long.MIN_VALUE : left.getTime();
        long rightTime = right == null ? Long.MIN_VALUE : right.getTime();
        return Long.compare(leftTime, rightTime);
    }

    private OzonChatSession requireSession(OzonAuth auth, String sessionId) {
        OzonChatSession session = sessionMapper.selectOne(new QueryWrapper<OzonChatSession>()
                .eq("auth_id", auth.getId())
                .eq("session_id", sessionId)
                .last("limit 1"));
        if (session == null) {
            throw new IllegalArgumentException("Ozon聊天会话不存在");
        }
        return session;
    }

    private OzonChatReplyAudit buildReplyAudit(OzonAuth auth, UserInfo user, String sessionId, String replyText, String status, Date now) {
        OzonChatReplyAudit audit = new OzonChatReplyAudit();
        audit.setId(nextId());
        audit.setAuthId(auth.getId());
        audit.setShopId(auth.getShopId());
        audit.setSessionId(sessionId);
        audit.setReplyText(replyText);
        audit.setReplyStatus(status);
        audit.setOperator(user == null ? null : user.getId());
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        return audit;
    }

    private void upsertSellerMessage(OzonAuth auth, OzonChatSession session, String replyText, String response, Date now) {
        OzonChatMessage message = new OzonChatMessage();
        message.setId(nextId());
        message.setAuthId(auth.getId());
        message.setShopId(auth.getShopId());
        message.setSessionId(session.getSessionId());
        message.setMessageId(resolveRemoteMessageId(response));
        message.setSenderType(SELLER);
        message.setMessageText(replyText);
        message.setMessageTime(now);
        message.setReadFlag(Boolean.TRUE);
        message.setRawLineJson(response);
        message.setCreateTime(now);
        messageMapper.upsert(message);
    }

    private void refreshSessionAfterSellerReply(OzonAuth auth, OzonChatSession existing, String replyText, Date now) {
        OzonChatSession session = new OzonChatSession();
        session.setId(StrUtil.blankToDefault(existing.getId(), nextId()));
        session.setAuthId(auth.getId());
        session.setShopId(auth.getShopId());
        session.setSessionId(existing.getSessionId());
        session.setCustomerName(existing.getCustomerName());
        session.setSessionStatus(existing.getSessionStatus());
        session.setLastMessageText(replyText);
        session.setLastMessageAt(now);
        session.setUnreadCount(existing.getUnreadCount());
        session.setCreateTime(existing.getCreateTime() == null ? now : existing.getCreateTime());
        session.setUpdateTime(now);
        sessionMapper.upsert(session);
    }

    private String resolveRemoteMessageId(String response) {
        try {
            JSONObject payload = JSON.parseObject(response);
            String messageId = firstText(payload, "result", "message_id", "messageId");
            if (StrUtil.isNotBlank(messageId)) {
                return messageId;
            }
        } catch (RuntimeException ignored) {
        }
        return "local-send-" + nextId();
    }

    private Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Date.from(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException ex) {
            try {
                return Date.from(Instant.parse(value));
            } catch (DateTimeParseException ignored) {
                try {
                    return Date.from(LocalDateTime.parse(value).toInstant(ZoneOffset.UTC));
                } catch (DateTimeParseException ignoredAgain) {
                    return null;
                }
            }
        }
    }

    private String firstText(JSONObject item, String... keys) {
        for (String key : keys) {
            String value = item.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String requireText(String value, String message) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trim(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private String nextId() {
        try {
            return IdUtil.getSnowflakeNextIdStr();
        } catch (IllegalStateException ex) {
            long fallback = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            return String.valueOf(fallback);
        }
    }

    private String normalizeRemoteErrorMessage(String rawMessage) {
        if (StrUtil.isBlank(rawMessage)) {
            return "Ozon聊天发送失败";
        }
        try {
            JSONObject payload = JSON.parseObject(rawMessage);
            String message = firstText(payload, "message", "error_message", "details");
            if (StrUtil.isNotBlank(message)) {
                return message;
            }
        } catch (RuntimeException ignored) {
        }
        return rawMessage;
    }

    private void recordApiLog(
            OzonAuth auth,
            UserInfo user,
            String objectId,
            String requestPayload,
            String responsePayload,
            String status,
            String errorMessage,
            long startedAt
    ) {
        opsService.recordApiLog(new OzonApiLogRecordCommand(
                auth.getId(),
                auth.getShopId(),
                API_GROUP,
                "SEND_CHAT_MESSAGE",
                CHAT_SEND_MESSAGE_ENDPOINT,
                "POST",
                "CHAT",
                objectId,
                requestPayload,
                responsePayload,
                status,
                errorMessage,
                Math.max(System.currentTimeMillis() - startedAt, 0L),
                user == null ? null : user.getId()
        ));
    }

    private void recordOperationAudit(
            OzonAuth auth,
            UserInfo user,
            String operationType,
            String objectId,
            String objectCode,
            String requestPayload,
            String resultStatus,
            String resultMessage
    ) {
        opsService.recordOperationAudit(new OzonOperationAuditRecordCommand(
                auth.getId(),
                auth.getShopId(),
                operationType,
                "CHAT",
                objectId,
                objectCode,
                requestPayload,
                resultStatus,
                resultMessage,
                user == null ? null : user.getId()
        ));
    }
}

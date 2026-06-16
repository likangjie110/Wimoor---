package com.wimoor.ozon.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wimoor.common.result.Result;
import com.wimoor.common.user.UserInfo;
import com.wimoor.common.user.UserInfoContext;
import com.wimoor.ozon.config.OzonFeatureGate;
import com.wimoor.ozon.chat.pojo.dto.OzonChatImportCommand;
import com.wimoor.ozon.chat.pojo.dto.OzonChatMessageQuery;
import com.wimoor.ozon.chat.pojo.dto.OzonChatReplyRecordCommand;
import com.wimoor.ozon.chat.pojo.entity.OzonChatMessage;
import com.wimoor.ozon.chat.pojo.entity.OzonChatReplyAudit;
import com.wimoor.ozon.chat.pojo.entity.OzonChatSession;
import com.wimoor.ozon.chat.pojo.vo.OzonChatImportResult;
import com.wimoor.ozon.chat.service.IOzonChatService;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class OzonChatController {

    private final IOzonChatService chatService;
    private final OzonFeatureGate featureGate;

    @PostMapping("/import")
    public Result<OzonChatImportResult> importMessages(@RequestBody OzonChatImportCommand command) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            return chatService.importMessages(currentUser(), command);
        });
    }

    @GetMapping("/session/list")
    public Result<List<OzonChatSession>> listSessions(
            @RequestParam String authId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sessionStatus
    ) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            return chatService.listSessions(currentUser(), authId, unreadOnly, keyword, sessionStatus);
        });
    }

    @GetMapping("/message/list")
    public Result<List<OzonChatMessage>> listMessages(OzonChatMessageQuery query) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            return chatService.listMessages(currentUser(), query);
        });
    }

    @PostMapping("/reply/record")
    public Result<OzonChatReplyAudit> recordReply(@RequestBody OzonChatReplyRecordCommand command) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            return chatService.recordReply(currentUser(), command);
        });
    }

    @PostMapping("/reply/send")
    public Result<OzonChatReplyAudit> sendReply(@RequestBody OzonChatReplyRecordCommand command) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            featureGate.assertChatSendEnabled();
            return chatService.sendReply(currentUser(), command);
        });
    }

    @GetMapping("/reply/audit/list")
    public Result<List<OzonChatReplyAudit>> listReplyAudits(
            @RequestParam String authId,
            @RequestParam(required = false) String sessionId
    ) {
        return execute(() -> {
            featureGate.assertChatEnabled();
            return chatService.listReplyAudits(currentUser(), authId, sessionId);
        });
    }

    private UserInfo currentUser() {
        return UserInfoContext.get();
    }

    private <T> Result<T> execute(ChatCall<T> call) {
        try {
            return Result.success(call.run());
        } catch (RuntimeException ex) {
            return Result.failed(normalizeMessage(ex.getMessage()));
        }
    }

    private String normalizeMessage(String rawMessage) {
        if (StrUtil.isBlank(rawMessage)) {
            return "Ozon聊天操作失败";
        }
        try {
            JSONObject payload = JSON.parseObject(rawMessage);
            String message = payload.getString("message");
            if (StrUtil.isNotBlank(message)) {
                return message;
            }
        } catch (RuntimeException ignored) {
        }
        return rawMessage;
    }

    @FunctionalInterface
    private interface ChatCall<T> {
        T run();
    }
}

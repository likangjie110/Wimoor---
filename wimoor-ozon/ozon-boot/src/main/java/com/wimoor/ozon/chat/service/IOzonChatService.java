package com.wimoor.ozon.chat.service;

import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.chat.pojo.dto.OzonChatImportCommand;
import com.wimoor.ozon.chat.pojo.dto.OzonChatMessageQuery;
import com.wimoor.ozon.chat.pojo.dto.OzonChatReplyRecordCommand;
import com.wimoor.ozon.chat.pojo.entity.OzonChatMessage;
import com.wimoor.ozon.chat.pojo.entity.OzonChatReplyAudit;
import com.wimoor.ozon.chat.pojo.entity.OzonChatSession;
import com.wimoor.ozon.chat.pojo.vo.OzonChatImportResult;

public interface IOzonChatService {

    OzonChatImportResult importMessages(UserInfo user, OzonChatImportCommand command);

    List<OzonChatSession> listSessions(UserInfo user, String authId, Boolean unreadOnly, String keyword, String sessionStatus);

    List<OzonChatMessage> listMessages(UserInfo user, OzonChatMessageQuery query);

    OzonChatReplyAudit recordReply(UserInfo user, OzonChatReplyRecordCommand command);

    OzonChatReplyAudit sendReply(UserInfo user, OzonChatReplyRecordCommand command);

    List<OzonChatReplyAudit> listReplyAudits(UserInfo user, String authId, String sessionId);

    // API Sync Methods
    OzonChatImportResult syncChatsFromApi(UserInfo user, String authId);

    OzonChatImportResult syncMessagesFromApi(UserInfo user, String authId, String chatId);
}

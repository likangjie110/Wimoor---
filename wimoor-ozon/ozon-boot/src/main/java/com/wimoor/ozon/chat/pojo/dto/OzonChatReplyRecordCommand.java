package com.wimoor.ozon.chat.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonChatReplyRecordCommand {

    private String authId;

    private String sessionId;

    private String replyText;
}

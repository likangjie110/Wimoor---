package com.wimoor.ozon.chat.pojo.vo;

import java.util.Date;

import lombok.Data;

@Data
public class OzonChatImportResult {

    private Integer sessionCount;

    private Integer messageCount;

    private Date importedAt;
}

package com.wimoor.ozon.chat.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_chat_message")
public class OzonChatMessage {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("session_id")
    private String sessionId;

    @TableField("message_id")
    private String messageId;

    @TableField("sender_type")
    private String senderType;

    @TableField("message_text")
    private String messageText;

    @TableField("message_time")
    private Date messageTime;

    @TableField("read_flag")
    private Boolean readFlag;

    @TableField("raw_line_json")
    private String rawLineJson;

    @TableField("create_time")
    private Date createTime;

    public void setChatId(String chatId) {
        this.sessionId = chatId;
    }
}

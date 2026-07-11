package com.wimoor.ozon.chat.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_chat_session")
public class OzonChatSession {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("session_id")
    private String sessionId;

    @TableField("customer_name")
    private String customerName;

    @TableField("last_message_text")
    private String lastMessageText;

    @TableField("last_message_at")
    private Date lastMessageAt;

    @TableField("unread_count")
    private Integer unreadCount;

    @TableField("session_status")
    private String sessionStatus;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    public void setChatId(String chatId) {
        this.sessionId = chatId;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageAt = lastMessageTime;
    }
}

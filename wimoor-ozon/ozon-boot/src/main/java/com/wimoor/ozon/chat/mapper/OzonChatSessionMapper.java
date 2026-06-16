package com.wimoor.ozon.chat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.chat.pojo.entity.OzonChatSession;

@Mapper
public interface OzonChatSessionMapper extends BaseMapper<OzonChatSession> {

    @Insert("insert into t_ozon_chat_session (id, auth_id, shop_id, session_id, customer_name, last_message_text, last_message_at, unread_count, session_status, create_time, update_time) "
            + "values (#{id}, #{authId}, #{shopId}, #{sessionId}, #{customerName}, #{lastMessageText}, #{lastMessageAt}, #{unreadCount}, #{sessionStatus}, #{createTime}, #{updateTime}) "
            + "on duplicate key update customer_name = values(customer_name), last_message_text = values(last_message_text), "
            + "last_message_at = values(last_message_at), unread_count = values(unread_count), session_status = values(session_status), update_time = values(update_time)")
    int upsert(OzonChatSession session);
}

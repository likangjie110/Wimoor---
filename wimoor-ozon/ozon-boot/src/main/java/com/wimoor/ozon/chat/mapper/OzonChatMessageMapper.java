package com.wimoor.ozon.chat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wimoor.ozon.chat.pojo.entity.OzonChatMessage;

@Mapper
public interface OzonChatMessageMapper extends BaseMapper<OzonChatMessage> {

    @Insert("insert into t_ozon_chat_message (id, auth_id, shop_id, session_id, message_id, sender_type, message_text, message_time, read_flag, raw_line_json, create_time) "
            + "values (#{id}, #{authId}, #{shopId}, #{sessionId}, #{messageId}, #{senderType}, #{messageText}, #{messageTime}, #{readFlag}, #{rawLineJson}, #{createTime}) "
            + "on duplicate key update session_id = values(session_id), sender_type = values(sender_type), message_text = values(message_text), "
            + "message_time = values(message_time), read_flag = values(read_flag), raw_line_json = values(raw_line_json)")
    int upsert(OzonChatMessage message);
}

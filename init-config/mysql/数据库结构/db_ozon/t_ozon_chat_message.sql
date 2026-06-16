CREATE TABLE IF NOT EXISTS `t_ozon_chat_message` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '消息ID',
  `sender_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '发送方',
  `message_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '消息内容',
  `message_time` datetime DEFAULT NULL COMMENT '消息时间',
  `read_flag` bit(1) DEFAULT b'0' COMMENT '是否已读',
  `raw_line_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '原始消息JSON',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_message` (`auth_id`,`message_id`),
  KEY `idx_auth_session` (`auth_id`,`session_id`),
  KEY `idx_message_time` (`message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon聊天消息表';

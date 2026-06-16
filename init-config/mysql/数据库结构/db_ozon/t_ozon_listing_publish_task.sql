CREATE TABLE IF NOT EXISTS `t_ozon_listing_publish_task` (
  `id` bigint unsigned NOT NULL,
  `draft_id` bigint unsigned NOT NULL COMMENT '草稿ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `task_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务状态',
  `remote_task_id` bigint unsigned DEFAULT NULL COMMENT 'Ozon远端任务ID',
  `request_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '请求原文',
  `response_payload_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '响应原文',
  `error_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '错误信息',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_draft_status` (`draft_id`,`task_status`),
  KEY `idx_remote_task` (`remote_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon刊登发布任务表';

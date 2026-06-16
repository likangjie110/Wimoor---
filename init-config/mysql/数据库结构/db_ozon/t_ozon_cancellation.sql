CREATE TABLE IF NOT EXISTS `t_ozon_cancellation` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `posting_id` bigint unsigned NOT NULL COMMENT 'Posting主表ID',
  `posting_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Posting Number',
  `cancellation_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '取消单号',
  `cancellation_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '取消状态',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '取消原因',
  `raw_payload_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '原始载荷',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_posting_cancellation` (`auth_id`,`posting_id`,`cancellation_number`),
  KEY `idx_posting_id` (`posting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon取消记录表';

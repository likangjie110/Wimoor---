CREATE TABLE IF NOT EXISTS `t_ozon_return` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `posting_id` bigint unsigned NOT NULL COMMENT 'Posting主表ID',
  `posting_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Posting Number',
  `return_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '退货号',
  `return_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '退货状态',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '退货原因',
  `quantity` int DEFAULT NULL COMMENT '退货数量',
  `raw_payload_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '原始载荷',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_posting_return` (`auth_id`,`posting_id`,`return_number`),
  KEY `idx_posting_id` (`posting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon退货记录表';

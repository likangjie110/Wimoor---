CREATE TABLE IF NOT EXISTS `t_ozon_shipment` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `posting_id` bigint unsigned NOT NULL COMMENT 'Posting主表ID',
  `posting_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Posting Number',
  `tracking_number` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '追踪号',
  `delivery_service` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '物流服务商',
  `shipment_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '履约状态',
  `request_payload_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '请求载荷',
  `response_payload_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '响应载荷',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_posting_id` (`posting_id`),
  KEY `idx_auth_posting_number` (`auth_id`,`posting_number`),
  KEY `idx_tracking_number` (`tracking_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon Shipment履约记录表';

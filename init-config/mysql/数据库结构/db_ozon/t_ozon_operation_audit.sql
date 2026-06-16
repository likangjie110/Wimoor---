CREATE TABLE IF NOT EXISTS `t_ozon_operation_audit` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned DEFAULT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned DEFAULT NULL COMMENT '店铺ID',
  `operation_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '操作类型',
  `object_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '对象类型',
  `object_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '对象ID',
  `object_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '对象编码',
  `request_payload_json` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '请求或操作载荷',
  `result_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '结果状态',
  `result_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '结果说明',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_auth_operation_status` (`auth_id`,`operation_type`,`result_status`),
  KEY `idx_object_ref` (`object_type`,`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_bin COMMENT='Ozon人工操作审计表';

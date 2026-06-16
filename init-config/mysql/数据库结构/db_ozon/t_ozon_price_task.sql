CREATE TABLE IF NOT EXISTS `t_ozon_price_task` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `task_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '任务状态',
  `requested_count` int NOT NULL DEFAULT '0' COMMENT '请求明细数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '受理明细数',
  `error_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '错误信息',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_auth_id` (`auth_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon价格推送任务表';

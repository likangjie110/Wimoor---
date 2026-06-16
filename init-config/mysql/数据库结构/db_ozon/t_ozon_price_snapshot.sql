CREATE TABLE IF NOT EXISTS `t_ozon_price_snapshot` (
  `id` bigint unsigned NOT NULL,
  `task_id` bigint unsigned NOT NULL COMMENT '任务ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `material_sku` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'ERP SKU',
  `ozon_offer_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Offer ID',
  `price` decimal(18,2) NOT NULL COMMENT '推送价格',
  `old_price` decimal(18,2) DEFAULT NULL COMMENT '划线价',
  `currency_code` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '币种',
  `sync_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '同步状态',
  `sync_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '同步消息',
  `synced_at` datetime DEFAULT NULL COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_auth_id` (`auth_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon价格推送快照表';

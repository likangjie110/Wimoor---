CREATE TABLE IF NOT EXISTS `t_ozon_stock_snapshot` (
  `id` bigint unsigned NOT NULL,
  `task_id` bigint unsigned NOT NULL COMMENT '任务ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `warehouse_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '仓库ID',
  `material_sku` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'ERP SKU',
  `ozon_offer_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Offer ID',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '推送库存',
  `sync_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '同步状态',
  `sync_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '同步消息',
  `synced_at` datetime DEFAULT NULL COMMENT '同步时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_auth_id` (`auth_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon库存推送快照表';

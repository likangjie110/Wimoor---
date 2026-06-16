CREATE TABLE IF NOT EXISTS `t_ozon_shop_config` (
  `id` bigint unsigned NOT NULL,
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '店铺展示名',
  `seller_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '卖家编码',
  `default_warehouse_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '默认仓库ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT 'ACTIVE' COMMENT '状态',
  `last_warehouse_sync_time` datetime DEFAULT NULL COMMENT '最近仓库同步时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_id` (`auth_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon店铺配置表';

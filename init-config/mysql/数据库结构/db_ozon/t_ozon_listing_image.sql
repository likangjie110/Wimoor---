CREATE TABLE IF NOT EXISTS `t_ozon_listing_image` (
  `id` bigint unsigned NOT NULL,
  `draft_id` bigint unsigned NOT NULL COMMENT '草稿ID',
  `variant_id` bigint unsigned DEFAULT NULL COMMENT '变体ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'GROUP/VARIANT',
  `source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'ERP/OPERATOR',
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `is_primary` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否主图',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_draft_scope_sort` (`draft_id`,`scope`,`sort_order`),
  KEY `idx_variant_scope_sort` (`variant_id`,`scope`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon刊登图片表';

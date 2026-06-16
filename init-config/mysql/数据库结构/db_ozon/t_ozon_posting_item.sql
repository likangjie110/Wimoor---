CREATE TABLE IF NOT EXISTS `t_ozon_posting_item` (
  `id` bigint unsigned NOT NULL,
  `posting_id` bigint unsigned NOT NULL COMMENT 'Posting主表ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `posting_number` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'Ozon Posting Number',
  `material_sku` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'ERP SKU',
  `ozon_offer_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Ozon Offer ID',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '数量',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_posting_id` (`posting_id`),
  KEY `idx_auth_posting_number` (`auth_id`,`posting_number`),
  KEY `idx_material_sku` (`material_sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon Posting商品明细表';

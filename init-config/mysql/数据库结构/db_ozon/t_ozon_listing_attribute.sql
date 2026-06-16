CREATE TABLE IF NOT EXISTS `t_ozon_listing_attribute` (
  `id` bigint unsigned NOT NULL,
  `draft_id` bigint unsigned NOT NULL COMMENT '草稿ID',
  `variant_id` bigint unsigned DEFAULT NULL COMMENT '变体ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `scope` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'COMMON/VARIANT',
  `attribute_id` bigint unsigned NOT NULL COMMENT '属性ID',
  `attribute_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '属性名称',
  `attribute_value_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '属性值JSON',
  `required_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_draft_scope` (`draft_id`,`scope`),
  KEY `idx_variant_scope` (`variant_id`,`scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon刊登属性表';

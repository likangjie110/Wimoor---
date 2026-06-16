CREATE TABLE IF NOT EXISTS `t_ozon_ads_campaign` (
  `id` bigint unsigned NOT NULL,
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `account_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '广告账号ID',
  `campaign_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '活动ID',
  `campaign_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '活动名称',
  `campaign_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '活动类型',
  `campaign_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '活动状态',
  `budget` decimal(18,4) DEFAULT NULL COMMENT '预算',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_campaign` (`auth_id`,`campaign_id`),
  KEY `idx_auth_campaign_type` (`auth_id`,`campaign_type`),
  KEY `idx_auth_update_time` (`auth_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon广告活动表';

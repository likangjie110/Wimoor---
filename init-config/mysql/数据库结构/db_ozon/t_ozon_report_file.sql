CREATE TABLE IF NOT EXISTS `t_ozon_report_file` (
  `id` bigint unsigned NOT NULL,
  `task_id` bigint unsigned NOT NULL COMMENT '导入任务ID',
  `auth_id` bigint unsigned NOT NULL COMMENT '授权ID',
  `shop_id` bigint unsigned NOT NULL COMMENT '店铺ID',
  `report_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '报表ID',
  `report_date` date DEFAULT NULL COMMENT '报表日期',
  `content_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'JSON' COMMENT '原文类型',
  `raw_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '报表原文',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_auth_report_id` (`auth_id`,`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='Ozon财务报表原文表';

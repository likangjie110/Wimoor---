-- Ozon 功能灰度发布配置表
CREATE TABLE IF NOT EXISTS t_ozon_feature_rollout (
    feature_key VARCHAR(50) NOT NULL COMMENT '功能键',
    rollout_strategy VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '灰度策略: WHITELIST-白名单, PERCENTAGE-百分比, ALL-全量, NONE-关闭',
    rollout_value VARCHAR(500) DEFAULT NULL COMMENT '灰度值: 白名单为店铺ID逗号分隔, 百分比为0-100的数字',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用: 0-未启用, 1-已启用',
    description VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
    created_by VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (feature_key),
    INDEX idx_enabled (enabled),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Ozon功能灰度发布配置表';

-- 初始化默认配置（全部关闭）
INSERT INTO t_ozon_feature_rollout (feature_key, rollout_strategy, enabled, description) VALUES
('auth', 'NONE', 0, '店铺授权功能'),
('product', 'NONE', 0, '商品管理功能（只读）'),
('productWrite', 'NONE', 0, '商品发布写操作'),
('stockWrite', 'NONE', 0, '库存推送写操作'),
('priceWrite', 'NONE', 0, '价格推送写操作'),
('postingWrite', 'NONE', 0, '订单履约写操作'),
('finance', 'NONE', 0, '财务管理功能'),
('chat', 'NONE', 0, '聊天管理功能（只读）'),
('chatSend', 'NONE', 0, '聊天发送写操作'),
('ads', 'NONE', 0, '广告管理功能（只读）'),
('adsSync', 'NONE', 0, '广告同步写操作'),
('task', 'NONE', 0, '任务中心功能'),
('error', 'NONE', 0, '错误中心功能')
ON DUPLICATE KEY UPDATE
    rollout_strategy = VALUES(rollout_strategy),
    enabled = VALUES(enabled),
    description = VALUES(description);

-- 示例：内部测试阶段配置（白名单）
-- UPDATE t_ozon_feature_rollout
-- SET rollout_strategy = 'WHITELIST',
--     rollout_value = 'shop-internal-1,shop-internal-2',
--     enabled = 1,
--     description = '内部测试账号',
--     updated_by = 'admin'
-- WHERE feature_key IN ('auth', 'product', 'productWrite');

-- 示例：小范围灰度配置（5-10个客户）
-- UPDATE t_ozon_feature_rollout
-- SET rollout_strategy = 'WHITELIST',
--     rollout_value = 'shop-1,shop-2,shop-3,shop-4,shop-5',
--     enabled = 1,
--     description = '小范围灰度测试',
--     updated_by = 'admin'
-- WHERE feature_key IN ('auth', 'product', 'productWrite', 'stockWrite', 'priceWrite');

-- 示例：中等范围灰度配置（20-30%）
-- UPDATE t_ozon_feature_rollout
-- SET rollout_strategy = 'PERCENTAGE',
--     rollout_value = '30',
--     enabled = 1,
--     description = '30%用户灰度',
--     updated_by = 'admin'
-- WHERE feature_key IN ('auth', 'product', 'productWrite', 'stockWrite', 'priceWrite', 'postingWrite', 'finance', 'chat', 'ads');

-- 示例：全量发布配置
-- UPDATE t_ozon_feature_rollout
-- SET rollout_strategy = 'ALL',
--     rollout_value = NULL,
--     enabled = 1,
--     description = '全量发布',
--     updated_by = 'admin'
-- WHERE feature_key IN ('auth', 'product', 'productWrite', 'stockWrite', 'priceWrite', 'postingWrite', 'finance', 'chat', 'chatSend', 'ads', 'adsSync', 'task', 'error');

-- 查询当前灰度配置
-- SELECT
--     feature_key,
--     rollout_strategy,
--     rollout_value,
--     enabled,
--     description,
--     updated_by,
--     updated_at
-- FROM t_ozon_feature_rollout
-- ORDER BY feature_key;

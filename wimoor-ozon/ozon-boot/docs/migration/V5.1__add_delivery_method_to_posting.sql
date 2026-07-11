-- Phase 5: 添加配送方式字段到 Posting 表
-- 创建日期: 2026-06-25
-- 描述: 支持订单与配送方式关联

ALTER TABLE t_ozon_posting
ADD COLUMN delivery_method_id VARCHAR(64) COMMENT '配送方式ID' AFTER erp_order_id;

-- 添加索引以优化查询
CREATE INDEX idx_delivery_method ON t_ozon_posting(delivery_method_id);

-- 添加外键约束（可选，取决于是否严格要求引用完整性）
-- ALTER TABLE t_ozon_posting
-- ADD CONSTRAINT fk_posting_delivery_method
-- FOREIGN KEY (delivery_method_id) REFERENCES t_ozon_delivery_method(id)
-- ON DELETE SET NULL;

-- ============================================================
-- 内容带货 + 商家评分/店铺页 迁移脚本（2026-09-26）
-- 1) article 增加 product_ids：带货商品ID，逗号分隔（tags 列已存在于 migration.sql）
-- 2) merchant_rating：商家评分表（一单一评，uk_order 防重复）
-- 执行方式：在已有库上按顺序执行（与 migration.sql / mall_migration.sql 幂等风格一致）
-- ============================================================

ALTER TABLE article ADD COLUMN product_ids VARCHAR(200) DEFAULT NULL COMMENT '带货商品ID，逗号分隔';

CREATE TABLE IF NOT EXISTS `merchant_rating` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
  `merchant_user_id` INT UNSIGNED NOT NULL COMMENT '商家用户ID(merchant_info.user_id)',
  `order_id` INT NOT NULL COMMENT '订单ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '评价用户ID',
  `score` TINYINT NOT NULL COMMENT '评分1-5',
  `content` VARCHAR(255) COMMENT '评价内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  UNIQUE KEY `uk_order` (`order_id`),
  INDEX `idx_merchant` (`merchant_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家评分表（订单维度一单一评）';

-- ============================================
-- 社交与评价升级迁移脚本（2026-09-26）
-- 1) 商品评论升级：评分 + 晒图 + 关联购买订单（快照从 order_item 读取）
-- 2) 用户自我介绍（博主主页/个人主页展示）
-- 3) 关注表：博主(type=1) 与 店铺(type=2) 统一一张表
-- 依赖：comment_migration.sql、mall_migration.sql 已执行
-- ============================================

-- 1. 商品评论增强
ALTER TABLE `comment` ADD COLUMN `score` TINYINT DEFAULT NULL COMMENT '商品评分1-5（仅商品顶级评论）';
ALTER TABLE `comment` ADD COLUMN `images` VARCHAR(500) DEFAULT NULL COMMENT '评论图片URL，逗号分隔（最多4张）';
ALTER TABLE `comment` ADD COLUMN `order_id` INT DEFAULT NULL COMMENT '关联购买订单ID（商品晒单，展示从 order_item 快照读取）';

-- 2. 用户自我介绍
ALTER TABLE `user` ADD COLUMN `bio` VARCHAR(200) DEFAULT NULL COMMENT '自我介绍';

-- 3. 关注（博主/店铺统一）
CREATE TABLE IF NOT EXISTS `follow` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` INT UNSIGNED NOT NULL COMMENT '关注者用户ID',
  `followee_id` INT UNSIGNED NOT NULL COMMENT '被关注者ID（博主用户ID 或 商家用户ID）',
  `follow_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1=关注博主 2=关注店铺',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`, `follow_type`),
  INDEX `idx_followee` (`follow_type`, `followee_id`),
  INDEX `idx_follower` (`follower_id`, `follow_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注表（博主/店铺）';

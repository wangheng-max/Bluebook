-- ============================================
-- 收藏夹功能迁移脚本（2026-09-26）
-- 1) article_favorite 增加 folder_id：NULL = 默认收藏夹（虚拟，无需建行）
-- 2) favorite_folder：用户自定义收藏夹
-- 依赖：migration.sql（article/article_favorite 表）已执行
-- ============================================

ALTER TABLE `article_favorite` ADD COLUMN `folder_id` INT DEFAULT NULL COMMENT '收藏夹ID，NULL=默认收藏夹';

CREATE TABLE IF NOT EXISTS `favorite_folder` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏夹ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(30) NOT NULL COMMENT '收藏夹名称',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_user_name` (`user_id`, `name`),
  INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏夹（用户自定义；默认收藏夹为虚拟夹）';

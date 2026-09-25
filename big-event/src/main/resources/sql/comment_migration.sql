-- ============================================
-- 评论模块（文章/商品通用）- 数据库迁移脚本
-- 功能：评论、回复（楼中楼）、评论点赞、按时间/点赞排序
-- 说明：全部使用 IF NOT EXISTS，可重复执行（幂等）
-- 依赖：big_event 库存在 user 表、article 表、product 表
-- ============================================

-- 1. 评论表（文章与商品共用，target_type 区分）
--    两级展示模型：parent_id=0 为顶级评论；回复记录 root_id 指向所属顶级评论，
--    parent_id 指向被回复的评论（支持回复的回复，UI 上仍平铺在根评论楼层内）
CREATE TABLE IF NOT EXISTS `comment` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  `target_type` VARCHAR(20) NOT NULL COMMENT '评论目标类型：article-文章笔记 / product-商品',
  `target_id` INT NOT NULL COMMENT '目标ID（article.id 或 product.id）',
  `user_id` INT NOT NULL COMMENT '评论人ID',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `parent_id` INT NOT NULL DEFAULT 0 COMMENT '父评论ID，0=顶级评论',
  `root_id` INT NOT NULL DEFAULT 0 COMMENT '根评论ID，顶级评论为0，回复为其所属顶级评论ID',
  `reply_user_id` INT DEFAULT NULL COMMENT '被回复用户ID（顶级评论为NULL）',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_target` (`target_type`, `target_id`, `parent_id`),
  INDEX `idx_root` (`root_id`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_like` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表（文章/商品）';

-- 2. 评论点赞明细表（唯一索引防重复点赞）
CREATE TABLE IF NOT EXISTS `comment_like` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `comment_id` INT NOT NULL COMMENT '评论ID',
  `user_id` INT NOT NULL COMMENT '点赞用户ID',
  `create_time` DATETIME NOT NULL,
  UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
  INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞明细表';

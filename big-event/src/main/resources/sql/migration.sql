-- ============================================
-- 好友管理 & 公开笔记搜索模块 - 数据库迁移
-- ============================================

-- 1. 创建好友关系表
CREATE TABLE IF NOT EXISTS friend_relation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '发起请求的用户ID',
    friend_id INT NOT NULL COMMENT '目标用户ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-待确认, 1-已接受, 2-已拒绝',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_user_friend (user_id, friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

-- 2. 为article表添加tags字段
ALTER TABLE article ADD COLUMN tags VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔';

-- 3. 添加全文索引（MySQL 5.7+ 支持 ngram）
ALTER TABLE article ADD FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram;

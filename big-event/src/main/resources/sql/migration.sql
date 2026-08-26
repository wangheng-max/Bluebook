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

-- 4. 文章列表查询联合索引：命中 create_user + category_id + state 三个过滤条件（最左前缀匹配）
ALTER TABLE article ADD INDEX idx_user_category_state (create_user, category_id, state);

-- ============================================
-- 社区功能扩展（按《大事件社区功能扩展设计方案》）
-- ============================================

-- 5. 文章表新增计数字段
ALTER TABLE article ADD COLUMN view_count INT DEFAULT 0 COMMENT '浏览量';
ALTER TABLE article ADD COLUMN like_count INT DEFAULT 0 COMMENT '点赞数';
ALTER TABLE article ADD COLUMN favorite_count INT DEFAULT 0 COMMENT '收藏数';
ALTER TABLE article ADD COLUMN forward_count INT DEFAULT 0 COMMENT '转发数';

-- 6. 内置系统分类（create_user=0 标识系统分类，全体共享；幂等，可重复执行）
-- ① 先查 article 表引用了 category 的外键名（确认后用真实名字替换）
SHOW CREATE TABLE article;
-- 一般是 fk_article_category

-- ② 删掉 article 表上的这个外键（否则无法 DROP category）
ALTER TABLE article DROP FOREIGN KEY fk_article_category;

-- ③ 删除旧 category 表
DROP TABLE IF EXISTS category;

-- ④ 重建 category 表：create_user 可空，加 is_system 标记系统分类
CREATE TABLE category (
                          id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                          category_name VARCHAR(32) NOT NULL COMMENT '分类名称',
                          category_alias VARCHAR(32) NOT NULL COMMENT '分类别名',
                          create_user INT UNSIGNED NULL COMMENT '创建人ID，NULL=系统内置分类',
                          is_system TINYINT NOT NULL DEFAULT 0 COMMENT '1-系统内置分类 0-用户分类',
                          create_time DATETIME NOT NULL,
                          update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ⑤ 插入内置分类（create_user 传 NULL，不再有外键冲突）
INSERT INTO category(category_name, category_alias, create_user, is_system, create_time, update_time) VALUES
                                                                                                          ('科技数码','tech',NULL,1,NOW(),NOW()),
                                                                                                          ('生活日常','life',NULL,1,NOW(),NOW()),
                                                                                                          ('美食探店','food',NULL,1,NOW(),NOW()),
                                                                                                          ('旅行游记','travel',NULL,1,NOW(),NOW()),
                                                                                                          ('游戏电竞','game',NULL,1,NOW(),NOW()),
                                                                                                          ('财经职场','finance',NULL,1,NOW(),NOW()),
                                                                                                          ('教育学习','edu',NULL,1,NOW(),NOW()),
                                                                                                          ('健康运动','health',NULL,1,NOW(),NOW()),
                                                                                                          ('影视娱乐','entertainment',NULL,1,NOW(),NOW()),
                                                                                                    ('体育赛事','sports',NULL,1,NOW(),NOW());

-- ⑥ 重建外键（与 user.id 类型保持一致，这里按 int unsigned）
ALTER TABLE category ADD CONSTRAINT fk_category_user
    FOREIGN KEY (create_user) REFERENCES user(id);
ALTER TABLE article ADD CONSTRAINT fk_article_category
    FOREIGN KEY (category_id) REFERENCES category(id);

-- 7. 点赞表（唯一索引防重复赞）
CREATE TABLE IF NOT EXISTS article_like (
  id INT AUTO_INCREMENT PRIMARY KEY,
  article_id INT NOT NULL,
  user_id INT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_article_user (article_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏表
CREATE TABLE IF NOT EXISTS article_favorite (
  id INT AUTO_INCREMENT PRIMARY KEY,
  article_id INT NOT NULL,
  user_id INT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_article_user (article_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 转发记录表（转发给好友）
CREATE TABLE IF NOT EXISTS article_forward (
  id INT AUTO_INCREMENT PRIMARY KEY,
  article_id INT NOT NULL,
  from_user_id INT NOT NULL,
  to_user_id INT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_forward (article_id, from_user_id, to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS message (
  id INT AUTO_INCREMENT PRIMARY KEY,
  from_user_id INT NOT NULL COMMENT '发送者',
  to_user_id INT NOT NULL COMMENT '接收者',
  msg_type TINYINT NOT NULL DEFAULT 0 COMMENT '0-文本 1-文章转发',
  content VARCHAR(500) DEFAULT NULL COMMENT '文本内容',
  article_id INT DEFAULT NULL COMMENT '转发文章ID（msg_type=1时）',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
  create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_msg_pair ON message(from_user_id, to_user_id, create_time);

-- 8. 按分类浏览已发布文章索引（category_id, state, create_time）
ALTER TABLE article ADD INDEX idx_category_state_time (category_id, state, create_time);

-- 9. 热点文章初始种子：把存量已发布文章按浏览量写入 hot:articles（运行时由点赞/收藏/浏览增量维护）
--    说明：Redis 的 hot:articles ZSET 首次冷启动为空，社区接口会先查库按 view_count 兜底填充。

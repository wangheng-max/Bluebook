-- ============================================
-- 商城团购功能 - 数据库迁移脚本
-- 依据：《商城团购功能开发文档（v1.1 商家认证版）》
-- 说明：全部使用 IF NOT EXISTS，可重复执行（幂等）
-- 依赖：big_event 库存在 user 表（user.id 为 INT UNSIGNED）
-- ============================================

-- 1. 商品分类表（管理员维护，商家仅可选用）
CREATE TABLE IF NOT EXISTS `product_category` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` INT DEFAULT 0 COMMENT '父分类ID',
  `icon` VARCHAR(255) COMMENT '分类图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `is_system` TINYINT DEFAULT 0 COMMENT '是否系统分类(1=系统)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2. 商品基本信息表
CREATE TABLE IF NOT EXISTS `product` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `category_id` INT NOT NULL COMMENT '分类ID',
  `cover_img` VARCHAR(500) NOT NULL COMMENT '封面图',
  `images` TEXT COMMENT '详情图(JSON数组)',
  `video_url` VARCHAR(500) COMMENT '商品视频',
  `description` TEXT COMMENT '商品详情',
  `price` DECIMAL(10,2) NOT NULL COMMENT '原价',
  `market_price` DECIMAL(10,2) DEFAULT 0 COMMENT '市场价',
  `status` TINYINT DEFAULT 1 COMMENT '状态(1=上架,0=下架)',
  `stock` INT DEFAULT 0 COMMENT '总库存',
  `sales_count` INT DEFAULT 0 COMMENT '销量',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `create_user_id` INT COMMENT '创建者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_category_id` (`category_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`),
  INDEX `idx_sales` (`sales_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 3. 商品SKU表（支持规格、库存）
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT 'SKU ID',
  `product_id` INT NOT NULL COMMENT '商品ID',
  `sku_name` VARCHAR(100) NOT NULL COMMENT 'SKU名称(如:红/XXL)',
  `spec_values` JSON COMMENT '规格值(JSON数组)',
  `price` DECIMAL(10,2) NOT NULL COMMENT 'SKU价格',
  `stock` INT DEFAULT 0 COMMENT 'SKU库存',
  `sales_count` INT DEFAULT 0 COMMENT '销量',
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 4. 团购活动表
CREATE TABLE IF NOT EXISTS `group_buy` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '团购ID',
  `product_id` INT NOT NULL COMMENT '商品ID',
  `title` VARCHAR(200) NOT NULL COMMENT '团购活动标题',
  `description` TEXT COMMENT '团购活动描述',
  `group_price` DECIMAL(10,2) NOT NULL COMMENT '团购价',
  `group_size` INT NOT NULL DEFAULT 1 COMMENT '成团人数门槛',
  `progress_count` INT DEFAULT 0 COMMENT '当前已参团人数',
  `max_group_size` INT DEFAULT 100 COMMENT '最大团购人数',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态(0=未开始,1=进行中,2=已成团结束,3=已关闭(未成团/手动关闭))',
  `create_user_id` INT COMMENT '创建者ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_product_id` (`product_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_end_time` (`end_time`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购活动表';

-- 5. 团购记录表
CREATE TABLE IF NOT EXISTS `group_buy_record` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `group_buy_id` INT NOT NULL COMMENT '团购ID',
  `order_id` INT NOT NULL COMMENT '关联订单ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `group_num` INT DEFAULT 1 COMMENT '参与人数',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  UNIQUE KEY `uk_user_group` (`user_id`, `group_buy_id`),
  INDEX `idx_group_buy_id` (`group_buy_id`),
  INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购记录表';

-- 6. 优惠券类型表
CREATE TABLE IF NOT EXISTS `coupon_type` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '类型ID',
  `name` VARCHAR(50) NOT NULL COMMENT '类型名称(满减券/折扣券/赠品券)',
  `min_spend` DECIMAL(10,2) DEFAULT 0 COMMENT '最低消费(满减券)',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '减免金额(满减券)',
  `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '折扣率(折扣券,如0.85=85折)',
  `gift_product_id` INT DEFAULT NULL COMMENT '赠品商品ID',
  `max_uses` INT DEFAULT 0 COMMENT '最大使用次数(0=不限)',
  `valid_days` INT DEFAULT 7 COMMENT '领取后有效期(天)',
  `create_user_id` INT NOT NULL COMMENT '创建商家ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_create_user` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券类型表';

-- 7. 优惠券库存表（秒杀抢券用）
CREATE TABLE IF NOT EXISTS `coupon_stock` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '库存ID',
  `coupon_type_id` INT NOT NULL COMMENT '优惠券类型ID',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总数量',
  `remain_count` INT NOT NULL DEFAULT 0 COMMENT '剩余数量',
  `used_count` INT DEFAULT 0 COMMENT '已使用数量',
  `start_time` DATETIME NOT NULL COMMENT '抢购开始时间',
  `end_time` DATETIME NOT NULL COMMENT '抢购结束时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态(0=未开始,1=进行中,2=已结束,3=已售罄)',
  `create_user_id` INT NOT NULL COMMENT '创建商家ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_coupon_type_time` (`coupon_type_id`, `start_time`, `end_time`),
  INDEX `idx_create_user` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券库存表';

-- 8. 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户优惠券ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `coupon_type_id` INT NOT NULL COMMENT '优惠券类型ID',
  `coupon_stock_id` INT NOT NULL COMMENT '库存ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态(0=未使用,1=已使用,2=已过期)',
  `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `end_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '过期时间',
  `use_order_id` INT DEFAULT NULL COMMENT '使用订单ID',
  UNIQUE KEY `uk_user_stock` (`user_id`, `coupon_stock_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_end_time` (`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- 9. 订单主表（order 是 MySQL 保留字，表名使用 t_order）
CREATE TABLE IF NOT EXISTS `t_order` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `order_type` TINYINT NOT NULL COMMENT '订单类型(1=普通订单,2=团购订单,3=秒杀订单(预留))',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `group_buy_id` INT DEFAULT NULL COMMENT '团购活动ID(团购订单)',
  `address_id` INT DEFAULT NULL COMMENT '收货地址ID(user_address.id)',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `user_coupon_id` INT DEFAULT NULL COMMENT '使用的用户优惠券ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态(0=待支付,1=待发货,2=已发货,3=已完成,4=已取消,5=退款中,6=已退款)',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
  `cancel_reason` VARCHAR(255) COMMENT '取消原因',
  `remark` VARCHAR(500) COMMENT '订单备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_order_no` (`order_no`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_order_type` (`order_type`),
  INDEX `idx_group_buy_id` (`group_buy_id`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 10. 订单明细表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
  `order_id` INT NOT NULL COMMENT '订单ID',
  `product_id` INT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `sku_id` INT COMMENT 'SKU ID',
  `sku_name` VARCHAR(100) COMMENT 'SKU名称',
  `product_img` VARCHAR(500) COMMENT '商品图片',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `coupon_discount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠券减免',
  UNIQUE KEY `uk_order_product` (`order_id`, `product_id`, `sku_id`),
  INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 11. 支付流水表
CREATE TABLE IF NOT EXISTS `payment_log` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  `order_id` INT NOT NULL COMMENT '订单ID',
  `pay_type` TINYINT NOT NULL COMMENT '支付方式(1=微信,2=支付宝,3=积分兑换)',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `pay_status` TINYINT DEFAULT 0 COMMENT '支付状态(0=待支付,1=已支付,2=失败)',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `third_party_trade_no` VARCHAR(64) COMMENT '第三方交易号',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- 12. 退款订单表
CREATE TABLE IF NOT EXISTS `refund_order` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '退款ID',
  `order_id` INT NOT NULL COMMENT '原订单ID',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `refund_reason` VARCHAR(255) COMMENT '退款原因',
  `refund_status` TINYINT DEFAULT 0 COMMENT '状态(0=待处理,1=已通过,2=已拒绝,3=已退款)',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
  `remark` VARCHAR(255) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_refund_status` (`refund_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款订单表';

-- 13. 收货地址表
CREATE TABLE IF NOT EXISTS `user_address` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '地址ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人电话',
  `province` VARCHAR(50) COMMENT '省份',
  `city` VARCHAR(50) COMMENT '城市',
  `district` VARCHAR(50) COMMENT '区县',
  `detail_address` VARCHAR(200) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认(1=是)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- 14. 用户角色表（普通用户/商家/管理员；status 控制角色是否生效）
CREATE TABLE IF NOT EXISTS `user_role` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `role_type` TINYINT NOT NULL COMMENT '角色类型(1=普通用户,2=商家,3=管理员)',
  `status` TINYINT DEFAULT 1 COMMENT '状态(1=正常,0=禁用)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_role` (`user_id`, `role_type`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_role_type` (`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色表';

-- 15. 商家认证信息表
CREATE TABLE IF NOT EXISTS `merchant_info` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '商家信息ID',
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID(与 user.id 类型一致,外键要求)',
  `shop_name` VARCHAR(100) NOT NULL COMMENT '店铺名称',
  `shop_logo` VARCHAR(500) COMMENT '店铺头像',
  `shop_description` TEXT COMMENT '店铺描述',
  `license_number` VARCHAR(50) NOT NULL COMMENT '营业执照号',
  `license_img` VARCHAR(500) COMMENT '营业执照图片',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系人电话',
  `province` VARCHAR(50) COMMENT '省份',
  `city` VARCHAR(50) COMMENT '城市',
  `district` VARCHAR(50) COMMENT '区县',
  `address` VARCHAR(200) COMMENT '详细地址',
  `merchant_status` TINYINT DEFAULT 0 COMMENT '商家状态(0=待审核,1=审核通过,2=审核拒绝,3=已封禁)',
  `audit_remark` VARCHAR(255) COMMENT '审核备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  UNIQUE KEY `uk_user` (`user_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_merchant_status` (`merchant_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家认证信息表';

-- 16. 商家店铺统计表（按日统计）
CREATE TABLE IF NOT EXISTS `merchant_stats` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
  `merchant_id` INT NOT NULL COMMENT '商家ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `order_count` INT DEFAULT 0 COMMENT '订单数',
  `sales_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '销售额',
  `product_view_count` INT DEFAULT 0 COMMENT '商品浏览量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_merchant_date` (`merchant_id`, `stat_date`),
  INDEX `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家统计表';

-- ============================================
-- 角色种子数据（按需手动执行，user_id 换成实际用户ID）
-- ============================================
-- 管理员角色（审核商家需要）：
-- INSERT INTO user_role(user_id,role_type,status,create_time,update_time) VALUES(7,3,1,NOW(),NOW());
-- 商家角色无需手动种：认证申请/审核流程会自动写入并管理 user_role(role_type=2) 的生效状态。

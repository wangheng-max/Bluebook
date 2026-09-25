-- ============================================
-- 商城团购功能 - 接口测试数据（可重复执行，幂等）
-- 说明：仅插入测试数据，不修改/删除任何已有数据
-- 依赖：先执行 mall_migration.sql（16 张表已建好）
-- 账号密码统一为 123456
-- ============================================

-- ---------- 1. 测试账号 ----------
-- t_user      普通用户（下单/抢券）
-- t_merchant  已认证商家（发商品/团购/券）
-- t_merchant2 待审核商家（测管理员审核流程）
-- t_admin     管理员（审核商家/维护分类）
INSERT INTO `user`(username, password, nickname, email, user_pic, create_time, update_time)
SELECT 't_user', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', 't_user@test.com', '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 't_user');

INSERT INTO `user`(username, password, nickname, email, user_pic, create_time, update_time)
SELECT 't_merchant', 'e10adc3949ba59abbe56e057f20f883e', '测试商家', 't_merchant@test.com', '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 't_merchant');

INSERT INTO `user`(username, password, nickname, email, user_pic, create_time, update_time)
SELECT 't_merchant2', 'e10adc3949ba59abbe56e057f20f883e', '待审商家', 't_merchant2@test.com', '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 't_merchant2');

INSERT INTO `user`(username, password, nickname, email, user_pic, create_time, update_time)
SELECT 't_admin', 'e10adc3949ba59abbe56e057f20f883e', '测试管理员', 't_admin@test.com', '', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username = 't_admin');

-- ---------- 2. 角色（role_type: 1=普通用户 2=商家 3=管理员，status: 1=生效 0=待生效/禁用）----------
INSERT INTO `user_role`(user_id, role_type, status, create_time, update_time)
SELECT u.id, 1, 1, NOW(), NOW() FROM `user` u
WHERE u.username = 't_user'
  AND NOT EXISTS (SELECT 1 FROM `user_role` r WHERE r.user_id = u.id AND r.role_type = 1);

INSERT INTO `user_role`(user_id, role_type, status, create_time, update_time)
SELECT u.id, 2, 1, NOW(), NOW() FROM `user` u
WHERE u.username = 't_merchant'
  AND NOT EXISTS (SELECT 1 FROM `user_role` r WHERE r.user_id = u.id AND r.role_type = 2);

INSERT INTO `user_role`(user_id, role_type, status, create_time, update_time)
SELECT u.id, 2, 0, NOW(), NOW() FROM `user` u
WHERE u.username = 't_merchant2'
  AND NOT EXISTS (SELECT 1 FROM `user_role` r WHERE r.user_id = u.id AND r.role_type = 2);

INSERT INTO `user_role`(user_id, role_type, status, create_time, update_time)
SELECT u.id, 3, 1, NOW(), NOW() FROM `user` u
WHERE u.username = 't_admin'
  AND NOT EXISTS (SELECT 1 FROM `user_role` r WHERE r.user_id = u.id AND r.role_type = 3);

-- ---------- 3. 商家认证信息（merchant_status: 0=待审核 1=通过 2=拒绝 3=封禁）----------
INSERT INTO `merchant_info`(user_id, shop_name, shop_logo, shop_description, license_number, license_img,
                            contact_name, contact_phone, province, city, district, address, merchant_status,
                            create_time, update_time)
SELECT u.id, '测试旗舰店', '', '接口测试用认证商家', 'LICENSE-TEST-0001', '',
       '张三', '13800138000', '广东省', '深圳市', '南山区', '科技园1号', 1, NOW(), NOW()
FROM `user` u
WHERE u.username = 't_merchant'
  AND NOT EXISTS (SELECT 1 FROM `merchant_info` m WHERE m.user_id = u.id);

INSERT INTO `merchant_info`(user_id, shop_name, shop_logo, shop_description, license_number, license_img,
                            contact_name, contact_phone, province, city, district, address, merchant_status,
                            create_time, update_time)
SELECT u.id, '待审小店', '', '用于测试管理员审核流程', 'LICENSE-TEST-0002', '',
       '李四', '13900139000', '广东省', '广州市', '天河区', '体育西路2号', 0, NOW(), NOW()
FROM `user` u
WHERE u.username = 't_merchant2'
  AND NOT EXISTS (SELECT 1 FROM `merchant_info` m WHERE m.user_id = u.id);

-- ---------- 4. 商品分类（系统分类，管理员可维护）----------
INSERT INTO `product_category`(name, parent_id, icon, sort_order, is_system, create_time, update_time)
SELECT '数码电器', 0, '', 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `product_category` WHERE name = '数码电器' AND parent_id = 0);

INSERT INTO `product_category`(name, parent_id, icon, sort_order, is_system, create_time, update_time)
SELECT '服饰鞋包', 0, '', 2, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `product_category` WHERE name = '服饰鞋包' AND parent_id = 0);

INSERT INTO `product_category`(name, parent_id, icon, sort_order, is_system, create_time, update_time)
SELECT '食品生鲜', 0, '', 3, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `product_category` WHERE name = '食品生鲜' AND parent_id = 0);

-- ---------- 5. 样例商品 + SKU（商家 t_merchant 名下，用于下单/团购测试）----------
INSERT INTO `product`(name, category_id, cover_img, images, description, price, market_price,
                      status, stock, sales_count, view_count, create_user_id, create_time, update_time)
SELECT '测试商品-蓝牙耳机',
       (SELECT id FROM `product_category` WHERE name = '数码电器' AND parent_id = 0 LIMIT 1),
       'https://dummyimage.com/600x600/409eff/ffffff.png&text=TestProduct',
       '["https://dummyimage.com/600x600/409eff/ffffff.png&text=1"]',
       '接口测试用商品', 199.00, 299.00, 1, 100, 0, 0, u.id, NOW(), NOW()
FROM `user` u
WHERE u.username = 't_merchant'
  AND NOT EXISTS (SELECT 1 FROM `product` WHERE name = '测试商品-蓝牙耳机');

INSERT INTO `product_sku`(product_id, sku_name, spec_values, price, stock, sales_count)
SELECT p.id, '黑色', '["黑色"]', 199.00, 60, 0 FROM `product` p
WHERE p.name = '测试商品-蓝牙耳机'
  AND NOT EXISTS (SELECT 1 FROM `product_sku` s WHERE s.product_id = p.id AND s.sku_name = '黑色');

INSERT INTO `product_sku`(product_id, sku_name, spec_values, price, stock, sales_count)
SELECT p.id, '白色', '["白色"]', 209.00, 40, 0 FROM `product` p
WHERE p.name = '测试商品-蓝牙耳机'
  AND NOT EXISTS (SELECT 1 FROM `product_sku` s WHERE s.product_id = p.id AND s.sku_name = '白色');

-- ---------- 6. 进行中的团购活动（3 人成团，7 天后结束）----------
INSERT INTO `group_buy`(product_id, title, description, group_price, group_size, progress_count,
                        max_group_size, start_time, end_time, status, create_user_id, create_time, update_time)
SELECT p.id, '蓝牙耳机3人团', '接口测试用团购', 149.00, 3, 0, 50,
       DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, u.id, NOW(), NOW()
FROM `product` p
JOIN `user` u ON u.username = 't_merchant'
WHERE p.name = '测试商品-蓝牙耳机'
  AND NOT EXISTS (SELECT 1 FROM `group_buy` WHERE title = '蓝牙耳机3人团');

-- ---------- 7. 优惠券类型 + 发放活动（进行中，库存 100）----------
INSERT INTO `coupon_type`(name, min_spend, discount_amount, discount_rate, valid_days, max_uses,
                          create_user_id, create_time)
SELECT '测试满减券', 100.00, 20.00, 1.00, 7, 0, u.id, NOW()
FROM `user` u
WHERE u.username = 't_merchant'
  AND NOT EXISTS (SELECT 1 FROM `coupon_type` WHERE name = '测试满减券');

INSERT INTO `coupon_stock`(coupon_type_id, total_count, remain_count, used_count, start_time, end_time,
                           status, create_user_id, create_time, update_time)
SELECT t.id, 100, 100, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY),
       1, t.create_user_id, NOW(), NOW()
FROM `coupon_type` t
WHERE t.name = '测试满减券'
  AND NOT EXISTS (SELECT 1 FROM `coupon_stock` s WHERE s.coupon_type_id = t.id);

-- ---------- 8. 收货地址（用户 t_user 的默认地址）----------
INSERT INTO `user_address`(user_id, receiver_name, receiver_phone, province, city, district,
                           detail_address, is_default, create_time, update_time)
SELECT u.id, '测试用户', '13800138000', '广东省', '深圳市', '南山区', '科技园2号', 1, NOW(), NOW()
FROM `user` u
WHERE u.username = 't_user'
  AND NOT EXISTS (SELECT 1 FROM `user_address` a WHERE a.user_id = u.id AND a.receiver_name = '测试用户');

-- ---------- 结果核对 ----------
SELECT '用户' AS item, COUNT(*) AS cnt FROM `user` WHERE username IN ('t_user','t_merchant','t_merchant2','t_admin')
UNION ALL SELECT '角色', COUNT(*) FROM `user_role` WHERE role_type IN (1,2,3)
UNION ALL SELECT '商家认证', COUNT(*) FROM `merchant_info`
UNION ALL SELECT '分类', COUNT(*) FROM `product_category`
UNION ALL SELECT '商品', COUNT(*) FROM `product` WHERE name = '测试商品-蓝牙耳机'
UNION ALL SELECT 'SKU', COUNT(*) FROM `product_sku`
UNION ALL SELECT '团购', COUNT(*) FROM `group_buy` WHERE title = '蓝牙耳机3人团'
UNION ALL SELECT '券类型', COUNT(*) FROM `coupon_type` WHERE name = '测试满减券'
UNION ALL SELECT '券活动', COUNT(*) FROM `coupon_stock`
UNION ALL SELECT '地址', COUNT(*) FROM `user_address`;

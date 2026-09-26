# 更新日志（CHANGELOG）

本文件记录「小蓝书」的功能迭代与重要修复。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本以日期划分（学习型项目，未采用语义化版本号）。

---

## 2026-09-26

### 新增
- **内容电商打通（社区 × 商城）**
  - 发布文章可选择标签（热门标签联想 + 自定义，最多 5 个），替代原手填文本框
  - Feed 卡片与文章详情展示标签，点击直接进入标签检索结果（`/community/feed?tag=`）
  - 发布文章可搜索商城在架商品挂「带货商品」（最多 3 个，新增 `article.product_ids` 列）
  - Feed 卡片展示带货条、文章详情展示商品卡，点击直达商品详情页；下架商品置灰
- **商家评分系统（淘宝式一单一评）**
  - 新表 `merchant_rating`（`uk_order` 唯一键防重复评价）
  - 订单已发货/完成后可在「我的订单」评价商家：1-5 星（支持半星）+ 文字评价
  - 评价对象按订单明细自动解析所属店铺；接口校验只能评自己的订单
- **店铺主页（公开）**
  - `GET /shop/{merchantUserId}`：店铺信息 + 平均分/评价数 + 在架商品数
  - `GET /shop/{merchantUserId}/products`、`/ratings`：全部在架商品、买家评价（分页）
  - 入口：商品详情店铺卡片（店名 + 星级 + 进店按钮）、文章详情「进店逛逛」、路由 `/mall/shop/:merchantUserId`
- **热门标签接口** `GET /community/hot-tags`：统计近期已发布文章标签词频 Top N

### 修复
- **团购可重复参团/成团后仍可参团（无限买）**：下单防重改为查订单表有效订单（原查支付后才写入的参团记录表）；人数达标后禁止再下单；支付前复检活动状态；满员**即时成团**（状态置 2 + WebSocket 通知）；团购单笔限购不超过成团门槛
- **优惠券过期任务 SQL 语法错误**：`@Update` 注解中误写 HTML 转义符 `&lt;`（注解 SQL 应使用原始 `<`），导致 `CouponExpireTask` 每次执行报 `BadSqlGrammarException`
- **重构期间引入的 XML 引用截断**：7 个 Mapper XML 的 namespace 与 resultType 被截断类名，已按 git 原始内容逐一配对还原（详见重构条目）

### 变更
- **收货地址**：省市区由手填文本框改为可搜索级联下拉（element-china-area-data），省市区必填
- **商家入驻**：所在地区同样改为级联下拉并必填
- **密码规则**：统一为 6-16 位且须同时包含字母和数字——后端注册 `@Pattern` + 改密接口校验，前端注册/重置密码页同规则 + 格式提示文案；登录不校验格式，兼容老账号
- **商品上传**：`spring.servlet.multipart` 上限从默认 1MB 放开至单文件 50MB；`AliOssUtil` 按扩展名设置 OSS Content-Type（修复 Safari 播视频黑屏/图片渲染问题）；新增 `MaxUploadSizeExceededException` 友好提示
- **商品视频**：发布商品「视频地址」增加视频上传入口（accept=video/*，走 /upload），详情页 video 加载失败显示内联提示

### 性能
- 库存变动（下单扣减/取消回补/退款回补）后统一失效商品缓存 `product:*`，修复页面库存数最长 5 分钟不更新的问题

### 重构
- **后端包结构由「按层」改为「按业务域」**：`controller/service/mapper/pojo` 大平层 → 14 个业务模块（user/address/article/comment/friend/message/product/groupbuy/coupon/order/merchant/search/admin/file），模块内统一 controller/service(impl)/mapper/pojo 子包；跨切设施（anno/aspect/auth/config/exception/interceptors/task/utils/websocket）保留；共享类收敛至 `common/`；7 个 Mapper XML 与接口同包镜像迁移；接口路径零变化

---

## 2026-09-25

### 新增
- **商城团购模块 v1.1（商家认证版）**：商品/SKU、团购、优惠券（抢券限流）、订单（状态机/防超卖/退款）、收货地址、商家认证与管理员审核、商家工作台五页
- 评论模块：文章/商品双目标评论、点赞、回复、排序
- 社区 Feed 集成商品详情评论
- 修复记录见《商城开发Bug清单》：B-004 新增公开 SKU 列表接口、B-005 商家编辑回显、B-006 前端错误提示字段兼容

### 已知问题登记
- B-001/B-002/B-003/B-007~B-009 待确认项详见《商城开发Bug清单》（本地文档）

---

## 2026-09-25（更早）

### 首批提交
- 社区平台基线：用户/文章/分类、热点推荐（ZSET + 时间衰减）、全文搜索（ngram）、点赞/收藏/转发、好友聊天（WebSocket）、定时落库任务

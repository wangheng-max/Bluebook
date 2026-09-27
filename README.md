# 小蓝书 · 社区（Big Event）

基于 **Spring Boot 3 + Vue 3** 的「社区 + 社区电商」一体化平台：

- **内容社区**：图文笔记发布、热点推荐、分类浏览、全文/标签搜索、点赞收藏转发、好友聊天（WebSocket）
- **商城电商**：商品与规格管理、下单支付、退款售后、库存防超卖
- **团购与优惠券**：多人成团（人数达标即时成团/到期未成团自动退款）、限时抢券（Redis 防超卖）
- **商家体系**：商家入驻认证（管理员审核）、店铺主页、买家评分、商家独立工作台
- **内容带货**：文章挂载商品标签，读者点击直达商品页，社区种草 → 下单闭环

---

## 目录

- [功能总览](#功能总览)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [数据库设计](#数据库设计)
- [Redis 设计](#redis-设计)
- [接口速览](#接口速览)
- [WebSocket 协议](#websocket-协议)
- [配置说明](#配置说明)
- [注意事项](#注意事项)
- [相关文档](#相关文档)

---

## 功能总览

### 用户侧
| 模块 | 能力 |
|---|---|
| 社区 | 图文笔记（富文本）、热点榜（Redis ZSET + 时间衰减）、系统分类浏览、全文搜索（ngram）/标签搜索、点赞/收藏/转发 |
| 好友 | 申请/确认/拒绝/删除、WebSocket 实时聊天、文章转发卡片、离线消息与已读回执 |
| 商城 | 商品搜索/详情（图集+视频）、SKU 规格选择、地址管理（省市区级联选择）、下单/模拟支付/取消/退款 |
| 团购 | 团购列表/详情/进度、参团下单（防重复参团、满员拦截、单笔限购）、满员即时成团、未成团自动退款 |
| 优惠券 | 券中心抢券（Redis 原子扣减 + 限流）、我的优惠券、下单抵扣（满减/折扣） |
| 个人 | 资料维护、头像上传（OSS）、密码修改（6-16 位须含字母和数字）、我的订单/团购/优惠券 |

### 商家侧
- 入驻认证：提交资质（营业执照号 + 图片、省市区级联）→ 管理员审核 → 拒绝后可重新提交
- 商品管理：发布/编辑/下架，封面 + 最多 5 张详情图 + 商品视频（OSS 上传）
- 团购管理：为本店商品创建团购（时间/价格/成团人数校验）、关闭活动
- 优惠券管理：创建券类型、发放活动（限量库存）
- 订单管理：本店订单列表、模拟发货、退款审核（通过自动回补库存）
- 店铺主页：公开访问，展示店铺信息、评分、全部在架商品、买家评价

### 平台管理侧
- 商家审核（通过/拒绝/封禁）、商品分类管理

---

## 技术栈

| 端 | 技术 | 说明 |
|---|---|---|
| 后端 | Java 21 / Spring Boot 3.5.9 | 按业务域分包（package-by-feature） |
| 后端 | MyBatis 3.0.4 | 注解 SQL + XML 动态 SQL（XML 与接口同包镜像） |
| 后端 | MySQL 5.7+/8.x | ngram 全文索引、联合索引、唯一键防重 |
| 后端 | Redis 5+ | 会话、热点榜、互动计数、抢券、商品/团购缓存 |
| 后端 | JWT (auth0) 4.4.0 + Redis 会话 | 改密即失效 |
| 后端 | PageHelper 1.4.6 / 阿里云 OSS 3.15.1 / Lombok | 分页 / 对象存储 / 简化 POJO |
| 前端 | Vue 3 + Vite 4 + Pinia + Vue Router | 组合式 API |
| 前端 | Element Plus / vue-quill / element-china-area-data | 组件库 / 富文本 / 省市区数据 |

---

## 项目结构

后端按**业务域分包**，每个模块内统一 `controller / service(impl) / mapper / pojo` 子包：

```
big-event/
├── big-event/                                # 后端 Spring Boot（包名 com.wangheng）
│   └── src/main/java/com/wangheng/
│       ├── common/                           # Result / PageBean（跨模块共享）
│       ├── user/  address/                   # 用户账号、收货地址
│       ├── article/ comment/ friend/ message/# 社区：文章/评论/好友/消息
│       ├── product/ groupbuy/ coupon/ order/ # 商城：商品/团购/优惠券/订单
│       ├── merchant/                         # 商家认证/店铺/评分
│       ├── search/ admin/ file/              # 搜索/平台管理/上传
│       ├── anno/ aspect/ auth/               # @RequireAdmin/@RequireMerchant + 切面鉴权
│       ├── config/ interceptors/             # WebConfig / LoginInterceptor(JWT+Redis)
│       ├── exception/ validation/            # 全局异常 / 自定义校验
│       ├── task/                             # 定时任务（热度/对账/结算/超时/券过期）
│       ├── utils/ websocket/                 # 工具 / WebSocket 聊天
│       └── BigEventApplication.java
│   └── src/main/resources/
│       ├── application.yml / application-dev.yml
│       ├── com/wangheng/<模块>/mapper/*.xml   # Mapper XML（与接口同包镜像）
│       └── sql/                              # 数据库迁移脚本（按序执行，见快速启动）
├── frontend/                                 # 前端 Vue3
│   └── src/
│       ├── api/                              # axios 按模块封装
│       ├── views/ mall/ merchant/ admin/ community/ article/ user/ friend/
│       ├── components/                       # ImageUploader / CommentSection / ChatWindow
│       └── stores/ router/ utils/ assets/
└── README.md / CHANGELOG.md
```

---

## 快速启动

### 环境要求
JDK 21+ · Maven 3.8+ · MySQL 5.7+（建议 8.x）· Redis 5+ · Node.js 16+

### 1. 初始化数据库
```sql
CREATE DATABASE big_event DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
**按顺序**执行 `big-event/src/main/resources/sql/` 下 4 个脚本：

| 顺序 | 脚本 | 内容 |
|---|---|---|
| 1 | `migration.sql` | 社区功能 DDL（user/category/article/friend/互动/message + 全文索引） |
| 2 | `mall_migration.sql` | 商城团购 DDL（商品/SKU/团购/优惠券/订单/地址/商家/角色等 17 张表） |
| 3 | `comment_migration.sql` | 评论模块（comment/comment_like） |
| 4 | `content_shop_migration.sql` | 内容带货 + 商家评分（article.product_ids 列 + merchant_rating 表） |
| 5 | `social_migration.sql` | 社交与评价（comment 评分/晒图列、user.bio、follow 表） |
| 6 | `favorite_folder_migration.sql` | 收藏夹（article_favorite.folder_id 列 + favorite_folder 表） |

> 除 `migration.sql` 第 6 步（重建分类表，依赖旧外键名）外，其余语句均 `IF NOT EXISTS` / 幂等，可重复执行。

### 2. 配置后端
编辑 `big-event/src/main/resources/application-dev.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/big_event
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379
```
OSS 凭证用环境变量注入（不配置仅图片/视频上传不可用）：
```bash
# Windows PowerShell
$env:OSS_ACCESS_KEY_ID = "你的AccessKeyId"
$env:OSS_ACCESS_KEY_SECRET = "你的AccessKeySecret"
```

### 3. 启动
```bash
# 后端（localhost:8080）
cd big-event && mvn spring-boot:run

# 前端（localhost:5173，/api 与 /ws 已代理到 8080）
cd frontend && npm install && npm run dev
```

### 4. 验证主链路
1. 注册（密码须 6-16 位含字母和数字）→ 登录
2. 发布笔记：选标签（可自建）、搜索商品挂带货卡 → 社区 Feed 点标签检索、点带货卡跳商品页
3. 商城下单支付 → 商家工作台发货 → 我的订单「评价商家」→ 商品详情进店铺页看评分
4. 抢券下单抵扣；两人参团达标即时成团，到期未成团自动退款

---

## 数据库设计

数据库 `big_event`（utf8mb4），共 24 张表，按域划分：

| 域 | 表 |
|---|---|
| 用户/地址/角色 | `user`、`user_role`、`user_address` |
| 社区 | `category`（is_system 内置分类）、`article`（tags/product_ids）、`article_like/_favorite/_forward` |
| 评论 | `comment`、`comment_like` |
| 好友/消息 | `friend_relation`、`message` |
| 商品 | `product_category`、`product`、`product_sku` |
| 团购 | `group_buy`、`group_buy_record`（uk_user_group 一人一单） |
| 优惠券 | `coupon_type`、`coupon_stock`、`user_coupon`（uk_user_stock 防重抢） |
| 订单 | `t_order`、`order_item`（商品快照）、`payment_log`、`refund_order` |
| 商家 | `merchant_info`、`merchant_rating`（uk_order 一单一评） |

关键防重/防超卖设计：
- 库存扣减用条件更新 `UPDATE ... SET stock=stock-? WHERE stock>=?`，取消/退款回补
- `group_buy_record(user_id, group_buy_id)`、`merchant_rating(order_id)`、`article_like(article_id, user_id)` 等唯一键兜底防重
- 抢券走 Redis 原子扣减 + 限流器，库存由对账任务校准

---

## Redis 设计

| Key | 类型 | 用途 | 维护 |
|---|---|---|---|
| `token:{token}` | String | 登录会话 | TTL 1h，登出/改密删除 |
| `hot:articles` | ZSET | 热点榜（浏览×1+赞×2+藏×3+转×2，按时间衰减） | 互动实时 ZINCRBY；10 分钟全量重建 |
| `article:like:{id}` / `article:fav:{id}` | SET | 点赞/收藏集合 | 每 5 分钟对账落库 |
| `article:view:{id}` | String | 浏览计数 | 每 5 分钟增量写库 |
| `search:notes:*` | String(JSON) | 搜索结果缓存 | TTL 5 分钟，发文即清 |
| `product:{id}` / `product:list:*` / `product:search:*` | String(JSON) | 商品详情/列表/搜索缓存 | TTL 5-10 分钟；库存变动即失效 |
| `group-buy:{id}` / `group-buy:list:*` | String(JSON) | 团购详情/列表缓存 | 参团/成团/结算时失效 |
| `coupon:user:available:{uid}` | String(JSON) | 可用券缓存 | 下单/退券失效 |
| `coupon:stock:{id}` | String | 抢券库存（原子扣减 + 限流器） | 定时任务对账校准 |

所有 Redis 读写均 try-catch（fail-open），Redis 故障不阻断主流程。

---

## 接口速览

> 统一响应 `Result<T>{ code(0=成功), message, data }`。除白名单（登录/注册/商城公开读/店铺页/评论列表等）外均需请求头 `Authorization: <token>`。

| 模块 | 代表接口 |
|---|---|
| 用户 `/user` | register、login、userInfo、update、updateAvatar、updatePwd |
| 社区 `/community` `article` `category` | hot、articles、article/{id}、hot-tags；发文/我的文章；分类 CRUD |
| 互动 `article/{id}` | like/favorite/forward、interact-status |
| 评论 `/comment` | 列表（公开）、发表/删除、点赞、回复 |
| 搜索 `/search` | notes（全文）、notes/by-tag（标签）、users |
| 好友/消息 `/friends` `/message` | 申请/确认/列表/删除；history、offline |
| 商品 `/product` | 列表/搜索/详情/规格（公开）、分类树；商家端 `/merchant/product` |
| 团购 `/group-buy` | 列表/详情/参与进度（公开）；商家端 `/merchant/group-buy`（创建/关闭/我的活动） |
| 优惠券 `/coupon` | 券类型/库存（公开）、抢券、我的券；商家端 `/merchant/coupon` |
| 订单 `/order` | 创建/支付/取消/退款；商家端 `/merchant/order`（列表/详情/发货/退款审核） |
| 店铺 `/shop/{merchantUserId}` | 店铺信息+评分、在架商品、买家评价（全部公开） |
| 商家 `/merchant` | apply/re-submit/status、info（认证后）、rating（订单评价） |
| 地址 `/user/address` | CRUD（省市区级联存储） |
| 上传 `/upload` | 图片/视频（OSS，按扩展名设置 Content-Type，单文件 ≤50MB） |
| 管理 `/admin` | 商家审核/封禁/列表、商品分类管理（@RequireAdmin） |

---

## WebSocket 协议

- 地址：`ws://localhost:8080/ws/chat?token=<JWT>`（前端走 `/ws` 代理）
- 客户端发送：
```json
{ "toUserId": 2, "msgType": 0, "content": "你好", "articleId": null }
```
`msgType`：0 文本 / 1 文章转发卡片；服务端校验好友关系后落库，在线实时推送，离线转未读。
- 服务端还会推送系统通知：`ORDER_STATUS_CHANGE`（订单状态）、`GROUP_BUY_PROGRESS`（成团/退款）。

---

## 配置说明

| 配置项 | 位置 | 默认值 |
|---|---|---|
| 数据库 / Redis | `application-dev.yml` | root@localhost:3306 / localhost:6379 |
| 服务端口 | `application-dev.yml` | 8080 |
| 上传大小限制 | `application.yml` | 单文件 50MB / 单请求 100MB |
| OSS 凭证 | 环境变量 | `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`（Endpoint 默认北京） |
| 前端代理 | `frontend/vite.config.js` | /api、/ws → 8080 |

---

## 注意事项

- **包名** `com.wangheng`；后端按业务域分包，跨模块共享类收敛在 `common/`
- **JWT 密钥**为固定值，生产环境请外置
- **分类删除**受文章外键约束，需先处理关联文章
- **热点榜冷启动**：首次启动 ZSET 为空，接口会按浏览量兜底回填，属正常现象
- **数据脚本**：升级库结构务必按「快速启动」的脚本顺序补执行，尤其是 `content_shop_migration.sql`

---

## 相关文档

> 详细设计文档（商城团购功能开发文档、接口测试指南、前端页面说明、Bug 清单、前端优化方案）仅保留在本地，不入版本库。
> 版本变更记录见 [CHANGELOG.md](./CHANGELOG.md)。

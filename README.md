# 小蓝书 · 社区（Big Event）

基于 **Spring Boot 3 + Vue 3** 的社区内容平台：用户发布图文笔记、好友互关聊天、社区热点推荐、按分类浏览、全文搜索、点赞/收藏/转发互动，配套 Redis 缓存与定时落库、WebSocket 实时聊天。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [Redis 设计](#redis-设计)
- [核心模块](#核心模块)
- [接口文档](#接口文档)
- [WebSocket 协议](#websocket-协议)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [注意事项](#注意事项)

---

## 功能特性

### 基础功能
- 用户注册 / 登录（JWT + Redis 会话，1 小时有效期，改密即失效）
- 个人信息维护、头像上传（阿里云 OSS）、密码重置
- 图文笔记发布（富文本编辑器 vue-quill）、草稿与发布状态管理、分类管理

### 好友与搜索
- 好友申请 / 确认 / 拒绝 / 删除，好友列表分页与搜索
- 公开笔记全文搜索：MySQL ngram 全文索引 `MATCH...AGAINST`，单字符关键词自动回退 LIKE
- 标签搜索、用户搜索

### 社区功能（按《大事件社区功能扩展设计方案》实现）
- **社区首页**：热点推荐（浏览量 + 互动加权，Redis ZSET + 时间衰减）+ 系统内置分类浏览
- **内置分类**：10 个全局系统分类（`is_system=1`），全站共享，用户发布时从分类列表挑选，无需手动输入
- **互动**：点赞 / 收藏（Redis Set 实时计数 + 定时落库）、转发给好友（转发即聊天消息）
- **实时聊天**：好友间 WebSocket 文字消息与文章转发卡片（点击卡片跳转文章详情）
- **消息体系**：聊天历史、离线消息（上线拉取未读）、已读回执
- **计数展示**：文章卡片展示浏览量 / 点赞数 / 收藏数

### 性能优化
- 搜索与分类列表 Redis 缓存（TTL 5 分钟 / 1 天，发布文章自动失效）
- 热点榜 ZSET 内存计算，10 分钟全量重建 + 互动实时增量
- 互动计数先写 Redis，每 5 分钟批量落库，读写分离压力
- 联合索引：`(create_user, category_id, state)` 命中列表查询、`(category_id, state, create_time)` 命中分类浏览

---

## 技术栈

| 端 | 技术 | 版本 |
|---|---|---|
| 后端 | Java / Spring Boot | 21 / 3.5.9 |
| 后端 | MyBatis（注解 SQL + XML 动态 SQL） | 3.0.4 |
| 后端 | MySQL（含 ngram 全文索引） | 5.7+ / 8.x |
| 后端 | Redis（数据缓存 + 会话 + 计数） | 任意 5+ |
| 后端 | JWT（auth0 java-jwt） | 4.4.0 |
| 后端 | PageHelper 分页 | 1.4.6 |
| 后端 | Spring WebSocket | 内置 |
| 后端 | 阿里云 OSS SDK | 3.15.1 |
| 后端 | Lombok | 1.18.46 |
| 前端 | Vue 3 + Vite | 3 / 4.5.0 |
| 前端 | Pinia / Vue Router | — |
| 前端 | Element Plus / vue-quill | — |

---

## 系统架构

```
┌─────────────────────────── 前端 Vue3 (localhost:5173) ───────────────────────────┐
│  Login · Layout · CommunityFeed · ArticleDetail · ArticleManage · CategoryManage │
│  DiscoverUsers · FriendList · FriendRequests · ChatWindow(WebSocket)             │
└───────┬──────────────────────────────┬───────────────────────────────────────────┘
        │ /api/*  (Vite 代理→8080)      │ /ws/chat?token= (Vite ws 代理)
┌───────▼──────────────────────────────▼───────────────────────────────────────────┐
│                        Spring Boot 3.5.9  (localhost:8080)                        │
│                                                                                   │
│  LoginInterceptor ── JWT 校验 ── ThreadLocalUtil(当前用户上下文)                    │
│                                                                                   │
│  Controller 层: User / Article / Category / Search / Community / Interaction      │
│                Friend / Message / FileUpload                                      │
│  Service 层:    业务逻辑 + Redis 读写                                              │
│  Mapper 层:     MyBatis 注解 SQL + ArticleMapper.xml 动态 SQL                      │
│  WebSocket:     ChatWebSocketHandler (JWT 握手 → 会话管理 → 消息收发)               │
│  定时任务:      DataSyncTask (5min 计数落库 / 10min 热度重建)                      │
└───────┬──────────────────────────────┬───────────────────────────────────────────┘
        │                              │
┌───────▼──────────┐        ┌──────────▼──────────┐
│   MySQL big_event │        │  Redis localhost:6379│
│   user/category/  │        │  token:*  会话        │
│   article/friend_ │        │  category:list:* 分类 │
│   relation/       │        │  hot:articles 热点榜  │
│   article_like/_favorite/_forward/message │  计数  │
└──────────────────┘        └─────────────────────┘
```

一次请求的完整链路（以社区首页为例）：
1. 前端 `GET /community/hot`，`Authorization: Bearer <token>`
2. `LoginInterceptor` 校验 JWT + Redis 会话，把用户 ID 写入 `ThreadLocalUtil`
3. `CommunityController → CommunityService`：从 Redis `hot:articles` ZSET 取一页（冷启动时按浏览量查库兜底并回填 ZSET）
4. 返回 `Result<PageBean<NoteSearchVO>>`，前端渲染卡片列表

---

## 项目结构

```
big-event/
├── big-event/                          # 后端 Spring Boot 工程（包名 com.wangheng）
│   ├── src/main/java/com/wangheng/
│   │   ├── controller/                 # REST 接口层
│   │   ├── service/ + service/impl/    # 业务逻辑
│   │   ├── mapper/                     # MyBatis Mapper（注解 + XML）
│   │   ├── pojo/                       # 实体（Article/Category/User/Message…）
│   │   ├── vo/                         # 视图对象（NoteSearchVO/InteractStatusVO…）
│   │   ├── config/                     # WebConfig 拦截器 / WebSocketConfig
│   │   ├── interceptors/               # LoginInterceptor（JWT+Redis 校验）
│   │   ├── websocket/                  # ChatWebSocketHandler（聊天核心）
│   │   ├── task/                       # DataSyncTask（定时落库/热度重建）
│   │   ├── cache/                      # SearchCacheService（Redis 缓存封装，fail-open）
│   │   ├── utils/                      # JwtUtil / Md5Util / ThreadLocalUtil / AliOssUtil
│   │   ├── exception/ anno/ validation/ # 全局异常 / 自定义校验
│   │   └── BigEventApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml / application-dev.yml
│   │   ├── mapper/ArticleMapper.xml    # 搜索 + 分类浏览动态 SQL
│   │   └── sql/migration.sql           # 数据库迁移脚本（社区功能全部 DDL）
│   └── pom.xml
├── frontend/                           # 前端 Vue3 工程
│   ├── src/
│   │   ├── api/                        # axios 封装（user/article/category/search/community/interaction/friend/message）
│   │   ├── views/                      # Login / Layout / community / article / friend / user
│   │   ├── components/ChatWindow.vue   # WebSocket 聊天窗口（转发卡片）
│   │   ├── stores/                     # Pinia（token / userInfo）
│   │   ├── router/ main.js assets/
│   └── vite.config.js                  # /api、/ws 代理
├── 大事件社区功能扩展设计方案.docx      # 需求设计文档
└── README.md
```

---

## 数据库设计

数据库：`big_event`（utf8mb4），初始化脚本见 `big-event/src/main/resources/sql/migration.sql`。

### 表清单

| 表 | 说明 | 关键字段 |
|---|---|---|
| `user` | 用户 | id, username, password(MD5), nickname, user_pic |
| `category` | 文章分类（混合模式） | `is_system`(1=系统内置), `create_user`(**NULL=系统分类**, 有值=用户分类) |
| `article` | 文章/笔记 | title, content(富文本), cover_img, category_id, state(已发布/草稿), tags, `view_count/like_count/favorite_count/forward_count` |
| `friend_relation` | 好友关系 | user_id, friend_id, status(0待确认/1已接受/2已拒绝), 唯一键(user_id, friend_id) |
| `article_like` | 点赞明细 | article_id, user_id, 唯一键(article_id, user_id) |
| `article_favorite` | 收藏明细 | article_id, user_id, 唯一键(article_id, user_id) |
| `article_forward` | 转发明细 | article_id, from_user_id, to_user_id, 唯一键(article_id, from_user_id, to_user_id) |
| `message` | 聊天消息 | from_user_id, to_user_id, msg_type(0文本/1转发), content, article_id, is_read, create_time |

### 内置系统分类（is_system=1, create_user=NULL）

科技数码 / 生活日常 / 美食探店 / 旅行游记 / 游戏电竞 / 财经职场 / 教育学习 / 健康运动 / 影视娱乐 / 体育赛事

> 用户也可自建分类（`is_system=0, create_user=当前用户`），仅自己可见；社区首页只展示系统分类。

### 关键索引

| 索引 | 目的 |
|---|---|
| `idx_user_category_state(create_user, category_id, state)` | 「我的文章」列表过滤三条件 |
| `idx_category_state_time(category_id, state, create_time)` | 社区按分类浏览已发布文章 |
| `ft_title_content(title, content) WITH PARSER ngram` | 中文全文搜索（单字符自动回退 LIKE） |
| `idx_msg_pair(from_user_id, to_user_id, create_time)` | 两人聊天历史查询 |
| `uk_article_user(article_id, user_id)` ×2 | 点赞/收藏去重（配合 `INSERT IGNORE`） |
| `uk_forward(article_id, from_user_id, to_user_id)` | 同一篇文章不可重复转发给同一好友 |

---

## Redis 设计

| Key | 类型 | 用途 | 失效/维护 |
|---|---|---|---|
| `token:{token}` | String | 登录会话（JWT 的 Redis 侧校验） | TTL 1h，登出/改密删除 |
| `category:list:{userId}` | String(JSON) | 分类列表缓存（系统分类 + 本人自建） | TTL 1 天；增删改分类时清空 |
| `hot:articles` | ZSET | 热点榜（member=文章ID, score=热度分） | 互动实时 ZINCRBY；10 分钟全量重建 |
| `article:like:{id}` | SET | 点赞用户集合（member=userId） | 每 5 分钟对账落库后保留 |
| `article:fav:{id}` | SET | 收藏用户集合 | 每 5 分钟对账落库后保留 |
| `article:view:{id}` | String | 浏览计数（INCR 累加） | 每 5 分钟取增量写库后清空 |
| `search:notes:kw:{kw}:{page}:{size}` | String(JSON) | 关键词搜索结果缓存 | TTL 5 分钟；发布文章时清空 |
| `search:notes:tag:{tag}:{page}:{size}` | String(JSON) | 标签搜索结果缓存 | 同上 |

### 热度算法

```
score = 浏览量×1 + 点赞数×2 + 收藏数×3 + 转发数×2
最终分 = score / log2(发布时间距今天数 + 2)     # 时间衰减，新文章更易上榜
```

- 用户浏览/点赞/收藏/转发时对 ZSET 实时 `ZINCRBY`；
- `DataSyncTask` 每 10 分钟从库中全量重建一次（`findAllPublished` 计算衰减分），保证与库一致。

### 缓存失效策略

- 发布/编辑/删除文章 → 清空全部搜索缓存（`searchCacheService.evictNotes()`）
- 分类增删改 → 清空分类缓存
- 所有 Redis 操作均 try-catch（fail-open），Redis 挂掉不影响主流程

---

## 核心模块

### 1. 认证与会话
- `POST /user/login`：MD5 校验 → 生成 JWT（含 id/username）→ 写入 Redis `token:{token}`（TTL 1h）
- `LoginInterceptor`：除白名单外所有接口校验 `Authorization` 头 → JWT 解析 + Redis 存在性校验 → 用户 ID 写入 `ThreadLocalUtil`
- 改密/登出删除 Redis 会话，令牌立即失效
- 白名单：`/user/login`、`/user/register`、`/search/notes`、`/search/notes/by-tag`

### 2. 文章与分类（is_system 混合模式）
- 发布文章必须携带 `categoryId`，后端通过 `categoryService.isValidForUser(id)` 校验：
  该分类存在，且是系统分类（`is_system=1`）或属于当前用户
- 分类列表 = `WHERE is_system = 1 OR create_user = #{userId}`，Redis 缓存 key 按用户区分
- 用户自建分类 `is_system=0`，仅本人可见、可管理

### 3. 社区首页
- `GET /community/hot`：ZSET `reverseRange` 分页取榜；冷启动（ZSET 为空）按 `view_count` 查库取 Top100 回填
- `GET /community/articles?categoryId=`：PageHelper 分页 + 索引命中
- `GET /community/article/{id}`：文章详情，同时 `INCR article:view:{id}` 计一次浏览

### 4. 点赞 / 收藏 / 转发
- 点赞：`SADD/SREM article:like:{id}` + `ZINCRBY hot:articles ±2`；收藏 ±3
- 转发：校验好友关系 → 写 `article_forward` + 计数 +1 + 热度 +2 → 生成 `msg_type=1` 聊天消息（含 articleId）→ WebSocket 实时推送
- 计数读取：实时走 Redis `SCARD`，落库后由定时任务同步

### 5. 搜索（ngram 全文索引）
- `MATCH(title, content) AGAINST(?)`（关键词长度 > 1）；长度 = 1 自动回退 `LIKE '%x%'`
- 结果经 Redis 缓存（5 分钟），`NoteSearchVO` 统一由 `NoteVOConverter` 转换（摘要截断 150 字、计数默认 0）

### 6. 好友聊天（WebSocket）
- 握手：`ws://localhost:8080/ws/chat?token=<JWT>`，`JwtHandshakeInterceptor` 解析 token 后放入 session attributes
- 消息协议见下方 [WebSocket 协议](#websocket-协议)
- 离线消息：接收方不在线时消息落库为未读，上线后 `GET /message/offline` 拉取并置已读

### 7. 定时任务（DataSyncTask）
| 周期 | 任务 |
|---|---|
| 每 5 分钟 | ① 浏览计数落库：`INCR` 取增量 → 累加到 view_count → 删除 key<br>② 互动对账：Set 成员 `INSERT IGNORE` 进明细表 → 删除已移除成员 → `SCARD` 同步计数 |
| 每 10 分钟 | 热度重建：全量已发布文章按衰减公式重算 score，重建 `hot:articles` |

---

## 接口文档

> 统一响应：`Result<T>{ code(0=成功), message, data }`；除白名单外全部需要请求头 `Authorization: <token>`

### 用户 `UserController`
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/user/register` | 注册（白名单） |
| POST | `/user/login` | 登录，返回 token |
| GET | `/user/userInfo` | 当前用户信息 |
| PUT | `/user/update` | 更新昵称/邮箱 |
| PATCH | `/user/updateAvatar` | 更新头像 URL |
| PATCH | `/user/updatePwd` | 重置密码（原密码+新密码） |

### 分类 `CategoryController`
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/category/list` | 分类列表（系统分类+本人自建，Redis 缓存；`GET /category` 为等价别名） |
| GET | `/category/detail` | 分类详情 |
| POST/PUT/DELETE | `/category` | 增/改/删（仅用户自建分类） |

### 文章 `ArticleController`
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/article` | 发布文章（categoryId 必填且必须有效） |
| GET | `/article` | 我的文章分页（pageNum/pageSize/categoryId/state） |

### 社区 `CommunityController`
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/community/hot` | 热点文章（pageNum/pageSize） |
| GET | `/community/articles` | 按分类浏览已发布文章（categoryId/pageNum/pageSize） |
| GET | `/community/article/{id}` | 文章详情（浏览 +1） |

### 互动 `InteractionController`
| 方法 | 路径 | 说明 |
|---|---|---|
| POST / DELETE | `/article/{id}/like` | 点赞 / 取消点赞，返回最新点赞数 |
| POST / DELETE | `/article/{id}/favorite` | 收藏 / 取消收藏，返回最新收藏数 |
| GET | `/article/{id}/interact-status` | 互动状态：liked/favorited + likeCount/favoriteCount/forwardCount |
| POST | `/article/{id}/forward` | 转发给好友，body `{toUserId}`，同时推送聊天消息 |

### 好友 `FriendController`
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/friends/request` | 发起好友申请（toUserId） |
| GET | `/friends/pending` | 待处理申请列表 |
| PUT | `/friends/confirm/{relationId}` | 接受 |
| PUT | `/friends/reject/{relationId}` | 拒绝 |
| GET | `/friends/list` | 好友列表（keyword 模糊 + 分页，含头像/笔记数） |
| DELETE | `/friends/{friendId}` | 删除好友 |

### 消息 `MessageController`
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/message/history` | 与某好友的聊天记录（friendId/pageNum/pageSize），返回后自动置已读 |
| GET | `/message/offline` | 拉取离线未读消息并置已读 |

### 搜索 `SearchController`
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/search/notes` | 关键词全文搜索已发布笔记（keyword/page/size，Redis 缓存） |
| GET | `/search/notes/by-tag` | 标签搜索 |
| GET | `/search/users` | 用户搜索（发现好友用） |

### 其他
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/upload` | 图片上传（阿里云 OSS），返回 URL |

---

## WebSocket 协议

- 地址：`ws://localhost:8080/ws/chat?token=<JWT>`（前端开发环境走 `/ws` 代理）
- 客户端 → 服务端（发送消息）：

```json
{
  "toUserId": 2,
  "msgType": 0,          // 0-文本消息；1-文章转发
  "content": "你好",      // 文本消息必填
  "articleId": 12        // msgType=1 时必填
}
```

- 服务端 → 客户端（收到后原样推送给会话对端，数据来自 message 表）

```json
{
  "fromUserId": 1,
  "toUserId": 2,
  "msgType": 0,
  "content": "你好",
  "articleId": null,
  "createTime": "2026-08-26 10:00:00"
}
```

- 消息流程：发送方 `send` → 服务端校验/落库 → 接收方在线则 `sendToUser` 实时推送；离线则只落库，上线后由 `/message/offline` 补齐
- 连接管理：`ConcurrentHashMap<Integer, WebSocketSession>`，断线（close/error）自动移除

---

## 快速启动

### 环境要求
- JDK 21+、Maven 3.8+
- MySQL 5.7+（建议 8.x，需支持 ngram 全文索引）、Redis 5+
- Node.js 16+

### 1. 初始化数据库
```sql
-- 创建数据库
CREATE DATABASE big_event DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
执行迁移脚本 `big-event/src/main/resources/sql/migration.sql`。
> ⚠️ 脚本第 6 步（重建分类表）依赖旧库中 `fk_article_category` 外键名，执行前先用 `SHOW CREATE TABLE article;` 确认；
> 若已手动重建过 category 表（`is_system` 混合结构），跳过该步骤即可，其余 `IF NOT EXISTS` 语句可幂等重复执行。

### 2. 配置后端
编辑 `big-event/src/main/resources/application-dev.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/big_event
    username: root
    password: 123456wh        # 改成你的数据库密码
  data:
    redis:
      host: localhost
      port: 6379
```
OSS 凭证通过环境变量注入（不配置则图片上传功能不可用，其余功能不受影响）：
```bash
# Windows PowerShell
$env:OSS_ACCESS_KEY_ID = "你的AccessKeyId"
$env:OSS_ACCESS_KEY_SECRET = "你的AccessKeySecret"
```

### 3. 启动后端
```bash
cd big-event
mvn spring-boot:run
# 或打包运行：mvn clean package -DskipTests && java -jar target/big-event-1.0-SNAPSHOT.jar
```
服务启动于 `http://localhost:8080`。

### 4. 启动前端
```bash
cd frontend
npm install
npm run dev
```
访问 `http://localhost:5173`。

### 5. 验证
1. 注册账号 → 登录
2. 在「写笔记」发布一篇笔记（分类从系统内置分类中挑选）
3. 社区首页查看热点榜，按分类浏览
4. 发现用户 → 添加好友 → 好友列表点「聊天」，验证 WebSocket 实时消息与转发卡片

---

## 配置说明

| 配置项 | 位置 | 默认值 | 说明 |
|---|---|---|---|
| 数据库连接 | `application-dev.yml` | root / 123456wh | 本地开发 |
| Redis | `application-dev.yml` | localhost:6379 | 会话+缓存+计数 |
| 服务端口 | `application-dev.yml` | 8080 | 后端 |
| OSS 凭证 | 环境变量 | 无 | `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET` / `OSS_BUCKET_NAME`(默认 `biggetst-event`) / `OSS_ENDPOINT`(默认北京) |
| 前端代理 | `frontend/vite.config.js` | /api→8080、/ws→8080 | 开发环境免跨域 |

---

## 注意事项

- **包名**：代码实际包名为 `com.wangheng`
- **JWT 密钥**：`JwtUtil` 中为固定密钥，生产环境请外置为环境变量
- **OSS 凭证**：`AliOssUtil` 中若存在硬编码 AccessKey，生产环境务必改为环境变量注入
- **分类删除**：已被文章引用的分类删除会受外键约束阻止，需先处理关联文章
- **热点榜冷启动**：首次启动 ZSET 为空，社区接口会按浏览量兜底回填，属正常现象
- **前端构建提示**：`npm run build` 提示 chunk 超 500KB 仅为警告，不影响产物；如需优化可对路由组件做动态 import
- **搜索缓存**：关键词搜索缓存 5 分钟有效，发布新文章后立即清空，极端情况下存在短暂延迟属预期

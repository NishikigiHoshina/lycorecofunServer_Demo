# lycorisfunServer — 后端服务

本站点**后端 REST 服务**，为前端 `posts` 提供接口。Spring Boot 3 + MyBatis + MySQL。
- 登录态采用 **HttpOnly Cookie** 会话承载 JWT；
- 使用 **Caffeine 本地缓存**（Spring Cache 抽象）加速高频只读接口；
- 写接口鉴权分层：作者由 token 推导、改/删校验“属主或管理员”、新闻管理“仅管理员”。

> 前端见同目录 `posts/`；详细结构见工作区根目录 `后端项目结构说明(lycorisfunServer).md`。

---

## 技术栈

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 3.5.6 | Java 17 |
| Web | spring-boot-starter-web | 内嵌 Tomcat |
| ORM | MyBatis 3 | XML 映射 `classpath:mapper/*.xml` |
| 数据库 | MySQL 8 + Druid | 库 `useforcommon`（localhost:3306），**表均 utf8mb3** |
| 鉴权 | JWT（java-jwt 4.4） | HMAC256，7 天有效 |
| 加密 | commons-codec | MD5 加盐 |
| 本地缓存 | spring-boot-starter-cache + Caffeine | `@Cacheable/@CacheEvict`；region 见 `CacheConfig` |
| 其它 | AOP / validation / Lombok / Gson | — |

---

## 运行

```bash
./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run
./mvnw clean package -DskipTests   # 打包
./mvnw test
```
- 端口 `12808`，上下文 `/lycorisfunServer`；接口基础地址 `http://localhost:12808/lycorisfunServer/api/**`
- 数据库账号口令见 `src/main/resources/application.properties`；`mybatis.type-aliases-package` 须为 **`com.lycorisfun.fun.Entity`**（大写 E）。

---

## 目录结构

```
lycorisfunServer/
├── pom.xml  mvnw  HELP.md
└── src/main/
    ├── resources/  application.properties + mapper/*.xml
    └── java/com/lycorisfun/fun/
        ├── LycorisfunServerApplication.java      # @SpringBootApplication + @MapperScan
        ├── Annotation/RequireToken.java          # 方法级鉴权注解
        ├── Aop/LogRecordAspect.java              # Service 日志切面（落库注释中）
        ├── config/WebConfig.java                 # 拦截器 + 凭证 CORS + /upload/** 静态资源
        ├── config/CacheConfig.java               # ★ Caffeine 缓存 region（容量/TTL/recordStats）
        ├── Interceptor/TokenInterceptor.java     # 生效鉴权 + static currentUserId()
        ├── Controller/  Auth / User / Post / Message / File / Setting
        ├── Service + impl/
        ├── Mapper/  Post / User / News / Func / Log
        ├── Entity/  User / Post / News / LogRecord / function
        ├── Exception/ BusinessException + GlobalExceptionHandler
        └── util/  JWTUtil / Md5SaltUtil / TextValidator(★输入校验)
```

---

## 认证（HttpOnly Cookie 会话）

- **登录** `POST /api/admin/login`：校验通过 → 生成 JWT → 写入 **HttpOnly cookie `lycorecofun_token`**（SameSite=Lax、path=/、7 天；本地 http 不加 Secure，生产可配）。响应返回非机密画像：`userId / username / avater / status`（不再下发 token）。
- **登出** `POST /api/logout`：把该 cookie 置空并 `Max-Age=0` 清除。
- **鉴权读取** `TokenInterceptor`（拦 `/api/**`）：token 取 cookie 优先，`Authorization: Bearer` 兜底；带 `@RequireToken` → 必须有效否则 401；可选登录则成功才解析。验证成功把 `id` claim 写入 `request` 属性；Controller 用 `TokenInterceptor.currentUserId(request)`（Integer）读取。
- **CORS** `WebConfig`：`/api/**` 允许 `http://localhost:*` / `127.0.0.1:*` 且 `allowCredentials(true)`。

### 写接口鉴权分层（2026-09 加固）

| 层 | 规则 | 判定依据 |
| --- | --- | --- |
| 作者身份 | `/writepost` `/writecomment` 的作者 `post_userid` **由 token 推导并覆盖 body**，杜绝伪造 | `request.userId` |
| 帖子改/删 | `/updatePostinfo` `/deletePost`：**属主 或 管理员**，否则 `BusinessException(403)`；update 的作者列仅管理员且显式 >0 才可改，否则保留现作者 | DB：`post_userid` 对比 + `users.status==3` |
| 新闻管理 | `/addnews` `/updatenews` `/deletenews`：**仅管理员**，否则 403 | DB：`users.status==3` |
| 留言 | `/takemessage` 匿名可写（**刻意设计**，留言本） | — |

> 管理员判定一律走 DB（`userMapper.findById(...).getStatus()==3`），**不信任**前端 profile cookie / 请求体的 status。

### 输入字符校验（2026-09）

- 背景：库表为 **utf8mb3**（旧 utf8，单字符最长 3 字节，只支持 BMP/码点 ≤ U+FFFF）；emoji 等 4 字节字符入库触发 MySQL “Incorrect string value”。
- `util/TextValidator.requireStorable(text, field)` 在入库前拦截，抛友好 `BusinessException(400)`。
- 已接入：`NewsServiceImpl.add/update`（标题/图片/新闻链接）、`PostServiceImpl.addcontent`（评论内容）/`addmessage`（留言内容/昵称/主页）、`UserServiceImpl.add`（注册用户名）。
- **例外**：`/writepost`（Posts 富文本正文/标题）**未接**该校验——属审计项，见前端 README 文末“Post 富文本输入审计”。

---

## 缓存（Caffeine 本地缓存）

策略：**cache-aside**。读 `@Cacheable`；写 `@CacheEvict`；TTL 兜底；`allowNullValues=false`，**空结果一律返回空集合缓存**（避免 null 写入抛异常）。

| region | 命中接口/方法 | 容量 | TTL | 失效时机 |
| --- | --- | --- | --- | --- |
| `newsLatest` | `/newslist`（findLatestNews） | 20 | 10m | TTL；新闻增/改/删时 region 清空 |
| `newsAll` | `/newslistAll`（findAllNews） | 100 | 10m | 新闻增/改/删时 region 清空 |
| `announcement` | `/getAnnouncement` | 10 | 30m | TTL |
| `funcStatus` | `/getfuncstatus`（funcStatus，按 funcname） | 100 | 30m | `updateStatus`（按该 funcname） |
| `indexImg` | `/getIndexIMG`（getIndexImg） | 20 | 30m | 开关/宣传图增删改 |
| `postPage` | `/postlistPage`（findPageList/countPostList，page+size） | 500 | 5m | 发帖/软删/改帖（region 清空） |
| `postDetail` | `/getPostByid`（findById，按 postid） | 500 | 10m | 删/改该帖按 id 失效 |
| `replyList` | `/getReply`（findContentPointaPost，按 parent_id） | 500 | 3m | 写评论/回复（region 清空） |

> 注：缓存前列表正文已在 Service 内清空；被缓存对象不可在 Controller 二次改写。授权读属主用的 `findById` 属 Impl 内自调用，绕过缓存代理=一次直读。登录/注册/登出、个人中心、用户/搜索等不缓存。

---

## 主要接口

| 功能 | 接口 | 鉴权 |
| --- | --- | --- |
| 登录 / 注册 / 登出 | `POST /admin/login`、`/admin/register`、`POST /logout` | 开 |
| 帖子分页 | `POST /postlistPage?page=&size=` | 开 |
| 帖子全量 / 详情 / 搜索 | `GET /getPostList`、`POST /getPostByid`、`/searchBytitle`、`/searchByuser` | 开 |
| 发帖 / 评论 | `POST /writepost`、`POST /writecomment` | 需登录；**作者由 token 推导** |
| 帖子改 / 删 | `POST /updatePostinfo`、`POST /deletePost?postid=` | 需登录；**属主或管理员**，越权 403 |
| 留言 | `POST /takemessage` | 匿名（刻意设计） |
| 个人中心 | `POST /getMyProfile` | 需登录（cookie token → userId） |
| 新闻管理 | `POST /addnews`、`/updatenews`、`/deletenews?news_id=` | **仅管理员 status==3** |
| 功能开关 / 站内图 | `getfuncstatus`、`updateStatus`、`getIndexIMG`、`addIndexIMG`、`deleteIndexIMG` | 写操作需登录 |
| 文件上传 | `POST /uploadFile` | 需登录 |
| 新闻 / 公告 | `newslist`、`newslistAll`、`getAnnouncement` | 开 |

---

## 数据要点

- `posts` 一表多用：`status`（发帖/评论=1，留言=3，软删=0）；`parent_id`/`root_id` 表达评论与楼中楼层级；写入按作者 id 回填 `post_username` 快照；列表视图只取 `status=1 and title is not null`。
- `news` 软删机制：`status` 1=显示 / 0=不显示（软删），默认 1；公开读取（`newslist`）只取 `status=1`，管理端 `newslistAll` 返回含已删行以便管理。
- `users.status`：1=正常 / 2=停用 / 3=管理员（登录时 status==2 拒绝）。
- 功能开关与首页宣传图：`functionform` 表。

---

## 数据库架构

库：`useforcommon`（localhost:3306）；**所有表 CHARSET=utf8mb3**（utf8，只支持 BMP → 无 emoji；相关约束见“输入字符校验”）。

### users（用户）
| 列 | 类型 | 说明 |
| --- | --- | --- |
| userId | int PK AUTO_INCREMENT | — |
| userName | varchar(50) NOT NULL default 'unknownUser' | — |
| gender | varchar(50) NOT NULL default 'unknown' | — |
| registerTime | datetime default '1970-01-01 00:00:00' | 注册时间 |
| signature / avaterURL / PersonalIndexLink | varchar(255) | 签名 / 头像 / 个人主页 |
| email | varchar(255) **UNIQUE** | 登录账号 |
| password | varchar(255) NOT NULL | MD5 加盐密文 |
| status | int NOT NULL default 1 | 1 正常 / 2 停用 / 3 管理员 |

### posts（帖子 + 评论 + 留言，一表多用）
| 列 | 类型 | 说明 |
| --- | --- | --- |
| postid | int PK AUTO_INCREMENT | — |
| title | varchar(50) | 帖子标题；评论/留言为 null |
| post_userid | int NOT NULL default 0 | 作者 id（服务端由 token 推导） |
| content | varchar(255) | 正文 / 评论 / 留言内容 |
| created_at | datetime | — |
| like_count / reply_count | int default 0 | 冗余计数 |
| link / imgurl | varchar(255) | 外链 / 附图 |
| parent_id / root_id | int | 评论层级：parent_id=被评论行 id，root_id=顶层帖 id |
| status | int NOT NULL default 1 | 发帖/评论=1，留言=3，软删=0 |
| post_username | varchar(50) | 作者昵称快照（冗余，改作者后不自动刷新） |

### news（新闻）
| 列 | 类型 | 说明 |
| --- | --- | --- |
| news_id | int PK AUTO_INCREMENT | — |
| title | varchar(100) | 标题 |
| imgUrl | varchar(255) default '1' | 配图 URL（列名大小写注意） |
| news_link | varchar(255) | 原文链接（公告用该列承载文字内容） |
| time | datetime | 发布时间 |
| status | int default 1 | **1=显示 / 0=不显示（软删，2026-09-10 新增）** |

> 无 `content` / `author` 列；`Entity/News` 中的 content/author 为冗余字段（恒为 null）。

### tags / post_tags（标签）
- `tags`：tag_id PK AUTO_INCREMENT、tag_name varchar(32) UNIQUE（uk_tag_name）、use_count int unsigned default 0、created_at datetime default CURRENT_TIMESTAMP。
- `post_tags`：post_id + tag_id，复合 PK，另建索引 idx_tag_post（tag_id, post_id）。

### functionform（功能开关 / 首页宣传图）
| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | int PK AUTO_INCREMENT | — |
| function_name | varchar(50) | 开关名（如 uploadWork / connectWebSiteOwner 等） |
| function_status | tinyint default 0 | 开关值 |
| function_link | varchar(255) | 附加链接 / 宣传图地址 |

### logrecords（访问日志）
| 列 | 类型 | 说明 |
| --- | --- | --- |
| id | int PK AUTO_INCREMENT | — |
| ip / time / classmethod / httpmethod / classname | … | AOP 已拦截但 `insertLog` 为注释，当前**未落库** |

---

## 变更记录（2026-09 会话）

- **写接口鉴权加固（09-09）**：作者由 token 推导；改/删加“属主或管理员”（`PostService.delById/updatePostInfo` 加 callerId）；`TokenInterceptor.currentUserId()`。
- **新闻管理（09-10）**：news 表新增 `status` 实体补字段；补全 `NewsService.add/delById/update`；修复 `NewsMapper.delbyid` 软删 SQL（原 `WHERE postd` 笔误）；`findLatestNews` 加 `status=1` 过滤；`MessageController` 新增 `/addnews` `/deletenews` `/updatenews`（管理员）。
- **输入字符校验（09-10）**：新增 `util/TextValidator` 接入新闻增改、评论、留言、注册用户名（Posts 富文本除外）。
- **UserController 修复 + 异常状态码 + 死代码清理（09-10）**：移除 UserController 不当构造注入；异常按 code 映射 HTTP 状态；`UserService` 列表/搜索补 password 脱敏；删除 LoginInterceptor/JdbcUtils/PostConvert/PostListVO/DemoController/旧测试文件；前端删 LikeBtn 死导入与 PostDetail 死 data。

---

## 备注 / 待办

- 密钥、数据库口令明文在源码/配置；CORS 白名单限 localhost（生产改成配置项并收紧）。
- 遗留小项：`Entity/function.java` 类名首字母小写；`LogRecordAspect` 的日志落库 `insertLog` 仍为注释（链路保留未启用）。
- **已清理（2026-09-10）**：删除 `LoginInterceptor`、`JdbcUtils`（含硬编码口令）、`PostConvert`、`PostListVO`、`DemoController`、无法编译的遗留测试文件；`UserController` 不当构造注入已移除。
- **待办**：`updatePostinfo` 对原始 int 字段（like_count 等）用 `<if test="x!=null">` 恒真 → body 缺省会被写 0（作者列已由 Service 守卫排除，计数列仍受影响）；admin 改作者后 `post_username` 不刷新；`FileController/uploadFile` 写盘/回显路径与 `WebConfig` 静态映射及端口不符（产出 URL 不可访问）；`UserControl` 后台页仍把 User 对象 POST 到帖子端点（不可用）。
- `GlobalExceptionHandler`（2026-09-10 起）把 `BusinessException.code` 映射为 HTTP 状态码（不再一律 400），未匹配接口返回 HTTP 404；被禁用用户旧 token 仍可写（拦截器不查 status）。

# lycorisfunServer — 后端服务

本站点**后端 REST 服务**，为前端 `posts` 提供接口。Spring Boot 3 + MyBatis + MySQL。
- 登录态：**HttpOnly Cookie** 会话承载 JWT；
- 高性能只读：**Caffeine 本地缓存**（Spring Cache 抽象）；
- 写接口分层鉴权：作者由 token 推导、改/删校验“属主或管理员”、新闻管理“仅管理员”；
- 帖子正文：**结构化文档**（白名单 JSON，独立 `post_bodies` 表）——不存 HTML，因此不需要 HTML 清洗库。

> 前端见同目录 `posts/`；更详细的分层/接口/历史变更/遗留问题见工作区根目录 `后端项目结构说明(lycorisfunServer).md`。

---

## 技术栈

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 3.5.6 | Java 17 |
| Web | spring-boot-starter-web | 内嵌 Tomcat |
| ORM | MyBatis 3 | XML 映射 `classpath:mapper/*.xml` |
| 数据库 | MySQL 8 + Druid | 库 `useforcommon`；表均 utf8mb3 |
| 鉴权 | JWT（java-jwt 4.4） | HMAC256，7 天有效 |
| 加密 | commons-codec | MD5 加盐 |
| 本地缓存 | spring-boot-starter-cache + Caffeine | region 见 `CacheConfig` |
| 其它 | AOP / validation / Lombok / Gson | — |

---

## 快速开始

```bash
./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run
./mvnw clean package -DskipTests   # 打包
```
- 端口 `12808`，上下文 `/lycorisfunServer`；接口前缀 `http://localhost:12808/lycorisfunServer/api/**`。
- 数据库账号口令见 `src/main/resources/application.properties`（当前为占位 `XXX`，运行前按本地库填回）；`mybatis.type-aliases-package` 须为 **`com.lycorisfun.fun.Entity`**（大写 E）。
- **首次运行前先建表**（帖子正文表，不建则发帖会 500）：

  ```bash
  mysql -uroot -p --default-character-set=utf8mb4 useforcommon < sql/post_bodies.sql
  ```

  ⚠️ `--default-character-set=utf8mb4` 不能省：少了它，表能建出来但**表/列的中文注释会变成乱码**（客户端按本地代码页解释 UTF-8 脚本）。建错了 `DROP TABLE post_bodies;` 重建即可。

---

## 目录结构

```
lycorisfunServer/
├── sql/post_bodies.sql                      # 帖子正文表建表脚本（首次运行前执行一次）
└── src/main/
    ├── resources/  application.properties + mapper/*.xml
    └── java/com/lycorisfun/fun/
        ├── LycorisfunServerApplication.java   # @SpringBootApplication + @MapperScan
        ├── Annotation/RequireToken.java       # 方法级鉴权注解
        ├── Aop/LogRecordAspect.java           # Service 日志切面（落库未启用）
        ├── config/  WebConfig（拦截器+CORS+上传静态映射） · CacheConfig（Caffeine） · UploadProperties（上传配置）
        ├── Interceptor/TokenInterceptor.java  # token 解析 + static currentUserId()
        ├── Controller/   Auth / User / Post / Message / Setting
        ├── Service + impl/  Post/User/News/Func · FileStorageService（上传落盘） · PostDocService（正文文档）
        ├── Mapper/  Post / PostBody / User / News / Func / Log
        ├── Entity/  User / Post（含非列字段 doc）/ PostBody / News / LogRecord / function
        ├── Exception/  BusinessException + GlobalExceptionHandler
        └── util/  JWTUtil / Md5SaltUtil / TextValidator / AuthUtil
                    PostDocValidator（正文白名单校验/规范化） · PostDocText（摘要/首图/存量剥标签）
```

---

## 认证与鉴权模型

- **登录** `POST /api/admin/login`：校验通过写 HttpOnly cookie `lycorecofun_token`（SameSite=Lax、path=/、7 天），响应仅回非机密画像。
- **登出** `POST /api/logout`：清空该 cookie。
- **TokenInterceptor**（拦 `/api/**`）：token 取 cookie 优先、`Authorization: Bearer` 兜底；`@RequireToken` 强制登录，否则可选登录。验证成功后把 `id` 写入 request 属性，Controller 经 `TokenInterceptor.currentUserId(request)` 读取。
- **写接口权限分层**（管理员一律 **DB** 判定 `users.status==3`，不信任前端传值）：

| 层 | 规则 |
| --- | --- |
| 作者身份 | `/writepost` `/writecomment` 作者 `post_userid` 由 token 推导并覆盖 body |
| 帖子改/删 | `/updatePostinfo` `/deletePost`：属主或管理员，否则 403 |
| 新闻管理 | `/addnews` `/updatenews` `/deletenews` `/updateAnnouncement`：仅管理员 |
| 系统开关 / 站内图（含上传） | `updateStatus` / `addIndexIMG` / `deleteIndexIMG`：仅管理员 |
| 后台用户管理 | `/updateUserinfo` `/deleteUser`：仅管理员（`/deleteUser` 是软删 → `status=2`） |
| **帖子配图上传** | `/uploadPostImg`：**仅需登录，不需管理员**（作者要能插图）—— 这是唯一放宽的上传权限点，故走 `storeImage()` 重编码 |
| 留言 | `/takemessage` 匿名可写（刻意设计） |

- **字符约束**：库为 utf8mb3（仅 BMP），emoji 等 4 字节字符无法入库；`util/TextValidator` 在新闻/评论/留言/注册用户名入库前拦截并给友好 400。**帖子正文与标题也受此约束**——正文在 `PostDocValidator` 里整串校验（内部调用 `TextValidator`），前端发帖前另有 `utils/validate.js` 友好拦截。

---

## 文件上传（配置化）

两个入口，共用一套校验：

| 类型 key | 入口 | 权限 | 落盘目录 | 对外 URL |
| --- | --- | --- | --- | --- |
| `index-img` | `POST /addIndexIMG`（站内宣传图） | 仅管理员 | `{root}/img` | `/upload/…` |
| `post-img` | `POST /uploadPostImg`（帖子正文配图） | **仅需登录** | `{root}/post` | `/upload/post/…` |

- 根目录与每类型规则集中在 `application.properties` 的 `lycorisfun.upload.*`（root 默认 `${user.home}/lycorisfun-upload`；`max-size` 支持 `5MB` 等 DataSize 单位）。新增类型 = 追加 `types[n]`，静态映射与校验自动跟随。
- `FileStorageService` 两条落盘路径：
  - `store()` —— **原样落盘**（管理员上传的宣传图走这条，行为不变）；
  - `storeImage()` —— **解码后重新编码**再落盘（帖子配图走这条）。新文件完全由解出的像素生成，EXIF/ICC 与"追加在文件尾部的载荷"（polyglot 文件）都会丢掉。jpg/png 重编码；gif（会丢动图）与 webp（JDK 无编码器）退化为原样落盘，仍受魔数校验保护。
  - 两条路径都做：扩展名白名单 + **图片魔数校验** + 大小限制 + uuid 命名 + 路径穿越防护；`storeImage()` 另有**解压炸弹防护**（先只读文件头拿宽高、校验像素 ≤40MP，再真正解码 —— 顺序不能反）。
- `WebConfig` 按类型注册静态目录并加 `nosniff`；前端 `SiteControl` / `WangEditor` 与后端同口径预检（≤5MB、jpg/jpeg/png/gif/webp）。
- ⚠️ `types[n].urlPath` **必须互不相同**：静态映射按 urlPath 注册，两个类型共用同一 urlPath 会互相覆盖，导致其中一个目录整体 404。

---

## 帖子正文（结构化文档）

**正文不存 HTML**，只存白名单节点树（schema v1）。渲染端按 schema 出标签、文本走框架转义，因此**"HTML 注入"这一类漏洞在架构上不存在**，也就不需要 DOMPurify / Jsoup —— 没有清洗器，就没有可被绕过的清洗器。

| 环节 | 位置 | 职责 |
| --- | --- | --- |
| 契约与上限 | `util/PostDocValidator` 常量 | `v:1`；块级 `p/h2/h3/blockquote/ul/ol/pre/img`、行内 `b/i/u/code/a/img`；JSON ≤64KB、节点 ≤500、图片 ≤20、行内嵌套 ≤4、单文本 ≤5000 字符 |
| 校验 | `util/PostDocValidator` | **唯一信任边界**。拒绝未知键、`href` 仅 http/https/mailto、`img.src` 必须匹配上传 `urlPath` 前缀、所有字符串过 `TextValidator`。落库的是**服务端产出的规范 JSON** |
| 落库 | `posts.content` + `post_bodies.doc` | `content` = **纯文本摘要**（≤255 字，正好适配该列 `varchar(255)`，**无需改列类型**）；`doc` = 规范 JSON。`add()` 带 `@Transactional`，两步原子 |
| 派生字段 | `util/PostDocText` | 摘要（`toExcerpt`）、封面首图（`firstImageSrc`，回填 `posts.imgurl`）、存量 HTML 剥标签（`legacyExcerpt`） |
| 读取 | `PostServiceImpl` | 详情用 `PostBodyMapper` 把 `doc` 挂到 Post 上；列表只给纯文本摘要 |

- **入参形态**：`doc` 是**JSON 字符串**（HTTP body 里 `{"title": "...", "doc": "{\"v\":1,...}"}`），不是嵌套对象。
- **存量帖子无需迁移**：老帖 `doc` 为 NULL、`content` 里是短 HTML，由前端用同一套白名单转换器"现转现渲染"，列表摘要由 `legacyExcerpt` 剥标签得到。
- 为什么当初必须动存储：`posts.content` 是 `varchar(255)`（**单位是字符**），一段 wangEditor 产出的 HTML 约 240 个可见字就到顶，长文发帖会直接报 1406（本机 `sql_mode` 含 `STRICT_TRANS_TABLES`，是响亮报错而非静默截断）。

---

## 缓存（Caffeine，cache-aside）

读 `@Cacheable`，写 `@CacheEvict`，`allowNullValues=false`（空结果缓存空集合而非 null）。

| region | 命中接口 | TTL | 失效时机 |
| --- | --- | --- | --- |
| `newsLatest` / `newsAll` | `/newslist` / `/newslistAll` | 10m | 新闻增/改/删清空 |
| `announcement` | `/getAnnouncement` | 30m | `/updateAnnouncement` 清空 |
| `funcStatus` / `indexImg` | `/getfuncstatus` / `/getIndexIMG` | 30m | 对应写接口 |
| `postPage` | `/postlistPage` | 5m | 发帖/软删/改帖清空 |
| `postDetail` | `/getPostByid` | 10m | 删/改该帖 |
| `replyList` | `/getReply` | 3m | 写评论/回复清空 |

---

## 主要接口

| 功能 | 接口 | 鉴权 |
| --- | --- | --- |
| 登录 / 注册 / 登出 | `POST /admin/login`、`/admin/register`、`/logout` | 开 |
| 帖子（分页/详情/搜索/发/评/改/删） | `/postlistPage`、`/getPostByid`、`/searchBytitle`、`/searchByuser`、`/writepost`、`/writecomment`、`/updatePostinfo`、`/deletePost` | 写需登录（见鉴权模型） |
| 帖子配图上传 | `POST /uploadPostImg`（表单字段 `file`） | **需登录**（不需管理员） |
| 留言 | `POST /takemessage` | 匿名 |
| 个人中心 | `POST /getMyProfile` | 需登录 |
| 用户查询 | `/searchUser`、`/searchByUsername`、`/searchByuserid`、`/userlist` | 开 |
| 用户管理（后台） | `POST /updateUserinfo`、`/deleteUser` | 仅管理员 |
| 新闻 / 公告 | `newslist`、`newslistAll`、`getAnnouncement` | 开 |
| 新闻管理 | `addnews`、`updatenews`、`deletenews` | 仅管理员 |
| 公告更新 | `POST /updateAnnouncement`（body `{content}`） | 仅管理员 |
| 功能开关 / 站内图（含图片上传） | `getfuncstatus`、`updateStatus`、`getIndexIMG`、`addIndexIMG`、`deleteIndexIMG` | 读开放，写仅管理员 |

---

## 数据库（概要）

库 `useforcommon`，全部表 **CHARSET=utf8mb3**。`posts` 一表多用（发帖/评论=1、留言=3、软删=0，评论经 `parent_id`/`root_id` 分层并回填 `post_username` 快照）；**`posts.content` 对帖子而言是"纯文本摘要"**，正文在 `post_bodies`（`postid` 一对一、`doc` MEDIUMTEXT）；`users.status`：1 正常 / 2 停用 / 3 管理员；`news.status`：1 显示 / 0 不显示（软删）；后台公告 = `news` 表中 `imgUrl IS NULL` 的那一行（正文寄存在 `news_link`）；`news` 无 content/author 列。另有标签（`tags`/`post_tags`）、功能开关 `functionform`、未落库的访问日志 `logrecords`。逐列说明见结构文档。

---

## 相关文档

- 后端历史变更、遗留问题、数据库逐列说明：工作区根目录 `后端项目结构说明(lycorisfunServer).md`。
- 前端：`posts/README.md` 及 `前端项目结构说明(posts).md`。

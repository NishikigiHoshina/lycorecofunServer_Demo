# lycorisfunServer — 后端服务

本站点**后端 REST 服务**，为前端 `posts` 提供接口。Spring Boot 3 + MyBatis + MySQL。
- 登录态：**HttpOnly Cookie** 会话承载 JWT；
- 高性能只读：**Caffeine 本地缓存**（Spring Cache 抽象）；
- 写接口分层鉴权：作者由 token 推导、改/删校验“属主或管理员”、新闻管理“仅管理员”。

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

---

## 目录结构

```
lycorisfunServer/
└── src/main/
    ├── resources/  application.properties + mapper/*.xml
    └── java/com/lycorisfun/fun/
        ├── LycorisfunServerApplication.java   # @SpringBootApplication + @MapperScan
        ├── Annotation/RequireToken.java       # 方法级鉴权注解
        ├── Aop/LogRecordAspect.java           # Service 日志切面（落库未启用）
        ├── config/  WebConfig（拦截器+CORS+上传静态映射） · CacheConfig（Caffeine） · UploadProperties（上传配置）
        ├── Interceptor/TokenInterceptor.java  # token 解析 + static currentUserId()
        ├── Controller/   Auth / User / Post / Message / Setting
        ├── Service + impl/ · Mapper/ · Entity/
        ├── Exception/  BusinessException + GlobalExceptionHandler
        └── util/  JWTUtil / Md5SaltUtil / TextValidator / AuthUtil
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
| 新闻管理 | `/addnews` `/updatenews` `/deletenews`：仅管理员 |
| 系统开关 / 站内图（含上传） | `updateStatus` / `addIndexIMG` / `deleteIndexIMG`：仅管理员 |
| 留言 | `/takemessage` 匿名可写（刻意设计） |

- **字符约束**：库为 utf8mb3（仅 BMP），emoji 等 4 字节字符无法入库；`util/TextValidator` 在新闻/评论/留言/注册用户名入库前拦截并给友好 400。Post 富文本正文未接该校验（审计见前端结构文档）。

---

## 文件上传（配置化）

- 唯一上传入口：后台「站内管理」宣传图 `POST /addIndexIMG`（**仅管理员**）。类型 `index-img`：jpg/jpeg/png/gif/webp、**≤5MB**；后端做扩展名白名单 + 图片魔数校验 + uuid 命名 + 路径穿越防护。
- 根目录与每类型规则集中在 `application.properties` 的 `lycorisfun.upload.*`（root 默认 `${user.home}/lycorisfun-upload`；`max-size` 支持 `5MB` 等 DataSize 单位）。新增类型 = 追加 `types[n]`，静态映射与校验自动跟随。
- `WebConfig` 按类型注册静态目录并加 `nosniff`；前端 `SiteControl` 与后端同口径预检（≤5MB、jpg/jpeg/png/gif/webp）。

---

## 缓存（Caffeine，cache-aside）

读 `@Cacheable`，写 `@CacheEvict`，`allowNullValues=false`（空结果缓存空集合而非 null）。

| region | 命中接口 | TTL | 失效时机 |
| --- | --- | --- | --- |
| `newsLatest` / `newsAll` | `/newslist` / `/newslistAll` | 10m | 新闻增/改/删清空 |
| `announcement` | `/getAnnouncement` | 30m | TTL |
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
| 留言 | `POST /takemessage` | 匿名 |
| 个人中心 | `POST /getMyProfile` | 需登录 |
| 用户查询 | `/searchUser`、`/searchByUsername`、`/searchByuserid`、`/userlist` | 开 |
| 新闻 / 公告 | `newslist`、`newslistAll`、`getAnnouncement` | 开 |
| 新闻管理 | `addnews`、`updatenews`、`deletenews` | 仅管理员 |
| 功能开关 / 站内图（含图片上传） | `getfuncstatus`、`updateStatus`、`getIndexIMG`、`addIndexIMG`、`deleteIndexIMG` | 读开放，写仅管理员 |

---

## 数据库（概要）

库 `useforcommon`，全部表 **CHARSET=utf8mb3**。`posts` 一表多用（发帖/评论=1、留言=3、软删=0，评论经 `parent_id`/`root_id` 分层并回填 `post_username` 快照）；`users.status`：1 正常 / 2 停用 / 3 管理员；`news.status`：1 显示 / 0 不显示（软删）；`news` 无 content/author 列。另有标签（`tags`/`post_tags`）、功能开关 `functionform`、未落库的访问日志 `logrecords`。逐列说明见结构文档。

---

## 相关文档

- 后端历史变更、遗留问题、数据库逐列说明：工作区根目录 `后端项目结构说明(lycorisfunServer).md`。
- 前端：`posts/README.md` 及 `前端项目结构说明(posts).md`。

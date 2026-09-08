# lycorisfunServer — 后端服务

本站点**后端 REST 服务**，为前端 `posts` 提供接口。Spring Boot 3 + MyBatis + MySQL。
登录态采用 **HttpOnly Cookie** 会话承载 JWT。

> 前端见同目录 `posts/`；详细结构见工作区根目录 `后端项目结构说明(lycorisfunServer).md`。

---

## 技术栈

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 3.5.6 | Java 17 |
| Web | spring-boot-starter-web | 内嵌 Tomcat |
| ORM | MyBatis 3 | XML 映射 `classpath:mapper/*.xml` |
| 数据库 | MySQL + Druid | 库 `useforcommon`（localhost:3306） |
| 鉴权 | JWT（java-jwt 4.4） | HMAC256，7 天有效 |
| 加密 | commons-codec | MD5 加盐 |
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
        ├── Interceptor/TokenInterceptor.java     # 生效鉴权；LoginInterceptor.java 旧版未注册
        ├── Controller/  Auth / User / Post / Message / File / Setting / Demo
        ├── Service + impl/
        ├── Mapper/  Post / User / News / Func / Log
        ├── Entity/  User / Post / News / LogRecord / function
        ├── VO/ · Convert/（Convert 注释未启用）
        ├── Exception/ BusinessException + GlobalExceptionHandler
        └── util/  JWTUtil / Md5SaltUtil / JdbcUtils(遗留)
```

---

## 认证（HttpOnly Cookie 会话）

- **登录** `POST /api/admin/login`：校验通过 → 生成 JWT → 写入 **HttpOnly cookie `lycorecofun_token`**（SameSite=Lax、path=/、7 天；本地 http 不加 Secure，生产可配）。响应返回非机密画像：`userId / username / avater / status`（不再下发 token）。
- **登出** `POST /api/logout`：把该 cookie 置空并 `Max-Age=0` 清除。
- **鉴权读取** `TokenInterceptor`（拦 `/api/**`）：token 取 cookie `lycorecofun_token` 优先，`Authorization: Bearer` 兜底（便于 curl 调试）；带 `@RequireToken` → 必须有效否则 401；否则可选登录（解析成功则把 `id` claim 放入 `request.userId`）。
- **CORS** `WebConfig`：`/api/**` 允许 `http://localhost:*` / `http://127.0.0.1:*` 且 **`allowCredentials(true)`**（带 cookie 跨源不能是 `*`）。各 Controller 不再写通配 `@CrossOrigin("*")`。

## 主要接口

| 功能 | 接口 | 鉴权 |
| --- | --- | --- |
| 登录 / 注册 / 登出 | `POST /admin/login`、`/admin/register`、`POST /logout` | 开 |
| 帖子分页 | `POST /postlistPage?page=&size=` | 开 |
| 帖子全量 / 详情 / 搜索 | `GET /getPostList`、`POST /getPostByid`、`/searchBytitle`、`/searchByuser` | 开 |
| 发帖 / 留言 | `POST /writepost`、`POST /takemessage` | writepost 需登录 |
| 评论 / 楼中楼 | `POST /getReply?parent_id=`、`POST /writecomment` | writecomment 需登录 |
| 个人中心 | `POST /getMyProfile` | 需登录（cookie token → userId） |
| 功能开关 / 站内图 | `getfuncstatus`、`updateStatus`、`getIndexIMG`、`addIndexIMG`、`deleteIndexIMG` | 写操作需登录 |
| 文件上传 | `POST /uploadFile` | 需登录 |
| 新闻 / 公告 | `newslist`、`newslistAll`、`getAnnouncement` | 开 |

## 数据要点

- `posts` 一表多用：`status`（发帖1/留言3/软删0），`parent_id`/`root_id` 评论层级；写入时按 `post_userid` 回填 `post_username`。
- `users.status`：2=停用、3=管理员（前端据此放行后台）。
- `functionform`：功能开关（`connectWebSiteOwner` 等）与首页宣传图。

---



# lycorisfunServer — 后端服务

本站点**后端 REST 服务**，为前端 `posts` 提供接口。Spring Boot 3 + MyBatis + MySQL。

> 前端见同目录 `posts/`；详细目录结构见工作区根目录 `后端项目结构说明(lycorisfunServer).md`。

---

## 技术栈

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 3.5.6 | Java 17 |
| Web | spring-boot-starter-web | 内嵌 Tomcat |
| ORM | MyBatis 3（spring-boot-starter 3.0.3） | XML 映射，`classpath:mapper/*.xml` |
| 数据库 | MySQL + Druid 连接池 | 库 `useforcommon`（本机 localhost:3306） |
| 鉴权 | JWT（com.auth0 java-jwt 4.4） | HMAC256，7 天有效 |
| 加密 | commons-codec | MD5 加盐 `salt$md5(salt+password)` |
| 其它 | AOP / validation / Lombok / Gson | — |

---

## 运行

```bash
# 需本机 MySQL 已启动，且 application.properties 账号口令正确
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
./mvnw clean package -DskipTests   # 打包可执行 jar
./mvnw test
```

- 端口 `12808`，上下文 `/lycorisfunServer`
- 接口基础地址：`http://localhost:12808/lycorisfunServer/api/**`
- 数据库账号口令、上传大小等见 `src/main/resources/application.properties`

> 注意：`mybatis.type-aliases-package` 必须是大写 `com.lycorisfun.fun.Entity`（小写会在 jar 运行时别名解析失败）。

---

## 目录结构

```
lycorisfunServer/
├── pom.xml  mvnw  HELP.md
└── src/main/
    ├── resources/
    │   ├── application.properties
    │   └── mapper/*.xml              # MyBatis SQL（Post/User/News/Log/Func）
    └── java/com/lycorisfun/fun/
        ├── LycorisfunServerApplication.java   # @SpringBootApplication + @MapperScan
        ├── Annotation/RequireToken.java       # 方法级鉴权注解
        ├── Aop/LogRecordAspect.java           # Service 日志切面（落库注释中）
        ├── config/WebConfig.java              # 拦截器 + /upload/** 静态资源
        ├── Interceptor/
        │   ├── TokenInterceptor.java          # 生效鉴权：@RequireToken 强制 / 可选登录
        │   └── LoginInterceptor.java          # 旧版，未注册
        ├── Controller/                        # Auth/User/Post/Message/File/Setting/Demo
        ├── Service  + impl/
        ├── Mapper/                            # MyBatis 接口
        ├── Entity/                            # User/Post/News/LogRecord/function
        ├── VO/  Convert/                      # PostListVO；Convert 注释未启用
        ├── Exception/                         # BusinessException + 全局兜底
        └── util/                              # JWTUtil / Md5SaltUtil / JdbcUtils(遗留)
```

---

## 主要接口

| 功能 | 接口 | 鉴权 |
| --- | --- | --- |
| 登录 / 注册 | `POST /admin/login`、`/admin/register` | 开 |
| 帖子分页列表 | `POST /postlistPage?page=&size=` | 开 |
| 帖子全量 / 详情 / 搜索 | `GET /getPostList`、`POST /getPostByid`、`/searchBytitle`、`/searchByuser` | 开 |
| 发帖 / 留言 | `POST /writepost`、`POST /takemessage` | writepost 需登录 |
| 评论 / 楼中楼 | `POST /getReply?parent_id=`、`POST /writecomment` | writecomment 需登录 |
| 个人中心 | `POST /getMyProfile` | 需登录（token 推 userId） |
| 功能开关 / 站内图 | `getfuncstatus`、`updateStatus`、`getIndexIMG`、`addIndexIMG`、`deleteIndexIMG` | 写操作需登录 |
| 文件上传 | `POST /uploadFile` | 需登录 |
| 新闻 / 公告 | `newslist`、`newslistAll`、`getAnnouncement` | 开 |

## 鉴权与加密

- **注解驱动拦截**：`TokenInterceptor` 拦 `/api/**`，仅当方法标 `@RequireToken` 才强制有效 token（读 `Authorization: Bearer`，把 JWT 的 `id` claim 放入 `request.userId`）；未标注解则"可选登录"（有 token 就解析、无则游客放行）。
- **JWT**：7 天有效；payload 含 `id`、`username`；密钥硬编码于 `JWTUtil.secretKey`。
- **密码**：`Md5SaltUtil` 加盐 MD5，注册加密、登录校验；个人中心等接口的 `password` 在 SQL 查询列中即排除。

## 数据要点

- `posts` 表一表多用：`status` 区分（发帖=1 / 留言=3 / 软删=0），`parent_id`/`root_id` 表达评论层级；评论/回复写入时后端按 `post_userid` 回填 `post_username`。
- `users.status`：2=停用、3=管理员（前端据此放行后台）。
- `functionform` 表：`function_name` 存功能开关（`connectWebSiteOwner` 等）与首页宣传图。

---



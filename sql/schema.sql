-- ============================================================================
-- lycorisfun · 数据库部署脚本（建库 + 建表 + 全字段注释）
--
-- 用途：在一台新机器/新环境上从零建出与本项目一致的库结构。
-- 来源：**依据当前开发库（useforcommon）的真实结构逐表导出的 DDL**，字段类型、
--      默认值、索引名、字符集都保持一致；字段注释在原库基础上做了补全
--      （原库只有 users.status / posts.status / news.status 三处有注释）。
--
-- 执行（**务必带 --default-character-set=utf8mb4**）：
--   mysql -uroot -p --default-character-set=utf8mb4 < schema.sql
--
-- ⚠️ 少了 --default-character-set=utf8mb4，客户端会按本地代码页（中文 Windows 上通常是
--    gbk）解释这个 UTF-8 文件 —— 表建得出来、结构也对，但**中文注释会变成乱码**。
--    若已建错：DROP DATABASE useforcommon; 再按上面的命令重建。
--
-- 说明：
--   1) 用 `CREATE TABLE IF NOT EXISTS`，**不包含任何 DROP**，可重复执行、不会毁数据。
--   2) 不含 INSERT：本脚本只负责结构。需要初始数据请另行导入。
--   3) 每张表的 AUTO_INCREMENT 取自当前开发库，保留是为了让新库的 id 与现有内容连续；
--      **全新部署可以删掉这些 AUTO_INCREMENT 子句**（新库从 1 开始即可）。
--   4) 无视图 / 触发器 / 存储过程 / 外键（已核对 information_schema）。
--   5) 全库字符集 utf8mb3（旧 utf8，只支持 BMP）：**不支持 emoji 等 4 字节字符**，
--      服务端 util/TextValidator 会在入库前拦截。若日后要支持 emoji，
--      需整库 `ALTER TABLE xxx CONVERT TO CHARACTER SET utf8mb4` 并同步放宽校验。
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 0. 建库
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `useforcommon`
    DEFAULT CHARACTER SET utf8mb3;

USE `useforcommon`;


-- ---------------------------------------------------------------------------
-- 1. users —— 用户
-- status 一列三义（见列注释）；password 存 MD5 加盐密文，对外接口一律脱敏为 null。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `userId`            int          NOT NULL AUTO_INCREMENT           COMMENT '用户 id（主键，自增）',
  `userName`          varchar(50)  NOT NULL DEFAULT 'unknownUser'   COMMENT '用户名（昵称，展示用）',
  `gender`            varchar(50)  NOT NULL DEFAULT 'unknown'       COMMENT '性别',
  `registerTime`      datetime              DEFAULT '1970-01-01 00:00:00' COMMENT '注册时间',
  `signature`         varchar(255)          DEFAULT NULL            COMMENT '个性签名',
  `avaterURL`         varchar(255)          DEFAULT NULL            COMMENT '头像 URL（注意列名拼写为 avater）',
  `email`             varchar(255)          DEFAULT NULL            COMMENT '邮箱：登录账号，唯一',
  `password`          varchar(255) NOT NULL DEFAULT '000555'        COMMENT '口令的 MD5 加盐密文，格式 salt$hash；对外接口一律脱敏。注：默认值 000555 不是合法的 salt$hash，故该默认值下无法通过登录校验',
  `PersonalIndexLink` varchar(255)          DEFAULT NULL            COMMENT '个人主页链接',
  `status`            int          NOT NULL DEFAULT '1'             COMMENT '1 正常账号 / 2 停用账号 / 3 管理员账号',
  PRIMARY KEY (`userId`),
  UNIQUE KEY `users_pk` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 COMMENT='用户';


-- ---------------------------------------------------------------------------
-- 2. posts —— 帖子 / 评论 / 留言（一表多用）
-- 靠 status + title + parent_id/root_id 区分三种用途：
--   帖子：status=1 且 title 非空，正文在 post_bodies，content 只是摘要
--   评论：status=1 且 title 为空（含楼中楼，靠 parent_id/root_id 自关联）
--   留言：status=3
--   软删除：status=0
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `posts` (
  `postid`        int          NOT NULL AUTO_INCREMENT        COMMENT '主键，自增',
  `title`         varchar(50)           DEFAULT NULL          COMMENT '帖子标题；评论/留言该列为 NULL（也是区分用途的依据之一）',
  `post_userid`   int          NOT NULL DEFAULT '0'           COMMENT '作者 id：写接口由 token 推导覆盖，不信任前端传值',
  `content`       varchar(255)          DEFAULT NULL          COMMENT '帖子=纯文本摘要（≤255 字，正文见 post_bodies）；评论/留言=内容本体。列宽单位是字符，装不下富文本，这正是正文另表存储的原因',
  `created_at`    datetime              DEFAULT NULL          COMMENT '创建时间',
  `like_count`    int          NOT NULL DEFAULT '0'           COMMENT '点赞数（冗余计数）',
  `link`          varchar(255)          DEFAULT NULL          COMMENT '外链',
  `imgurl`        varchar(255)          DEFAULT NULL          COMMENT '帖子=封面（服务端用正文首图回填，供列表展示）；评论/留言可为附图',
  `parent_id`     int                   DEFAULT NULL          COMMENT '评论层级：被评论的那一行的 id',
  `root_id`       int                   DEFAULT NULL          COMMENT '评论层级：所属顶层帖的 id',
  `status`        int          NOT NULL DEFAULT '1'           COMMENT '0 软删除 / 1 正常（帖子、评论）/ 2 审核中（当前未使用）/ 3 留言',
  `reply_count`   int                   DEFAULT '0'           COMMENT '回复数（冗余计数）',
  `post_username` varchar(50)           DEFAULT NULL          COMMENT '作者昵称快照（改作者后不自动刷新）',
  PRIMARY KEY (`postid`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb3 COMMENT='帖子/评论/留言（一表多用）';


-- ---------------------------------------------------------------------------
-- 3. post_bodies —— 帖子正文（结构化文档）
-- 与 posts 一对一（postid 既是主键也是外键语义，库内不建物理外键）。
-- 单独成表的原因：① posts 是一表多用，正文文档只属于"帖子"；② 文档可能很大，
-- 放主表会让列表查询把大字段一起拖出来。posts.content 因此正好留作纯文本摘要。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_bodies` (
  `postid`     int        NOT NULL COMMENT '帖子 id，与 posts.postid 一对一',
  `doc`        mediumtext NOT NULL COMMENT '结构化正文文档 JSON（schema v1，由 PostDocValidator 规范化后产出）',
  `updated_at` datetime   NOT NULL COMMENT '最后写入时间',
  PRIMARY KEY (`postid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='帖子正文（结构化文档）';


-- ---------------------------------------------------------------------------
-- 4. news —— 新闻 / 全站公告
-- 约定：`imgUrl IS NULL` 的那一行是**全站公告**，公告正文寄存在 news_link 列
-- （/getAnnouncement 读、/updateAnnouncement 写）。注意列名大小写为 `imgUrl`。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `news` (
  `news_id`   int          NOT NULL AUTO_INCREMENT     COMMENT '新闻 id（主键，自增）',
  `title`     varchar(100)          DEFAULT NULL       COMMENT '标题',
  `imgUrl`     varchar(255)          DEFAULT '1'        COMMENT '配图 URL（注意列名大小写：imgUrl）；为 NULL 的行是"全站公告"',
  `news_link` varchar(255)          DEFAULT NULL       COMMENT '原文链接；公告行以该列承载公告正文',
  `time`      datetime              DEFAULT NULL       COMMENT '发布时间',
  `status`    int                   DEFAULT '1'        COMMENT '1=显示 / 0=不显示（软删除）',
  PRIMARY KEY (`news_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb3 COMMENT='新闻/公告';


-- ---------------------------------------------------------------------------
-- 5. tags —— 标签（当前业务未启用，表结构已就绪）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tags` (
  `tag_id`     int          NOT NULL AUTO_INCREMENT  COMMENT '标签 id（主键，自增）',
  `tag_name`   varchar(32)  NOT NULL                 COMMENT '标签名（唯一）',
  `use_count`  int unsigned          DEFAULT '0'     COMMENT '被引用次数',
  `created_at` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='标签';


-- ---------------------------------------------------------------------------
-- 6. post_tags —— 帖子与标签的多对多（当前业务未启用）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_tags` (
  `post_id` int NOT NULL COMMENT '帖子 id',
  `tag_id`  int NOT NULL COMMENT '标签 id',
  PRIMARY KEY (`post_id`,`tag_id`),
  KEY `idx_tag_post` (`tag_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='帖子-标签关联';


-- ---------------------------------------------------------------------------
-- 7. functionform —— 功能开关 / 首页宣传图
-- 两类数据共表，靠 function_name 区分：
--   开关：connectWebSiteOwner / uploadWork / databaseFunction（1 开 / 0 关）
--   宣传图：function_name='IndexIMGlink'，图片地址存在 function_link
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `functionform` (
  `id`              int          NOT NULL AUTO_INCREMENT  COMMENT '主键，自增',
  `function_name`   varchar(50)           DEFAULT NULL    COMMENT '功能名：开关为 connectWebSiteOwner / uploadWork / databaseFunction；宣传图为 IndexIMGlink',
  `function_status` tinyint               DEFAULT '0'     COMMENT '状态：1 启用 / 0 停用（被删的宣传图置 0）',
  `function_link`   varchar(255)          DEFAULT NULL    COMMENT '附加链接；宣传图类型时存图片 URL',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COMMENT='功能开关/首页宣传图';


-- ---------------------------------------------------------------------------
-- 8. logrecords —— 访问日志
-- 注意：AOP 切面（Aop/LogRecordAspect）已拦截，但落库语句当前是注释状态，
--       因此**这张表暂时不会写入数据**（表结构先就绪）。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `logrecords` (
  `id`          int          NOT NULL AUTO_INCREMENT  COMMENT '主键，自增',
  `ip`          varchar(100)          DEFAULT NULL    COMMENT '请求来源 IP',
  `time`        datetime              DEFAULT NULL    COMMENT '访问时间',
  `classmethod` varchar(100)          DEFAULT NULL    COMMENT '被调用的方法名',
  `httpmethod`  varchar(30)           DEFAULT NULL    COMMENT 'HTTP 方法',
  `classname`   varchar(100)          DEFAULT NULL    COMMENT '所属类名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb3 COMMENT='访问日志（当前未落库）';


-- ============================================================================
-- 建库完成后的自检
-- ============================================================================
-- SHOW TABLES;                      -- 应有 8 张表
-- SELECT table_name, table_comment FROM information_schema.tables
--   WHERE table_schema='useforcommon' ORDER BY table_name;
-- SELECT table_name, column_name, column_type, column_comment
--   FROM information_schema.columns
--   WHERE table_schema='useforcommon' ORDER BY table_name, ordinal_position;
--
-- 应用侧还需确认（application.properties）：
--   spring.datasource.url 指向本库，且 mybatis.type-aliases-package=com.lycorisfun.fun.Entity（大写 E）

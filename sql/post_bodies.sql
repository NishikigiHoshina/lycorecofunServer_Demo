-- ============================================================================
-- 帖子正文结构化文档表 —— 增量建表脚本（给**已有**的 useforcommon 库补一张表）
--
-- ⚠️ 全新部署请用同目录的 `schema.sql`（建库 + 全部 8 张表 + 全字段注释），
--    它已包含本表；本文件只用于"库已经在跑、需要补这一张新表"的场景。
--
-- 背景：posts.content 是 varchar(255)（单位是"字符"），装不下富文本：一段 wangEditor
--       产出的 HTML 约 240 个可见字就到顶，超了在严格模式下报 1406、非严格模式下**静默截断**。
--       因此正文改存「结构化文档 JSON」，posts.content 退化为「纯文本摘要」——
--       摘要长度正好落在 varchar(255) 内，**不需要改列类型**，广场列表也终于有真实摘要可显示。
--
-- 执行（**务必带 --default-character-set=utf8mb4**）：
--   mysql -uroot -p --default-character-set=utf8mb4 useforcommon < post_bodies.sql
--
-- ⚠️ 不带该参数时，客户端会按本地代码页（中文 Windows 上通常是 gbk）解释这个 UTF-8 文件，
--    表建得出来、结构也对，但**表名/列注释会永久写成乱码**（实测踩过）。
--    若已建错：DROP TABLE post_bodies; 再按上面的命令重建（表为空时无数据损失）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS post_bodies (
  postid     INT        NOT NULL COMMENT '帖子 id，与 posts.postid 一对一',
  doc        MEDIUMTEXT NOT NULL COMMENT '结构化正文文档 JSON（schema v1，由 PostDocValidator 规范化后产出）',
  updated_at DATETIME   NOT NULL COMMENT '最后写入时间',
  PRIMARY KEY (postid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COMMENT='帖子正文（结构化文档）';

-- ----------------------------------------------------------------------------
-- 说明
-- ----------------------------------------------------------------------------
-- 1) 不建外键：与库内其余表保持一致。软删帖子（status=0）时正文一并保留，便于恢复。
--
-- 2) 字符集沿用 utf8mb3（与整库一致，本次决策：暂不升级 utf8mb4）。
--    所以正文仍不能出现 emoji —— 服务端 util/TextValidator 会在入库前拦下并返回友好 400。
--    若日后升级 utf8mb4，本表 doc 与 posts.content/ title 需一并 CONVERT TO CHARACTER SET。
--
-- 3) 写入路径：POST /api/writepost（body 里的 doc 字段）。
--    服务端会做白名单校验与规范化（util/PostDocValidator），落库的是服务端产出的规范 JSON，
--    不是客户端提交的原样内容。读取路径：/api/getPostByid 把 doc 一并挂在响应的 post 上。
--
-- 4) **存量帖子无需数据迁移**：它们的 posts.content 里是短 HTML、doc 为 NULL，
--    前端 PostDetail.vue 会用同一个白名单转换器「现转现渲染」（DOMParser 只解析、不执行脚本，
--    且不插入真实文档），因此老帖照常打开、且同样不经过 v-html。
--    列表页的摘要由服务端 PostDocText.legacyExcerpt 剥标签得到。
--    若希望把老帖也落成文档，可逐条重新发表，或另写一次性迁移（本次未做）。
-- ----------------------------------------------------------------------------

-- ----------------------------------------------------------------------------
-- 验证
-- ----------------------------------------------------------------------------
-- SHOW CREATE TABLE post_bodies;
-- SELECT COUNT(*) FROM post_bodies;
--
-- 帖子行（title 非空）应逐渐出现对应正文；评论/留言（title 为空）不应有：
-- SELECT p.postid, p.title, (b.postid IS NOT NULL) AS has_doc, LEFT(p.content, 40) AS excerpt
-- FROM posts p LEFT JOIN post_bodies b ON b.postid = p.postid
-- WHERE p.title IS NOT NULL AND p.status = 1
-- ORDER BY p.postid DESC LIMIT 20;

package com.lycorisfun.fun.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 帖子 / 评论 / 留言接口测试（含正文结构化文档的落库与鉴权）。 */
@DisplayName("帖子接口")
class PostApiTest extends ApiTestBase {

    /* ==================== 工具 ==================== */

    /** 一段"文字 + 加粗 + 内嵌图片"的正文文档，图片在段落内部（编辑器的真实产出形态） */
    private static String validDoc() {
        return "{\"v\":1,\"nodes\":["
                + "{\"t\":\"p\",\"c\":[\"正文\",{\"t\":\"b\",\"c\":[\"加粗\"]}]},"
                + "{\"t\":\"p\",\"c\":[{\"t\":\"img\",\"src\":\"/upload/post/cover.png\",\"alt\":\"图\"}]}"
                + "]}";
    }

    private static String doc(String nodesJson) {
        return "{\"v\":1,\"nodes\":[" + nodesJson + "]}";
    }

    private MvcResult writePost(String title, String docJson, Cookie cookie) throws Exception {
        return mvc.perform(post("/api/writepost")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "doc", docJson))))
                .andReturn();
    }

    private MvcResult getPost(int postid) throws Exception {
        return mvc.perform(post("/api/getPostByid").param("postid", String.valueOf(postid)))
                .andReturn();
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("发帖")
    class WritePost {

        @Test
        @DisplayName("发帖成功：摘要进 posts.content、正文进 post_bodies、首图回填 imgurl")
        void writeOk() throws Exception {
            Cookie cookie = login(newNormalUser("post_author"));
            String title = "发帖成功用例";
            var res = writePost(title, validDoc(), cookie);
            assertThat(res.getResponse().getStatus()).isEqualTo(200);

            int postid = lastPostIdByTitle(title);
            Map<String, Object> row = postRow(postid);

            // 摘要：纯文本、图片以 [图片] 占位、不含任何 HTML 标签。
            // 行内标记（加粗）之间紧邻不加空格，块级/图片之间加空格 —— 与编辑器里的视觉一致
            String excerpt = (String) row.get("content");
            assertThat(excerpt).isEqualTo("正文加粗 [图片]");
            assertThat(excerpt).doesNotContain("<", ">");

            // 封面：服务端用正文首图回填
            assertThat(row.get("imgurl")).isEqualTo("/upload/post/cover.png");

            // 正文文档另表存储，且是服务端产出的规范 JSON
            assertThat(bodyCount(postid)).isEqualTo(1);
            assertThat(docOf(postid)).contains("\"v\":1").contains("/upload/post/cover.png");

            assertThat(row.get("status")).isEqualTo(1);
        }

        @Test
        @DisplayName("行内图片文档可正常发帖（回归：此前会报「不支持的行内类型：img」）")
        void inlineImageAccepted() throws Exception {
            Cookie cookie = login(newNormalUser("post_inline_img"));
            String title = "行内图片用例";
            var res = writePost(title, doc("{\"t\":\"p\",\"c\":[{\"t\":\"img\","
                    + "\"src\":\"/upload/post/inline.png\",\"alt\":\"\"}]}"), cookie);
            assertThat(res.getResponse().getStatus()).isEqualTo(200);
            assertThat(postRow(lastPostIdByTitle(title)).get("imgurl")).isEqualTo("/upload/post/inline.png");
        }

        @Test
        @DisplayName("作者身份由 token 推导：body 里塞别人的 post_userid 会被忽略")
        void authorFromToken() throws Exception {
            String authorEmail = newNormalUser("post_owner_real");
            newNormalUser("post_owner_fake");           // 存在的另一个用户，但不是发起人
            Cookie cookie = login(authorEmail);
            String title = "作者推导用例";

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("doc", validDoc());
            payload.put("post_userid", 999999);         // 伪造：不存在的作者 id
            mvc.perform(post("/api/writepost").cookie(cookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            int postid = lastPostIdByTitle(title);
            assertThat(postRow(postid).get("post_userid")).isEqualTo(userIdOf(authorEmail));
        }

        @Test
        @DisplayName("未登录发帖 → 401")
        void writeWithoutLogin() throws Exception {
            mvc.perform(post("/api/writepost")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("title", "x", "doc", validDoc()))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("正文含外链图片 → 400（阻断追踪像素 / data: URI）")
        void externalImageRejected() throws Exception {
            Cookie cookie = login(newNormalUser("post_ext_img"));
            var res = writePost("外链图用例", doc("{\"t\":\"p\",\"c\":[{\"t\":\"img\","
                    + "\"src\":\"https://evil.test/x.png\",\"alt\":\"\"}]}"), cookie);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
            assertThat(body(res).get("msg").asText()).contains("本站上传");
        }

        @Test
        @DisplayName("正文含 emoji → 400（库为 utf8mb3）")
        void emojiRejected() throws Exception {
            Cookie cookie = login(newNormalUser("post_emoji"));
            var res = writePost("emoji用例", doc("{\"t\":\"p\",\"c\":[\"笑死 😀\"]}"), cookie);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("正文为空 → 400")
        void emptyDocRejected() throws Exception {
            Cookie cookie = login(newNormalUser("post_empty_doc"));
            assertThat(writePost("空正文用例", doc(""), cookie).getResponse().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("正文含未知节点类型 → 400")
        void unknownNodeRejected() throws Exception {
            Cookie cookie = login(newNormalUser("post_bad_node"));
            var res = writePost("坏节点用例", doc("{\"t\":\"script\",\"c\":[\"alert(1)\"]}"), cookie);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
            assertThat(body(res).get("msg").asText()).contains("不支持的块类型");
        }

        @Test
        @DisplayName("标题为空时落库为「无标题」")
        void blankTitleFallback() throws Exception {
            Cookie cookie = login(newNormalUser("post_blank_title"));
            writePost("", validDoc(), cookie);
            assertThat(postRow(lastPostIdByTitle("无标题")).get("title")).isEqualTo("无标题");
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("读取：详情与分页")
    class Read {

        @Test
        @DisplayName("详情返回结构化正文 doc（供前端渲染，前端不再用 v-html）")
        void detailHasDoc() throws Exception {
            Cookie cookie = login(newNormalUser("post_detail"));
            String title = "详情用例";
            writePost(title, validDoc(), cookie);
            int postid = lastPostIdByTitle(title);

            MvcResult res = getPost(postid);
            assertThat(res.getResponse().getStatus()).isEqualTo(200);
            var b = body(res);
            assertThat(b.get("postid").asInt()).isEqualTo(postid);
            assertThat(b.get("doc").isNull()).isFalse();
            assertThat(b.get("doc").asText()).contains("/upload/post/cover.png");
            assertThat(b.get("imgurl").asText()).isEqualTo("/upload/post/cover.png");
        }

        @Test
        @DisplayName("详情：帖子不存在 → 404")
        void detailNotFound() throws Exception {
            assertThat(getPost(99999999).getResponse().getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("分页接口返回 list/total/page/size，且列表里的正文是纯文本摘要")
        void pageList() throws Exception {
            Cookie cookie = login(newNormalUser("post_page"));
            String title = "分页用例";
            writePost(title, validDoc(), cookie);

            MvcResult res = mvc.perform(post("/api/postlistPage")
                            .param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andReturn();
            var b = body(res);
            assertThat(b.get("code").asInt()).isEqualTo(200);
            assertThat(b.get("list").isArray()).isTrue();
            assertThat(b.get("total").asInt()).isPositive();
            assertThat(b.has("page")).isTrue();
            assertThat(b.has("size")).isTrue();

            // 列表项不应携带正文文档（大字段不进列表）
            b.get("list").forEach(item -> assertThat(item.get("doc").isNull()).isTrue());

            // 刚发的帖子应出现在第一页，且其摘要不含 HTML 标签
            var mine = findInList(b, title);
            assertThat(mine).as("列表应包含刚发布的帖子").isNotNull();
            assertThat(mine.get("content").asText()).doesNotContain("<", ">");
        }

        @Test
        @DisplayName("getPostList 返回全部帖子行（后台管理用：不过滤状态，含评论/留言）")
        void getAll() throws Exception {
            MvcResult res = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/getPostList"))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(res).isArray()).isTrue();
            assertThat(body(res).size()).isPositive();
        }

        @Test
        @DisplayName("postlist 返回最近若干条，且列表视图把正文清空")
        void listTopN() throws Exception {
            MvcResult res = mvc.perform(post("/api/postlist")).andExpect(status().isOk()).andReturn();
            var arr = body(res);
            assertThat(arr.isArray()).isTrue();
            assertThat(arr.size()).isPositive();
            arr.forEach(p -> assertThat(p.get("content").asText()).isEmpty());
        }

        @Test
        @DisplayName("searchByuser 按作者昵称反查其帖子")
        void searchByUser() throws Exception {
            Cookie cookie = login(newNormalUser("post_by_user"));
            writePost("按作者搜索用例", validDoc(), cookie);

            MvcResult res = mvc.perform(post("/api/searchByuser").param("username", "post_by_user"))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(res).size()).isPositive();
        }

        @Test
        @DisplayName("按标题搜索：命中返回列表，未命中 → 404")
        void searchByTitle() throws Exception {
            Cookie cookie = login(newNormalUser("post_search"));
            writePost("可被搜到的独特标题", validDoc(), cookie);

            MvcResult hit = mvc.perform(post("/api/searchBytitle").param("title", "可被搜到的独特标题"))
                    .andReturn();
            assertThat(hit.getResponse().getStatus()).isEqualTo(200);
            assertThat(body(hit).isArray()).isTrue();
            assertThat(body(hit).size()).isPositive();

            MvcResult miss = mvc.perform(post("/api/searchBytitle").param("title", "不可能存在的标题zzz"))
                    .andReturn();
            assertThat(miss.getResponse().getStatus()).isEqualTo(404);
        }

        private com.fasterxml.jackson.databind.JsonNode findInList(com.fasterxml.jackson.databind.JsonNode b, String title) {
            for (var item : b.get("list")) {
                if (title.equals(item.get("title").asText())) {
                    return item;
                }
            }
            return null;
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("修改与删除：属主 / 管理员鉴权")
    class Modify {

        @Test
        @DisplayName("属主可以改自己的帖子")
        void ownerCanUpdate() throws Exception {
            String ownerEmail = newNormalUser("post_upd_owner");
            Cookie owner = login(ownerEmail);
            String title = "改帖-属主用例";
            writePost(title, validDoc(), owner);
            int postid = lastPostIdByTitle(title);

            MvcResult res = mvc.perform(post("/api/updatePostinfo").cookie(owner)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("postid", postid, "title", "改后的标题"))))
                    .andReturn();
            assertThat(res.getResponse().getStatus()).isEqualTo(200);
            assertThat(postRow(postid).get("title")).isEqualTo("改后的标题");
        }

        @Test
        @DisplayName("非属主的普通用户改帖 → 403")
        void strangerCannotUpdate() throws Exception {
            Cookie owner = login(newNormalUser("post_upd_owner2"));
            String title = "改帖-越权用例";
            writePost(title, validDoc(), owner);
            int postid = lastPostIdByTitle(title);

            Cookie stranger = login(newNormalUser("post_upd_stranger"));
            mvc.perform(post("/api/updatePostinfo").cookie(stranger)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("postid", postid, "title", "越权改的标题"))))
                    .andExpect(status().isForbidden());
            assertThat(postRow(postid).get("title")).isEqualTo(title);
        }

        @Test
        @DisplayName("管理员可以改别人的帖子")
        void adminCanUpdate() throws Exception {
            Cookie owner = login(newNormalUser("post_upd_owner3"));
            String title = "改帖-管理员用例";
            writePost(title, validDoc(), owner);
            int postid = lastPostIdByTitle(title);

            Cookie admin = login(newAdmin("post_upd_admin"));
            mvc.perform(post("/api/updatePostinfo").cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("postid", postid, "title", "管理员改的标题"))))
                    .andExpect(status().isOk());
            assertThat(postRow(postid).get("title")).isEqualTo("管理员改的标题");
        }

        @Test
        @DisplayName("属主删帖：软删除（status→0），非属主 → 403")
        void deletePost() throws Exception {
            Cookie owner = login(newNormalUser("post_del_owner"));
            String title = "删帖用例";
            writePost(title, validDoc(), owner);
            int postid = lastPostIdByTitle(title);

            Cookie stranger = login(newNormalUser("post_del_stranger"));
            mvc.perform(post("/api/deletePost").cookie(stranger).param("postid", String.valueOf(postid)))
                    .andExpect(status().isForbidden());
            assertThat(postRow(postid).get("status")).isEqualTo(1);      // 未被改动

            mvc.perform(post("/api/deletePost").cookie(owner).param("postid", String.valueOf(postid)))
                    .andExpect(status().isOk());
            assertThat(postRow(postid).get("status")).isEqualTo(0);      // 软删
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("评论与留言")
    class Comment {

        @Test
        @DisplayName("发评论：作者由 token 推导、status=1、title 置空")
        void writeComment() throws Exception {
            String email = newNormalUser("cmt_author");
            Cookie cookie = login(email);
            String title = "评论宿主帖";
            writePost(title, validDoc(), cookie);
            int rootPost = lastPostIdByTitle(title);

            mvc.perform(post("/api/writecomment").cookie(cookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "parent_id", rootPost, "root_id", rootPost,
                                    "post_userid", 888888,            // 伪造，应被忽略
                                    "content", "这是一条测试评论"))))
                    .andExpect(status().isOk());

            Integer commentId = jdbc.queryForObject(
                    "select max(postid) from posts where content = ?", Integer.class, "这是一条测试评论");
            Map<String, Object> row = postRow(commentId);
            assertThat(row.get("post_userid")).isEqualTo(userIdOf(email));
            assertThat(row.get("status")).isEqualTo(1);
            assertThat(row.get("title")).isNull();
            assertThat(row.get("parent_id")).isEqualTo(rootPost);
        }

        @Test
        @DisplayName("楼中楼：getReply 返回该 parent_id 下的回复；无回复时 code=404")
        void getReply() throws Exception {
            Cookie cookie = login(newNormalUser("cmt_reply"));
            String title = "楼中楼用例";
            writePost(title, validDoc(), cookie);
            int rootPost = lastPostIdByTitle(title);

            mvc.perform(post("/api/writecomment").cookie(cookie)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "parent_id", rootPost, "root_id", rootPost, "content", "楼中楼回复"))))
                    .andExpect(status().isOk());

            MvcResult res = mvc.perform(post("/api/getReply").param("parent_id", String.valueOf(rootPost)))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(res).get("code").asInt()).isEqualTo(200);
            assertThat(body(res).get("replylist").size()).isPositive();

            MvcResult empty = mvc.perform(post("/api/getReply").param("parent_id", "99999999"))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(empty).get("code").asInt()).isEqualTo(404);
        }

        @Test
        @DisplayName("未登录发评论 → 401")
        void commentNeedsLogin() throws Exception {
            mvc.perform(post("/api/writecomment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "parent_id", 1, "root_id", 1, "content", "匿名评论"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("留言：匿名可写（刻意设计），落库 status=3")
        void guestMessage() throws Exception {
            String content = "匿名留言用例";
            mvc.perform(post("/api/takemessage")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of(
                                    "content", content, "post_username", "过路访客"))))
                    .andExpect(status().isOk());

            Integer id = jdbc.queryForObject("select max(postid) from posts where content = ?",
                    Integer.class, content);
            assertThat(postRow(id).get("status")).isEqualTo(3);
        }

        @Test
        @DisplayName("留言含 emoji → 400")
        void guestMessageEmoji() throws Exception {
            mvc.perform(post("/api/takemessage")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("content", "留言😀"))))
                    .andExpect(status().isBadRequest());
        }
    }
}

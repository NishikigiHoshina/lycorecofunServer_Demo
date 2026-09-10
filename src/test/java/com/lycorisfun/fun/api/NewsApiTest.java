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

/** 新闻 / 全站公告接口测试（含后台管理接口的管理员鉴权与缓存失效）。 */
@DisplayName("新闻与公告接口")
class NewsApiTest extends ApiTestBase {

    /**
     * 保证库里存在"公告行"（news 表中 imgUrl IS NULL 的那行）并写入指定文本。
     * 先查后写而非直接插入：库中可能已有公告行，直接插入会出现两行，读取结果就不确定了。
     */
    private void setAnnouncement(String text) {
        Integer n = jdbc.queryForObject("select count(*) from news where imgUrl is null", Integer.class);
        if (n != null && n > 0) {
            jdbc.update("update news set news_link = ?, status = 1 where imgUrl is null", text);
        } else {
            jdbc.update("insert into news(title, imgUrl, news_link, time, status) values (?, null, ?, now(), 1)",
                    "公告", text);
        }
    }

    private MvcResult postJson(String url, Object payload, Cookie cookie) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload));
        if (cookie != null) {
            req.cookie(cookie);
        }
        return mvc.perform(req).andReturn();
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("公开读取")
    class Read {

        @Test
        @DisplayName("newslist 只返回显示中的新闻（status=1 且 imgUrl 非空）")
        void newslist() throws Exception {
            MvcResult res = mvc.perform(post("/api/newslist")).andExpect(status().isOk()).andReturn();
            var arr = body(res);
            assertThat(arr.isArray()).isTrue();
            arr.forEach(n -> {
                assertThat(n.get("status").asInt()).isEqualTo(1);
                // 注意：JSON 字段名是 imgurl（由 Java getter getImgurl() 决定），
                // 与数据库列名 imgUrl 的大小写**不一致**，前端取值时要按 imgurl
                assertThat(n.get("imgurl").isNull()).isFalse();
            });
        }

        @Test
        @DisplayName("newslistAll 返回全部（含已软删，供后台管理）")
        void newslistAll() throws Exception {
            MvcResult res = mvc.perform(post("/api/newslistAll")).andExpect(status().isOk()).andReturn();
            assertThat(body(res).isArray()).isTrue();
        }

        @Test
        @DisplayName("getAnnouncement 返回公告正文（寄存在 news_link 列）")
        void announcement() throws Exception {
            setAnnouncement("测试公告正文");
            MvcResult res = mvc.perform(post("/api/getAnnouncement")).andExpect(status().isOk()).andReturn();
            assertThat(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                    .isEqualTo("测试公告正文");
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("后台新闻管理")
    class Manage {

        @Test
        @DisplayName("普通用户新建新闻 → 403（仅管理员）")
        void createForbidden() throws Exception {
            Cookie user = login(newNormalUser("news_normal"));
            var res = postJson("/api/addnews", Map.of("title", "越权新闻"), user);
            assertThat(res.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("管理员新建新闻 → 200，返回自增 news_id 且已落库")
        void createOk() throws Exception {
            Cookie admin = login(newAdmin("news_admin1"));
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", "新建新闻用例");
            payload.put("imgurl", "/upload/a.png");
            payload.put("news_link", "https://example.com/n1");
            var res = postJson("/api/addnews", payload, admin);

            assertThat(res.getResponse().getStatus()).isEqualTo(200);
            int newsId = body(res).get("news_id").asInt();
            assertThat(newsId).isPositive();

            Map<String, Object> row = jdbc.queryForMap("select * from news where news_id = ?", newsId);
            assertThat(row.get("title")).isEqualTo("新建新闻用例");
            assertThat(row.get("status")).isEqualTo(1);
        }

        @Test
        @DisplayName("新建新闻：标题为空 / 含 emoji → 400")
        void createValidation() throws Exception {
            Cookie admin = login(newAdmin("news_admin2"));
            assertThat(postJson("/api/addnews", Map.of("title", ""), admin).getResponse().getStatus())
                    .isEqualTo(400);
            Map<String, Object> emoji = new HashMap<>();
            emoji.put("title", "新闻😀标题");
            assertThat(postJson("/api/addnews", emoji, admin).getResponse().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("管理员编辑新闻 → 200；状态可改回 1 实现「恢复显示」")
        void updateOk() throws Exception {
            Cookie admin = login(newAdmin("news_admin3"));
            Map<String, Object> create = new HashMap<>();
            create.put("title", "待编辑新闻");
            int newsId = body(postJson("/api/addnews", create, admin)).get("news_id").asInt();

            Map<String, Object> edit = new HashMap<>();
            edit.put("news_id", newsId);
            edit.put("title", "已编辑的新闻");
            edit.put("status", 0);
            assertThat(postJson("/api/updatenews", edit, admin).getResponse().getStatus()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select title from news where news_id = ?", String.class, newsId))
                    .isEqualTo("已编辑的新闻");
            assertThat(jdbc.queryForObject("select status from news where news_id = ?", Integer.class, newsId))
                    .isEqualTo(0);

            edit.put("status", 1);      // 恢复显示
            assertThat(postJson("/api/updatenews", edit, admin).getResponse().getStatus()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select status from news where news_id = ?", Integer.class, newsId))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("普通用户编辑/删除新闻 → 403")
        void updateDeleteForbidden() throws Exception {
            Cookie admin = login(newAdmin("news_admin4"));
            Map<String, Object> create = new HashMap<>();
            create.put("title", "越权目标新闻");
            int newsId = body(postJson("/api/addnews", create, admin)).get("news_id").asInt();

            Cookie user = login(newNormalUser("news_normal2"));
            assertThat(postJson("/api/updatenews", Map.of("news_id", newsId, "title", "越权改"), user)
                    .getResponse().getStatus()).isEqualTo(403);
            assertThat(mvc.perform(post("/api/deletenews").cookie(user).param("news_id", String.valueOf(newsId)))
                    .andReturn().getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("管理员删除新闻 → 软删（status=0），且不再出现在前台 newslist")
        void deleteSoft() throws Exception {
            Cookie admin = login(newAdmin("news_admin5"));
            Map<String, Object> create = new HashMap<>();
            create.put("title", "待删除新闻");
            create.put("imgurl", "/upload/b.png");
            int newsId = body(postJson("/api/addnews", create, admin)).get("news_id").asInt();

            mvc.perform(post("/api/deletenews").cookie(admin).param("news_id", String.valueOf(newsId)))
                    .andExpect(status().isOk());
            assertThat(jdbc.queryForObject("select status from news where news_id = ?", Integer.class, newsId))
                    .isEqualTo(0);

            // 缓存已失效，前台列表应立即看不到它
            MvcResult list = mvc.perform(post("/api/newslist")).andReturn();
            body(list).forEach(n -> assertThat(n.get("news_id").asInt()).isNotEqualTo(newsId));
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("全站公告更新")
    class Announcement {

        @Test
        @DisplayName("普通用户更新公告 → 403")
        void forbidden() throws Exception {
            Cookie user = login(newNormalUser("ann_normal"));
            var res = postJson("/api/updateAnnouncement", Map.of("content", "越权公告"), user);
            assertThat(res.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("管理员更新公告 → 200，且 getAnnouncement 立即返回新内容（验证缓存主动失效）")
        void okAndCacheEvicted() throws Exception {
            setAnnouncement("旧公告");
            // 先读一次把旧值放进缓存，再更新 —— 若缓存没被清掉，下面就会读到旧内容
            mvc.perform(post("/api/getAnnouncement")).andReturn();

            Cookie admin = login(newAdmin("ann_admin"));
            var res = postJson("/api/updateAnnouncement", Map.of("content", "新公告内容"), admin);
            assertThat(res.getResponse().getStatus()).isEqualTo(200);

            MvcResult after = mvc.perform(post("/api/getAnnouncement")).andReturn();
            assertThat(after.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                    .isEqualTo("新公告内容");
        }

        @Test
        @DisplayName("公告内容含 emoji → 400")
        void emojiRejected() throws Exception {
            Cookie admin = login(newAdmin("ann_admin2"));
            var res = postJson("/api/updateAnnouncement", Map.of("content", "公告😀"), admin);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
        }
    }
}

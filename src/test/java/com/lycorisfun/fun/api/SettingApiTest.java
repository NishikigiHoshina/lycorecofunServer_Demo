package com.lycorisfun.fun.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 功能开关 / 首页宣传图 / 公告相关接口测试（写操作一律要求管理员）。 */
@DisplayName("站内设置接口")
class SettingApiTest extends ApiTestBase {

    /** 造一条功能开关行，保证用例不依赖库里已有的数据 */
    private void setFunction(String name, int status) {
        Integer n = jdbc.queryForObject("select count(*) from functionform where function_name = ?",
                Integer.class, name);
        if (n != null && n > 0) {
            jdbc.update("update functionform set function_status = ? where function_name = ?", status, name);
        } else {
            jdbc.update("insert into functionform(function_name, function_status, function_link) values (?,?,null)",
                    name, status);
        }
    }

    private boolean getFuncStatus(String name) throws Exception {
        MvcResult res = mvc.perform(post("/api/getfuncstatus").param("funcname", name))
                .andExpect(status().isOk()).andReturn();
        return Boolean.parseBoolean(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(6, 6, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("功能开关")
    class FuncStatus {

        @Test
        @DisplayName("getfuncstatus 按名称返回开关状态（1→true / 0→false）")
        void read() throws Exception {
            setFunction("testSwitchOn", 1);
            setFunction("testSwitchOff", 0);
            assertThat(getFuncStatus("testSwitchOn")).isTrue();
            assertThat(getFuncStatus("testSwitchOff")).isFalse();
        }

        @Test
        @DisplayName("普通用户改开关 → 403")
        void updateForbidden() throws Exception {
            setFunction("testSwitchAuth", 0);
            Cookie user = login(newNormalUser("set_normal1"));
            mvc.perform(post("/api/updateStatus").cookie(user)
                            .param("funcname", "testSwitchAuth").param("status", "1"))
                    .andExpect(status().isForbidden());
            assertThat(getFuncStatus("testSwitchAuth")).isFalse();
        }

        @Test
        @DisplayName("管理员改开关 → 200 且立即生效（按 funcname 失效缓存）")
        void updateOk() throws Exception {
            setFunction("testSwitchAdmin", 0);
            assertThat(getFuncStatus("testSwitchAdmin")).isFalse();     // 先读一次进缓存

            Cookie admin = login(newAdmin("set_admin1"));
            mvc.perform(post("/api/updateStatus").cookie(admin)
                            .param("funcname", "testSwitchAdmin").param("status", "1"))
                    .andExpect(status().isOk());

            assertThat(getFuncStatus("testSwitchAdmin"))
                    .as("写接口应主动失效 funcStatus 缓存").isTrue();
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("首页宣传图")
    class IndexImage {

        @Test
        @DisplayName("无启用中的宣传图时 getIndexIMG 返回 code=400")
        void readEmpty() throws Exception {
            jdbc.update("update functionform set function_status = 0 where function_name = 'IndexIMGlink'");
            MvcResult res = mvc.perform(post("/api/getIndexIMG")).andExpect(status().isOk()).andReturn();
            assertThat(body(res).get("code").asInt()).isEqualTo(400);
        }

        @Test
        @DisplayName("有启用中的宣传图时返回 data 列表")
        void readNonEmpty() throws Exception {
            jdbc.update("update functionform set function_status = 0 where function_name = 'IndexIMGlink'");
            jdbc.update("insert into functionform(function_name, function_status, function_link) "
                    + "values ('IndexIMGlink', 1, '/upload/img/banner.png')");

            MvcResult res = mvc.perform(post("/api/getIndexIMG")).andExpect(status().isOk()).andReturn();
            assertThat(body(res).get("code").asInt()).isEqualTo(200);
            assertThat(body(res).get("data").size()).isPositive();
        }

        @Test
        @DisplayName("普通用户上传宣传图 → 403")
        void uploadForbidden() throws Exception {
            Cookie user = login(newNormalUser("set_normal2"));
            mvc.perform(multipart("/api/addIndexIMG").file(
                            new MockMultipartFile("file", "b.png", "image/png", png())).cookie(user))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("管理员上传宣传图 → 200，落盘并回写绝对 URL 入库")
        void uploadOk() throws Exception {
            Cookie admin = login(newAdmin("set_admin2"));
            MvcResult res = mvc.perform(multipart("/api/addIndexIMG").file(
                            new MockMultipartFile("file", "banner.png", "image/png", png())).cookie(admin))
                    .andExpect(status().isOk()).andReturn();

            String url = body(res).get("dataobject").asText();
            assertThat(url).contains("/upload/").endsWith(".png");

            Integer n = jdbc.queryForObject(
                    "select count(*) from functionform where function_link = ?", Integer.class, url);
            assertThat(n).as("URL 应已写入 functionform").isEqualTo(1);
        }

        @Test
        @DisplayName("管理员删除宣传图 → 200，软删（function_status→0）")
        void deleteOk() throws Exception {
            jdbc.update("insert into functionform(function_name, function_status, function_link) "
                    + "values ('IndexIMGlink', 1, '/upload/img/to-delete.png')");
            Integer id = jdbc.queryForObject("select max(id) from functionform where function_name='IndexIMGlink'",
                    Integer.class);

            Cookie admin = login(newAdmin("set_admin3"));
            mvc.perform(post("/api/deleteIndexIMG").cookie(admin).param("id", String.valueOf(id)))
                    .andExpect(status().isOk());
            assertThat(jdbc.queryForObject("select function_status from functionform where id = ?",
                    Integer.class, id)).isEqualTo(0);
        }

        @Test
        @DisplayName("普通用户删除宣传图 → 403")
        void deleteForbidden() throws Exception {
            jdbc.update("insert into functionform(function_name, function_status, function_link) "
                    + "values ('IndexIMGlink', 1, '/upload/img/keep.png')");
            Integer id = jdbc.queryForObject("select max(id) from functionform where function_name='IndexIMGlink'",
                    Integer.class);

            Cookie user = login(newNormalUser("set_normal3"));
            mvc.perform(post("/api/deleteIndexIMG").cookie(user).param("id", String.valueOf(id)))
                    .andExpect(status().isForbidden());
            assertThat(jdbc.queryForObject("select function_status from functionform where id = ?",
                    Integer.class, id)).isEqualTo(1);
        }
    }
}

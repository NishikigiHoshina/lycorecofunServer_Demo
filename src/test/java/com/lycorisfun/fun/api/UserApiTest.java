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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用户查询 / 个人中心 / 后台用户管理接口测试（含口令脱敏与管理员鉴权）。 */
@DisplayName("用户接口")
class UserApiTest extends ApiTestBase {

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
    @DisplayName("查询：口令必须脱敏")
    class Query {

        @Test
        @DisplayName("userlist 返回列表，且每条的 password 均为 null")
        void userlistMasked() throws Exception {
            newNormalUser("list_probe");
            MvcResult res = mvc.perform(get("/api/userlist")).andExpect(status().isOk()).andReturn();
            var arr = body(res);
            assertThat(arr.isArray()).isTrue();
            assertThat(arr.size()).isPositive();
            arr.forEach(u -> assertThat(u.get("password").isNull())
                    .as("列表接口不得外泄口令密文").isTrue());
        }

        @Test
        @DisplayName("searchByUsername 按昵称模糊查，password 同样脱敏")
        void searchByUsername() throws Exception {
            newNormalUser("name_probe_unique");
            MvcResult res = mvc.perform(post("/api/searchByUsername").param("name", "name_probe_unique"))
                    .andExpect(status().isOk()).andReturn();
            var arr = body(res);
            assertThat(arr.size()).isPositive();
            arr.forEach(u -> assertThat(u.get("password").isNull()).isTrue());
        }

        @Test
        @DisplayName("searchByuserid 按 id 查，password 脱敏")
        void searchByUserId() throws Exception {
            String email = newNormalUser("id_probe");
            int id = userIdOf(email);
            MvcResult res = mvc.perform(post("/api/searchByuserid").param("id", String.valueOf(id)))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(res).get("userId").asInt()).isEqualTo(id);
            assertThat(body(res).get("password").isNull()).isTrue();
        }

        @Test
        @DisplayName("searchUser 是回显桩：原样返回传入的 id / name")
        void searchUserEcho() throws Exception {
            MvcResult res = mvc.perform(get("/api/searchUser").param("id", "7").param("name", "回显名"))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(res).get("userId").asInt()).isEqualTo(7);
            assertThat(body(res).get("userName").asText()).isEqualTo("回显名");
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("个人中心 getMyProfile")
    class Profile {

        @Test
        @DisplayName("未登录 → 401")
        void needsLogin() throws Exception {
            mvc.perform(post("/api/getMyProfile")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("已登录 → 返回自己的画像（含 userId，且不含 password）")
        void ok() throws Exception {
            String email = newNormalUser("profile_user");
            Cookie cookie = login(email);
            MvcResult res = mvc.perform(post("/api/getMyProfile").cookie(cookie))
                    .andExpect(status().isOk()).andReturn();
            var data = body(res).get("data");
            assertThat(data.get("userId").asInt()).isEqualTo(userIdOf(email));
            assertThat(data.get("password").isNull()).isTrue();
        }
    }

    /* ==================================================================== */
    @Nested
    @DisplayName("后台用户管理（仅管理员）")
    class Manage {

        @Test
        @DisplayName("普通用户更新他人资料 → 403")
        void updateForbidden() throws Exception {
            String victim = newNormalUser("um_victim1");
            Cookie user = login(newNormalUser("um_normal1"));
            var res = postJson("/api/updateUserinfo",
                    Map.of("userId", userIdOf(victim), "userName", "越权改名"), user);
            assertThat(res.getResponse().getStatus()).isEqualTo(403);
        }

        @Test
        @DisplayName("管理员更新用户资料 → 200，昵称落库、响应不含口令密文")
        void updateOk() throws Exception {
            String email = newNormalUser("um_target");
            int id = userIdOf(email);
            Cookie admin = login(newAdmin("um_admin1"));

            MvcResult res = postJson("/api/updateUserinfo",
                    Map.of("userId", id, "userName", "管理员改的名字", "status", STATUS_NORMAL), admin);
            assertThat(res.getResponse().getStatus()).isEqualTo(200);

            assertThat(jdbc.queryForObject("select userName from users where userId = ?", String.class, id))
                    .isEqualTo("管理员改的名字");
            assertThat(body(res).get("data").get("password").isNull())
                    .as("更新响应不得回传口令密文").isTrue();
        }

        @Test
        @DisplayName("管理员把邮箱改成他人已占用的 → 400（先查重，而不是撞唯一键报 500）")
        void updateEmailConflict() throws Exception {
            String a = newNormalUser("um_email_a");
            String b = newNormalUser("um_email_b");
            Cookie admin = login(newAdmin("um_admin2"));

            var res = postJson("/api/updateUserinfo",
                    Map.of("userId", userIdOf(a), "email", b), admin);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
            assertThat(body(res).get("msg").asText()).contains("邮箱已被其他用户使用");
        }

        @Test
        @DisplayName("非法的状态值 → 400（状态列是原始 int，必须白名单校验）")
        void updateInvalidStatus() throws Exception {
            String email = newNormalUser("um_bad_status");
            Cookie admin = login(newAdmin("um_admin3"));
            var res = postJson("/api/updateUserinfo",
                    Map.of("userId", userIdOf(email), "status", 9), admin);
            assertThat(res.getResponse().getStatus()).isEqualTo(400);
            assertThat(body(res).get("msg").asText()).contains("状态只能为");
        }

        @Test
        @DisplayName("管理员设置口令时会被加盐哈希后落库（绝不存明文）")
        void updatePasswordHashed() throws Exception {
            String email = newNormalUser("um_pwd_target");
            int id = userIdOf(email);
            Cookie admin = login(newAdmin("um_admin4"));

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", id);
            payload.put("password", "NewPass@123");
            assertThat(postJson("/api/updateUserinfo", payload, admin).getResponse().getStatus()).isEqualTo(200);

            String stored = jdbc.queryForObject("select password from users where userId = ?", String.class, id);
            assertThat(stored).contains("$").isNotEqualTo("NewPass@123");
        }

        @Test
        @DisplayName("普通用户删除用户 → 403")
        void deleteForbidden() throws Exception {
            String victim = newNormalUser("um_victim2");
            Cookie user = login(newNormalUser("um_normal2"));
            mvc.perform(post("/api/deleteUser").cookie(user).param("userid", String.valueOf(userIdOf(victim))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("管理员删除用户 → 软删（status→2 停用），用户行仍在")
        void deleteSoft() throws Exception {
            String email = newNormalUser("um_del_target");
            Cookie admin = login(newAdmin("um_admin5"));
            mvc.perform(post("/api/deleteUser").cookie(admin).param("userid", String.valueOf(userIdOf(email))))
                    .andExpect(status().isOk());
            assertThat(userStatus(email)).isEqualTo(STATUS_DISABLED);

            // 停用后无法再登录：HTTP 200 但业务码为 403（"账户停用"）
            MvcResult relogin = mvc.perform(post("/api/admin/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "password", RAW_PWD))))
                    .andExpect(status().isOk()).andReturn();
            assertThat(body(relogin).get("code").asInt()).isEqualTo(403);
        }
    }
}

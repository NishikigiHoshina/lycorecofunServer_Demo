package com.lycorisfun.fun.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 认证接口测试：注册 / 登录 / 登出 / 未授权拦截。 */
@DisplayName("认证接口")
class AuthApiTest extends ApiTestBase {

    private MvcResult register(String email, String userName) throws Exception {
        return mvc.perform(post("/api/admin/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", RAW_PWD, "userName", userName))))
                .andReturn();
    }

    @Test
    @DisplayName("注册成功：返回 code=200，且库中落库、默认状态为 1（普通用户）")
    void registerOk() throws Exception {
        String email = "reg_ok@test.local";
        var res = register(email, "注册用户");
        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(res).get("code").asInt()).isEqualTo(200);
        assertThat(userStatus(email)).isEqualTo(STATUS_NORMAL);
        // 口令必须是加盐密文，不能是明文
        String stored = columnOf("users", "password", "email", email);
        assertThat(stored).contains("$").isNotEqualTo(RAW_PWD);
    }

    @Test
    @DisplayName("重复邮箱注册：返回 code=400 且提示已存在")
    void registerDuplicate() throws Exception {
        String email = "reg_dup@test.local";
        register(email, "第一个");
        var res = register(email, "第二个");
        assertThat(body(res).get("code").asInt()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("已存在");
    }

    @Test
    @DisplayName("注册用户名含 emoji：被 TextValidator 拦下（库为 utf8mb3）")
    void registerEmojiName() throws Exception {
        var res = register("reg_emoji@test.local", "emoji😀名字");
        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("特殊字符");
    }

    @Test
    @DisplayName("登录成功：下发 HttpOnly token cookie，并返回非机密画像")
    void loginOk() throws Exception {
        String email = newNormalUser("login_ok");
        MvcResult res = mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", RAW_PWD))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andReturn();

        var b = body(res);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("userId").asInt()).isEqualTo(userIdOf(email));
        assertThat(b.get("status").asInt()).isEqualTo(STATUS_NORMAL);
        assertThat(res.getResponse().getCookie("lycorecofun_token")).isNotNull();
    }

    @Test
    @DisplayName("登录失败：密码错误 → HTTP 400")
    void loginWrongPassword() throws Exception {
        String email = newNormalUser("login_bad_pwd");
        mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("停用账号登录：HTTP 200 但业务码 403（走的是正常返回分支，不是异常）")
    void loginDisabledAccount() throws Exception {
        String email = newUser("login_disabled", STATUS_DISABLED);
        MvcResult res = mvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", RAW_PWD))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(body(res).get("code").asInt()).isEqualTo(403);
        assertThat(body(res).get("msg").asText()).contains("停用");
        // 关键：被停用的账号不应拿到 token cookie
        assertThat(res.getResponse().getCookie("lycorecofun_token")).isNull();
    }

    @Test
    @DisplayName("未登录访问受保护接口 → HTTP 401")
    void protectedWithoutToken() throws Exception {
        mvc.perform(post("/api/writepost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "x", "doc", "{}"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("伪造 token → HTTP 401")
    void protectedWithForgedToken() throws Exception {
        mvc.perform(post("/api/getMyProfile")
                        .cookie(new Cookie("lycorecofun_token", "not-a-real-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登出：清空 token cookie（Max-Age=0）")
    void logoutClearsCookie() throws Exception {
        String email = newNormalUser("logout_user");
        Cookie cookie = login(email);
        mvc.perform(post("/api/logout").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
}

package com.lycorisfun.fun.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 统一异常处理测试：确认业务异常码会映射到对应的 HTTP 状态码，
 * 未匹配的接口返回 404，非法参数不会漏成 200。
 *
 * <p>前端同时依赖两层：HTTP 状态码（axios 捕获错误用）与响应体里的 {@code code}。</p>
 */
@DisplayName("异常与状态码映射")
class ExceptionMappingTest extends ApiTestBase {

    @Test
    @DisplayName("未匹配的接口路径 → HTTP 404，body.code=404")
    void unknownEndpoint() throws Exception {
        MvcResult res = mvc.perform(post("/api/this-endpoint-does-not-exist"))
                .andExpect(status().isNotFound()).andReturn();
        assertThat(body(res).get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("业务异常 400 → HTTP 400（举例：发帖正文为空）")
    void business400() throws Exception {
        Cookie cookie = login(newNormalUser("exc_400"));
        MvcResult res = mvc.perform(post("/api/writepost").cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "空正文", "doc", "{\"v\":1,\"nodes\":[]}"))))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(body(res).get("code").asInt()).isEqualTo(400);
        assertThat(body(res).get("msg").asText()).contains("正文不能为空");
    }

    @Test
    @DisplayName("业务异常 401 → HTTP 401（举例：未登录调用受保护接口）")
    void business401() throws Exception {
        mvc.perform(post("/api/getMyProfile")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("业务异常 403 → HTTP 403（举例：越权修改他人帖子）")
    void business403() throws Exception {
        Cookie owner = login(newNormalUser("exc_owner"));
        String title = "越权用例-403";
        mvc.perform(post("/api/writepost").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title,
                                "doc", "{\"v\":1,\"nodes\":[{\"t\":\"p\",\"c\":[\"内容\"]}]}"))))
                .andExpect(status().isOk());
        int postid = lastPostIdByTitle(title);

        Cookie stranger = login(newNormalUser("exc_stranger"));
        MvcResult res = mvc.perform(post("/api/updatePostinfo").cookie(stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("postid", postid, "title", "越权"))))
                .andExpect(status().isForbidden()).andReturn();
        assertThat(body(res).get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("业务异常 404 → HTTP 404（举例：查询不存在的帖子）")
    void business404() throws Exception {
        MvcResult res = mvc.perform(post("/api/getPostByid").param("postid", "99999999"))
                .andExpect(status().isNotFound()).andReturn();
        assertThat(body(res).get("code").asInt()).isEqualTo(404);
    }

    /**
     * 【已知问题 · 未修】缺少必填参数会落到兜底异常分支，返回 500 而不是 400。
     * 理想行为是 400（这是客户端请求错误，不是服务端故障）。此处记录当前真实行为。
     */
    @Test
    @DisplayName("【已知问题】缺少必填参数 → 当前返回 5xx（理想应为 400）")
    void missingRequiredParam() throws Exception {
        mvc.perform(post("/api/getReply"))                  // 缺少必填的 parent_id
                .andExpect(status().is5xxServerError());
    }

    /**
     * 【已知问题 · 未修】参数类型不匹配同样落到兜底分支返回 500。
     */
    @Test
    @DisplayName("【已知问题】参数类型不匹配 → 当前返回 5xx（理想应为 400）")
    void wrongParamType() throws Exception {
        mvc.perform(post("/api/getPostByid").param("postid", "不是数字"))
                .andExpect(status().is5xxServerError());
    }

    /**
     * 【已知问题 · 未修】HTTP 方法不匹配（用 GET 调仅支持 POST 的接口）会被兜底异常分支
     * 吞成 500，而理想行为是 405 / 404。此处记录当前真实行为。
     */
    @Test
    @DisplayName("【已知问题】HTTP 方法不匹配 → 当前返回 5xx（理想应为 405/404）")
    void wrongHttpMethod() throws Exception {
        int status = mvc.perform(get("/api/writepost")).andReturn().getResponse().getStatus();
        assertThat(status).isGreaterThanOrEqualTo(500);
    }
}

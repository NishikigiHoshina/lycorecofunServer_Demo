package com.lycorisfun.fun.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lycorisfun.fun.Controller.AuthController;
import com.lycorisfun.fun.util.Md5SaltUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口集成测试基类。
 *
 * <p>三个关键约定：</p>
 * <ol>
 *   <li><b>事务回滚，不污染数据</b>：类级 {@code @Transactional} + MockMvc 同线程执行，
 *       因此控制器里的写操作会加入同一个测试事务，方法结束后统一回滚 ——
 *       用例可以放心地注册用户、发帖、删帖。（注意：换成 TestRestTemplate 走真实 HTTP
 *       就跑在别的线程上，回滚会失效。）</li>
 *   <li><b>上传目录改到 target/ 下</b>：{@code lycorisfun.upload.root=target/test-upload}。
 *       事务回滚管不到磁盘文件，若不改就会往开发者真实的上传目录里扔测试图片。</li>
 *   <li><b>每个用例前清空缓存</b>：Caffeine 缓存不随事务回滚，跨用例残留会造成偶发失败。</li>
 * </ol>
 *
 * <p>请求路径注意：MockMvc 直接打到 DispatcherServlet，<b>不含</b>
 * {@code server.servlet.context-path}（即写 {@code /api/xxx} 而非 {@code /lycorisfunServer/api/xxx}）。</p>
 */
@SpringBootTest(properties = {
        "lycorisfun.upload.root=target/test-upload",
        // 静音 MyBatis 的 StdOut SQL 日志：否则每个用例都刷几十行 SQL，测试输出完全没法看
        "mybatis.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
@AutoConfigureMockMvc
@Transactional
abstract class ApiTestBase {

    /** 所有测试账号统一口令，便于造数据 */
    protected static final String RAW_PWD = "Test@123456";

    protected static final int STATUS_NORMAL = 1;
    protected static final int STATUS_DISABLED = 2;
    protected static final int STATUS_ADMIN = 3;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    /* ==================== 造数据 ==================== */

    /**
     * 直接落库造一个用户，返回其邮箱（作为登录账号）。
     * 用 JdbcTemplate 而非走注册接口，是为了能精确控制 status（普通/停用/管理员）。
     */
    protected String newUser(String tag, int status) {
        String email = tag + "@test.local";
        jdbc.update("insert into users(userName,email,password,status,registerTime) values (?,?,?,?,now())",
                tag, email, Md5SaltUtil.encrypt(RAW_PWD), status);
        return email;
    }

    protected String newNormalUser(String tag) {
        return newUser(tag, STATUS_NORMAL);
    }

    protected String newAdmin(String tag) {
        return newUser(tag, STATUS_ADMIN);
    }

    protected int userIdOf(String email) {
        return jdbc.queryForObject("select userId from users where email = ?", Integer.class, email);
    }

    /* ==================== 登录 / 认证 ==================== */

    /** 登录并返回 token cookie（后续请求用它携带登录态） */
    protected Cookie login(String email) throws Exception {
        return login(email, RAW_PWD);
    }

    protected Cookie login(String email, String rawPassword) throws Exception {
        MvcResult res = mvc.perform(post("/api/admin/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", rawPassword))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = res.getResponse().getCookie(AuthController.TOKEN_COOKIE);
        assertThat(cookie).as("登录成功应下发 %s cookie", AuthController.TOKEN_COOKIE).isNotNull();
        return cookie;
    }

    /* ==================== 响应读取 ==================== */

    /** 按 UTF-8 读响应体并解析成 JSON（不指定编码时中文会乱码） */
    protected JsonNode body(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /* ==================== 数据库断言辅助 ==================== */

    /** 写入时由 useGeneratedKeys 回填，这里按标题反查（接口不返回 postid） */
    protected int lastPostIdByTitle(String title) {
        Integer id = jdbc.queryForObject("select max(postid) from posts where title = ?", Integer.class, title);
        assertThat(id).as("应能查到刚创建的帖子").isNotNull();
        return id;
    }

    protected Map<String, Object> postRow(int postid) {
        return jdbc.queryForMap("select * from posts where postid = ?", postid);
    }

    /** 帖子的结构化正文（无则返回 null） */
    protected String docOf(int postid) {
        var list = jdbc.queryForList("select doc from post_bodies where postid = ?", String.class, postid);
        return list.isEmpty() ? null : list.get(0);
    }

    protected int bodyCount(int postid) {
        Integer n = jdbc.queryForObject("select count(*) from post_bodies where postid = ?", Integer.class, postid);
        return n == null ? 0 : n;
    }

    protected Integer userStatus(String email) {
        return jdbc.queryForObject("select status from users where email = ?", Integer.class, email);
    }

    protected String columnOf(String table, String column, String whereColumn, Object whereValue) {
        var list = jdbc.queryForList(
                "select " + column + " from " + table + " where " + whereColumn + " = ?", String.class, whereValue);
        return list.isEmpty() ? null : list.get(0);
    }
}

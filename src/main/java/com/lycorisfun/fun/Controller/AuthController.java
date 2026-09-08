package com.lycorisfun.fun.Controller;
import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Service.UserService;
import com.lycorisfun.fun.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController                 // ★ 1. 让 Spring 接管
@RequestMapping("/api")        // ★ 2. 统一前缀（可选）
public class AuthController {

    /** 登录态 Cookie 名（HttpOnly，前端 JS 不可读；与 TokenInterceptor 一致） */
    public static final String TOKEN_COOKIE = "lycorecofun_token";

    @Autowired
    private UserService userService;

    @PostMapping("/admin/login")
    public Map<String, Object> login(@RequestBody User user, HttpServletResponse response) {

        Map<String, Object> map = new HashMap<>();

        User u = userService.Login(user.getEmail(), user.getPassword());

        if (u != null) {
            Map<String, String> payload = new HashMap<>();
            if (u.getStatus() == 2) {
                map.put("code", 403);
                map.put("msg", "账户停用");
                return map;
            }
            payload.put("id", u.getUserId().toString());
            payload.put("username", u.getUserName());
            String token = JWTUtil.createToken(payload);

            // 登录态写入 HttpOnly Cookie（方案 B）
            ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE, token)
                    .httpOnly(true)
                    .secure(false)                 // 本地 http 调试；生产改配置开启
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofDays(7))    // 与 JWT 有效期一致
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            map.put("code", 200);
            map.put("msg", "登录成功");
            map.put("userId", u.getUserId());
            map.put("username", u.getUserName());
            map.put("avater", u.getAvaterURL());
            map.put("status", u.getStatus());
        }
        else {
            map.put("code", 400);
            map.put("msg", "登录失败");
        }
        return map;
    }

    // 退出：清空 HttpOnly token Cookie
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "已退出登录");
        return map;
    }

    @PostMapping("/admin/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> map = new HashMap<>();
        if(user!=null){
            User u=new User();
            try {
                u=userService.findByEmail(user.getEmail());
                map.put("code", 400);
                map.put("msg","注册失败,用户已存在");
                return map;
                //throw new BusinessException(405,"用户已注册！");
            }catch (Exception e){
                System.out.println(e);
                userService.add(user);
                map.put("code", 200);
                map.put("msg","注册成功,请前往登录");
                return map;
            }

        }
        else
        {
            map.put("code", 400);
            map.put("msg","注册失败,信息为空");
        }
        return map;
    }
}

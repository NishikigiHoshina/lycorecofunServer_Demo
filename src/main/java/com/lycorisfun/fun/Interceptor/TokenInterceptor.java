package com.lycorisfun.fun.Interceptor;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.lycorisfun.fun.Annotation.RequireToken;
import com.lycorisfun.fun.Controller.AuthController;
import com.lycorisfun.fun.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        /* 只处理方法级别的控制器 */
        if (!(handler instanceof HandlerMethod hm)) return true;

        boolean needToken = hm.getMethod().isAnnotationPresent(RequireToken.class);

        String token = resolveToken(request);

        if (needToken) {                     // ===== 必须登录 =====
            if (token == null) {
                return refuse(response, "Missing Token");
            }
            try {
                DecodedJWT jwt = JWTUtil.verifyToken(token);   // 复用你的工具类
                // 把 userId 放到 request 属性，后面业务直接拿
                request.setAttribute("userId", jwt.getClaim("id").asString());
            } catch (Exception e) {          // 签名/过期都算失败
                return refuse(response, "Invalid Token");
            }
        } else {                              // ===== 可选登录 =====
            if (token != null) {              // 有 token 就解析，没有就放过
                try {
                    DecodedJWT jwt = JWTUtil.verifyToken(token);
                    request.setAttribute("userId", jwt.getClaim("id").asString());
                } catch (Exception ignore) { /* 解析失败当游客 */ }
            }
        }
        return true;
    }

    /* 取 token：HttpOnly Cookie 优先，Authorization: Bearer 兜底（便于 curl 调试） */
    private String resolveToken(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : req.getCookies()) {
                if (AuthController.TOKEN_COOKIE.equals(c.getName())
                        && c.getValue() != null && !c.getValue().isEmpty()) {
                    return c.getValue();
                }
            }
        }
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /* 统一返回 401 */
    private boolean refuse(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\"}");
        return false;
    }
}
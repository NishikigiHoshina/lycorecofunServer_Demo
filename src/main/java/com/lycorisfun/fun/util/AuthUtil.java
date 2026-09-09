package com.lycorisfun.fun.util;

import com.lycorisfun.fun.Entity.User;
import com.lycorisfun.fun.Exception.BusinessException;
import com.lycorisfun.fun.Interceptor.TokenInterceptor;
import com.lycorisfun.fun.Mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 后端鉴权辅助：把"当前登录用户是否管理员(status==3)"收敛到一处，
 * DB 判定，不信任前端传来的 status。供后台系统级写/删接口统一使用。
 */
public final class AuthUtil {

    private AuthUtil() {
    }

    /**
     * 要求当前登录用户是管理员；通过返回其 userId，否则抛 401/403。
     */
    public static int requireAdmin(HttpServletRequest request, UserMapper userMapper) {
        Integer uid = TokenInterceptor.currentUserId(request);
        if (uid == null) {
            throw new BusinessException(401, "未登录");
        }
        User u = userMapper.findById(uid);
        if (u == null || u.getStatus() != 3) {
            throw new BusinessException(403, "无权操作：仅管理员可进行此操作");
        }
        return uid;
    }
}

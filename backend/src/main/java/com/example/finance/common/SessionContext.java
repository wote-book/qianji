package com.example.finance.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 会话上下文：所有 user_id / role 一律从这里取，绝不信任前端传参。
 */
public final class SessionContext {

    public static final String KEY_USER_ID = "USER_ID";
    public static final String KEY_USERNAME = "USERNAME";
    public static final String KEY_NICKNAME = "NICKNAME";
    public static final String KEY_ROLE = "ROLE";
    public static final String KEY_ONBOARDED = "ONBOARDED";

    private SessionContext() {
    }

    public static HttpSession session() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw BizException.unauthorized("未登录");
        }
        return attrs.getRequest().getSession(true);
    }

    public static HttpServletRequest request() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw BizException.unauthorized("未登录");
        }
        return attrs.getRequest();
    }

    public static Long userId() {
        Object value = session().getAttribute(KEY_USER_ID);
        if (value == null) {
            throw BizException.unauthorized("未登录或会话已过期");
        }
        return (Long) value;
    }

    public static String role() {
        Object value = session().getAttribute(KEY_ROLE);
        return value == null ? "USER" : value.toString();
    }

    public static String nickname() {
        Object value = session().getAttribute(KEY_NICKNAME);
        return value == null ? "" : value.toString();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }
}

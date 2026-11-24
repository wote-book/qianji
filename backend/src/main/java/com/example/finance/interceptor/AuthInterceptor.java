package com.example.finance.interceptor;

import com.example.finance.common.SessionContext;
import com.example.finance.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SessionContext.KEY_USER_ID);
        if (userId == null) {
            write(response, 401, "未登录或会话已过期");
            return false;
        }

        Integer status = userMapper.selectStatusById(Long.valueOf(userId.toString()));
        if (status == null) {
            session.invalidate();
            write(response, 401, "用户不存在，请重新登录");
            return false;
        }
        if (status == 0) {
            session.invalidate();
            write(response, 401, "账号已被禁用，请联系管理员");
            return false;
        }

        Object role = session.getAttribute(SessionContext.KEY_ROLE);
        String uri = request.getRequestURI();
        if ("ADMIN".equals(role) && uri.startsWith("/api/")
                && !uri.startsWith("/api/admin/") && !uri.startsWith("/api/auth/")) {
            write(response, 403, "管理员账号仅可访问管理控制台接口");
            return false;
        }
        return true;
    }

    public static void write(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}

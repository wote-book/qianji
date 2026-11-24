package com.example.finance.interceptor;

import com.example.finance.common.SessionContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession(false);
        Object role = session == null ? null : session.getAttribute(SessionContext.KEY_ROLE);
        if (role == null || !"ADMIN".equals(role.toString())) {
            AuthInterceptor.write(response, 403, "无管理员权限");
            return false;
        }
        return true;
    }
}

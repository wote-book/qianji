package com.example.finance.interceptor;

import com.example.finance.admin.entity.UsageEvent;
import com.example.finance.admin.mapper.UsageEventMapper;
import com.example.finance.common.RequestUtils;
import com.example.finance.common.SessionContext;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 统一埋点：按请求路径映射模块，异步写入 usage_event，失败不影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageEventInterceptor implements HandlerInterceptor {

    private final UsageEventMapper usageEventMapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "usage-event-writer");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (response.getStatus() != 200) {
            return;
        }
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SessionContext.KEY_USER_ID);
        if (userId == null) {
            return;
        }
        String uri = request.getRequestURI();
        String module = resolveModule(uri);
        if (module == null) {
            return;
        }

        UsageEvent event = new UsageEvent();
        event.setUserId(Long.valueOf(userId.toString()));
        event.setModule(module);
        event.setAction(resolveAction(request.getMethod(), uri, module));
        event.setRefId(resolveRefId(uri));
        event.setIp(RequestUtils.clientIp(request));

        executor.submit(() -> {
            try {
                usageEventMapper.insert(event);
            } catch (Exception e) {
                log.warn("写入使用事件失败：{}", e.getMessage());
            }
        });
    }

    private String resolveModule(String uri) {
        if (uri.startsWith("/api/ai/records/commit")) {
            return "RECORD";
        }
        if (uri.startsWith("/api/records/import")) {
            return "IMPORT";
        }
        if (uri.startsWith("/api/records")) {
            return "RECORD";
        }
        if (uri.startsWith("/api/accounts")) {
            return "ACCOUNT";
        }
        if (uri.startsWith("/api/categories")) {
            return "CATEGORY";
        }
        if (uri.startsWith("/api/budgets")) {
            return "BUDGET";
        }
        if (uri.startsWith("/api/stats") || uri.startsWith("/api/insights")) {
            return "STATS";
        }
        if (uri.startsWith("/api/goals")) {
            return "GOAL";
        }
        if (uri.startsWith("/api/report/financial-brief")) {
            return "BRIEF";
        }
        if (uri.startsWith("/api/export")) {
            return "REPORT";
        }
        if (uri.startsWith("/api/backup")) {
            return "BACKUP";
        }
        return null;
    }

    private String resolveAction(String method, String uri, String module) {
        if ("GET".equalsIgnoreCase(method)) {
            if ("REPORT".equals(module) || "BACKUP".equals(module)
                    || uri.endsWith("/template") || uri.contains("/export")) {
                return "EXPORT";
            }
            return "VIEW";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "BRIEF".equals(module) ? "GENERATE" : "CREATE";
        }
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return "UPDATE";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "DELETE";
        }
        return "VIEW";
    }

    private Long resolveRefId(String uri) {
        String[] parts = uri.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part.matches("\\d{1,18}")) {
                return Long.valueOf(part);
            }
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}

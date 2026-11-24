package com.example.finance.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 单页应用路由回退：打包后前端位于 static/，刷新子路由时转发到 index.html。
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/register", "/onboarding", "/dashboard", "/records", "/records/import",
            "/accounts", "/categories", "/budgets", "/stats", "/goals", "/reports", "/brief",
            "/settings", "/admin", "/admin/overview", "/admin/users", "/admin/usage", "/admin/logs"})
    public String index() {
        return "forward:/index.html";
    }
}

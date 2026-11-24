package com.example.finance.admin.controller;

import com.example.finance.admin.dto.LoginLogVO;
import com.example.finance.admin.dto.UsageEventVO;
import com.example.finance.admin.service.AdminLogService;
import com.example.finance.common.PageResult;
import com.example.finance.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService logService;

    @GetMapping("/login")
    public Result<PageResult<LoginLogVO>> loginLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(logService.loginLogs(username, success, start, end, pageNum, pageSize));
    }

    @GetMapping("/events")
    public Result<PageResult<UsageEventVO>> events(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize) {
        return Result.ok(logService.usageEvents(username, module, start, end, pageNum, pageSize));
    }
}

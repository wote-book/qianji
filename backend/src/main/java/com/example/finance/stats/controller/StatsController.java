package com.example.finance.stats.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam String month) {
        return Result.ok(statsService.summary(SessionContext.userId(), month));
    }

    @GetMapping("/category")
    public Result<Map<String, Object>> category(@RequestParam String month,
                                                @RequestParam(defaultValue = "EXPENSE") String type) {
        return Result.ok(statsService.category(SessionContext.userId(), month, type));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "day") String granularity) {
        return Result.ok(statsService.trend(SessionContext.userId(), start, end, granularity));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestParam String month) {
        return Result.ok(statsService.dashboard(SessionContext.userId(), month));
    }

    @GetMapping("/daily-quota")
    public Result<Map<String, Object>> dailyQuota(@RequestParam String month) {
        return Result.ok(statsService.dailyQuota(SessionContext.userId(), month));
    }
}

package com.example.finance.insight.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.insight.service.InsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/anomalies")
    public Result<List<Map<String, Object>>> anomalies(@RequestParam String month) {
        return Result.ok(insightService.anomalies(SessionContext.userId(), month));
    }

    @GetMapping("/subscriptions")
    public Result<List<Map<String, Object>>> subscriptions() {
        return Result.ok(insightService.subscriptions(SessionContext.userId()));
    }
}

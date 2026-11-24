package com.example.finance.admin.controller;

import com.example.finance.admin.dto.ModuleUsage;
import com.example.finance.admin.dto.RetentionCohort;
import com.example.finance.admin.dto.SegmentItem;
import com.example.finance.admin.dto.UsageTrendVO;
import com.example.finance.admin.service.AdminStatsService;
import com.example.finance.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usage")
@RequiredArgsConstructor
public class AdminUsageController {

    private final AdminStatsService statsService;

    @GetMapping("/trend")
    public Result<UsageTrendVO> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.usageTrend(days));
    }

    @GetMapping("/modules")
    public Result<List<ModuleUsage>> modules(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.modules(days));
    }

    @GetMapping("/retention")
    public Result<List<RetentionCohort>> retention() {
        return Result.ok(statsService.retention());
    }

    @GetMapping("/segments")
    public Result<List<SegmentItem>> segments() {
        return Result.ok(statsService.segments());
    }
}

package com.example.finance.admin.controller;

import com.example.finance.admin.dto.OverviewVO;
import com.example.finance.admin.service.AdminStatsService;
import com.example.finance.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final AdminStatsService statsService;

    @GetMapping("/overview")
    public Result<OverviewVO> overview(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.overview(days));
    }
}

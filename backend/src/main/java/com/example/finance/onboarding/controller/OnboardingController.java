package com.example.finance.onboarding.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.onboarding.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(onboardingService.status(SessionContext.userId()));
    }

    @PostMapping("/complete")
    public Result<Void> complete() {
        onboardingService.complete();
        return Result.ok();
    }
}

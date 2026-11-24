package com.example.finance.brief.controller;

import com.example.finance.brief.service.BriefService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class BriefController {

    private final BriefService briefService;

    @PostMapping("/financial-brief")
    public Result<Map<String, Object>> generate(@RequestBody(required = false) Map<String, String> body) {
        String month = body == null ? null : body.get("month");
        if (!StringUtils.hasText(month)) {
            month = YearMonth.now().toString();
        }
        return Result.ok(briefService.generate(SessionContext.userId(), month));
    }
}

package com.example.finance.ai.controller;

import com.example.finance.ai.dto.AiCommitRequest;
import com.example.finance.ai.dto.AiParseRequest;
import com.example.finance.ai.dto.AiParseResult;
import com.example.finance.ai.service.AiRecordService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/records")
@RequiredArgsConstructor
public class AiRecordController {

    private final AiRecordService aiRecordService;

    @PostMapping("/parse")
    public Result<AiParseResult> parse(@Valid @RequestBody AiParseRequest request) {
        return Result.ok(aiRecordService.parse(SessionContext.userId(), request));
    }

    @PostMapping("/commit")
    public Result<Map<String, Object>> commit(@Valid @RequestBody AiCommitRequest request) {
        int count = aiRecordService.commit(SessionContext.userId(), request);
        return Result.ok(Map.of("count", count));
    }
}

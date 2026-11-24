package com.example.finance.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiCommitRequest(
        @NotEmpty(message = "请至少保留一笔记录")
        @Size(max = 10, message = "单次最多入账 10 笔")
        List<@Valid AiCommitItem> items
) {
}

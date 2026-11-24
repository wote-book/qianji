package com.example.finance.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiParseRequest(
        @NotBlank(message = "请描述这笔收支")
        @Size(max = 500, message = "最多输入 500 个字符")
        String text,

        Long defaultAccountId,

        String defaultDate
) {
}

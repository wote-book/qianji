package com.example.finance.ai.dto;

import java.util.List;

public record AiParseResult(
        String source,
        boolean degraded,
        String message,
        List<AiParseItem> items
) {
}

package com.example.finance.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiParseItem(
        String type,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        Long accountId,
        String accountName,
        LocalDate recordDate,
        String remark,
        String warn
) {
}

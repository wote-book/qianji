package com.example.finance.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiCommitItem(
        @NotNull(message = "请选择账户") Long accountId,
        @NotNull(message = "请选择分类") Long categoryId,
        @NotNull(message = "金额不能为空")
        @DecimalMin(value = "0.01", message = "金额必须大于 0")
        @DecimalMax(value = "1000000", message = "金额超出范围")
        BigDecimal amount,
        @NotNull(message = "类型不能为空")
        @Pattern(regexp = "INCOME|EXPENSE", message = "类型只能是 INCOME 或 EXPENSE")
        String type,
        @NotNull(message = "日期不能为空") LocalDate recordDate,
        @Size(max = 200, message = "备注最多 200 个字符") String remark
) {
}

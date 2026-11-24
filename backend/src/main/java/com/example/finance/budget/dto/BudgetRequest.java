package com.example.finance.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank(message = "月份不能为空")
        @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "月份格式应为 yyyy-MM")
        String month,

        /** 0 表示月度总预算 */
        @NotNull(message = "分类不能为空") Long categoryId,

        @NotNull(message = "预算金额不能为空")
        @DecimalMin(value = "0", message = "预算金额不能为负")
        BigDecimal limitAmt,

        @Min(value = 1, message = "阈值应在 1~100 之间")
        @Max(value = 100, message = "阈值应在 1~100 之间")
        Integer warnThreshold
) {
}

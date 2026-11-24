package com.example.finance.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank(message = "目标名称不能为空")
        @Size(max = 50, message = "目标名称最多 50 个字符")
        String name,

        @NotNull(message = "目标金额不能为空")
        @DecimalMin(value = "0.01", message = "目标金额必须大于 0")
        BigDecimal targetAmt,

        LocalDate startDate
) {
}

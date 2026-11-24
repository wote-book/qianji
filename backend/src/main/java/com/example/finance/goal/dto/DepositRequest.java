package com.example.finance.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "存入金额不能为空")
        @DecimalMin(value = "0.01", message = "存入金额必须大于 0")
        BigDecimal amount
) {
}

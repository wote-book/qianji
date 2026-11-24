package com.example.finance.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "账户名称不能为空")
        @Size(max = 50, message = "账户名称最多 50 个字符")
        String accName,

        @NotBlank(message = "账户类型不能为空")
        @Pattern(regexp = "CASH|BANK|ALIPAY|WECHAT|OTHER", message = "账户类型不合法")
        String type,

        BigDecimal initBalance,

        Boolean allowOverdraft
) {
}

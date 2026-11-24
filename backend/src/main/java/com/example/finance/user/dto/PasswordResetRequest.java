package com.example.finance.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "安全问题答案不能为空") String securityAnswer,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6~32 位") String newPassword
) {
}

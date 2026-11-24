package com.example.finance.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名为 4~20 位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度需在 6~32 位")
        String password,

        @Size(max = 50, message = "昵称最多 50 个字符")
        String nickname,

        @Size(max = 100, message = "安全问题最多 100 个字符")
        String securityQuestion,

        @Size(max = 100, message = "安全问题答案最多 100 个字符")
        String securityAnswer
) {
}

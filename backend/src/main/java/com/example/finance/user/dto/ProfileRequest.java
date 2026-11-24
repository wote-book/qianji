package com.example.finance.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称最多 50 个字符")
        String nickname
) {
}

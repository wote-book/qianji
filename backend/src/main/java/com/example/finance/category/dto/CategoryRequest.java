package com.example.finance.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 30, message = "分类名称最多 30 个字符")
        String name,

        @NotBlank(message = "分类类型不能为空")
        @Pattern(regexp = "INCOME|EXPENSE", message = "分类类型只能是 INCOME 或 EXPENSE")
        String type,

        @Size(max = 50, message = "图标标识最多 50 个字符")
        String icon
) {
}

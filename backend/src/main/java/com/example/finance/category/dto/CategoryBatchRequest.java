package com.example.finance.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategoryBatchRequest(
        @NotBlank(message = "分类类型不能为空")
        @Pattern(regexp = "INCOME|EXPENSE", message = "分类类型只能是 INCOME 或 EXPENSE")
        String type,

        @NotEmpty(message = "请选择要添加的分类")
        @Size(max = 50, message = "单次最多添加 50 个分类")
        List<@NotBlank(message = "分类名称不能为空")
             @Size(max = 30, message = "分类名称最多 30 个字符") String> names
) {
}

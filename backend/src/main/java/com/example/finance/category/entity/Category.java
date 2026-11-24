package com.example.finance.category.entity;

import lombok.Data;

@Data
public class Category {
    private Long id;
    private Long userId;
    private String name;
    private String type;
    private String icon;
    private Integer isPreset;
    private Integer status;
    /** 关联流水条数，删除校验时使用 */
    private Long recordCount;
}

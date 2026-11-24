package com.example.finance.budget.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Budget {
    private Long id;
    private Long userId;
    private String month;
    private Long categoryId;
    private BigDecimal limitAmt;
    private Integer warnThreshold;
    private LocalDateTime updateTime;

    /** 展示与计算字段 */
    private String categoryName;
    private BigDecimal usedAmt;
    private BigDecimal ratio;
    private String level;
}

package com.example.finance.goal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Goal {
    private Long id;
    private Long userId;
    private String name;
    private BigDecimal targetAmt;
    private BigDecimal savedAmt;
    private LocalDate startDate;
    private String status;
    private LocalDateTime createTime;

    /** 计算字段 */
    private BigDecimal percent;
    private LocalDate expectedDate;
    private Long remainDays;
}

package com.example.finance.record.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Record {
    private Long id;
    private Long userId;
    private Long accountId;
    private Long categoryId;
    private BigDecimal amount;
    private String type;
    private LocalDate recordDate;
    private String remark;
    private String source;
    private String dedupKey;
    private LocalDateTime createTime;

    /** 展示字段（联表查询） */
    private String accountName;
    private String categoryName;
}

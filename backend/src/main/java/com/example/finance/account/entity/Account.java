package com.example.finance.account.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Account {
    private Long id;
    private Long userId;
    private String accName;
    private String type;
    private BigDecimal initBalance;
    private BigDecimal balance;
    private Integer allowOverdraft;
    private LocalDateTime createTime;
}

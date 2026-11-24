package com.example.finance.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountDiff {
    private Long id;
    private String accName;
    private BigDecimal initBalance;
    private BigDecimal balance;
    private BigDecimal calc;
    private BigDecimal diff;
}

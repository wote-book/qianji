package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDetailVO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private long registerDays;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private long accountCount;
    private long recordCount;
    private long categoryCount;
    private long budgetCount;
    private long goalCount;
    private long briefCount;
    private long activeDays30;
    private String lastModule;
}

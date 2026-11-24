package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserRowVO {
    private Long id;
    private String username;
    private String nickname;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private long activeDays30;
    private long recordCount;
}

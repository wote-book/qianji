package com.example.finance.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLog {
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private LocalDateTime createTime;
}

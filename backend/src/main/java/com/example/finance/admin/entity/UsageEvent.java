package com.example.finance.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsageEvent {
    private Long id;
    private Long userId;
    private String module;
    private String action;
    private Long refId;
    private String ip;
    private LocalDateTime createTime;
}

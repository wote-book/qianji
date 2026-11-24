package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsageEventVO {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private Long refId;
    private String ip;
    private LocalDateTime createTime;
}

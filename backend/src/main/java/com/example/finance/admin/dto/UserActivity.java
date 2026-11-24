package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserActivity {
    private Long userId;
    private LocalDateTime createTime;
    private LocalDate lastActiveDay;
    private long days7;
}

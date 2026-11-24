package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ActiveUserDay {
    private Long userId;
    private LocalDate day;
}

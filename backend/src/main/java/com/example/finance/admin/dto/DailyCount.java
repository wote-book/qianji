package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyCount {
    private LocalDate date;
    private long value;
}

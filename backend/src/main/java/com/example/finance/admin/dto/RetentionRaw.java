package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RetentionRaw {
    private LocalDate cohortDate;
    private long size;
    private long d1;
    private long d7;
    private long d30;
}

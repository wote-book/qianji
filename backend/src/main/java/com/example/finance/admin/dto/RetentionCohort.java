package com.example.finance.admin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RetentionCohort {
    private LocalDate cohortDate;
    private long size;
    private Double d1;
    private Double d7;
    private Double d30;
}

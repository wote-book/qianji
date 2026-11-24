package com.example.finance.admin.dto;

import lombok.Data;

@Data
public class ModuleUsage {
    private String module;
    private String label;
    private long users;
    private long events;
    private double rate;
}

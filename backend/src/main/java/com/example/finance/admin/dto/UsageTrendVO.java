package com.example.finance.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UsageTrendVO {
    private List<String> labels;
    private List<Long> dau;
    private List<Long> wau;
    private List<Long> mau;
}

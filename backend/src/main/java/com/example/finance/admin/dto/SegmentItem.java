package com.example.finance.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SegmentItem {
    private String key;
    private String label;
    private long count;
    private double ratio;
}

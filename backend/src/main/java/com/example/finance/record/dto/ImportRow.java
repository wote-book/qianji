package com.example.finance.record.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CSV 导入提交行（前端把预览结果纠正后回传）。
 */
@Data
public class ImportRow {
    private LocalDate date;
    private String type;
    private BigDecimal amount;
    private Long accountId;
    private Long categoryId;
    private String remark;
}

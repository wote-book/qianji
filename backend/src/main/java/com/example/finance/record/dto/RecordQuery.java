package com.example.finance.record.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class RecordQuery {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate start;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate end;
    private String type;
    private Long categoryId;
    private Long accountId;
    private String source;
    private String keyword;
    private int pageNum = 1;
    private int pageSize = 10;

    public int getOffset() {
        int page = pageNum <= 0 ? 1 : pageNum;
        int size = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        return (page - 1) * size;
    }

    public int getLimit() {
        return pageSize <= 0 ? 10 : Math.min(pageSize, 100);
    }
}

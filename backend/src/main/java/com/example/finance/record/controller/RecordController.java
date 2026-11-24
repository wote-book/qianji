package com.example.finance.record.controller;

import com.example.finance.common.PageResult;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.record.dto.RecordQuery;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.entity.Record;
import com.example.finance.record.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    public Result<PageResult<Record>> page(RecordQuery query) {
        return Result.ok(recordService.page(SessionContext.userId(), query));
    }

    @GetMapping("/{id}")
    public Result<Record> get(@PathVariable Long id) {
        return Result.ok(recordService.getOwned(SessionContext.userId(), id));
    }

    @PostMapping
    public Result<Record> create(@Valid @RequestBody RecordRequest request) {
        return Result.ok(recordService.create(SessionContext.userId(), request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RecordRequest request) {
        recordService.update(SessionContext.userId(), id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(SessionContext.userId(), id);
        return Result.ok();
    }
}

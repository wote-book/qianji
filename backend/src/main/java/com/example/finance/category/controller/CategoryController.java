package com.example.finance.category.controller;

import com.example.finance.category.dto.CategoryBatchRequest;
import com.example.finance.category.dto.CategoryRequest;
import com.example.finance.category.entity.Category;
import com.example.finance.category.service.CategoryService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> list(@RequestParam(required = false) String type) {
        return Result.ok(categoryService.list(SessionContext.userId(), type));
    }

    @PostMapping
    public Result<Category> create(@Valid @RequestBody CategoryRequest request) {
        return Result.ok(categoryService.create(SessionContext.userId(), request));
    }

    @PostMapping("/batch")
    public Result<Map<String, Object>> batchCreate(@Valid @RequestBody CategoryBatchRequest request) {
        return Result.ok(categoryService.batchCreate(SessionContext.userId(), request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        categoryService.update(SessionContext.userId(), id, request);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        categoryService.updateStatus(SessionContext.userId(), id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(SessionContext.userId(), id);
        return Result.ok();
    }
}

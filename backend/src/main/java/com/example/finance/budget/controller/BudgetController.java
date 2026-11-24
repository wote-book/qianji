package com.example.finance.budget.controller;

import com.example.finance.budget.dto.BudgetRequest;
import com.example.finance.budget.entity.Budget;
import com.example.finance.budget.service.BudgetService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public Result<List<Budget>> list(@RequestParam String month) {
        return Result.ok(budgetService.list(SessionContext.userId(), month));
    }

    @GetMapping("/execution")
    public Result<Map<String, Object>> execution(@RequestParam String month) {
        return Result.ok(budgetService.execution(SessionContext.userId(), month));
    }

    @PutMapping
    public Result<Budget> upsert(@Valid @RequestBody BudgetRequest request) {
        return Result.ok(budgetService.upsert(SessionContext.userId(), request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        budgetService.delete(SessionContext.userId(), id);
        return Result.ok();
    }
}

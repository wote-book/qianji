package com.example.finance.goal.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.goal.dto.DepositRequest;
import com.example.finance.goal.dto.GoalRequest;
import com.example.finance.goal.entity.Goal;
import com.example.finance.goal.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public Result<List<Goal>> list() {
        return Result.ok(goalService.listWithProgress(SessionContext.userId()));
    }

    @PostMapping
    public Result<Goal> create(@Valid @RequestBody GoalRequest request) {
        return Result.ok(goalService.create(SessionContext.userId(), request));
    }

    @PostMapping("/{id}/deposit")
    public Result<Goal> deposit(@PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return Result.ok(goalService.deposit(SessionContext.userId(), id, request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        goalService.update(SessionContext.userId(), id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.delete(SessionContext.userId(), id);
        return Result.ok();
    }
}

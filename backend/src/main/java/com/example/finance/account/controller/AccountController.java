package com.example.finance.account.controller;

import com.example.finance.account.dto.AccountDiff;
import com.example.finance.account.dto.AccountRequest;
import com.example.finance.account.entity.Account;
import com.example.finance.account.service.AccountService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public Result<List<Account>> list() {
        return Result.ok(accountService.list(SessionContext.userId()));
    }

    @PostMapping
    public Result<Account> create(@Valid @RequestBody AccountRequest request) {
        return Result.ok(accountService.create(SessionContext.userId(), request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        accountService.update(SessionContext.userId(), id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        accountService.delete(SessionContext.userId(), id);
        return Result.ok();
    }

    @GetMapping("/reconcile")
    public Result<List<AccountDiff>> reconcile() {
        return Result.ok(accountService.reconcile(SessionContext.userId()));
    }

    @PostMapping("/reconcile/fix")
    public Result<Map<String, Integer>> fix() {
        return Result.ok(Map.of("fixed", accountService.fix(SessionContext.userId())));
    }
}

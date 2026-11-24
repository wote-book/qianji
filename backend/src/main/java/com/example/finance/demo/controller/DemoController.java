package com.example.finance.demo.controller;

import com.example.finance.account.dto.AccountRequest;
import com.example.finance.account.entity.Account;
import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.account.service.AccountService;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.mapper.RecordMapper;
import com.example.finance.record.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示接口：验证「余额不足时流水与余额均不变」的同事务红线。
 * 仅在 app.demo.enabled=true 时注册（demo profile）。
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final RecordMapper recordMapper;
    private final RecordService recordService;

    @PostMapping("/transaction-failure")
    public Result<Map<String, Object>> transactionFailure() {
        Long userId = SessionContext.userId();
        String accName = "演示零钱-" + System.currentTimeMillis();
        Account account = accountService.create(userId,
                new AccountRequest(accName, "CASH", BigDecimal.ZERO, Boolean.FALSE));

        Category category = categoryMapper.selectByUserTypeName(userId, "EXPENSE", "餐饮");
        if (category == null) {
            throw new BizException("请先初始化分类后再演示");
        }

        BigDecimal balanceBefore = account.getBalance();
        long recordsBefore = recordMapper.countByAccount(userId, account.getId());
        String error;
        try {
            recordService.create(userId, new RecordRequest(account.getId(), category.getId(),
                    new BigDecimal("100.00"), "EXPENSE", LocalDate.now(), "演示：余额不足的支出"));
            error = "未触发失败";
        } catch (BizException e) {
            error = e.getMessage();
        }
        Account after = accountMapper.selectByIdAndUser(account.getId(), userId);
        long recordsAfter = recordMapper.countByAccount(userId, account.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountName", accName);
        result.put("message", error);
        result.put("balanceBefore", balanceBefore);
        result.put("balanceAfter", after == null ? balanceBefore : after.getBalance());
        result.put("recordsBefore", recordsBefore);
        result.put("recordsAfter", recordsAfter);
        result.put("rolledBack", balanceBefore.compareTo(after == null ? balanceBefore : after.getBalance()) == 0
                && recordsBefore == recordsAfter);
        return Result.ok(result);
    }
}

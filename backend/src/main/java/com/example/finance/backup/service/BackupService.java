package com.example.finance.backup.service;

import com.example.finance.account.entity.Account;
import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.account.service.AccountService;
import com.example.finance.backup.dto.BackupPayload;
import com.example.finance.budget.entity.Budget;
import com.example.finance.budget.mapper.BudgetMapper;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.goal.entity.Goal;
import com.example.finance.goal.mapper.GoalMapper;
import com.example.finance.record.dto.RecordQuery;
import com.example.finance.record.entity.Record;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final RecordMapper recordMapper;
    private final BudgetMapper budgetMapper;
    private final GoalMapper goalMapper;
    private final AccountService accountService;

    public BackupPayload export(Long userId) {
        BackupPayload payload = new BackupPayload();
        payload.setExportTime(LocalDateTime.now().toString());
        payload.setAccounts(accountMapper.selectByUserId(userId));
        payload.setCategories(categoryMapper.selectByUser(userId, null));
        RecordQuery query = new RecordQuery();
        payload.setRecords(recordMapper.selectForExport(query, userId));
        payload.setBudgets(budgetMapper.selectByUser(userId));
        payload.setGoals(goalMapper.selectByUser(userId));
        return payload;
    }

    /**
     * 导入：全部归属当前用户；账户按名称复用、分类按（类型,名称）复用；
     * 流水按指纹去重；余额按导入流水重算（Q-12）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importData(Long userId, BackupPayload payload) {
        if (payload == null) {
            throw new BizException("导入内容为空");
        }
        Map<String, Account> accountByName = new HashMap<>();
        Map<String, Category> categoryByKey = new HashMap<>();
        accountMapper.selectByUserId(userId).forEach(a -> accountByName.put(a.getAccName(), a));
        categoryMapper.selectByUser(userId, null)
                .forEach(c -> categoryByKey.put(c.getType() + "#" + c.getName(), c));

        int accountCreated = 0;
        int categoryCreated = 0;
        int recordInserted = 0;
        int recordSkipped = 0;
        int recordFailed = 0;
        int goalCreated = 0;
        int goalSkipped = 0;

        if (payload.getAccounts() != null) {
            for (Account source : payload.getAccounts()) {
                if (source.getAccName() == null || accountByName.containsKey(source.getAccName())) {
                    continue;
                }
                Account account = new Account();
                account.setUserId(userId);
                account.setAccName(source.getAccName());
                account.setType(source.getType() == null ? "OTHER" : source.getType());
                account.setInitBalance(source.getInitBalance() == null ? BigDecimal.ZERO : source.getInitBalance());
                account.setBalance(BigDecimal.ZERO);
                account.setAllowOverdraft(source.getAllowOverdraft() == null ? 0 : source.getAllowOverdraft());
                accountMapper.insert(account);
                accountByName.put(account.getAccName(), account);
                accountCreated++;
            }
        }

        if (payload.getCategories() != null) {
            for (Category source : payload.getCategories()) {
                String key = source.getType() + "#" + source.getName();
                if (source.getName() == null || categoryByKey.containsKey(key)) {
                    continue;
                }
                Category category = new Category();
                category.setUserId(userId);
                category.setName(source.getName());
                category.setType(source.getType());
                category.setIcon(source.getIcon());
                category.setIsPreset(0);
                category.setStatus(1);
                categoryMapper.insert(category);
                categoryByKey.put(key, category);
                categoryCreated++;
            }
        }

        if (payload.getRecords() != null) {
            for (Record source : payload.getRecords()) {
                try {
                    Account account = accountByName.get(source.getAccountName());
                    String type = source.getType();
                    Category category = categoryByKey.get(type + "#" + source.getCategoryName());
                    if (account == null || category == null || source.getAmount() == null
                            || source.getRecordDate() == null) {
                        recordFailed++;
                        continue;
                    }
                    String dedupKey = source.getDedupKey();
                    if (dedupKey == null || dedupKey.isBlank()) {
                        dedupKey = buildDedupKey(source.getRecordDate(), source.getAmount(), type,
                                source.getCategoryName(), source.getRemark());
                    }
                    if (recordMapper.selectByDedupKey(userId, dedupKey) != null) {
                        recordSkipped++;
                        continue;
                    }
                    Record record = new Record();
                    record.setUserId(userId);
                    record.setAccountId(account.getId());
                    record.setCategoryId(category.getId());
                    record.setAmount(source.getAmount());
                    record.setType(type);
                    record.setRecordDate(source.getRecordDate());
                    record.setRemark(source.getRemark());
                    record.setSource("RESTORE");
                    record.setDedupKey(dedupKey);
                    recordMapper.insert(record);
                    recordInserted++;
                } catch (Exception e) {
                    log.warn("导入流水失败", e);
                    recordFailed++;
                }
            }
        }

        if (payload.getBudgets() != null) {
            for (Budget source : payload.getBudgets()) {
                Long categoryId = 0L;
                if (source.getCategoryId() != null && source.getCategoryId() != 0) {
                    Category category = categoryByKey.get("EXPENSE#" + source.getCategoryName());
                    if (category == null) {
                        continue;
                    }
                    categoryId = category.getId();
                }
                if (budgetMapper.selectByMonthAndCategory(userId, source.getMonth(), categoryId) != null) {
                    continue;
                }
                Budget budget = new Budget();
                budget.setUserId(userId);
                budget.setMonth(source.getMonth());
                budget.setCategoryId(categoryId);
                budget.setLimitAmt(source.getLimitAmt());
                budget.setWarnThreshold(source.getWarnThreshold() == null ? 80 : source.getWarnThreshold());
                budgetMapper.upsert(budget);
            }
        }

        if (payload.getGoals() != null) {
            java.util.Set<String> goalNames = new java.util.HashSet<>();
            goalMapper.selectByUser(userId).forEach(g -> goalNames.add(g.getName()));
            for (Goal source : payload.getGoals()) {
                if (source.getName() == null || goalNames.contains(source.getName())) {
                    goalSkipped++;
                    continue;
                }
                Goal goal = new Goal();
                goal.setUserId(userId);
                goal.setName(source.getName());
                goal.setTargetAmt(source.getTargetAmt());
                goal.setSavedAmt(source.getSavedAmt() == null ? BigDecimal.ZERO : source.getSavedAmt());
                goal.setStartDate(source.getStartDate() == null ? LocalDate.now() : source.getStartDate());
                goal.setStatus(source.getStatus() == null ? "DOING" : source.getStatus());
                goalMapper.insert(goal);
                goalNames.add(goal.getName());
                goalCreated++;
            }
        }

        // 余额按导入后的流水重算，保证账实一致
        accountService.fix(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountCreated", accountCreated);
        result.put("categoryCreated", categoryCreated);
        result.put("recordInserted", recordInserted);
        result.put("recordSkipped", recordSkipped);
        result.put("recordFailed", recordFailed);
        result.put("goalCreated", goalCreated);
        result.put("goalSkipped", goalSkipped);
        return result;
    }

    private String buildDedupKey(LocalDate date, BigDecimal amount, String type, String categoryName, String remark) {
        String raw = date + "|" + amount.stripTrailingZeros().toPlainString() + "|" + type
                + "|" + (categoryName == null ? "" : categoryName)
                + "|" + (remark == null ? "" : remark);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}

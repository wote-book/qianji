package com.example.finance.budget.service;

import com.example.finance.budget.dto.BudgetRequest;
import com.example.finance.budget.entity.Budget;
import com.example.finance.budget.mapper.BudgetMapper;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetMapper budgetMapper;
    private final RecordMapper recordMapper;
    private final CategoryMapper categoryMapper;

    public List<Budget> list(Long userId, String month) {
        YearMonth ym = parseMonth(month);
        List<Budget> budgets = budgetMapper.selectByMonth(userId, month);
        BigDecimal totalExpense = toDecimal(recordMapper.sumByRange(userId, ym.atDay(1), ym.atEndOfMonth())
                .get("expense"));
        Map<Long, BigDecimal> byCategory = categoryExpense(userId, ym);
        for (Budget budget : budgets) {
            BigDecimal used = budget.getCategoryId() == 0
                    ? totalExpense
                    : byCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
            fill(budget, used);
        }
        return budgets;
    }

    public Map<String, Object> execution(Long userId, String month) {
        List<Budget> budgets = list(userId, month);
        Budget total = budgets.stream().filter(b -> b.getCategoryId() == 0).findFirst().orElse(null);
        BigDecimal used = total != null ? total.getUsedAmt() : BigDecimal.ZERO;
        BigDecimal limit = total != null ? total.getLimitAmt() : BigDecimal.ZERO;
        BigDecimal ratio = limit.compareTo(BigDecimal.ZERO) > 0
                ? used.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("hasBudget", total != null);
        result.put("limitAmt", limit);
        result.put("usedAmt", used);
        result.put("remainAmt", limit.subtract(used));
        result.put("ratio", ratio);
        result.put("level", level(ratio, total == null ? 80 : total.getWarnThreshold()));
        result.put("budgets", budgets);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Budget upsert(Long userId, BudgetRequest request) {
        parseMonth(request.month());
        if (request.categoryId() != 0) {
            Category category = categoryMapper.selectByIdAndUser(request.categoryId(), userId);
            if (category == null) {
                throw BizException.notFound();
            }
            if (!"EXPENSE".equals(category.getType())) {
                throw new BizException("只能为支出分类设置预算");
            }
        }
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(request.month());
        budget.setCategoryId(request.categoryId());
        budget.setLimitAmt(request.limitAmt());
        budget.setWarnThreshold(request.warnThreshold() == null ? 80 : request.warnThreshold());
        budgetMapper.upsert(budget);
        return budgetMapper.selectByMonthAndCategory(userId, request.month(), request.categoryId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        if (budgetMapper.selectByIdAndUser(id, userId) == null) {
            throw BizException.notFound();
        }
        budgetMapper.delete(id, userId);
    }

    /** 月度总预算，供每日可花额度使用 */
    public Budget totalBudget(Long userId, String month) {
        return budgetMapper.selectByMonthAndCategory(userId, month, 0L);
    }

    private Map<Long, BigDecimal> categoryExpense(Long userId, YearMonth ym) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : recordMapper.sumExpenseByCategory(userId, ym.atDay(1), ym.atEndOfMonth())) {
            Object id = row.get("categoryId");
            if (id != null) {
                map.put(((Number) id).longValue(), toDecimal(row.get("amount")));
            }
        }
        return map;
    }

    private void fill(Budget budget, BigDecimal used) {
        budget.setUsedAmt(used);
        BigDecimal limit = budget.getLimitAmt();
        BigDecimal ratio = limit.compareTo(BigDecimal.ZERO) > 0
                ? used.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        budget.setRatio(ratio);
        budget.setLevel(level(ratio, budget.getWarnThreshold()));
    }

    private String level(BigDecimal ratio, Integer threshold) {
        int warn = threshold == null ? 80 : threshold;
        if (ratio.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "RED";
        }
        if (ratio.compareTo(BigDecimal.valueOf(warn)) >= 0) {
            return "YELLOW";
        }
        return "GREEN";
    }

    public static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
    }

    public static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    public static LocalDate today() {
        return LocalDate.now();
    }
}

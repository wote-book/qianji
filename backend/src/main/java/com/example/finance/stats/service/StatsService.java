package com.example.finance.stats.service;

import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.budget.entity.Budget;
import com.example.finance.budget.service.BudgetService;
import com.example.finance.goal.service.GoalService;
import com.example.finance.record.entity.Record;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final RecordMapper recordMapper;
    private final AccountMapper accountMapper;
    private final BudgetService budgetService;
    private final GoalService goalService;

    public Map<String, Object> summary(Long userId, String month) {
        YearMonth ym = BudgetService.parseMonth(month);
        Map<String, Object> raw = recordMapper.sumByRange(userId, ym.atDay(1), ym.atEndOfMonth());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("income", money(raw.get("income")));
        result.put("expense", money(raw.get("expense")));
        result.put("net", money(raw.get("net")));
        result.put("totalBalance", money(accountMapper.sumBalance(userId)));
        return result;
    }

    public Map<String, Object> category(Long userId, String month, String type) {
        YearMonth ym = BudgetService.parseMonth(month);
        String actualType = "INCOME".equals(type) ? "INCOME" : "EXPENSE";
        List<Map<String, Object>> rows = recordMapper.sumByCategory(userId, ym.atDay(1), ym.atEndOfMonth(), actualType);

        BigDecimal total = rows.stream()
                .map(r -> BudgetService.toDecimal(r.get("amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BigDecimal> ratios = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal amount = BudgetService.toDecimal(row.get("amount"));
            BigDecimal ratio = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            ratios.add(ratio);
            sum = sum.add(ratio);
        }
        // 尾差归入最大项（rows 已按金额降序），保证占比合计 100%
        if (!ratios.isEmpty() && total.compareTo(BigDecimal.ZERO) > 0) {
            ratios.set(0, ratios.get(0).add(BigDecimal.valueOf(100).subtract(sum)));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryId", row.get("categoryId"));
            item.put("categoryName", row.get("categoryName"));
            item.put("amount", money(row.get("amount")));
            item.put("ratio", ratios.get(i));
            list.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("type", actualType);
        result.put("total", money(total));
        result.put("list", list);
        return result;
    }

    public Map<String, Object> trend(Long userId, LocalDate start, LocalDate end, String granularity) {
        boolean byMonth = "month".equalsIgnoreCase(granularity);
        Map<String, BigDecimal[]> map = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(byMonth ? "yyyy-MM" : "yyyy-MM-dd");

        if (byMonth) {
            YearMonth cursor = YearMonth.from(start);
            YearMonth last = YearMonth.from(end);
            while (!cursor.isAfter(last)) {
                map.put(cursor.format(fmt), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                cursor = cursor.plusMonths(1);
            }
            for (Map<String, Object> row : recordMapper.trendByMonth(userId, start, end)) {
                String key = String.valueOf(row.get("month"));
                map.put(key, new BigDecimal[]{
                        BudgetService.toDecimal(row.get("income")),
                        BudgetService.toDecimal(row.get("expense"))});
            }
        } else {
            LocalDate cursor = start;
            while (!cursor.isAfter(end)) {
                map.put(cursor.format(fmt), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                cursor = cursor.plusDays(1);
            }
            for (Map<String, Object> row : recordMapper.trendByDay(userId, start, end)) {
                String key = String.valueOf(row.get("day"));
                map.put(key, new BigDecimal[]{
                        BudgetService.toDecimal(row.get("income")),
                        BudgetService.toDecimal(row.get("expense"))});
            }
        }

        List<String> labels = new ArrayList<>(map.keySet());
        List<BigDecimal> income = new ArrayList<>();
        List<BigDecimal> expense = new ArrayList<>();
        for (BigDecimal[] values : map.values()) {
            income.add(values[0]);
            expense.add(values[1]);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", byMonth ? "month" : "day");
        result.put("labels", labels);
        result.put("income", income);
        result.put("expense", expense);
        return result;
    }

    public Map<String, Object> dailyQuota(Long userId, String month) {
        YearMonth ym = BudgetService.parseMonth(month);
        Budget total = budgetService.totalBudget(userId, month);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);

        BigDecimal expense = money(recordMapper.sumByRange(userId, ym.atDay(1), ym.atEndOfMonth()).get("expense"));
        result.put("expense", expense);

        if (total == null) {
            result.put("hasBudget", false);
            result.put("dailyQuota", null);
            result.put("message", "设置本月总预算后可用");
            return result;
        }

        LocalDate today = LocalDate.now();
        BigDecimal remain = total.getLimitAmt().subtract(expense);
        int remainDays;
        if (YearMonth.from(today).equals(ym)) {
            remainDays = ym.atEndOfMonth().getDayOfMonth() - today.getDayOfMonth() + 1;
        } else if (ym.isBefore(YearMonth.from(today))) {
            remainDays = 0;
        } else {
            remainDays = ym.lengthOfMonth();
        }

        result.put("hasBudget", true);
        result.put("limitAmt", money(total.getLimitAmt()));
        result.put("remainAmt", money(remain));
        result.put("remainDays", remainDays);

        if (remain.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("dailyQuota", BigDecimal.ZERO);
            result.put("message", "本月已超支 " + money(remain.abs()) + " 元");
        } else if (remainDays <= 0) {
            result.put("dailyQuota", BigDecimal.ZERO);
            result.put("message", "本月已结束");
        } else {
            result.put("dailyQuota", remain.divide(BigDecimal.valueOf(remainDays), 2, RoundingMode.DOWN));
            result.put("message", "按剩余 " + remainDays + " 天计算");
        }
        return result;
    }

    public Map<String, Object> dashboard(Long userId, String month) {
        Map<String, Object> summary = summary(userId, month);
        Map<String, Object> quota = dailyQuota(userId, month);
        Map<String, Object> budget = budgetService.execution(userId, month);
        List<Record> recent = recordMapper.selectRecent(userId, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("quota", quota);
        result.put("budget", budget);
        result.put("goals", goalService.listWithProgress(userId));
        result.put("recent", recent);
        return result;
    }

    private BigDecimal money(Object value) {
        return BudgetService.toDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}

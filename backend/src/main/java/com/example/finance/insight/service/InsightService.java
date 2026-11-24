package com.example.finance.insight.service;

import com.example.finance.budget.service.BudgetService;
import com.example.finance.record.entity.Record;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 纯规则洞察：异常大额与订阅周期识别（不依赖 AI）。
 */
@Service
@RequiredArgsConstructor
public class InsightService {

    private final RecordMapper recordMapper;

    public List<Map<String, Object>> anomalies(Long userId, String month) {
        YearMonth target = BudgetService.parseMonth(month);
        LocalDate start = target.minusMonths(5).atDay(1);
        LocalDate end = target.atEndOfMonth();
        List<Record> records = recordMapper.selectBetween(userId, start, end);

        Map<Long, List<Record>> byCategory = records.stream()
                .filter(r -> "EXPENSE".equals(r.getType()))
                .collect(Collectors.groupingBy(Record::getCategoryId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<Record>> entry : byCategory.entrySet()) {
            List<Record> samples = entry.getValue();
            if (samples.size() < 10) {
                continue; // 样本不足，不计算
            }
            double mean = samples.stream().mapToDouble(r -> r.getAmount().doubleValue()).average().orElse(0);
            double variance = samples.stream()
                    .mapToDouble(r -> Math.pow(r.getAmount().doubleValue() - mean, 2))
                    .average().orElse(0);
            double std = Math.sqrt(variance);
            double threshold = mean + 2 * std;

            for (Record record : samples) {
                if (!YearMonth.from(record.getRecordDate()).equals(target)) {
                    continue;
                }
                if (record.getAmount().doubleValue() > threshold) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("recordId", record.getId());
                    item.put("categoryName", record.getCategoryName());
                    item.put("amount", record.getAmount());
                    item.put("recordDate", record.getRecordDate());
                    item.put("remark", record.getRemark());
                    item.put("average", BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_UP));
                    item.put("threshold", BigDecimal.valueOf(threshold).setScale(2, RoundingMode.HALF_UP));
                    result.add(item);
                }
            }
        }
        result.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    public List<Map<String, Object>> subscriptions(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(5).withDayOfMonth(1);
        List<Record> records = recordMapper.selectBetween(userId, start, end);

        Map<String, List<Record>> groups = records.stream()
                .filter(r -> "EXPENSE".equals(r.getType()))
                .collect(Collectors.groupingBy(r -> r.getCategoryId() + "#" + r.getAmount().setScale(2, RoundingMode.HALF_UP)));

        List<Map<String, Object>> result = new ArrayList<>();
        for (List<Record> group : groups.values()) {
            Set<YearMonth> months = new TreeSet<>();
            Set<Integer> days = new TreeSet<>();
            for (Record record : group) {
                months.add(YearMonth.from(record.getRecordDate()));
                days.add(record.getRecordDate().getDayOfMonth());
            }
            if (months.size() < 3) {
                continue;
            }
            int minDay = Collections.min(days);
            int maxDay = Collections.max(days);
            if (maxDay - minDay > 3) {
                continue;
            }
            Record sample = group.get(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categoryName", sample.getCategoryName());
            item.put("amount", sample.getAmount());
            item.put("months", months.size());
            item.put("avgDay", (minDay + maxDay) / 2);
            item.put("suggestion", "疑似固定订阅，可在预算中单独设置分类上限");
            result.add(item);
        }
        result.sort((a, b) -> ((Integer) b.get("months")).compareTo((Integer) a.get("months")));
        return result;
    }
}

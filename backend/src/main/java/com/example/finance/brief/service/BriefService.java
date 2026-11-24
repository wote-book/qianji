package com.example.finance.brief.service;

import com.example.finance.budget.service.BudgetService;
import com.example.finance.common.BizException;
import com.example.finance.goal.entity.Goal;
import com.example.finance.goal.service.GoalService;
import com.example.finance.insight.service.InsightService;
import com.example.finance.record.mapper.RecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 智能财务简报：本地聚合脱敏 -> 受控 Prompt -> LLM，失败自动降级为规则生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BriefService {

    private final RecordMapper recordMapper;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final InsightService insightService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.brief.timeout-ms:15000}")
    private long timeoutMs;
    @Value("${app.brief.force-rule:false}")
    private boolean forceRule;
    @Value("${app.brief.max-chars:400}")
    private int maxChars;
    @Value("${app.brief.model:agnes-3.0-flash}")
    private String model;
    @Value("${app.brief.prompt:}")
    private String prompt;
    @Value("${app.brief.api-key:${AGNES_API_KEY:}}")
    private String apiKey;
    @Value("${app.brief.api-base:${AGNES_API_BASE:https://api.agnes-ai.cn/v1}}")
    private String apiBase;

    public Map<String, Object> generate(Long userId, String month) {
        YearMonth ym = BudgetService.parseMonth(month);
        Map<String, Object> summary = aggregate(userId, ym);
        String masked = mask(summary);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", month);

        if (forceRule || !StringUtils.hasText(apiKey)) {
            if (!forceRule) {
                log.warn("未配置 Agnes API Key，简报使用规则生成");
            }
            result.put("content", fallback(summary));
            result.put("source", "RULE");
            result.put("degraded", true);
            return result;
        }

        try {
            String content = callModel(masked);
            validate(content);
            result.put("content", content);
            result.put("source", "MODEL");
            result.put("degraded", false);
        } catch (Exception e) {
            log.warn("智能简报调用失败，降级为规则生成：{}", e.getMessage());
            result.put("content", fallback(summary));
            result.put("source", "RULE");
            result.put("degraded", true);
        }
        return result;
    }

    private Map<String, Object> aggregate(Long userId, YearMonth ym) {
        Map<String, Object> raw = recordMapper.sumByRange(userId, ym.atDay(1), ym.atEndOfMonth());
        BigDecimal income = BudgetService.toDecimal(raw.get("income"));
        BigDecimal expense = BudgetService.toDecimal(raw.get("expense"));

        YearMonth prevMonth = ym.minusMonths(1);
        Map<String, Object> prevRaw = recordMapper.sumByRange(userId, prevMonth.atDay(1), prevMonth.atEndOfMonth());
        BigDecimal prevIncome = BudgetService.toDecimal(prevRaw.get("income"));
        BigDecimal prevExpense = BudgetService.toDecimal(prevRaw.get("expense"));

        LocalDate today = LocalDate.now();
        Map<String, Object> todayRaw = recordMapper.sumByRange(userId, today, today);
        Map<String, Object> yesterdayRaw = recordMapper.sumByRange(userId, today.minusDays(1), today.minusDays(1));
        BigDecimal todayIncome = BudgetService.toDecimal(todayRaw.get("income"));
        BigDecimal todayExpense = BudgetService.toDecimal(todayRaw.get("expense"));
        BigDecimal yesterdayExpense = BudgetService.toDecimal(yesterdayRaw.get("expense"));

        List<Map<String, Object>> top = new ArrayList<>();
        List<Map<String, Object>> categories = recordMapper.sumByCategory(
                userId, ym.atDay(1), ym.atEndOfMonth(), "EXPENSE");
        for (int i = 0; i < Math.min(3, categories.size()); i++) {
            Map<String, Object> row = categories.get(i);
            BigDecimal amount = BudgetService.toDecimal(row.get("amount"));
            BigDecimal ratio = expense.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(expense, 0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("categoryName"));
            item.put("amount", amount.setScale(0, RoundingMode.HALF_UP));
            item.put("ratio", ratio);
            top.add(item);
        }

        Map<String, Object> budget = budgetService.execution(userId, ym.toString());
        List<Goal> goals = goalService.listWithProgress(userId);
        BigDecimal avgPercent = goals.isEmpty() ? BigDecimal.ZERO
                : goals.stream().map(Goal::getPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(goals.size()), 0, RoundingMode.HALF_UP);

        int anomalyCount = insightService.anomalies(userId, ym.toString()).size();

        BigDecimal net = income.subtract(expense);
        BigDecimal savingsRate = income.compareTo(BigDecimal.ZERO) > 0
                ? net.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.HALF_UP)
                : null;

        Map<String, Object> prev = new LinkedHashMap<>();
        prev.put("month", prevMonth.toString());
        prev.put("income", prevIncome.setScale(0, RoundingMode.HALF_UP));
        prev.put("expense", prevExpense.setScale(0, RoundingMode.HALF_UP));
        prev.put("net", prevIncome.subtract(prevExpense).setScale(0, RoundingMode.HALF_UP));

        Map<String, Object> day = new LinkedHashMap<>();
        day.put("date", today.toString());
        day.put("income", todayIncome.setScale(0, RoundingMode.HALF_UP));
        day.put("expense", todayExpense.setScale(0, RoundingMode.HALF_UP));
        day.put("yesterdayExpense", yesterdayExpense.setScale(0, RoundingMode.HALF_UP));
        day.put("expenseChangeRate", changeRate(todayExpense, yesterdayExpense));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("month", ym.toString());
        summary.put("income", income.setScale(0, RoundingMode.HALF_UP));
        summary.put("expense", expense.setScale(0, RoundingMode.HALF_UP));
        summary.put("net", net.setScale(0, RoundingMode.HALF_UP));
        summary.put("savingsRate", savingsRate);
        summary.put("prevMonth", prev);
        summary.put("expenseChangeRate", changeRate(expense, prevExpense));
        summary.put("incomeChangeRate", changeRate(income, prevIncome));
        summary.put("today", day);
        summary.put("topCategories", top);
        summary.put("budgetLimit", budget.get("limitAmt"));
        summary.put("budgetUsed", budget.get("usedAmt"));
        summary.put("budgetRatio", budget.get("ratio"));
        summary.put("budgetLevel", budget.get("level"));
        summary.put("goalCount", goals.size());
        summary.put("goalAvgPercent", avgPercent);
        summary.put("anomalyCount", anomalyCount);
        return summary;
    }

    /** 环比变化率（%）：基数为 0 或缺失时返回 null，避免模型自行编造。 */
    private BigDecimal changeRate(BigDecimal current, BigDecimal base) {
        if (current == null || base == null || base.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return current.subtract(base).multiply(BigDecimal.valueOf(100))
                .divide(base, 0, RoundingMode.HALF_UP);
    }

    /**
     * 白名单脱敏：只序列化摘要对象，备注、日期、账号、姓名一律不进入。
     */
    private String mask(Map<String, Object> summary) {
        Map<String, Object> whitelist = new LinkedHashMap<>();
        whitelist.put("month", summary.get("month"));
        whitelist.put("income", summary.get("income"));
        whitelist.put("expense", summary.get("expense"));
        whitelist.put("net", summary.get("net"));
        whitelist.put("savingsRate", summary.get("savingsRate"));
        whitelist.put("prevMonth", summary.get("prevMonth"));
        whitelist.put("expenseChangeRate", summary.get("expenseChangeRate"));
        whitelist.put("incomeChangeRate", summary.get("incomeChangeRate"));
        whitelist.put("today", summary.get("today"));
        whitelist.put("topCategories", summary.get("topCategories"));
        whitelist.put("budgetRatio", summary.get("budgetRatio"));
        whitelist.put("budgetLevel", summary.get("budgetLevel"));
        whitelist.put("goalCount", summary.get("goalCount"));
        whitelist.put("goalAvgPercent", summary.get("goalAvgPercent"));
        whitelist.put("anomalyCount", summary.get("anomalyCount"));
        try {
            return objectMapper.writeValueAsString(whitelist);
        } catch (Exception e) {
            throw new BizException("摘要生成失败");
        }
    }

    private String callModel(String masked) throws Exception {
        String base = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        String body = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "temperature", 0.3,
                "max_tokens", 900,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", prompt == null ? "" : prompt),
                        Map.of("role", "user", "content", "统计摘要：" + masked)
                )
        ));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/chat/completions"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BizException("模型服务返回 " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new BizException("模型返回内容为空");
        }
        String content = choices.get(0).path("message").path("content").asText("");
        if (!StringUtils.hasText(content)) {
            throw new BizException("模型返回内容为空");
        }
        return content.trim();
    }

    private void validate(String content) {
        if (content == null || content.length() < 40) {
            throw new BizException("模型输出过短，已降级");
        }
        if (content.length() > maxChars * 3L) {
            throw new BizException("模型输出超长，已降级");
        }
    }

    @SuppressWarnings("unchecked")
    private String fallback(Map<String, Object> summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("本月收入 ").append(summary.get("income")).append(" 元，支出 ")
                .append(summary.get("expense")).append(" 元，结余 ")
                .append(summary.get("net")).append(" 元。");

        Map<String, Object> prev = (Map<String, Object>) summary.get("prevMonth");
        if (prev != null) {
            sb.append("上月支出 ").append(prev.get("expense")).append(" 元")
                    .append(ratePhrase("本月支出比上月", (BigDecimal) summary.get("expenseChangeRate")))
                    .append("。");
        }

        Map<String, Object> today = (Map<String, Object>) summary.get("today");
        if (today != null) {
            sb.append("今天支出 ").append(today.get("expense")).append(" 元，昨天 ")
                    .append(today.get("yesterdayExpense")).append(" 元")
                    .append(ratePhrase("今天比昨天", (BigDecimal) today.get("expenseChangeRate")))
                    .append("。");
        }

        BigDecimal net = (BigDecimal) summary.get("net");
        if (net != null && net.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("本月已存下 ").append(net).append(" 元");
            Object savingsRate = summary.get("savingsRate");
            if (savingsRate != null) {
                sb.append("，储蓄率 ").append(savingsRate).append("%");
            }
            sb.append("，继续保持这个节奏！");
        } else {
            sb.append("本月支出超过收入，建议先从压缩非必要开支开始。");
        }

        List<Map<String, Object>> top = (List<Map<String, Object>>) summary.get("topCategories");
        if (top != null && !top.isEmpty()) {
            sb.append("支出主要花在：");
            for (int i = 0; i < top.size(); i++) {
                Map<String, Object> item = top.get(i);
                if (i > 0) {
                    sb.append("、");
                }
                sb.append(item.get("name")).append(" ").append(item.get("amount"))
                        .append(" 元（占 ").append(item.get("ratio")).append("%）");
            }
            sb.append("。");
        } else {
            sb.append("本月暂无支出记录。");
        }

        sb.append("预算执行率 ").append(summary.get("budgetRatio")).append("%");
        if ("RED".equals(summary.get("budgetLevel"))) {
            sb.append("，已超预算，建议压缩非必要开支");
        } else if ("YELLOW".equals(summary.get("budgetLevel"))) {
            sb.append("，接近上限，注意控制后续支出");
        } else {
            sb.append("，处于安全区间");
        }
        sb.append("。目标平均完成度 ").append(summary.get("goalAvgPercent")).append("%");
        sb.append("，共 ").append(summary.get("goalCount")).append(" 个理财目标。");

        Object anomaly = summary.get("anomalyCount");
        if (anomaly != null && Integer.parseInt(anomaly.toString()) > 0) {
            sb.append("本月发现 ").append(anomaly).append(" 笔异常大额支出，建议核对。");
        } else {
            sb.append("本月未发现明显异常支出。");
        }
        sb.append("建议保持每日记账习惯，月底复盘分类占比，并为下月设定可执行的支出上限。");
        return sb.toString();
    }

    private String ratePhrase(String prefix, BigDecimal rate) {
        if (rate == null) {
            return "，暂无可比数据";
        }
        return "，" + prefix + (rate.compareTo(BigDecimal.ZERO) >= 0 ? "增加 " : "减少 ")
                + rate.abs().toPlainString() + "%";
    }
}

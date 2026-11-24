package com.example.finance.ai.service;

import com.example.finance.account.entity.Account;
import com.example.finance.account.service.AccountService;
import com.example.finance.ai.dto.AiCommitItem;
import com.example.finance.ai.dto.AiCommitRequest;
import com.example.finance.ai.dto.AiParseItem;
import com.example.finance.ai.dto.AiParseRequest;
import com.example.finance.ai.dto.AiParseResult;
import com.example.finance.category.entity.Category;
import com.example.finance.category.service.CategoryService;
import com.example.finance.common.BizException;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.service.RecordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecordService {

    private final RuleRecordParser ruleParser;
    private final RecordService recordService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.timeout-ms:15000}")
    private long timeoutMs;

    @Value("${app.ai.max-items:10}")
    private int maxItems;

    @Value("${app.ai.force-rule:false}")
    private boolean forceRule;

    @Value("${app.ai.model:agnes-3.0-flash}")
    private String model;

    @Value("${app.ai.api-key:${app.brief.api-key:${AGNES_API_KEY:}}}")
    private String apiKey;

    @Value("${app.ai.api-base:${app.brief.api-base:https://api.agnes-ai.cn/v1}}")
    private String apiBase;

    private static final int MAX_INPUT = 500;
    private static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(1_000_000);

    public AiParseResult parse(Long userId, AiParseRequest request) {
        String text = request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BizException("请描述这笔收支");
        }
        if (text.length() > MAX_INPUT) {
            text = text.substring(0, MAX_INPUT);
        }

        List<Account> accounts = accountService.list(userId);
        if (accounts.isEmpty()) {
            throw new BizException("请先在账户页面创建账户");
        }
        Account defaultAccount = pickAccount(accounts, request.defaultAccountId());
        List<Category> categories = loadCategories(userId);
        LocalDate defaultDate = parseDate(request.defaultDate());

        if (forceRule || !StringUtils.hasText(apiKey)) {
            return degraded(text, accounts, categories, defaultAccount, defaultDate);
        }
        try {
            String content = callModel(text, accounts, categories, defaultAccount, defaultDate);
            List<AiParseItem> items = parseModelItems(content, accounts, categories, defaultAccount, defaultDate);
            if (items.isEmpty()) {
                throw new BizException("没有识别到有效收支");
            }
            return new AiParseResult("MODEL", false, null, withCategoryFallback(items, categories));
        } catch (Exception e) {
            log.warn("AI 记账解析失败，降级规则解析：{}", e.getMessage());
            return degraded(text, accounts, categories, defaultAccount, defaultDate);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int commit(Long userId, AiCommitRequest request) {
        for (AiCommitItem item : request.items()) {
            RecordRequest recordRequest = new RecordRequest(
                    item.accountId(), item.categoryId(), item.amount(), item.type(),
                    item.recordDate(), item.remark());
            recordService.createWithSource(userId, recordRequest, "AI");
        }
        return request.items().size();
    }

    private AiParseResult degraded(String text, List<Account> accounts, List<Category> categories,
                                   Account defaultAccount, LocalDate defaultDate) {
        List<AiParseItem> items = ruleParser.parse(text, accounts, categories, defaultAccount, defaultDate);
        if (items.isEmpty()) {
            throw new BizException("没有识别到收支，请补充金额后重试，例如：午饭 25 元");
        }
        return new AiParseResult("RULE", true, "AI 暂不可用，已使用规则解析，请核对结果",
                withCategoryFallback(items, categories));
    }

    /**
     * 未匹配到分类时归入「其他支出 / 其他收入」，避免预览卡片信息不完整导致无法入账。
     */
    private List<AiParseItem> withCategoryFallback(List<AiParseItem> items, List<Category> categories) {
        List<AiParseItem> result = new ArrayList<>(items.size());
        for (AiParseItem item : items) {
            Category fallback = item.categoryId() == null ? findFallbackCategory(categories, item.type()) : null;
            if (fallback == null) {
                result.add(item);
                continue;
            }
            String hint = "未匹配到分类，已归入" + fallback.getName() + "，可修改";
            result.add(new AiParseItem(item.type(), item.amount(), fallback.getId(), fallback.getName(),
                    item.accountId(), item.accountName(), item.recordDate(), item.remark(),
                    item.warn() == null ? hint : item.warn() + "；" + hint));
        }
        return result;
    }

    private Category findFallbackCategory(List<Category> categories, String type) {
        String target = "INCOME".equals(type) ? "其他收入" : "其他支出";
        for (Category category : categories) {
            if (type.equals(category.getType()) && target.equals(category.getName())
                    && (category.getStatus() == null || category.getStatus() == 1)) {
                return category;
            }
        }
        return null;
    }

    private List<Category> loadCategories(Long userId) {
        List<Category> categories = new ArrayList<>();
        categories.addAll(categoryService.list(userId, "EXPENSE"));
        categories.addAll(categoryService.list(userId, "INCOME"));
        return categories;
    }

    private Account pickAccount(List<Account> accounts, Long accountId) {
        if (accountId != null) {
            for (Account account : accounts) {
                if (accountId.equals(account.getId())) {
                    return account;
                }
            }
        }
        return accounts.get(0);
    }

    private LocalDate parseDate(String text) {
        if (StringUtils.hasText(text)) {
            try {
                return LocalDate.parse(text.trim());
            } catch (Exception ignored) {
                // 非法日期使用今天
            }
        }
        return LocalDate.now();
    }

    private String callModel(String text, List<Account> accounts, List<Category> categories,
                             Account defaultAccount, LocalDate defaultDate) throws Exception {
        String expense = categories.stream()
                .filter(c -> "EXPENSE".equals(c.getType()))
                .filter(c -> c.getStatus() == null || c.getStatus() == 1)
                .map(Category::getName)
                .collect(Collectors.joining("、"));
        String income = categories.stream()
                .filter(c -> "INCOME".equals(c.getType()))
                .filter(c -> c.getStatus() == null || c.getStatus() == 1)
                .map(Category::getName)
                .collect(Collectors.joining("、"));
        String accountNames = accounts.stream().map(Account::getAccName).collect(Collectors.joining("、"));

        String system = "你是个人记账助手。把用户的一句话拆成若干笔收支，只输出 JSON，不要输出解释。\n"
                + "要求：\n"
                + "1. 只能使用给出的账户名与分类名，不得新造；\n"
                + "2. type 只能是 EXPENSE 或 INCOME；\n"
                + "3. amount 为数字（单位：元），不得编造；\n"
                + "4. date 格式 yyyy-MM-dd，用户未说明时使用 " + defaultDate + "；\n"
                + "5. 无法确定分类时 category 填 null；\n"
                + "6. 最多 " + maxItems + " 笔；\n"
                + "7. 输出格式：{\"items\":[{\"type\":\"EXPENSE\",\"amount\":25,\"category\":\"餐饮\","
                + "\"account\":\"支付宝\",\"date\":\"" + defaultDate + "\",\"remark\":\"午饭\"}]}";

        String user = "可用账户：" + accountNames + "\n"
                + "默认账户：" + defaultAccount.getAccName() + "\n"
                + "支出分类：" + expense + "\n"
                + "收入分类：" + income + "\n"
                + "当前日期：" + defaultDate + "\n"
                + "用户描述：" + text;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.1);
        payload.put("max_tokens", 800);
        payload.put("stream", false);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));
        String body = objectMapper.writeValueAsString(payload);

        String base = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(base + "/chat/completions"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
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
        return content;
    }

    private List<AiParseItem> parseModelItems(String content, List<Account> accounts,
                                              List<Category> categories, Account defaultAccount,
                                              LocalDate defaultDate) throws Exception {
        String cleaned = content.replace("```json", "").replace("```", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode array = root.path("items");
        if (!array.isArray()) {
            throw new BizException("模型输出格式不正确");
        }

        List<AiParseItem> items = new ArrayList<>();
        for (JsonNode node : array) {
            if (items.size() >= maxItems) {
                break;
            }
            BigDecimal amount = readAmount(node.path("amount"));
            if (amount == null) {
                continue;
            }
            String type = "INCOME".equalsIgnoreCase(node.path("type").asText("")) ? "INCOME" : "EXPENSE";
            LocalDate date = clampDate(node.path("date").asText(""), defaultDate);
            List<String> warnings = new ArrayList<>();

            Category category = matchCategory(node.path("category").asText(""), categories, type);
            Account account = matchAccount(node.path("account").asText(""), accounts);
            if (account == null) {
                account = defaultAccount;
                if (StringUtils.hasText(node.path("account").asText(""))) {
                    warnings.add("未匹配到账户，已使用默认账户");
                }
            }
            String remark = node.path("remark").asText("");
            if (remark.length() > 200) {
                remark = remark.substring(0, 200);
            }
            items.add(new AiParseItem(
                    type,
                    amount,
                    category == null ? null : category.getId(),
                    category == null ? null : category.getName(),
                    account.getId(),
                    account.getAccName(),
                    date,
                    remark,
                    warnings.isEmpty() ? null : String.join("；", warnings)
            ));
        }
        return items;
    }

    private BigDecimal readAmount(JsonNode node) {
        try {
            BigDecimal amount;
            if (node.isNumber()) {
                amount = node.decimalValue();
            } else {
                String text = node.asText("").replace("元", "").replace("块", "").trim();
                if (!StringUtils.hasText(text)) {
                    return null;
                }
                amount = new BigDecimal(text);
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
                return null;
            }
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate clampDate(String text, LocalDate defaultDate) {
        if (!StringUtils.hasText(text)) {
            return defaultDate;
        }
        try {
            LocalDate date = LocalDate.parse(text.trim());
            LocalDate today = LocalDate.now();
            if (date.isAfter(today) || date.isBefore(today.minusDays(365))) {
                return defaultDate;
            }
            return date;
        } catch (Exception e) {
            return defaultDate;
        }
    }

    private Category matchCategory(String name, List<Category> categories, String type) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String target = name.trim();
        for (Category category : categories) {
            if (type.equals(category.getType())
                    && target.equals(category.getName())
                    && (category.getStatus() == null || category.getStatus() == 1)) {
                return category;
            }
        }
        return null;
    }

    private Account matchAccount(String name, List<Account> accounts) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String target = name.trim();
        for (Account account : accounts) {
            if (target.equals(account.getAccName())) {
                return account;
            }
        }
        return null;
    }
}

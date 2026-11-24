package com.example.finance.ai.service;

import com.example.finance.account.entity.Account;
import com.example.finance.ai.dto.AiParseItem;
import com.example.finance.category.entity.Category;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则解析：模型不可用时兜底，支持金额、收支、日期、账户、分类关键词与多笔拆分。
 */
@Component
public class RuleRecordParser {

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(千|k|K|万|w|W|元|块|块钱)?");
    private static final Pattern MONTH_DAY = Pattern.compile("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]");
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final String[] INCOME_WORDS = {"工资", "薪", "奖金", "报销", "退款", "收入", "收到", "进账", "利息", "分红", "礼金", "兼职", "卖了", "转我"};

    private static final Map<String, String> SYNONYMS = new LinkedHashMap<>();

    static {
        put("餐饮", "吃", "饭", "早餐", "午餐", "晚餐", "外卖", "咖啡", "奶茶", "零食", "夜宵", "食堂", "聚餐");
        put("交通", "打车", "出租", "地铁", "公交", "加油", "停车", "高速", "高铁", "机票", "火车", "滴滴");
        put("居住", "房租", "水费", "电费", "燃气", "物业", "水电网");
        put("购物", "超市", "淘宝", "京东", "拼多多", "购物", "商场", "网购");
        put("娱乐", "电影", "游戏", "KTV", "演唱会", "娱乐", "剧本杀");
        put("医疗", "医院", "药", "挂号", "体检", "看病", "诊所");
        put("教育", "学费", "课程", "培训", "学习", "买书");
        put("通讯", "话费", "流量", "电话费");
        put("服饰", "衣服", "鞋", "裤子", "外套", "帽子");
        put("日用", "纸巾", "洗衣液", "日用品", "家居");
        put("美容", "理发", "美发", "护肤", "化妆");
        put("旅行", "旅游", "酒店", "民宿", "景点");
        put("健身", "健身房", "瑜伽", "运动", "跑步");
        put("宠物", "猫粮", "狗粮", "宠物");
        put("数码", "电脑", "手机", "耳机", "数码", "键盘");
        put("人情", "礼物", "随礼", "份子", "请客");
        put("办公", "打印", "办公用品", "文具");
        put("税费", "税", "手续费");
        put("保险", "保险");
        put("工资", "工资", "薪");
        put("奖金", "奖金", "年终奖");
        put("兼职", "兼职", "外快");
        put("理财收益", "利息", "分红", "收益");
        put("红包", "收红包");
        put("报销", "报销");
        put("退款", "退款", "退货");
        put("礼金", "礼金");
    }

    private static void put(String category, String... words) {
        for (String word : words) {
            SYNONYMS.put(word, category);
        }
    }

    public List<AiParseItem> parse(String text, List<Account> accounts, List<Category> categories,
                                   Account defaultAccount, LocalDate defaultDate) {
        List<AiParseItem> items = new ArrayList<>();
        String[] segments = text.split("[，,、;；\\n]+|然后|还有");
        Account fallbackAccount = resolveFallbackAccount(segments, accounts, defaultAccount);
        for (String segment : segments) {
            if (items.size() >= 10) {
                break;
            }
            String raw = segment.trim();
            if (raw.isEmpty()) {
                continue;
            }
            BigDecimal amount = findAmount(raw);
            if (amount == null) {
                continue;
            }
            String type = detectType(raw);
            LocalDate date = findDate(raw, defaultDate);
            Account account = findAccount(raw, accounts, fallbackAccount);
            Category category = findCategory(raw, categories, type);
            String remark = raw.length() > 200 ? raw.substring(0, 200) : raw;
            items.add(new AiParseItem(
                    type,
                    amount,
                    category == null ? null : category.getId(),
                    category == null ? null : category.getName(),
                    account.getId(),
                    account.getAccName(),
                    date,
                    remark,
                    null
            ));
        }
        return items;
    }

    private BigDecimal findAmount(String text) {
        Matcher matcher = AMOUNT.matcher(text);
        BigDecimal best = null;
        boolean bestHasUnit = false;
        while (matcher.find()) {
            String rest = text.substring(matcher.end());
            if (rest.startsWith("月") || rest.startsWith("日") || rest.startsWith("号")
                    || rest.startsWith("点") || rest.startsWith("%") || rest.startsWith("个")) {
                continue;
            }
            BigDecimal value = new BigDecimal(matcher.group(1));
            String unit = matcher.group(2);
            boolean hasUnit = unit != null;
            if ("千".equals(unit) || "k".equalsIgnoreCase(unit)) {
                value = value.multiply(BigDecimal.valueOf(1000));
            } else if ("万".equals(unit) || "w".equalsIgnoreCase(unit)) {
                value = value.multiply(BigDecimal.valueOf(10000));
            }
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
                continue;
            }
            if (best == null || (hasUnit && !bestHasUnit)
                    || (hasUnit == bestHasUnit && value.compareTo(best) > 0)) {
                best = value;
                bestHasUnit = hasUnit;
            }
        }
        return best == null ? null : best.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String detectType(String text) {
        for (String word : INCOME_WORDS) {
            if (text.contains(word)) {
                return "INCOME";
            }
        }
        return "EXPENSE";
    }

    private LocalDate findDate(String text, LocalDate defaultDate) {
        if (text.contains("大前天")) {
            return defaultDate.minusDays(3);
        }
        if (text.contains("前天")) {
            return defaultDate.minusDays(2);
        }
        if (text.contains("昨天") || text.contains("昨日")) {
            return defaultDate.minusDays(1);
        }
        Matcher iso = ISO_DATE.matcher(text);
        if (iso.find()) {
            try {
                return LocalDate.parse(iso.group(1) + "-"
                        + String.format("%02d", Integer.parseInt(iso.group(2))) + "-"
                        + String.format("%02d", Integer.parseInt(iso.group(3))), DateTimeFormatter.ISO_DATE);
            } catch (Exception ignored) {
                // 继续尝试其他格式
            }
        }
        Matcher monthDay = MONTH_DAY.matcher(text);
        if (monthDay.find()) {
            try {
                int month = Integer.parseInt(monthDay.group(1));
                int day = Integer.parseInt(monthDay.group(2));
                return LocalDate.of(defaultDate.getYear(), month, day);
            } catch (Exception ignored) {
                // 日期非法时使用默认日期
            }
        }
        return defaultDate;
    }

    private Account resolveFallbackAccount(String[] segments, List<Account> accounts, Account defaultAccount) {
        Account mentioned = null;
        for (String segment : segments) {
            Account account = matchAccount(segment.trim(), accounts);
            if (account == null) {
                continue;
            }
            if (mentioned == null) {
                mentioned = account;
            } else if (!mentioned.getId().equals(account.getId())) {
                // 多个分句提到不同账户时不统一继承，各自使用明确提到的账户
                return defaultAccount;
            }
        }
        return mentioned == null ? defaultAccount : mentioned;
    }

    private Account findAccount(String text, List<Account> accounts, Account fallback) {
        Account matched = matchAccount(text, accounts);
        return matched == null ? fallback : matched;
    }

    private Account matchAccount(String text, List<Account> accounts) {
        Account best = null;
        for (Account account : accounts) {
            String name = account.getAccName();
            if (name != null && !name.isEmpty() && text.contains(name)
                    && (best == null || name.length() > best.getAccName().length())) {
                best = account;
            }
        }
        return best;
    }

    private Category findCategory(String text, List<Category> categories, String type) {
        Category best = null;
        for (Category category : categories) {
            if (!type.equals(category.getType()) || (category.getStatus() != null && category.getStatus() == 0)) {
                continue;
            }
            String name = category.getName();
            if (name != null && text.contains(name)
                    && (best == null || name.length() > best.getName().length())) {
                best = category;
            }
        }
        if (best != null) {
            return best;
        }
        for (Map.Entry<String, String> entry : SYNONYMS.entrySet()) {
            if (!text.contains(entry.getKey())) {
                continue;
            }
            for (Category category : categories) {
                if (type.equals(category.getType())
                        && entry.getValue().equals(category.getName())
                        && (category.getStatus() == null || category.getStatus() == 1)) {
                    return category;
                }
            }
        }
        return null;
    }
}

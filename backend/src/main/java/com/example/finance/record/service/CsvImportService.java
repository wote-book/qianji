package com.example.finance.record.service;

import com.example.finance.account.entity.Account;
import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.common.PresetCategories;
import com.example.finance.record.dto.ImportRow;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV 导入：模板下载、解析预览（自动分类）、确认导入（去重 + 同事务）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private static final String[] TEMPLATE_HEADER = {"日期", "类型", "分类", "账户", "金额", "备注"};
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d")
    );

    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final RecordMapper recordMapper;
    private final RecordService recordService;

    @Value("${app.import.max-rows:5000}")
    private int maxRows;

    public byte[] template() {
        String content = String.join(",", TEMPLATE_HEADER) + "\n"
                + "2026-06-01,支出,餐饮,支付宝,35.50,午餐\n"
                + "2026-06-05,收入,工资,招商银行储蓄卡,15000.00,6月工资\n";
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        return result;
    }

    public Map<String, Object> preview(Long userId, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BizException("请选择要导入的 CSV 文件");
        }
        String text = decode(bytes);
        List<Account> accounts = accountMapper.selectByUserId(userId);
        List<Category> categories = categoryMapper.selectByUser(userId, null);
        Map<String, Category> categoryByName = new HashMap<>();
        for (Category category : categories) {
            categoryByName.put(category.getType() + "#" + category.getName(), category);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int valid = 0;
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(new StringReader(text))) {
            List<CSVRecord> all = parser.getRecords();
            if (all.isEmpty()) {
                throw new BizException("CSV 内容为空");
            }
            Map<String, Integer> header = headerIndex(all.get(0));
            if (!header.containsKey("日期") || !header.containsKey("金额")) {
                throw new BizException("CSV 表头需包含：日期,类型,账户,金额（分类、备注可空）");
            }
            for (int i = 1; i < all.size(); i++) {
                if (rows.size() >= maxRows) {
                    throw new BizException("单次最多导入 " + maxRows + " 行，请分批导入");
                }
                CSVRecord record = all.get(i);
                if (isBlankRow(record)) {
                    continue;
                }
                Map<String, Object> row = parseRow(record, header, i + 1, accounts, categoryByName);
                if (Boolean.TRUE.equals(row.get("valid"))) {
                    valid++;
                }
                rows.add(row);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析 CSV 失败", e);
            throw new BizException("CSV 解析失败，请检查文件格式");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("validCount", valid);
        result.put("invalidCount", rows.size() - valid);
        result.put("rows", rows);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> commit(Long userId, List<ImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException("没有可导入的数据");
        }
        if (rows.size() > maxRows) {
            throw new BizException("单次最多导入 " + maxRows + " 行，请分批导入");
        }
        Map<Long, Account> accountMap = new HashMap<>();
        accountMapper.selectByUserId(userId).forEach(a -> accountMap.put(a.getId(), a));
        Map<Long, Category> categoryMap = new HashMap<>();
        categoryMapper.selectByUser(userId, null).forEach(c -> categoryMap.put(c.getId(), c));

        int inserted = 0;
        int skipped = 0;
        for (int i = 0; i < rows.size(); i++) {
            ImportRow row = rows.get(i);
            int line = i + 1;
            if (row.getDate() == null) {
                throw new BizException("第 " + line + " 行：日期不能为空");
            }
            String type = normalizeType(row.getType());
            if (type == null) {
                throw new BizException("第 " + line + " 行：类型只能为收入或支出");
            }
            if (row.getAmount() == null || row.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("第 " + line + " 行：金额必须大于 0");
            }
            Account account = accountMap.get(row.getAccountId());
            if (account == null) {
                throw new BizException("第 " + line + " 行：账户不存在");
            }
            Category category = categoryMap.get(row.getCategoryId());
            if (category == null) {
                throw new BizException("第 " + line + " 行：分类不存在");
            }
            if (!type.equals(category.getType())) {
                throw new BizException("第 " + line + " 行：分类与收支类型不一致");
            }
            String dedupKey = dedupKey(row.getDate(), row.getAmount(), type, category.getName(), row.getRemark());
            if (recordMapper.selectByDedupKey(userId, dedupKey) != null) {
                skipped++;
                continue;
            }
            recordService.createImported(userId,
                    new RecordRequest(account.getId(), category.getId(), row.getAmount(), type,
                            row.getDate(), row.getRemark()),
                    "CSV", dedupKey);
            inserted++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("inserted", inserted);
        result.put("skipped", skipped);
        return result;
    }

    private Map<String, Object> parseRow(CSVRecord record, Map<String, Integer> header, int line,
                                         List<Account> accounts, Map<String, Category> categoryByName) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowNo", line);
        row.put("valid", false);
        row.put("message", "");

        String dateText = cell(record, header, "日期");
        String typeText = cell(record, header, "类型");
        String categoryText = cell(record, header, "分类");
        String accountText = cell(record, header, "账户");
        String amountText = cell(record, header, "金额");
        String remark = cell(record, header, "备注");

        LocalDate date = parseDate(dateText);
        String type = normalizeType(typeText);
        BigDecimal amount = parseAmount(amountText);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = amount.abs();
            if (type == null) {
                type = "EXPENSE";
            }
        }
        if (type == null) {
            type = "EXPENSE";
        }

        row.put("date", date == null ? dateText : date.toString());
        row.put("type", type);
        row.put("amount", amount);
        row.put("remark", remark);

        List<String> problems = new ArrayList<>();
        if (date == null) {
            problems.add("日期格式不正确");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            problems.add("金额格式不正确");
        }

        Account account = null;
        if (StringUtils.hasText(accountText)) {
            for (Account item : accounts) {
                if (item.getAccName().equals(accountText.trim())) {
                    account = item;
                    break;
                }
            }
        }
        String accountMessage = "";
        if (account == null && !accounts.isEmpty()) {
            account = accounts.get(0);
            accountMessage = "账户未匹配，默认使用 " + account.getAccName();
        }
        if (account == null) {
            problems.add("请先创建账户");
        }
        row.put("accountId", account == null ? null : account.getId());
        row.put("accountName", account == null ? accountText : account.getAccName());

        Category category = null;
        if (StringUtils.hasText(categoryText)) {
            category = categoryByName.get(type + "#" + categoryText.trim());
        }
        String categoryMessage = "";
        if (category == null) {
            String keyword = categoryText == null ? "" : categoryText;
            String matched = matchKeyword(keyword.isEmpty() ? remark : keyword + " " + (remark == null ? "" : remark));
            if (matched != null) {
                category = categoryByName.get(type + "#" + matched);
            }
            if (category == null) {
                category = categoryByName.get(type + "#" + ("EXPENSE".equals(type) ? "其他支出" : "其他收入"));
            }
            if (category != null) {
                categoryMessage = "已自动归类为 " + category.getName();
            }
        }
        row.put("categoryId", category == null ? null : category.getId());
        row.put("categoryName", category == null ? categoryText : category.getName());
        if (category == null) {
            problems.add("找不到可用分类");
        }

        StringBuilder message = new StringBuilder();
        if (!problems.isEmpty()) {
            message.append(String.join("；", problems));
        } else {
            message.append("可导入");
        }
        if (!accountMessage.isEmpty()) {
            message.append("；").append(accountMessage);
        }
        if (!categoryMessage.isEmpty()) {
            message.append("；").append(categoryMessage);
        }
        row.put("message", message.toString());
        row.put("valid", problems.isEmpty());
        return row;
    }

    private LocalDate parseDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim().replace("年", "-").replace("月", "-").replace("日", "");
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.replace(",", "").replace("¥", "").replace("￥", "")
                .replace("元", "").replace(" ", "").trim();
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeType(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("收") || "INCOME".equals(value) || "1".equals(value)) {
            return "INCOME";
        }
        if (value.startsWith("支") || "EXPENSE".equals(value) || "0".equals(value)) {
            return "EXPENSE";
        }
        return null;
    }

    private String matchKeyword(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Map.Entry<String, String> entry : PresetCategories.KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Integer> headerIndex(CSVRecord header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            map.put(header.get(i).trim(), i);
        }
        return map;
    }

    private String cell(CSVRecord record, Map<String, Integer> header, String name) {
        Integer index = header.get(name);
        if (index == null || index >= record.size()) {
            return "";
        }
        return record.get(index).trim();
    }

    private boolean isBlankRow(CSVRecord record) {
        for (String value : record) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private String decode(byte[] bytes) {
        int offset = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                    .toString();
            return stripBom(text);
        } catch (CharacterCodingException e) {
            return stripBom(new String(bytes, offset, bytes.length - offset, Charset.forName("GBK")));
        }
    }

    private String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private String dedupKey(LocalDate date, BigDecimal amount, String type, String categoryName, String remark) {
        String raw = date + "|" + amount.stripTrailingZeros().toPlainString() + "|" + type
                + "|" + (categoryName == null ? "" : categoryName)
                + "|" + (remark == null ? "" : remark);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}

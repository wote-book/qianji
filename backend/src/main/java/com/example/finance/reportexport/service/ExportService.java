package com.example.finance.reportexport.service;

import com.alibaba.excel.EasyExcel;
import com.example.finance.common.BizException;
import com.example.finance.record.dto.RecordQuery;
import com.example.finance.record.entity.Record;
import com.example.finance.record.mapper.RecordMapper;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final RecordMapper recordMapper;

    public void exportExcel(Long userId, LocalDate start, LocalDate end, HttpServletResponse response) {
        List<Record> records = query(userId, start, end);
        List<List<String>> head = List.of(
                List.of("日期"), List.of("类型"), List.of("分类"), List.of("账户"),
                List.of("金额"), List.of("备注"), List.of("来源"));
        List<List<Object>> rows = new ArrayList<>();
        for (Record record : records) {
            List<Object> row = new ArrayList<>();
            row.add(record.getRecordDate() == null ? "" : record.getRecordDate().toString());
            row.add("INCOME".equals(record.getType()) ? "收入" : "支出");
            row.add(record.getCategoryName() == null ? "" : record.getCategoryName());
            row.add(record.getAccountName() == null ? "" : record.getAccountName());
            row.add(record.getAmount().toPlainString());
            row.add(record.getRemark() == null ? "" : record.getRemark());
            row.add(sourceLabel(record.getSource()));
            rows.add(row);
        }

        try {
            String filename = URLEncoder.encode("流水_" + start + "_" + end + ".xlsx", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename);
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("流水")
                    .doWrite(rows);
        } catch (Exception e) {
            log.error("导出 Excel 失败", e);
            throw new BizException("导出 Excel 失败");
        }
    }

    public void exportPdf(Long userId, LocalDate start, LocalDate end, HttpServletResponse response) {
        List<Record> records = query(userId, start, end);
        BigDecimal income = sum(records, "INCOME");
        BigDecimal expense = sum(records, "EXPENSE");

        try {
            String filename = URLEncoder.encode("收支报表_" + start + "_" + end + ".pdf", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType("application/pdf");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename);

            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            BaseFont baseFont = loadFont();
            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font normal = new Font(baseFont, 10);
            Font headFont = new Font(baseFont, 10, Font.BOLD);

            Paragraph title = new Paragraph("个人理财收支报表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph range = new Paragraph("统计区间：" + start + " 至 " + end
                    + "    收入合计：" + income.toPlainString()
                    + " 元    支出合计：" + expense.toPlainString()
                    + " 元    结余：" + income.subtract(expense).toPlainString() + " 元", normal);
            range.setSpacingBefore(10);
            range.setSpacingAfter(10);
            range.setAlignment(Element.ALIGN_CENTER);
            document.add(range);

            PdfPTable table = new PdfPTable(new float[]{2f, 1.4f, 2.2f, 2.2f, 1.8f, 5f});
            table.setWidthPercentage(100);
            String[] heads = {"日期", "类型", "分类", "账户", "金额", "备注"};
            for (String h : heads) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(new Color(235, 240, 250));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            for (Record record : records) {
                addCell(table, record.getRecordDate() == null ? "" : record.getRecordDate().toString(), normal);
                addCell(table, "INCOME".equals(record.getType()) ? "收入" : "支出", normal);
                addCell(table, record.getCategoryName(), normal);
                addCell(table, record.getAccountName(), normal);
                addCell(table, record.getAmount().toPlainString(), normal);
                addCell(table, record.getRemark(), normal);
            }
            document.add(table);
            document.close();
        } catch (Exception e) {
            log.error("导出 PDF 失败", e);
            throw new BizException("导出 PDF 失败");
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private BaseFont loadFont() throws Exception {
        ClassPathResource resource = new ClassPathResource("fonts/simhei.ttf");
        byte[] bytes = resource.getInputStream().readAllBytes();
        return BaseFont.createFont("simhei.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
    }

    private List<Record> query(Long userId, LocalDate start, LocalDate end) {
        RecordQuery query = new RecordQuery();
        query.setStart(start);
        query.setEnd(end);
        query.setPageSize(100);
        List<Record> records = recordMapper.selectForExport(query, userId);
        if (records.size() > 20000) {
            throw new BizException("导出数据超过 2 万条，请缩小时间范围");
        }
        return records;
    }

    private BigDecimal sum(List<Record> records, String type) {
        return records.stream()
                .filter(r -> type.equals(r.getType()))
                .map(Record::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String sourceLabel(String source) {
        if ("CSV".equals(source)) {
            return "CSV导入";
        }
        if ("RESTORE".equals(source)) {
            return "备份恢复";
        }
        if ("AI".equals(source)) {
            return "AI 记账";
        }
        return "手工录入";
    }
}

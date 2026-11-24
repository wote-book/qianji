package com.example.finance.record.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.record.dto.ImportRow;
import com.example.finance.record.service.CsvImportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records/import")
@RequiredArgsConstructor
public class RecordImportController {

    private final CsvImportService csvImportService;

    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        byte[] bytes = csvImportService.template();
        String filename = URLEncoder.encode("流水导入模板.csv", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + filename);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
    }

    @PostMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(csvImportService.preview(SessionContext.userId(), file.getBytes()));
    }

    @PostMapping("/commit")
    public Result<Map<String, Object>> commit(@RequestBody List<ImportRow> rows) {
        return Result.ok(csvImportService.commit(SessionContext.userId(), rows));
    }
}

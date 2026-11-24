package com.example.finance.backup.controller;

import com.example.finance.backup.dto.BackupPayload;
import com.example.finance.backup.service.BackupService;
import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/export")
    public Result<BackupPayload> export() {
        return Result.ok(backupService.export(SessionContext.userId()));
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importData(@RequestBody BackupPayload payload) {
        return Result.ok(backupService.importData(SessionContext.userId(), payload));
    }
}

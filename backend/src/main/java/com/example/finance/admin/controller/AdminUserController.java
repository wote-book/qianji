package com.example.finance.admin.controller;

import com.example.finance.admin.dto.TempPasswordVO;
import com.example.finance.admin.dto.UserDetailVO;
import com.example.finance.admin.dto.UserQuery;
import com.example.finance.admin.dto.UserRowVO;
import com.example.finance.admin.service.AdminUserService;
import com.example.finance.common.PageResult;
import com.example.finance.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService userService;

    @GetMapping
    public Result<PageResult<UserRowVO>> page(@ModelAttribute UserQuery query,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(userService.page(query, pageNum, pageSize));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute UserQuery query) {
        byte[] data = userService.exportCsv(query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @GetMapping("/{id}")
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        return Result.ok(userService.detail(id));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result<TempPasswordVO> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }
}

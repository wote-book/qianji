package com.example.finance.user.controller;

import com.example.finance.common.Result;
import com.example.finance.common.SessionContext;
import com.example.finance.user.dto.ChangePasswordRequest;
import com.example.finance.user.dto.ProfileRequest;
import com.example.finance.user.dto.UserVO;
import com.example.finance.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.me());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileRequest request) {
        userService.updateProfile(request);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.ok();
    }

    @PutMapping("/security")
    public Result<Void> updateSecurity(@RequestBody Map<String, String> body) {
        userService.updateProfileSecurity(SessionContext.userId(), body.get("question"), body.get("answer"));
        return Result.ok();
    }
}

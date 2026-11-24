package com.example.finance.user.dto;

import com.example.finance.user.entity.User;

import java.time.LocalDateTime;

public record UserVO(Long id, String username, String nickname, String role, Integer status,
                     String securityQuestion, LocalDateTime createTime) {

    public static UserVO of(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getRole(),
                user.getStatus(), user.getSecurityQuestion(), user.getCreateTime());
    }
}

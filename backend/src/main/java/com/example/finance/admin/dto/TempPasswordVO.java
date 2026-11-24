package com.example.finance.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TempPasswordVO {
    private String username;
    private String tempPassword;
}

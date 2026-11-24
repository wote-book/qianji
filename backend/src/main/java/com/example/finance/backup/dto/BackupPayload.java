package com.example.finance.backup.dto;

import com.example.finance.account.entity.Account;
import com.example.finance.budget.entity.Budget;
import com.example.finance.category.entity.Category;
import com.example.finance.goal.entity.Goal;
import com.example.finance.record.entity.Record;
import lombok.Data;

import java.util.List;

@Data
public class BackupPayload {
    private String schemaVersion = "1.0";
    private String exportTime;
    private List<Account> accounts;
    private List<Category> categories;
    private List<Record> records;
    private List<Budget> budgets;
    private List<Goal> goals;
}

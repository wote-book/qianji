package com.example.finance.onboarding.service;

import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.budget.mapper.BudgetMapper;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.SessionContext;
import com.example.finance.goal.mapper.GoalMapper;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final RecordMapper recordMapper;
    private final BudgetMapper budgetMapper;
    private final GoalMapper goalMapper;

    public Map<String, Object> status(Long userId) {
        boolean hasAccount = !accountMapper.selectByUserId(userId).isEmpty();
        boolean hasCategory = !categoryMapper.selectByUser(userId, null).isEmpty();
        boolean hasRecord = recordMapper.countByUser(userId) > 0;
        boolean hasBudget = !budgetMapper.selectByMonth(userId, YearMonth.now().toString()).isEmpty();
        boolean hasGoal = !goalMapper.selectByUser(userId).isEmpty();

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("account", "创建账户", hasAccount));
        steps.add(step("category", "确认分类", hasCategory));
        steps.add(step("record", "记录第一笔", hasRecord));
        steps.add(step("budget", "设置本月预算", hasBudget));
        steps.add(step("goal", "建立理财目标", hasGoal));

        Object sessionFlag = SessionContext.session().getAttribute(SessionContext.KEY_ONBOARDED);
        boolean completed = Boolean.TRUE.equals(sessionFlag) || hasRecord;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", completed);
        result.put("steps", steps);
        result.put("date", LocalDate.now().toString());
        return result;
    }

    public void complete() {
        SessionContext.session().setAttribute(SessionContext.KEY_ONBOARDED, Boolean.TRUE);
    }

    private Map<String, Object> step(String key, String title, boolean done) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("title", title);
        item.put("done", done);
        return item;
    }
}

package com.example.finance.config;

import com.example.finance.account.dto.AccountRequest;
import com.example.finance.account.entity.Account;
import com.example.finance.account.service.AccountService;
import com.example.finance.admin.entity.LoginLog;
import com.example.finance.admin.entity.UsageEvent;
import com.example.finance.admin.mapper.LoginLogMapper;
import com.example.finance.admin.mapper.UsageEventMapper;
import com.example.finance.budget.dto.BudgetRequest;
import com.example.finance.budget.service.BudgetService;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.category.service.CategoryService;
import com.example.finance.goal.dto.DepositRequest;
import com.example.finance.goal.dto.GoalRequest;
import com.example.finance.goal.entity.Goal;
import com.example.finance.goal.service.GoalService;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.mapper.RecordMapper;
import com.example.finance.record.service.RecordService;
import com.example.finance.user.entity.User;
import com.example.finance.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * 首次启动初始化：管理员、演示用户与近 4 个月演示数据。
 * 已存在则跳过，避免重复写入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final AccountService accountService;
    private final RecordService recordService;
    private final RecordMapper recordMapper;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final LoginLogMapper loginLogMapper;
    private final UsageEventMapper usageEventMapper;

    private static final String[][] MOCK_USERS = {
            {"chenxi", "陈曦"}, {"linyu", "林雨"}, {"wangzhe", "王哲"}, {"zhaomin", "赵敏"},
            {"sunhao", "孙浩"}, {"liqian", "李倩"}, {"zhouyang", "周洋"}, {"wuqi", "吴琪"},
            {"xiaotong", "晓彤"}, {"hejun", "何俊"}
    };
    private static final int[] MOCK_REG_OFFSETS = {58, 52, 45, 38, 30, 24, 18, 40, 5, 3};
    private static final int[] MOCK_STATUS = {1, 1, 1, 1, 1, 1, 0, 0, 1, 1};
    private static final boolean[] MOCK_RETENTION = {true, true, true, false, false, true, true, false, false, false};
    private static final int[][] MOCK_ACTIVE_OFFSETS = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 17, 20, 23, 26, 29},
            {0, 1, 2, 3, 4, 5, 6},
            {1, 3, 6, 9, 13, 18},
            {20, 22, 25, 28},
            {25, 28},
            {0, 2, 4, 9, 15},
            {1, 4, 8, 12, 20},
            {26, 29},
            {},
            {}
    };
    private static final String[][] MODULE_ACTIONS = {
            {"RECORD", "CREATE"}, {"RECORD", "VIEW"}, {"BUDGET", "VIEW"}, {"BUDGET", "UPDATE"},
            {"STATS", "VIEW"}, {"GOAL", "CREATE"}, {"BRIEF", "GENERATE"}, {"ACCOUNT", "VIEW"},
            {"CATEGORY", "CREATE"}, {"REPORT", "EXPORT"}, {"IMPORT", "CREATE"}, {"BACKUP", "EXPORT"}
    };

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            if (userMapper.selectByUsername("admin") == null) {
                createUser("admin", "admin123", "系统管理员", "ADMIN", null);
                log.info("已初始化管理员账号 admin / admin123");
            }
            if (userMapper.selectByUsername("demo") == null) {
                User demo = createUser("demo", "123456", "演示用户", "USER", "demo");
                seedDemoData(demo.getId());
                log.info("已初始化演示账号 demo / 123456");
            } else {
                User demo = userMapper.selectByUsername("demo");
                if (recordMapper.countByUser(demo.getId()) == 0) {
                    seedDemoData(demo.getId());
                }
            }
            if (userMapper.selectByUsername(MOCK_USERS[0][0]) == null) {
                seedMockUsers();
            }
        } catch (Exception e) {
            log.warn("种子数据初始化失败：{}", e.getMessage(), e);
        }
    }

    private User createUser(String username, String password, String nickname, String role, String answer) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        if (answer != null) {
            user.setSecurityQuestion("演示账号找回密码答案");
            user.setSecurityAnswer(passwordEncoder.encode(answer));
        }
        userMapper.insert(user);
        if ("USER".equals(role)) {
            categoryService.initPresets(user.getId());
        }
        return user;
    }

    private void seedDemoData(Long userId) {
        Account bank = accountService.create(userId,
                new AccountRequest("招商银行储蓄卡", "BANK", new BigDecimal("12000.00"), Boolean.FALSE));
        Account alipay = accountService.create(userId,
                new AccountRequest("支付宝", "ALIPAY", new BigDecimal("20000.00"), Boolean.FALSE));
        Account cash = accountService.create(userId,
                new AccountRequest("现金", "CASH", new BigDecimal("5000.00"), Boolean.FALSE));

        Random random = new Random(20260601L);
        YearMonth current = YearMonth.now();
        for (int offset = 3; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            boolean currentMonth = offset == 0;
            int lastDay = currentMonth ? Math.min(month.lengthOfMonth(), LocalDate.now().getDayOfMonth())
                    : month.lengthOfMonth();

            add(userId, bank, "工资", "INCOME", 15000, day(month, 1), "月度工资");
            add(userId, bank, "居住", "EXPENSE", 4500, day(month, 3), "房租");
            add(userId, bank, "居住", "EXPENSE", 260 + random.nextInt(180), day(month, 8), "水电燃气");
            for (int i = 0; i < 10; i++) {
                int d = 1 + random.nextInt(lastDay);
                add(userId, i % 3 == 0 ? cash : alipay, "餐饮", "EXPENSE",
                        18 + random.nextInt(90), day(month, d), "日常餐饮");
            }
            for (int i = 0; i < 6; i++) {
                int d = 1 + random.nextInt(lastDay);
                add(userId, alipay, "交通", "EXPENSE", 5 + random.nextInt(55),
                        day(month, d), "通勤交通");
            }
            for (int i = 0; i < 3; i++) {
                int d = 1 + random.nextInt(lastDay);
                add(userId, alipay, "购物", "EXPENSE", 100 + random.nextInt(500),
                        day(month, d), "生活用品");
            }
            add(userId, alipay, "娱乐", "EXPENSE", 60 + random.nextInt(240),
                    day(month, 12 + random.nextInt(10)), "电影与娱乐");
            if (offset % 2 == 0) {
                add(userId, bank, "医疗", "EXPENSE", 180 + random.nextInt(400),
                        day(month, 15), "药品与门诊");
                add(userId, bank, "理财收益", "INCOME", 120 + random.nextInt(300),
                        day(month, 20), "货币基金收益");
            }
        }

        String month = current.toString();
        budgetService.upsert(userId, new BudgetRequest(month, 0L, new BigDecimal("8000.00"), 80));
        budgetService.upsert(userId, new BudgetRequest(month, categoryId(userId, "EXPENSE", "餐饮"),
                new BigDecimal("2000.00"), 80));
        budgetService.upsert(userId, new BudgetRequest(month, categoryId(userId, "EXPENSE", "交通"),
                new BigDecimal("500.00"), 80));
        budgetService.upsert(userId, new BudgetRequest(month, categoryId(userId, "EXPENSE", "购物"),
                new BigDecimal("1000.00"), 80));
        budgetService.upsert(userId, new BudgetRequest(month, categoryId(userId, "EXPENSE", "居住"),
                new BigDecimal("5000.00"), 85));

        Goal travel = goalService.create(userId, new GoalRequest("旅行基金",
                new BigDecimal("20000.00"), current.minusMonths(2).atDay(1)));
        goalService.deposit(userId, travel.getId(), new DepositRequest(new BigDecimal("6000.00")));
        Goal emergency = goalService.create(userId, new GoalRequest("应急备用金",
                new BigDecimal("50000.00"), current.minusMonths(3).atDay(1)));
        goalService.deposit(userId, emergency.getId(), new DepositRequest(new BigDecimal("12000.00")));
    }

    private void add(Long userId, Account account, String categoryName, String type,
                     int amount, LocalDate date, String remark) {
        if (date == null) {
            return;
        }
        Long categoryId = categoryId(userId, type, categoryName);
        if (categoryId == null) {
            return;
        }
        recordService.create(userId, new RecordRequest(account.getId(), categoryId,
                BigDecimal.valueOf(amount), type, date, remark));
    }

    private Long categoryId(Long userId, String type, String name) {
        Category category = categoryMapper.selectByUserTypeName(userId, type, name);
        return category == null ? null : category.getId();
    }

    private LocalDate day(YearMonth month, int day) {
        if (day < 1 || day > month.lengthOfMonth()) {
            return null;
        }
        LocalDate date = month.atDay(day);
        return date.isAfter(LocalDate.now()) ? null : date;
    }

    private void seedMockUsers() {
        Random random = new Random(20261123L);
        String encoded = passwordEncoder.encode("123456");
        LocalDate today = LocalDate.now();
        List<User> created = new ArrayList<>();

        for (int i = 0; i < MOCK_USERS.length; i++) {
            User user = new User();
            user.setUsername(MOCK_USERS[i][0]);
            user.setNickname(MOCK_USERS[i][1]);
            user.setPassword(encoded);
            user.setRole("USER");
            user.setStatus(MOCK_STATUS[i]);
            userMapper.insert(user);
            LocalDate regDate = today.minusDays(MOCK_REG_OFFSETS[i]);
            userMapper.updateCreateTime(user.getId(),
                    regDate.atTime(8 + random.nextInt(10), random.nextInt(60)));
            created.add(user);
        }

        for (int i = 0; i < created.size(); i++) {
            seedMockActivity(created.get(i), today.minusDays(MOCK_REG_OFFSETS[i]),
                    MOCK_ACTIVE_OFFSETS[i], MOCK_RETENTION[i], random);
        }
        log.info("已初始化 {} 个模拟用户与近 60 天行为数据", created.size());
    }

    private void seedMockActivity(User user, LocalDate regDate, int[] recentOffsets,
                                  boolean seedRetention, Random random) {
        if (recentOffsets.length == 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new TreeSet<>();
        for (int offset : recentOffsets) {
            LocalDate date = today.minusDays(offset);
            if (!date.isBefore(regDate)) {
                days.add(date);
            }
        }
        if (seedRetention) {
            for (int retentionDay : new int[]{1, 7, 30}) {
                LocalDate date = regDate.plusDays(retentionDay);
                if (!date.isAfter(today)) {
                    days.add(date);
                }
            }
        }
        if (days.isEmpty()) {
            return;
        }

        String ip = "192.168.1." + (10 + random.nextInt(200));
        LocalDateTime lastLogin = null;
        for (LocalDate date : days) {
            LocalDateTime loginTime = randomTime(date, random);

            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(user.getId());
            loginLog.setUsername(user.getUsername());
            loginLog.setIp(ip);
            loginLog.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/"
                    + (100 + random.nextInt(30)) + ".0");
            loginLog.setSuccess(1);
            loginLog.setCreateTime(loginTime);
            loginLogMapper.insert(loginLog);

            if (lastLogin == null || loginTime.isAfter(lastLogin)) {
                lastLogin = loginTime;
            }

            int eventCount = 2 + random.nextInt(3);
            for (int k = 0; k < eventCount; k++) {
                String[] moduleAction = MODULE_ACTIONS[random.nextInt(MODULE_ACTIONS.length)];
                LocalDateTime eventTime = loginTime.plusMinutes(1 + random.nextInt(90));
                if (eventTime.isAfter(LocalDateTime.now())) {
                    eventTime = LocalDateTime.now().minusMinutes(1 + random.nextInt(30));
                }
                if (eventTime.isBefore(date.atStartOfDay())) {
                    eventTime = date.atStartOfDay().plusMinutes(1);
                }

                UsageEvent event = new UsageEvent();
                event.setUserId(user.getId());
                event.setModule(moduleAction[0]);
                event.setAction(moduleAction[1]);
                event.setIp(ip);
                event.setCreateTime(eventTime);
                usageEventMapper.insert(event);
            }
        }
        userMapper.updateLastLogin(user.getId(), lastLogin, ip);
    }

    private LocalDateTime randomTime(LocalDate date, Random random) {
        if (date.equals(LocalDate.now())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime candidate = now.minusMinutes(10 + random.nextInt(120));
            return candidate.isBefore(date.atStartOfDay()) ? now.minusMinutes(1) : candidate;
        }
        return date.atTime(8 + random.nextInt(14), random.nextInt(60));
    }
}

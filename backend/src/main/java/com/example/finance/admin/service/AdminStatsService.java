package com.example.finance.admin.service;

import com.example.finance.admin.dto.ActiveUserDay;
import com.example.finance.admin.dto.DailyCount;
import com.example.finance.admin.dto.ModuleUsage;
import com.example.finance.admin.dto.OverviewVO;
import com.example.finance.admin.dto.RetentionCohort;
import com.example.finance.admin.dto.RetentionRaw;
import com.example.finance.admin.dto.SegmentItem;
import com.example.finance.admin.dto.TrendPoint;
import com.example.finance.admin.dto.UsageTrendVO;
import com.example.finance.admin.dto.UserActivity;
import com.example.finance.admin.mapper.AdminStatsMapper;
import com.example.finance.admin.mapper.LoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    public static final Map<String, String> MODULE_LABELS = new LinkedHashMap<>();

    static {
        MODULE_LABELS.put("RECORD", "收支流水");
        MODULE_LABELS.put("ACCOUNT", "账户与对账");
        MODULE_LABELS.put("CATEGORY", "分类管理");
        MODULE_LABELS.put("BUDGET", "预算管理");
        MODULE_LABELS.put("STATS", "统计分析");
        MODULE_LABELS.put("GOAL", "理财目标");
        MODULE_LABELS.put("BRIEF", "智能简报");
        MODULE_LABELS.put("REPORT", "报表导出");
        MODULE_LABELS.put("IMPORT", "CSV 导入");
        MODULE_LABELS.put("BACKUP", "备份恢复");
    }

    private final AdminStatsMapper statsMapper;
    private final LoginLogMapper loginLogMapper;

    public OverviewVO overview(int days) {
        int window = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long enabled = statsMapper.countUsersByStatus(1);
        long disabled = statsMapper.countUsersByStatus(0);
        long monthActive = statsMapper.countActiveUsersSince(monthStart);

        OverviewVO vo = new OverviewVO();
        vo.setTotalUsers(enabled + disabled);
        vo.setEnabledUsers(enabled);
        vo.setDisabledUsers(disabled);
        vo.setTodayActiveUsers(statsMapper.countTodayActiveUsers());
        vo.setNewUsers7d(statsMapper.countNewUsersSince(today.minusDays(6)));
        vo.setMonthRecordCount(statsMapper.countRecordsSince(monthStart));
        vo.setMonthBriefCount(statsMapper.countEventsSince("BRIEF", monthStart));
        vo.setMonthActiveRate(enabled == 0 ? 0 : round1(monthActive * 100.0 / enabled));
        vo.setTrend(trend(window));
        vo.setModules(modules(window));
        vo.setSegments(segments());
        vo.setRecentLogins(loginLogMapper.selectRecent(5));
        return vo;
    }

    public List<TrendPoint> trend(int days) {
        int window = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(window - 1L);

        Map<LocalDate, Long> active = toMap(statsMapper.selectDailyActive(start, today));
        Map<LocalDate, Long> created = toMap(statsMapper.selectDailyNewUsers(start, today));

        List<TrendPoint> list = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            LocalDate date = start.plusDays(i);
            list.add(new TrendPoint(date.toString(),
                    active.getOrDefault(date, 0L),
                    created.getOrDefault(date, 0L)));
        }
        return list;
    }

    public List<ModuleUsage> modules(int days) {
        int window = Math.min(Math.max(days, 7), 90);
        LocalDate start = LocalDate.now().minusDays(window - 1L);
        long enabled = Math.max(statsMapper.countUsersByStatus(1), 1);

        List<ModuleUsage> list = statsMapper.selectModuleUsage(start);
        for (ModuleUsage item : list) {
            item.setLabel(MODULE_LABELS.getOrDefault(item.getModule(), item.getModule()));
            item.setRate(round1(item.getUsers() * 100.0 / enabled));
        }
        return list;
    }

    public UsageTrendVO usageTrend(int days) {
        int window = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(window - 1L + 29);

        Map<LocalDate, Set<Long>> dayUsers = new HashMap<>();
        for (ActiveUserDay row : statsMapper.selectActiveUserDayRows(start)) {
            if (row.getDay() == null || row.getUserId() == null) {
                continue;
            }
            dayUsers.computeIfAbsent(row.getDay(), k -> new HashSet<>()).add(row.getUserId());
        }

        List<String> labels = new ArrayList<>(window);
        List<Long> dau = new ArrayList<>(window);
        List<Long> wau = new ArrayList<>(window);
        List<Long> mau = new ArrayList<>(window);

        for (int i = 0; i < window; i++) {
            LocalDate date = today.minusDays(window - 1L - i);
            labels.add(date.toString());
            dau.add((long) dayUsers.getOrDefault(date, Set.of()).size());
            wau.add(rolling(dayUsers, date, 7));
            mau.add(rolling(dayUsers, date, 30));
        }
        return new UsageTrendVO(labels, dau, wau, mau);
    }

    public List<RetentionCohort> retention() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusWeeks(8);

        List<RetentionCohort> list = new ArrayList<>();
        for (RetentionRaw raw : statsMapper.selectRetention(start)) {
            RetentionCohort item = new RetentionCohort();
            item.setCohortDate(raw.getCohortDate());
            item.setSize(raw.getSize());
            long age = ChronoUnit.DAYS.between(raw.getCohortDate(), today);
            item.setD1(age >= 1 && raw.getSize() > 0 ? round1(raw.getD1() * 100.0 / raw.getSize()) : null);
            item.setD7(age >= 7 && raw.getSize() > 0 ? round1(raw.getD7() * 100.0 / raw.getSize()) : null);
            item.setD30(age >= 30 && raw.getSize() > 0 ? round1(raw.getD30() * 100.0 / raw.getSize()) : null);
            list.add(item);
        }
        return list;
    }

    public List<SegmentItem> segments() {
        LocalDate today = LocalDate.now();
        long high = 0;
        long normal = 0;
        long dormant = 0;
        long inactive = 0;

        for (UserActivity row : statsMapper.selectUserActivity()) {
            LocalDate last = row.getLastActiveDay();
            if (last == null) {
                inactive++;
            } else if (row.getDays7() >= 5) {
                high++;
            } else if (last.isBefore(today.minusDays(14))) {
                dormant++;
            } else {
                normal++;
            }
        }

        long total = Math.max(high + normal + dormant + inactive, 1);
        List<SegmentItem> list = new ArrayList<>();
        list.add(new SegmentItem("HIGH", "高活跃", high, round1(high * 100.0 / total)));
        list.add(new SegmentItem("NORMAL", "普通", normal, round1(normal * 100.0 / total)));
        list.add(new SegmentItem("DORMANT", "沉睡", dormant, round1(dormant * 100.0 / total)));
        list.add(new SegmentItem("INACTIVE", "未激活", inactive, round1(inactive * 100.0 / total)));
        return list;
    }

    private long rolling(Map<LocalDate, Set<Long>> dayUsers, LocalDate end, int window) {
        Set<Long> users = new HashSet<>();
        for (int i = 0; i < window; i++) {
            users.addAll(dayUsers.getOrDefault(end.minusDays(i), Set.of()));
        }
        return users.size();
    }

    private Map<LocalDate, Long> toMap(List<DailyCount> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (DailyCount row : rows) {
            if (row.getDate() != null) {
                map.put(row.getDate(), row.getValue());
            }
        }
        return map;
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}

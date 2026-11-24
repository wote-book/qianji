package com.example.finance.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class OverviewVO {
    private long totalUsers;
    private long enabledUsers;
    private long disabledUsers;
    private long todayActiveUsers;
    private long newUsers7d;
    private long monthRecordCount;
    private long monthBriefCount;
    private double monthActiveRate;
    private List<TrendPoint> trend;
    private List<ModuleUsage> modules;
    private List<SegmentItem> segments;
    private List<LoginLogVO> recentLogins;
}

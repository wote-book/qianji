package com.example.finance.admin.service;

import com.example.finance.admin.dto.LoginLogVO;
import com.example.finance.admin.dto.UsageEventVO;
import com.example.finance.admin.mapper.LoginLogMapper;
import com.example.finance.admin.mapper.UsageEventMapper;
import com.example.finance.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final LoginLogMapper loginLogMapper;
    private final UsageEventMapper usageEventMapper;

    public PageResult<LoginLogVO> loginLogs(String username, Integer success, LocalDate start, LocalDate end,
                                            int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        return new PageResult<>(
                loginLogMapper.selectPage(username, success, start, end, (page - 1) * size, size),
                loginLogMapper.countPage(username, success, start, end),
                page, size);
    }

    public PageResult<UsageEventVO> usageEvents(String username, String module, LocalDate start, LocalDate end,
                                                int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        return new PageResult<>(
                usageEventMapper.selectPage(username, module, start, end, (page - 1) * size, size),
                usageEventMapper.countPage(username, module, start, end),
                page, size);
    }
}

package com.example.finance.admin.service;

import com.example.finance.admin.dto.TempPasswordVO;
import com.example.finance.admin.dto.UserDetailVO;
import com.example.finance.admin.dto.UserQuery;
import com.example.finance.admin.dto.UserRowVO;
import com.example.finance.admin.mapper.AdminStatsMapper;
import com.example.finance.common.BizException;
import com.example.finance.common.PageResult;
import com.example.finance.user.entity.User;
import com.example.finance.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdminStatsMapper statsMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PageResult<UserRowVO> page(UserQuery query, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.min(Math.max(pageSize, 1), 100);
        List<UserRowVO> list = statsMapper.selectUserPage(query, (page - 1) * size, size);
        long total = statsMapper.countUserPage(query);
        return new PageResult<>(list, total, page, size);
    }

    public UserDetailVO detail(Long id) {
        UserDetailVO detail = statsMapper.selectUserDetail(id);
        if (detail == null) {
            throw BizException.notFound();
        }
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态只能为 0 或 1");
        }
        User target = requireUser(id);
        if ("ADMIN".equals(target.getRole()) && status == 0) {
            throw BizException.conflict("不能禁用管理员账号");
        }
        userMapper.updateStatus(id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public TempPasswordVO resetPassword(Long id) {
        User target = requireUser(id);
        if ("ADMIN".equals(target.getRole())) {
            throw BizException.conflict("不能重置管理员账号密码");
        }
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(TEMP_CHARS.charAt(random.nextInt(TEMP_CHARS.length())));
        }
        String tempPassword = sb.toString();
        userMapper.updatePassword(id, passwordEncoder.encode(tempPassword));
        return new TempPasswordVO(target.getUsername(), tempPassword);
    }

    public byte[] exportCsv(UserQuery query) {
        List<UserRowVO> rows = statsMapper.selectUserExport(query);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("ID,用户名,昵称,角色,状态,注册时间,最近登录,最近登录IP,近30天活跃天数,流水笔数\n");
        for (UserRowVO row : rows) {
            sb.append(csv(row.getId()))
                    .append(',').append(csv(row.getUsername()))
                    .append(',').append(csv(row.getNickname()))
                    .append(',').append(csv("ADMIN".equals(row.getRole()) ? "管理员" : "普通用户"))
                    .append(',').append(csv(row.getStatus() != null && row.getStatus() == 1 ? "启用" : "禁用"))
                    .append(',').append(csv(format(row.getCreateTime())))
                    .append(',').append(csv(format(row.getLastLoginTime())))
                    .append(',').append(csv(row.getLastLoginIp()))
                    .append(',').append(row.getActiveDays30())
                    .append(',').append(row.getRecordCount())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound();
        }
        return user;
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}

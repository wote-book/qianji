package com.example.finance.user.service;

import com.example.finance.admin.entity.LoginLog;
import com.example.finance.admin.mapper.LoginLogMapper;
import com.example.finance.category.service.CategoryService;
import com.example.finance.common.BizException;
import com.example.finance.common.RequestUtils;
import com.example.finance.common.SessionContext;
import com.example.finance.user.dto.*;
import com.example.finance.user.entity.User;
import com.example.finance.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final CategoryService categoryService;
    private final PasswordEncoder passwordEncoder;
    private final LoginLogMapper loginLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        if (userMapper.selectByUsername(request.username()) != null) {
            throw BizException.conflict("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        user.setRole("USER");
        user.setStatus(1);
        if (StringUtils.hasText(request.securityQuestion()) && StringUtils.hasText(request.securityAnswer())) {
            user.setSecurityQuestion(request.securityQuestion().trim());
            user.setSecurityAnswer(passwordEncoder.encode(request.securityAnswer().trim()));
        }
        userMapper.insert(user);
        categoryService.initPresets(user.getId());
        putSession(user);
        return UserVO.of(user);
    }

    public UserVO login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            writeLoginLog(user == null ? null : user.getId(), request.username(), 0);
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            writeLoginLog(user.getId(), user.getUsername(), 0);
            throw BizException.forbidden("账号已被禁用，请联系管理员");
        }
        String ip = RequestUtils.clientIp(SessionContext.request());
        userMapper.updateLastLogin(user.getId(), LocalDateTime.now(), ip);
        writeLoginLog(user.getId(), user.getUsername(), 1);
        putSession(user);
        return UserVO.of(user);
    }

    public void logout() {
        SessionContext.session().invalidate();
    }

    public UserVO me() {
        User user = userMapper.selectById(SessionContext.userId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        return UserVO.of(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(ProfileRequest request) {
        userMapper.updateProfile(SessionContext.userId(), request.nickname().trim());
        SessionContext.session().setAttribute(SessionContext.KEY_NICKNAME, request.nickname().trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        User user = userMapper.selectById(SessionContext.userId());
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        SessionContext.session().invalidate();
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetRequest request) {
        User user = userMapper.selectByUsername(request.username());
        if (user == null) {
            throw new BizException("用户名或安全问题答案不正确");
        }
        if (!StringUtils.hasText(user.getSecurityAnswer())
                || !passwordEncoder.matches(request.securityAnswer().trim(), user.getSecurityAnswer())) {
            throw new BizException("用户名或安全问题答案不正确");
        }
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfileSecurity(Long userId, String question, String answer) {
        userMapper.updateSecurity(userId,
                StringUtils.hasText(question) ? question.trim() : null,
                StringUtils.hasText(answer) ? passwordEncoder.encode(answer.trim()) : null);
    }

    private void writeLoginLog(Long userId, String username, int success) {
        HttpServletRequest request = SessionContext.request();
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(RequestUtils.clientIp(request));
        log.setUserAgent(RequestUtils.userAgent(request));
        log.setSuccess(success);
        loginLogMapper.insert(log);
    }

    private void putSession(User user) {
        HttpSession session = SessionContext.session();
        session.setAttribute(SessionContext.KEY_USER_ID, user.getId());
        session.setAttribute(SessionContext.KEY_USERNAME, user.getUsername());
        session.setAttribute(SessionContext.KEY_NICKNAME, user.getNickname());
        session.setAttribute(SessionContext.KEY_ROLE, user.getRole());
    }
}

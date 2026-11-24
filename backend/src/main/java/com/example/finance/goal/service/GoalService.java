package com.example.finance.goal.service;

import com.example.finance.common.BizException;
import com.example.finance.goal.dto.DepositRequest;
import com.example.finance.goal.dto.GoalRequest;
import com.example.finance.goal.entity.Goal;
import com.example.finance.goal.mapper.GoalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalMapper goalMapper;

    public List<Goal> listWithProgress(Long userId) {
        List<Goal> goals = goalMapper.selectByUser(userId);
        goals.forEach(this::fillProgress);
        return goals;
    }

    @Transactional(rollbackFor = Exception.class)
    public Goal create(Long userId, GoalRequest request) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setName(request.name().trim());
        goal.setTargetAmt(request.targetAmt());
        goal.setSavedAmt(BigDecimal.ZERO);
        goal.setStartDate(request.startDate() == null ? LocalDate.now() : request.startDate());
        goal.setStatus("DOING");
        goalMapper.insert(goal);
        fillProgress(goal);
        return goal;
    }

    /**
     * 手工累加：不生成流水、不改账户余额、不计入收支统计（Q-14）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Goal deposit(Long userId, Long id, DepositRequest request) {
        Goal goal = getOwned(userId, id);
        if ("DONE".equals(goal.getStatus())) {
            throw BizException.conflict("该目标已完成，不能再存入");
        }
        BigDecimal next = goal.getSavedAmt().add(request.amount());
        if (next.compareTo(goal.getTargetAmt()) > 0) {
            throw new BizException("存入后超过目标金额，剩余可存 "
                    + goal.getTargetAmt().subtract(goal.getSavedAmt()).setScale(2, RoundingMode.HALF_UP) + " 元");
        }
        String status = next.compareTo(goal.getTargetAmt()) >= 0 ? "DONE" : "DOING";
        goalMapper.updateSaved(id, userId, next, status);
        goal.setSavedAmt(next);
        goal.setStatus(status);
        fillProgress(goal);
        return goal;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, GoalRequest request) {
        Goal goal = getOwned(userId, id);
        if (request.targetAmt().compareTo(goal.getSavedAmt()) < 0) {
            throw new BizException("目标金额不能小于已存金额");
        }
        goalMapper.updateBase(id, userId, request.name().trim(), request.targetAmt());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        goalMapper.delete(id, userId);
    }

    private Goal getOwned(Long userId, Long id) {
        Goal goal = goalMapper.selectByIdAndUser(id, userId);
        if (goal == null) {
            throw BizException.notFound();
        }
        return goal;
    }

    private void fillProgress(Goal goal) {
        BigDecimal target = goal.getTargetAmt();
        BigDecimal saved = goal.getSavedAmt();
        if (target != null && target.compareTo(BigDecimal.ZERO) > 0) {
            goal.setPercent(saved.multiply(BigDecimal.valueOf(100)).divide(target, 1, RoundingMode.HALF_UP));
        }
        if (goal.getStartDate() == null) {
            return;
        }
        long days = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now()) + 1;
        if (days <= 0) {
            days = 1;
        }
        if (saved.compareTo(BigDecimal.ZERO) <= 0) {
            goal.setExpectedDate(null);
            goal.setRemainDays(null);
            return;
        }
        BigDecimal dailyRate = saved.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
        BigDecimal remaining = target.subtract(saved);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            goal.setExpectedDate(LocalDate.now());
            goal.setRemainDays(0L);
            return;
        }
        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            goal.setExpectedDate(null);
            goal.setRemainDays(null);
            return;
        }
        long needDays = remaining.divide(dailyRate, 0, RoundingMode.CEILING).longValue();
        goal.setRemainDays(needDays);
        goal.setExpectedDate(LocalDate.now().plusDays(needDays));
    }
}

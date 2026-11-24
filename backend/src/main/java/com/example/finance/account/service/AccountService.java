package com.example.finance.account.service;

import com.example.finance.account.dto.AccountDiff;
import com.example.finance.account.dto.AccountRequest;
import com.example.finance.account.entity.Account;
import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.common.BizException;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final RecordMapper recordMapper;

    public List<Account> list(Long userId) {
        return accountMapper.selectByUserId(userId);
    }

    public Account getOwned(Long userId, Long id) {
        Account account = accountMapper.selectByIdAndUser(id, userId);
        if (account == null) {
            throw BizException.notFound();
        }
        return account;
    }

    @Transactional(rollbackFor = Exception.class)
    public Account create(Long userId, AccountRequest request) {
        if (accountMapper.selectByName(userId, request.accName().trim()) != null) {
            throw BizException.conflict("同名账户已存在");
        }
        BigDecimal init = request.initBalance() == null ? BigDecimal.ZERO : request.initBalance();
        if (init.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("初始余额不能为负数");
        }
        Account account = new Account();
        account.setUserId(userId);
        account.setAccName(request.accName().trim());
        account.setType(request.type());
        account.setInitBalance(init);
        account.setBalance(init);
        account.setAllowOverdraft(Boolean.TRUE.equals(request.allowOverdraft()) ? 1 : 0);
        accountMapper.insert(account);
        return account;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, AccountRequest request) {
        getOwned(userId, id);
        Account sameName = accountMapper.selectByName(userId, request.accName().trim());
        if (sameName != null && !sameName.getId().equals(id)) {
            throw BizException.conflict("同名账户已存在");
        }
        accountMapper.updateBasic(id, userId, request.accName().trim(), request.type(),
                Boolean.TRUE.equals(request.allowOverdraft()) ? 1 : 0);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        getOwned(userId, id);
        int count = recordMapper.countByAccount(userId, id);
        if (count > 0) {
            throw BizException.conflict("该账户存在 " + count + " 条流水，不能删除");
        }
        accountMapper.delete(id, userId);
    }

    public List<AccountDiff> reconcile(Long userId) {
        return accountMapper.selectDiffs(userId);
    }

    /**
     * 对账修复：唯一允许写 balance 的非流水入口，且只能写「期初 + 流水重算值」。
     */
    @Transactional(rollbackFor = Exception.class)
    public int fix(Long userId) {
        List<AccountDiff> diffs = accountMapper.selectDiffs(userId);
        int fixed = 0;
        for (AccountDiff diff : diffs) {
            if (diff.getDiff() == null || diff.getDiff().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            log.warn("对账修复 userId={}, account={}, from={}, to={}",
                    userId, diff.getAccName(), diff.getBalance(), diff.getCalc());
            accountMapper.updateBalanceTo(userId, diff.getId(), diff.getCalc());
            fixed++;
        }
        return fixed;
    }
}

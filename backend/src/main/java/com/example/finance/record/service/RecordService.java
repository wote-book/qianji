package com.example.finance.record.service;

import com.example.finance.account.entity.Account;
import com.example.finance.account.mapper.AccountMapper;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.common.PageResult;
import com.example.finance.record.dto.RecordQuery;
import com.example.finance.record.dto.RecordRequest;
import com.example.finance.record.entity.Record;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final RecordMapper recordMapper;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<Record> page(Long userId, RecordQuery query) {
        List<Record> list = recordMapper.selectPage(query, userId);
        long total = recordMapper.count(query, userId);
        return new PageResult<>(list, total, query.getPageNum(), query.getLimit());
    }

    public Record getOwned(Long userId, Long id) {
        Record record = recordMapper.selectByIdAndUser(id, userId);
        if (record == null) {
            throw BizException.notFound();
        }
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public Record create(Long userId, RecordRequest request) {
        return doCreate(userId, request, "MANUAL");
    }

    @Transactional(rollbackFor = Exception.class)
    public Record createWithSource(Long userId, RecordRequest request, String source) {
        return doCreate(userId, request, source);
    }

    private Record doCreate(Long userId, RecordRequest request, String source) {
        validateAccountAndCategory(userId, request);
        applyDelta(userId, request.accountId(), signed(request.type(), request.amount()));
        Record record = new Record();
        record.setUserId(userId);
        record.setAccountId(request.accountId());
        record.setCategoryId(request.categoryId());
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setRecordDate(request.recordDate());
        record.setRemark(request.remark());
        record.setSource(source);
        recordMapper.insert(record);
        return record;
    }

    /**
     * 导入/恢复专用：额外写入来源与去重指纹，余额仍按原子更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public Record createImported(Long userId, RecordRequest request, String source, String dedupKey) {
        validateAccountAndCategory(userId, request);
        applyDelta(userId, request.accountId(), signed(request.type(), request.amount()));
        Record record = new Record();
        record.setUserId(userId);
        record.setAccountId(request.accountId());
        record.setCategoryId(request.categoryId());
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setRecordDate(request.recordDate());
        record.setRemark(request.remark());
        record.setSource(source);
        record.setDedupKey(dedupKey);
        recordMapper.insert(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, RecordRequest request) {
        Record old = getOwned(userId, id);
        validateAccountAndCategory(userId, request);

        boolean sameAccount = old.getAccountId().equals(request.accountId());
        boolean sameType = old.getType().equals(request.type());

        if (sameAccount && sameType) {
            BigDecimal delta = signed(request.type(), request.amount())
                    .subtract(signed(old.getType(), old.getAmount()));
            applyDelta(userId, request.accountId(), delta);
        } else {
            // 跨账户或跨类型：删旧 + 建新（同一事务）
            applyDelta(userId, old.getAccountId(), signed(old.getType(), old.getAmount()).negate());
            applyDelta(userId, request.accountId(), signed(request.type(), request.amount()));
        }

        old.setAccountId(request.accountId());
        old.setCategoryId(request.categoryId());
        old.setAmount(request.amount());
        old.setType(request.type());
        old.setRecordDate(request.recordDate());
        old.setRemark(request.remark());
        recordMapper.update(old);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        Record old = getOwned(userId, id);
        applyDelta(userId, old.getAccountId(), signed(old.getType(), old.getAmount()).negate());
        recordMapper.delete(id, userId);
    }

    /**
     * 余额变更：支出走条件更新（校验透支），收入与回滚走无条件更新。
     */
    private void applyDelta(Long userId, Long accountId, BigDecimal delta) {
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        int affected;
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            affected = accountMapper.updateBalanceIfEnough(userId, accountId, delta);
            if (affected == 0) {
                throw new BizException("余额不足，且该账户不允许透支");
            }
        } else {
            affected = accountMapper.updateBalanceRaw(userId, accountId, delta);
            if (affected == 0) {
                throw BizException.notFound();
            }
        }
    }

    private BigDecimal signed(String type, BigDecimal amount) {
        return "INCOME".equals(type) ? amount : amount.negate();
    }

    private void validateAccountAndCategory(Long userId, RecordRequest request) {
        Account account = accountMapper.selectByIdAndUser(request.accountId(), userId);
        if (account == null) {
            throw BizException.notFound();
        }
        Category category = categoryMapper.selectByIdAndUser(request.categoryId(), userId);
        if (category == null) {
            throw BizException.notFound();
        }
        if (!category.getType().equals(request.type())) {
            throw new BizException("分类类型与收支类型不一致");
        }
        if (category.getStatus() != null && category.getStatus() == 0) {
            throw new BizException("该分类已停用，请选择其他分类");
        }
    }
}

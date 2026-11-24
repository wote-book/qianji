package com.example.finance.account.mapper;

import com.example.finance.account.dto.AccountDiff;
import com.example.finance.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountMapper {

    List<Account> selectByUserId(@Param("userId") Long userId);

    Account selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    Account selectByName(@Param("userId") Long userId, @Param("accName") String accName);

    int insert(Account account);

    int updateBasic(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("accName") String accName, @Param("type") String type,
                    @Param("allowOverdraft") Integer allowOverdraft);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 无条件更新余额（收入、回滚、对账修复） */
    int updateBalanceRaw(@Param("userId") Long userId, @Param("accountId") Long accountId,
                         @Param("delta") BigDecimal delta);

    /** 条件更新：不允许透支时余额不得为负 */
    int updateBalanceIfEnough(@Param("userId") Long userId, @Param("accountId") Long accountId,
                              @Param("delta") BigDecimal delta);

    int updateBalanceTo(@Param("userId") Long userId, @Param("accountId") Long accountId,
                        @Param("balance") BigDecimal balance);

    List<AccountDiff> selectDiffs(@Param("userId") Long userId);

    BigDecimal sumBalance(@Param("userId") Long userId);
}

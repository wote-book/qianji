package com.example.finance.budget.mapper;

import com.example.finance.budget.entity.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BudgetMapper {

    List<Budget> selectByMonth(@Param("userId") Long userId, @Param("month") String month);

    List<Budget> selectByUser(@Param("userId") Long userId);

    Budget selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    Budget selectByMonthAndCategory(@Param("userId") Long userId,
                                    @Param("month") String month,
                                    @Param("categoryId") Long categoryId);

    int upsert(Budget budget);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}

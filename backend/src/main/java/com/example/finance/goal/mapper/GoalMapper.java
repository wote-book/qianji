package com.example.finance.goal.mapper;

import com.example.finance.goal.entity.Goal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface GoalMapper {

    List<Goal> selectByUser(@Param("userId") Long userId);

    Goal selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Goal goal);

    int updateBase(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("name") String name, @Param("targetAmt") BigDecimal targetAmt);

    int updateSaved(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("savedAmt") BigDecimal savedAmt, @Param("status") String status);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}

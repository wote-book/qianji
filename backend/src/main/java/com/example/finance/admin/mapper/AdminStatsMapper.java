package com.example.finance.admin.mapper;

import com.example.finance.admin.dto.DailyCount;
import com.example.finance.admin.dto.ActiveUserDay;
import com.example.finance.admin.dto.ModuleUsage;
import com.example.finance.admin.dto.RetentionRaw;
import com.example.finance.admin.dto.UserActivity;
import com.example.finance.admin.dto.UserDetailVO;
import com.example.finance.admin.dto.UserQuery;
import com.example.finance.admin.dto.UserRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AdminStatsMapper {

    long countUsersByStatus(@Param("status") Integer status);

    long countTodayActiveUsers();

    long countNewUsersSince(@Param("start") LocalDate start);

    long countRecordsSince(@Param("start") LocalDate start);

    long countEventsSince(@Param("module") String module, @Param("start") LocalDate start);

    long countActiveUsersSince(@Param("start") LocalDate start);

    List<DailyCount> selectDailyActive(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<DailyCount> selectDailyNewUsers(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<ModuleUsage> selectModuleUsage(@Param("start") LocalDate start);

    List<ActiveUserDay> selectActiveUserDayRows(@Param("start") LocalDate start);

    List<UserActivity> selectUserActivity();

    List<RetentionRaw> selectRetention(@Param("start") LocalDate start);

    List<UserRowVO> selectUserPage(@Param("q") UserQuery query,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countUserPage(@Param("q") UserQuery query);

    List<UserRowVO> selectUserExport(@Param("q") UserQuery query);

    UserDetailVO selectUserDetail(@Param("id") Long id);
}

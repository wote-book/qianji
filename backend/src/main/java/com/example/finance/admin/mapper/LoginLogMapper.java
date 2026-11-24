package com.example.finance.admin.mapper;

import com.example.finance.admin.dto.LoginLogVO;
import com.example.finance.admin.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LoginLogMapper {

    int insert(LoginLog log);

    List<LoginLogVO> selectPage(@Param("username") String username,
                                @Param("success") Integer success,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end,
                                @Param("offset") int offset,
                                @Param("size") int size);

    long countPage(@Param("username") String username,
                   @Param("success") Integer success,
                   @Param("start") LocalDate start,
                   @Param("end") LocalDate end);

    List<LoginLogVO> selectRecent(@Param("limit") int limit);
}

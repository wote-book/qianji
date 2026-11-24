package com.example.finance.admin.mapper;

import com.example.finance.admin.dto.UsageEventVO;
import com.example.finance.admin.entity.UsageEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UsageEventMapper {

    int insert(UsageEvent event);

    List<UsageEventVO> selectPage(@Param("username") String username,
                                  @Param("module") String module,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    long countPage(@Param("username") String username,
                   @Param("module") String module,
                   @Param("start") LocalDate start,
                   @Param("end") LocalDate end);
}

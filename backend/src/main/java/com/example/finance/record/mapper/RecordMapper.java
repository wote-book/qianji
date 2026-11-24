package com.example.finance.record.mapper;

import com.example.finance.record.dto.RecordQuery;
import com.example.finance.record.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface RecordMapper {

    List<Record> selectPage(@Param("q") RecordQuery query, @Param("userId") Long userId);

    long count(@Param("q") RecordQuery query, @Param("userId") Long userId);

    Record selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    Record selectByDedupKey(@Param("userId") Long userId, @Param("dedupKey") String dedupKey);

    int insert(Record record);

    int update(Record record);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int countByAccount(@Param("userId") Long userId, @Param("accountId") Long accountId);

    int countByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    long countByUser(@Param("userId") Long userId);

    /** 区间收支合计：income / expense / net */
    Map<String, Object> sumByRange(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    /** 区间支出按分类汇总：categoryId / categoryName / amount */
    List<Map<String, Object>> sumExpenseByCategory(@Param("userId") Long userId,
                                                   @Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);

    /** 区间按类型、分类汇总 */
    List<Map<String, Object>> sumByCategory(@Param("userId") Long userId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end,
                                            @Param("type") String type);

    /** 按日趋势：day / income / expense */
    List<Map<String, Object>> trendByDay(@Param("userId") Long userId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    /** 按月趋势：month / income / expense */
    List<Map<String, Object>> trendByMonth(@Param("userId") Long userId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);

    List<Record> selectBetween(@Param("userId") Long userId,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end);

    List<Record> selectRecent(@Param("userId") Long userId, @Param("limit") int limit);

    /** 导出用：与查询同条件，不分页 */
    List<Record> selectForExport(@Param("q") RecordQuery query, @Param("userId") Long userId);
}

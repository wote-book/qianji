package com.example.finance.category.mapper;

import com.example.finance.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<Category> selectByUser(@Param("userId") Long userId, @Param("type") String type);

    Category selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    Category selectByUserTypeName(@Param("userId") Long userId,
                                  @Param("type") String type,
                                  @Param("name") String name);

    int insert(Category category);

    int update(@Param("id") Long id, @Param("userId") Long userId,
               @Param("name") String name, @Param("icon") String icon);

    int updateStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("status") Integer status);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}

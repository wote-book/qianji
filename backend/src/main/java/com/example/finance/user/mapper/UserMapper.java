package com.example.finance.user.mapper;

import com.example.finance.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int updateProfile(@Param("id") Long id, @Param("nickname") String nickname);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateSecurity(@Param("id") Long id,
                       @Param("question") String question,
                       @Param("answer") String answer);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateLastLogin(@Param("id") Long id,
                        @Param("lastLoginTime") LocalDateTime lastLoginTime,
                        @Param("lastLoginIp") String lastLoginIp);

    int updateCreateTime(@Param("id") Long id, @Param("createTime") LocalDateTime createTime);

    Integer selectStatusById(@Param("id") Long id);
}

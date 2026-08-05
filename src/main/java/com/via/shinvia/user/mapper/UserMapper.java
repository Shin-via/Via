package com.via.shinvia.user.mapper;

import com.via.shinvia.user.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    int insertUser(User user);
    boolean existsByLoginEmail(@Param("loginEmail") String loginEmail);
    User findByUserId(@Param("userId") Long userId);
    User findByLoginEmail(@Param("loginEmail") String loginEmail);
}

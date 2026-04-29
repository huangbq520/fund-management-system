package com.fund.mapper;

import com.fund.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Insert("INSERT INTO user (email, password, nickname, status, create_time, update_time) " +
            "VALUES (#{email}, #{password}, #{nickname}, 1, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET nickname = #{nickname}, update_time = NOW() WHERE id = #{id}")
    int updateNickname(User user);

    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    int countByEmail(@Param("email") String email);
}

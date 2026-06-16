package com.fund.mapper;

import com.fund.entity.WatchlistGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WatchlistGroupMapper {

    @Select("SELECT * FROM watchlist_group WHERE user_id = #{userId} ORDER BY sort_order ASC, create_time ASC")
    List<WatchlistGroup> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM watchlist_group WHERE id = #{id}")
    WatchlistGroup findById(@Param("id") Long id);

    @Insert("INSERT INTO watchlist_group (user_id, group_name, sort_order, create_time) " +
            "VALUES (#{userId}, #{groupName}, #{sortOrder}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WatchlistGroup group);

    @Update("UPDATE watchlist_group SET group_name = #{groupName} WHERE id = #{id} AND user_id = #{userId}")
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("groupName") String groupName);

    @Delete("DELETE FROM watchlist_group WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM watchlist_group WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}

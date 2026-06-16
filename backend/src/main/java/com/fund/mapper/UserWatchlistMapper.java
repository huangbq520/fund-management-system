package com.fund.mapper;

import com.fund.entity.UserWatchlist;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserWatchlistMapper {

    @Select("SELECT * FROM user_watchlist WHERE user_id = #{userId} ORDER BY add_time DESC")
    List<UserWatchlist> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_watchlist WHERE user_id = #{userId} AND fund_code = #{fundCode}")
    UserWatchlist findByUserIdAndFundCode(@Param("userId") Long userId, @Param("fundCode") String fundCode);

    @Select("SELECT * FROM user_watchlist WHERE user_id = #{userId} AND group_id = #{groupId} ORDER BY add_time DESC")
    List<UserWatchlist> findByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Insert("INSERT INTO user_watchlist (user_id, fund_code, fund_name, group_id, add_net_value, add_time, sort_order, notes) " +
            "VALUES (#{userId}, #{fundCode}, #{fundName}, #{groupId}, #{addNetValue}, NOW(), #{sortOrder}, #{notes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserWatchlist watchlist);

    @Delete("DELETE FROM user_watchlist WHERE user_id = #{userId} AND fund_code = #{fundCode}")
    int deleteByUserIdAndFundCode(@Param("userId") Long userId, @Param("fundCode") String fundCode);

    @Update("UPDATE user_watchlist SET group_id = #{groupId} WHERE id = #{id} AND user_id = #{userId}")
    int updateGroup(@Param("id") Long id, @Param("userId") Long userId, @Param("groupId") Long groupId);

    @Update("UPDATE user_watchlist SET notes = #{notes} WHERE id = #{id} AND user_id = #{userId}")
    int updateNotes(@Param("id") Long id, @Param("userId") Long userId, @Param("notes") String notes);

    @Select("SELECT COUNT(*) FROM user_watchlist WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_watchlist WHERE user_id = #{userId} AND group_id = #{groupId}")
    int countByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Delete("<script>" +
            "DELETE FROM user_watchlist WHERE user_id = #{userId} AND fund_code IN " +
            "<foreach collection='fundCodes' item='code' open='(' separator=',' close=')'>" +
            "#{code}" +
            "</foreach>" +
            "</script>")
    int deleteBatch(@Param("userId") Long userId, @Param("fundCodes") List<String> fundCodes);

    @Update("UPDATE user_watchlist SET group_id = NULL WHERE group_id = #{groupId} AND user_id = #{userId}")
    int clearGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);
}

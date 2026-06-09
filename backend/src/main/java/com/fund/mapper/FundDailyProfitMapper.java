package com.fund.mapper;

import com.fund.entity.FundDailyProfit;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FundDailyProfitMapper {

    @Insert("INSERT IGNORE INTO fund_daily_profit (user_id, fund_code, fund_name, record_date, " +
            "daily_profit, daily_return_rate, net_value, hold_share, hold_amount, create_time) " +
            "VALUES (#{userId}, #{fundCode}, #{fundName}, #{recordDate}, #{dailyProfit}, " +
            "#{dailyReturnRate}, #{netValue}, #{holdShare}, #{holdAmount}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FundDailyProfit record);

    @Select("SELECT record_date, SUM(daily_profit) as daily_profit, " +
            "SUM(hold_amount) as hold_amount, " +
            "AVG(daily_return_rate) as daily_return_rate " +
            "FROM fund_daily_profit WHERE user_id = #{userId} " +
            "GROUP BY record_date ORDER BY record_date ASC")
    List<FundDailyProfit> selectOverallByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "ORDER BY record_date ASC")
    List<FundDailyProfit> selectByUserAndFund(@Param("userId") Long userId,
                                               @Param("fundCode") String fundCode);

    @Select("SELECT * FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "AND record_date >= #{startDate} " +
            "ORDER BY record_date ASC")
    List<FundDailyProfit> selectByUserAndFundSince(@Param("userId") Long userId,
                                                    @Param("fundCode") String fundCode,
                                                    @Param("startDate") String startDate);

    @Select("SELECT * FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "AND record_date = #{recordDate}")
    FundDailyProfit selectByUserFundDate(@Param("userId") Long userId,
                                          @Param("fundCode") String fundCode,
                                          @Param("recordDate") String recordDate);

    @Select("SELECT daily_profit FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "AND record_date = #{recordDate}")
    BigDecimal getProfitByDate(@Param("userId") Long userId,
                                @Param("fundCode") String fundCode,
                                @Param("recordDate") String recordDate);

    @Select("SELECT * FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "ORDER BY record_date DESC LIMIT 1")
    FundDailyProfit getLatestProfit(@Param("userId") Long userId,
                                     @Param("fundCode") String fundCode);

    @Delete("DELETE FROM fund_daily_profit")
    void deleteAll();
}

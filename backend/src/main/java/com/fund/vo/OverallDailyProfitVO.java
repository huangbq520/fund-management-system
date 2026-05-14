package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OverallDailyProfitVO {
    private Summary summary;
    private List<CurvePoint> dailyList;

    @Data
    public static class Summary {
        private BigDecimal totalProfit;
        private BigDecimal avgDailyProfit;
        private BigDecimal maxDailyProfit;
        private BigDecimal maxDailyLoss;
        private String maxProfitDate;
        private String maxLossDate;
    }

    @Data
    public static class CurvePoint {
        private String recordDate;
        private BigDecimal dailyProfit;
        private BigDecimal dailyReturnRate;
        private BigDecimal holdAmount;
        private BigDecimal cumulativeProfit;
    }
}

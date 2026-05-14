package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DailyProfitVO {
    private String fundCode;
    private String fundName;
    private BigDecimal holdShare;
    private BigDecimal costPrice;
    private Summary summary;
    private List<CurvePoint> profitCurve;
    private List<DetailItem> detailList;

    @Data
    public static class Summary {
        private BigDecimal totalProfit;
        private BigDecimal totalReturnRate;
        private BigDecimal avgDailyProfit;
        private BigDecimal maxDailyProfit;
        private BigDecimal maxDailyLoss;
        private String maxProfitDate;
        private String maxLossDate;
        private int tradingDays;
    }

    @Data
    public static class CurvePoint {
        private String recordDate;
        private BigDecimal dailyProfit;
        private BigDecimal cumulativeProfit;
        private BigDecimal dailyReturnRate;
        private BigDecimal netValue;
    }

    @Data
    public static class DetailItem {
        private String recordDate;
        private BigDecimal dailyProfit;
        private BigDecimal dailyReturnRate;
        private BigDecimal netValue;
        private BigDecimal holdShare;
        private BigDecimal holdAmount;
    }
}

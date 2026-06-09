package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FundHoldingVO {
    private String fundCode;
    private String fundName;
    private BigDecimal holdShare;
    private BigDecimal holdAmount;
    private BigDecimal costPrice;
    private String buyDate;

    private String unitNetValue;
    private String estimatedNetValue;
    private Double estimatedChange;
    private String valuationTime;

    private BigDecimal todayProfit;
    private BigDecimal profitRate;
    private BigDecimal currentValue;

    private String currentNetValue;
    private String yesterdayNetValue;
    private BigDecimal shareForTodayProfit;
    private String profitSource;
    private Double yesterdayChange;

    private BigDecimal yesterdayProfit;

    private Integer profitStatus;
    private BigDecimal todayProfitConfirmed;
    private Boolean isPostClose;

    private Double oneWeekChange;
    private Double oneMonthChange;
    private Double threeMonthChange;
    private Double sixMonthChange;
    private Double oneYearChange;

    private String latestNetValueDate;

    // 持仓成本金额和持仓成本净值
    private BigDecimal costAmount;
}

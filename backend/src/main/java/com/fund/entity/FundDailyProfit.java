package com.fund.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class FundDailyProfit {
    private Long id;
    private Long userId;
    private String fundCode;
    private String fundName;
    private Date recordDate;
    private BigDecimal dailyProfit;
    private BigDecimal dailyReturnRate;
    private BigDecimal netValue;
    private BigDecimal holdShare;
    private BigDecimal holdAmount;
    private Date createTime;
}

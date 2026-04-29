package com.fund.vo;

import lombok.Data;

@Data
public class FundHistoryTrend {
    private String date;
    private Double netValue;
    private Double dailyChange;
}

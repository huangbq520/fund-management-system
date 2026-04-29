package com.fund.vo;

import lombok.Data;

@Data
public class FundHolding {
    private String stockCode;
    private String stockName;
    private String weight;
    private Double change;
}

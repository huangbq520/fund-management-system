package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MarketIndexData {
    private String code;
    private String name;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private String updateTime;
}
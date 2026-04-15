package com.fund.vo;

import lombok.Data;

/**
 * Fund Trend VO - 基金走势数据
 */
@Data
public class FundTrendVO {
    
    /**
     * 时间戳
     */
    private Long x;
    
    /**
     * 净值
     */
    private Double y;
    
    /**
     * 收益回报率
     */
    private Double equityReturn;
}
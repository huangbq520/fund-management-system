package com.fund.vo;

import lombok.Data;

/**
 * Fund Holding VO - 基金持仓
 */
@Data
public class FundHoldingVO {
    
    /**
     * 股票代码
     */
    private String code;
    
    /**
     * 股票名称
     */
    private String name;
    
    /**
     * 占净值比例
     */
    private String weight;
}
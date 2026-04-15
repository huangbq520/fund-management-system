package com.fund.vo;

import lombok.Data;

/**
 * Fund Holding VO - 基金持仓
 * 前十大持仓数组，每项结构：
 * - code: 股票代码(6位)
 * - name: 股票名称
 * - weight: 占净值比例(带%)
 * - change: 股票实时涨跌幅(来自腾讯行情)
 */
@Data
public class FundHoldingVO {
    
    /**
     * 股票代码(6位)
     */
    private String code;
    
    /**
     * 股票名称
     */
    private String name;
    
    /**
     * 占净值比例(带%)
     */
    private String weight;
    
    /**
     * 股票实时涨跌幅(来自腾讯行情，如 "0.50" 表示 0.50%)
     */
    private String change;
}
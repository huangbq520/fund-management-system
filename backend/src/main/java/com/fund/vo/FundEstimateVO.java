package com.fund.vo;

import lombok.Data;

/**
 * Fund Estimate VO - 基金估算数据
 * 对应天天基金接口返回的数据
 */
@Data
public class FundEstimateVO {
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 基金名称
     */
    private String fundName;
    
    /**
     * 单位净值
     */
    private String dwjz;
    
    /**
     * 估算净值
     */
    private String gsz;
    
    /**
     * 估算涨跌幅
     */
    private Double gszzl;
    
    /**
     * 估值时间
     */
    private String gztime;
    
    /**
     * 净值日期
     */
    private String jzrq;
}
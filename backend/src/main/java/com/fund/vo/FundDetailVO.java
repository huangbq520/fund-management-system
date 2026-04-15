package com.fund.vo;

import lombok.Data;
import java.util.List;

/**
 * Fund Detail VO - 基金详情
 */
@Data
public class FundDetailVO {
    
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
    
    /**
     * 持仓列表
     */
    private List<FundHoldingVO> holdings;
    
    /**
     * 历史走势
     */
    private List<FundTrendVO> historyTrend;
}
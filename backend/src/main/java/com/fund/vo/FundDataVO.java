package com.fund.vo;

import lombok.Data;

import java.util.List;

/**
 * 统一基金数据对象
 * 包含所有第三方接口聚合后的完整字段
 * 
 * 字段说明：
 * - code: 基金代码
 * - name: 基金名称  
 * - dwjz: 单位净值
 * - gsz: 估算净值
 * - gztime: 估值时间
 * - jzrq: 净值日期
 * - gszzl: 估算涨跌幅（来自天天基金）
 * - zzl: 涨跌幅（来自腾讯）
 * - holdings: 持仓列表
 * - historyTrend: 历史走势
 * - yesterdayChange: 昨日涨跌幅
 */
@Data
public class FundDataVO {
    
    /**
     * 基金代码
     */
    private String code;
    
    /**
     * 基金名称
     */
    private String name;
    
    /**
     * 单位净值
     */
    private String dwjz;
    
    /**
     * 估算净值
     */
    private String gsz;
    
    /**
     * 估值时间
     */
    private String gztime;
    
    /**
     * 净值日期
     */
    private String jzrq;
    
    /**
     * 估算涨跌幅（来自天天基金）
     */
    private Double gszzl;
    
    /**
     * 涨跌幅（来自腾讯）
     */
    private Double zzl;
    
    /**
     * 持仓列表
     */
    private List<FundHoldingVO> holdings;
    
    /**
     * 历史走势（最近90条）
     */
    private List<FundTrendVO> historyTrend;
    
    /**
     * 昨日涨跌幅（从走势数据倒数第二条获取）
     */
    private Double yesterdayChange;
}
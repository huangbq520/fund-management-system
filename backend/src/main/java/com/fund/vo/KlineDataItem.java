package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class KlineDataItem {
    /** 日期 yyyy-MM-dd */
    private String date;
    /** 开盘价 */
    private BigDecimal open;
    /** 收盘价 */
    private BigDecimal close;
    /** 最高价 */
    private BigDecimal high;
    /** 最低价 */
    private BigDecimal low;
    /** 成交量 */
    private Long volume;
    /** 成交额 */
    private BigDecimal amount;
    /** 振幅(%) */
    private BigDecimal amplitude;
    /** 涨跌幅(%) */
    private BigDecimal changePercent;
    /** 涨跌额 */
    private BigDecimal change;
    /** 换手率(%) */
    private BigDecimal turnoverRate;
    // opt: 新增无效标记字段，用于标记解析失败的K线数据
    private boolean valid = true;
}

package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WatchlistItemVO {
    // 数据库字段
    private Long id;
    private String fundCode;
    private String fundName;
    private Long groupId;
    private String groupName;
    private BigDecimal addNetValue;
    private String addTime;
    private Integer sortOrder;
    private String notes;

    // 实时行情（来自 FundDataService）
    private String unitNetValue;
    private String estimatedNetValue;
    private Double estimatedChange;
    private Double latestChange;       // 最新涨幅（基于确认净值）
    private String valuationTime;
    private String latestNetValueDate;

    // 周期收益率
    private Double oneWeekChange;
    private Double oneMonthChange;
    private Double threeMonthChange;
    private Double sixMonthChange;
    private Double oneYearChange;

    // 计算：自选以来收益率
    private Double returnSinceAdded;
}

package com.fund.vo;

import lombok.Data;
import java.util.List;

@Data
public class PerformanceData {
    private String fundCode;
    private String fundName;
    private List<FundHistoryTrend> netWorthTrend;
    private List<CompareIndex> compareIndices;
    private Double periodReturn;
    private String period;
}
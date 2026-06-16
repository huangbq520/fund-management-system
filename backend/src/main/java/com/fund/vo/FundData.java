package com.fund.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FundData {
    private String fundCode;
    private String fundName;
    @JsonProperty("dwjz")
    private String unitNetValue;
    @JsonProperty("gsz")
    private String estimatedNetValue;
    @JsonProperty("gszzl")
    private Double estimatedChange;
    @JsonProperty("gztime")
    private String valuationTime;

    private List<FundHolding> holdings = new ArrayList<>();
    private List<FundHistoryTrend> historyTrend = new ArrayList<>();
    private List<CompareIndex> compareIndices = new ArrayList<>();

    private Double yesterdayChange;

    private boolean basicInfoSuccess = false;
    private boolean holdingsSuccess = false;
    private boolean historySuccess = false;

    private List<String> errorMessages = new ArrayList<>();

    private String yesterdayNetValue;
    private boolean tradingDay;
    private boolean priced;
    private Double estPricedCoverage;

    // 周期涨跌幅数据
    private Double oneWeekChange;
    private Double oneMonthChange;
    private Double threeMonthChange;
    private Double sixMonthChange;
    private Double oneYearChange;

    // 最新净值对应的日期（MM-dd 格式，用于显示）
    private String latestNetValueDate;

    // 最新净值对应的完整日期（yyyy-MM-dd 格式，用于日期比对判断）
    private String netValueDate;

    // 确认净值日期是否为今天
    public boolean isNetValueForToday() {
        if (netValueDate == null || netValueDate.isEmpty()) {
            return false;
        }
        try {
            java.time.LocalDate nvDate = java.time.LocalDate.parse(netValueDate);
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
            return nvDate.equals(today);
        } catch (Exception e) {
            return false;
        }
    }

    // 确认净值日期是否为昨天
    public boolean isNetValueForYesterday() {
        if (netValueDate == null || netValueDate.isEmpty()) {
            return false;
        }
        try {
            java.time.LocalDate nvDate = java.time.LocalDate.parse(netValueDate);
            java.time.LocalDate yesterday = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(1);
            return nvDate.equals(yesterday);
        } catch (Exception e) {
            return false;
        }
    }

    public String getJzrq() {
        return netValueDate != null ? netValueDate : "";
    }

    public boolean isUseEstimatedValue() {
        return tradingDay && !priced;
    }

    public Double getOneWeekChange() { return oneWeekChange; }
    public void setOneWeekChange(Double oneWeekChange) { this.oneWeekChange = oneWeekChange; }
    public Double getOneMonthChange() { return oneMonthChange; }
    public void setOneMonthChange(Double oneMonthChange) { this.oneMonthChange = oneMonthChange; }
    public Double getThreeMonthChange() { return threeMonthChange; }
    public void setThreeMonthChange(Double threeMonthChange) { this.threeMonthChange = threeMonthChange; }
    public Double getSixMonthChange() { return sixMonthChange; }
    public void setSixMonthChange(Double sixMonthChange) { this.sixMonthChange = sixMonthChange; }
    public Double getOneYearChange() { return oneYearChange; }
    public void setOneYearChange(Double oneYearChange) { this.oneYearChange = oneYearChange; }

    public String getLatestNetValueDate() { return latestNetValueDate; }
    public void setLatestNetValueDate(String latestNetValueDate) { this.latestNetValueDate = latestNetValueDate; }
}
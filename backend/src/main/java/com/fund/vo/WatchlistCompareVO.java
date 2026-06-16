package com.fund.vo;

import lombok.Data;
import java.util.List;

@Data
public class WatchlistCompareVO {
    private List<CompareFundItem> funds;

    @Data
    public static class CompareFundItem {
        private String fundCode;
        private String fundName;
        private String unitNetValue;
        private Double estimatedChange;
        private Double returnSinceAdded;
        private Double oneWeekChange;
        private Double oneMonthChange;
        private Double threeMonthChange;
        private Double sixMonthChange;
        private Double oneYearChange;
    }
}

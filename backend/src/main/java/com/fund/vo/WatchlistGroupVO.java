package com.fund.vo;

import lombok.Data;

@Data
public class WatchlistGroupVO {
    private Long id;
    private String groupName;
    private Integer sortOrder;
    private Integer fundCount;
}

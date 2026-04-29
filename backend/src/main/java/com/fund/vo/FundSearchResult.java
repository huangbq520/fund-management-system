package com.fund.vo;

import lombok.Data;

@Data
public class FundSearchResult {
    private String fundCode;
    private String fundName;
    private String category;
    private String categoryDesc;
    private String spell;
    private String pinYin;
}
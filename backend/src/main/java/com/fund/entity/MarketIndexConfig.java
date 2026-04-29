package com.fund.entity;

import lombok.Data;

@Data
public class MarketIndexConfig {
    private String code;
    private String varKey;
    private String name;

    public MarketIndexConfig(String code, String varKey, String name) {
        this.code = code;
        this.varKey = varKey;
        this.name = name;
    }
}
package com.fund.vo;

import lombok.Data;

import java.util.List;

@Data
public class OcrResult {
    private List<String> fundCodes;
    private List<String> fundNames;
    private String rawText;
}

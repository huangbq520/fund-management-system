package com.fund.dto;

import lombok.Data;

@Data
public class SendVerifyCodeRequest {
    private String email;
    private String type;
}

package com.fund.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String verifyCode;
    private String nickname;
}

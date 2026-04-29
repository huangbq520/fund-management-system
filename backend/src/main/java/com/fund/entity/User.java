package com.fund.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}

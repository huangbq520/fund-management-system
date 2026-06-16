package com.fund.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserWatchlist {
    private Long id;
    private Long userId;
    private String fundCode;
    private String fundName;
    private Long groupId;
    private BigDecimal addNetValue;
    private Date addTime;
    private Integer sortOrder;
    private String notes;
}

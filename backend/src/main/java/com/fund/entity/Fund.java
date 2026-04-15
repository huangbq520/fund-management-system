package com.fund.entity;

import lombok.Data;
import java.util.Date;

/**
 * Fund Entity - 基金实体类
 */
@Data
public class Fund {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 基金名称
     */
    private String fundName;
    
    /**
     * 添加时间
     */
    private Date createTime;
}
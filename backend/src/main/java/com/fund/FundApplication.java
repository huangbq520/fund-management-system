package com.fund;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Fund Management System Application
 * 基金实时估值与持仓管理系统
 */
@SpringBootApplication
@MapperScan("com.fund.mapper")
public class FundApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(FundApplication.class, args);
    }
}
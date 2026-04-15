-- Fund Management System Database Schema
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS fund_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fund_db;

-- 基金信息表
CREATE TABLE IF NOT EXISTS fund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    fund_name VARCHAR(50) COMMENT '基金名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    UNIQUE KEY uk_fund_code (fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金信息表';

-- 插入测试数据
INSERT INTO fund (fund_code, fund_name) VALUES ('000001', '华夏成长混合');
INSERT INTO fund (fund_code, fund_name) VALUES ('161039', '富国中证500指数');
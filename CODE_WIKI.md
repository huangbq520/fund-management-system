# 基金实时估值与持仓管理系统 - Code Wiki

## 1. 项目概述

### 1.1 项目简介

本系统是一个基于 Spring Boot + Vue 3 的个人基金投资管理工具，聚合天天基金、东方财富、腾讯财经等公开数据源，为投资者提供基金搜索、实时估值、持仓管理、收益计算等一站式服务。

### 1.2 项目解决的核心痛点

| 痛点 | 解决方案 |
|------|----------|
| 基金数据分散、信息获取效率低 | 数据聚合技术，将多个数据源整合到一个系统 |
| 持仓收益手动计算繁琐 | 自动记录持仓信息，实时计算收益 |
| 净值数据更新不及时 | 定时任务技术，每日收盘后自动同步 |
| 缺乏统一的投资组合管理 | 组合汇总功能，全面掌握资产状况 |

### 1.3 技术栈

#### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 8 | 编程语言 |
| Spring Boot | 2.7.18 | 后端框架 |
| MyBatis | 2.3.2 | ORM框架 |
| MySQL | 8.0+ | 关系型数据库 |
| OkHttp | 4.12.0 | HTTP客户端 |
| Jsoup | 1.17.2 | HTML解析 |
| FastJSON2 | 2.0.47 | JSON数据解析 |
| Spring Security | - | 安全框架 |
| JWT | 0.11.5 | 身份认证 |
| Redis | - | 数据缓存 |
| Spring Mail | - | 邮件服务 |
| Lombok | 1.18.30 | 简化代码 |

#### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.32 | 前端框架 |
| Vite | 8.0.8 | 构建工具 |
| Axios | 1.15.0 | HTTP客户端 |
| ECharts | 6.0.0 | 数据可视化 |

---

## 2. 项目结构

```
fund-management-system/
│
├── backend/                           # 后端项目（Spring Boot）
│   ├── src/main/java/com/fund/
│   │   ├── FundApplication.java       # 应用入口
│   │   ├── config/                     # 配置类
│   │   │   ├── CorsConfig.java        # 跨域配置
│   │   │   ├── JwtAuthenticationFilter.java  # JWT认证过滤器
│   │   │   ├── OkHttpConfig.java      # OkHttp配置
│   │   │   ├── RedisConfig.java       # Redis配置
│   │   │   └── SecurityConfig.java   # Spring Security配置
│   │   ├── controller/                # REST API控制器
│   │   │   ├── AuthController.java    # 认证接口
│   │   │   ├── FundController.java    # 基金接口
│   │   │   └── MarketIndexController.java  # 行情指数接口
│   │   ├── service/                    # 业务逻辑层
│   │   │   ├── AuthService.java       # 认证服务
│   │   │   ├── FundDataService.java   # 基金数据服务
│   │   │   ├── FundHoldingService.java # 持仓管理服务
│   │   │   ├── FundSearchService.java # 基金搜索服务
│   │   │   └── MarketIndexService.java # 行情指数服务
│   │   ├── mapper/                     # 数据访问层
│   │   │   ├── FundMapper.java        # 基金Mapper
│   │   │   ├── UserFundMapper.java    # 用户持仓Mapper
│   │   │   └── UserMapper.java        # 用户Mapper
│   │   ├── entity/                     # 实体类
│   │   │   ├── Fund.java              # 基金实体
│   │   │   ├── MarketIndexConfig.java # 指数配置实体
│   │   │   ├── User.java              # 用户实体
│   │   │   └── UserFund.java          # 用户持仓实体
│   │   ├── vo/                         # 值对象（视图对象）
│   │   │   ├── ApiResponse.java       # API响应封装
│   │   │   ├── CompareIndex.java      # 对比指数
│   │   │   ├── FundData.java          # 基金数据
│   │   │   ├── FundHistoryTrend.java  # 历史走势
│   │   │   ├── FundHolding.java       # 持仓信息
│   │   │   ├── FundHoldingVO.java     # 持仓视图对象
│   │   │   ├── FundSearchResult.java  # 搜索结果
│   │   │   ├── MarketIndexData.java   # 指数数据
│   │   │   ├── PerformanceData.java   # 业绩数据
│   │   │   └── PortfolioSummary.java   # 组合汇总
│   │   ├── dto/                        # 数据传输对象
│   │   │   ├── LoginRequest.java      # 登录请求
│   │   │   ├── RegisterRequest.java    # 注册请求
│   │   │   └── SendVerifyCodeRequest.java  # 发送验证码请求
│   │   ├── scheduler/                  # 定时任务
│   │   │   └── NetValueSyncScheduler.java  # 净值同步调度器
│   │   └── util/                       # 工具类
│   │       ├── HttpUtil.java           # HTTP工具类
│   │       ├── JwtUtil.java           # JWT工具类
│   │       └── TradingDayChecker.java # 交易日检查器
│   ├── src/main/resources/
│   │   ├── mapper/                     # MyBatis XML映射文件
│   │   │   └── FundMapper.xml         # 基金Mapper XML
│   │   ├── application.yml             # 应用配置文件
│   │   └── schema.sql                  # 数据库脚本
│   └── pom.xml                         # Maven配置文件
│
└── frontend/                           # 前端项目（Vue 3）
    ├── src/
    │   ├── api/                        # API请求封装
    │   │   ├── auth.js                 # 认证API
    │   │   └── index.js                # 基金API
    │   ├── components/                  # Vue组件
    │   │   ├── EditHoldingModal.vue    # 编辑持仓弹窗
    │   │   ├── FundCard.vue            # 基金卡片
    │   │   ├── FundDetailModal.vue     # 基金详情弹窗
    │   │   ├── FundList.vue            # 基金列表
    │   │   ├── HoldingList.vue          # 持仓列表
    │   │   ├── Login.vue               # 登录页
    │   │   ├── MarketIndex.vue         # 行情指数
    │   │   ├── PerformanceChart.vue    # 业绩图表
    │   │   ├── PortfolioSummary.vue     # 组合汇总
    │   │   ├── Register.vue             # 注册页
    │   │   └── SearchFund.vue          # 基金搜索
    │   ├── App.vue                      # 主组件
    │   ├── main.js                     # 入口文件
    │   └── style.css                   # 全局样式
    ├── index.html
    ├── vite.config.js                  # Vite配置
    └── package.json                    # NPM配置文件
```

---

## 3. 系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                      前端 (Vue 3)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ 基金搜索  │  │ 基金详情  │  │ 持仓管理  │  │ 组合汇总 │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬────┘ │
│       └────────────┴────────────┴────────────┘       │
│                          │                              │
│                    Axios HTTP                          │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                   │
│  ┌──────────────────────────────────────────────────┐  │
│  │                  Security Filter                   │  │
│  │              (JWT Authentication)                  │  │
│  └──────────────────────────────────────────────────┘  │
│                           │                              │
│  ┌────────────┐  ┌────────┴────────┐  ┌─────────────┐   │
│  │AuthController│ │ FundController  │ │MarketController│
│  └──────┬─────┘  └──────┬─────────┘  └──────┬──────┘   │
│         │               │                   │          │
│  ┌──────▼───────────────▼───────────────────▼──────┐   │
│  │                    Service Layer                  │   │
│  │  ┌───────────┐ ┌───────────┐ ┌────────────────┐  │   │
│  │  │AuthService│ │FundData   │ │FundHolding     │  │   │
│  │  │           │ │Service    │ │Service         │  │   │
│  │  └───────────┘ └───────────┘ └────────────────┘  │   │
│  └──────────────────────────────────────────────────┘  │
│                           │                              │
│  ┌────────────────────────▼────────────────────────┐   │
│  │              Mapper Layer (MyBatis)              │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
    ┌──────────┐    ┌───────────┐    ┌───────────┐
    │  MySQL   │    │   Redis   │    │Third-Party│
    │          │    │  (Cache)  │    │   APIs    │
    └──────────┘    └───────────┘    └───────────┘
```

### 3.2 核心数据流

#### 基金数据获取流程

```
用户请求基金数据
      │
      ▼
┌────────────────────────────────────────────────────────┐
│                  FundDataService                         │
├────────────────────────────────────────────────────────┤
│ 1. fetchBasicInfo() - 天天基金API                        │
│    URL: https://fundgz.1234567.com.cn/js/{code}.js      │
│    获取: 基金名称、实时净值、估值涨跌幅、估值时间          │
├────────────────────────────────────────────────────────┤
│ 2. fetchHoldingsAndEnrich() - 东方财富API                │
│    URL: https://fundf10.eastmoney.com/FundArchivesDatas │
│    获取: 基金重仓股票信息                                │
│    └─► enrichStockChange() - 腾讯股票API                 │
│        URL: https://qt.gtimg.cn/q={stock_codes}        │
│        获取: 重仓股票的实时涨跌幅                         │
├────────────────────────────────────────────────────────┤
│ 3. fetchHistoryTrend() - 东方财富API                    │
│    URL: https://fund.eastmoney.com/pingzhongdata/{code}.js
│    获取: 历史净值走势、对比指数数据                       │
└────────────────────────────────────────────────────────┘
      │
      ▼
   返回 FundData
```

#### 用户认证流程

```
用户登录/注册
      │
      ▼
┌────────────────────────────────────────────────────────┐
│                  AuthController                         │
├────────────────────────────────────────────────────────┤
│ POST /api/auth/register                                │
│    ├─► 验证邮箱格式                                     │
│    ├─► 验证验证码                                       │
│    ├─► 密码加密 (BCrypt)                                │
│    ├─► 保存用户信息                                     │
│    └─► 生成JWT令牌                                      │
├────────────────────────────────────────────────────────┤
│ POST /api/auth/login                                   │
│    ├─► 验证邮箱是否存在                                 │
│    ├─► 验证密码是否正确                                 │
│    └─► 生成JWT令牌                                      │
└────────────────────────────────────────────────────────┘
      │
      ▼
   返回 Token + UserInfo
```

---

## 4. 核心模块详解

### 4.1 认证模块 (Auth)

#### 模块职责

用户注册、登录、验证码发送等认证相关功能。

#### 关键类说明

**AuthController.java**
- 位置: `backend/src/main/java/com/fund/controller/AuthController.java`
- 功能: 处理用户认证相关的HTTP请求
- 关键方法:
  - `sendVerifyCode()` - 发送验证码邮件
  - `register()` - 用户注册
  - `login()` - 用户登录
  - `getCurrentUser()` - 获取当前用户信息

**AuthService.java**
- 位置: `backend/src/main/java/com/fund/service/AuthService.java`
- 功能: 用户认证业务逻辑
- 关键方法:
  - `sendVerifyCode(String email)` - 发送验证码
  - `register(String email, String password, String nickname, String verifyCode)` - 注册用户
  - `login(String email, String password)` - 用户登录
  - `getUserById(Long id)` - 根据ID获取用户

**User.java (Entity)**
- 位置: `backend/src/main/java/com/fund/entity/User.java`
- 功能: 用户实体类
- 字段:
  - `id` - 用户ID
  - `email` - 邮箱
  - `password` - 密码（加密存储）
  - `nickname` - 昵称
  - `status` - 状态（1正常/0禁用）
  - `createTime` - 创建时间
  - `updateTime` - 更新时间

**JwtUtil.java**
- 位置: `backend/src/main/java/com/fund/util/JwtUtil.java`
- 功能: JWT令牌生成和验证
- 关键方法:
  - `generateToken(Long userId, String email)` - 生成令牌
  - `parseToken(String token)` - 解析令牌
  - `validateToken(String token)` - 验证令牌

**JwtAuthenticationFilter.java**
- 位置: `backend/src/main/java/com/fund/config/JwtAuthenticationFilter.java`
- 功能: JWT认证过滤器，验证请求中的JWT令牌

---

### 4.2 基金数据模块 (Fund)

#### 模块职责

基金搜索、基金数据获取、历史走势、业绩数据等核心功能。

#### 关键类说明

**FundController.java**
- 位置: `backend/src/main/java/com/fund/controller/FundController.java`
- 功能: 处理基金相关的HTTP请求
- 关键方法:
  - `getFundData(String code)` - 获取基金完整数据
  - `getPerformanceData(String code, String period)` - 获取基金业绩走势
  - `searchFunds(String keyword)` - 搜索基金
  - `listFunds()` - 获取用户的基金列表
  - `addFund(String fundCode, String fundName)` - 添加基金
  - `updateHolding()` - 更新持仓信息
  - `deleteFund(String fundCode)` - 删除基金

**FundDataService.java**
- 位置: `backend/src/main/java/com/fund/service/FundDataService.java`
- 功能: 基金数据获取和聚合的核心服务
- 关键方法:
  - `getFundData(String fundCode)` - 获取基金完整数据（主入口）
  - `fetchBasicInfo(String fundCode, FundData fundData)` - 获取天天基金基本信息
  - `fetchHoldingsAndEnrich(String fundCode, FundData fundData)` - 获取持仓并丰富数据
  - `fetchHistoryTrend(String fundCode, FundData fundData)` - 获取历史走势
  - `getPerformanceData(String fundCode, String period)` - 获取业绩数据

**FundData.java (VO)**
- 位置: `backend/src/main/java/com/fund/vo/FundData.java`
- 功能: 基金数据视图对象
- 字段:
  - `fundCode` - 基金代码
  - `fundName` - 基金名称
  - `unitNetValue` - 单位净值
  - `estimatedNetValue` - 估算净值
  - `estimatedChange` - 估算涨跌幅
  - `valuationTime` - 估值时间
  - `yesterdayNetValue` - 昨日净值
  - `yesterdayChange` - 昨日涨跌幅
  - `holdings` - 重仓股票列表
  - `historyTrend` - 历史走势
  - `compareIndices` - 对比指数

**PerformanceData.java (VO)**
- 位置: `backend/src/main/java/com/fund/vo/PerformanceData.java`
- 功能: 基金业绩数据视图对象
- 字段:
  - `fundCode` - 基金代码
  - `fundName` - 基金名称
  - `period` - 周期
  - `periodReturn` - 区间收益率
  - `netWorthTrend` - 净值走势
  - `compareIndices` - 对比指数

**FundSearchService.java**
- 位置: `backend/src/main/java/com/fund/service/FundSearchService.java`
- 功能: 基金搜索服务
- 关键方法:
  - `searchFunds(String keyword)` - 根据关键词搜索基金

---

### 4.3 持仓管理模块 (FundHolding)

#### 模块职责

用户持仓管理、收益计算、组合汇总等功能。

#### 关键类说明

**FundHoldingService.java**
- 位置: `backend/src/main/java/com/fund/service/FundHoldingService.java`
- 功能: 持仓管理和收益计算
- 关键方法:
  - `getHoldingList(Long userId)` - 获取持仓列表
  - `getPortfolioSummary(Long userId)` - 获取组合汇总
  - `calculateProfit(Fund fund, FundData fundData, UserFund userFund)` - 计算收益
  - `determineCurrentNetValue()` - 确定当前净值
  - `calculateProfitByChangePercent()` - 按涨跌幅计算收益

**FundHoldingVO.java**
- 位置: `backend/src/main/java/com/fund/vo/FundHoldingVO.java`
- 功能: 持仓视图对象
- 字段:
  - `fundCode` - 基金代码
  - `fundName` - 基金名称
  - `holdShare` - 持有份额
  - `holdAmount` - 投入金额
  - `costPrice` - 成本价
  - `currentNetValue` - 当前净值
  - `currentValue` - 当前市值
  - `todayProfit` - 今日收益
  - `profitRate` - 收益率
  - `shareForTodayProfit` - 用于计算今日收益的份额

**PortfolioSummary.java**
- 位置: `backend/src/main/java/com/fund/vo/PortfolioSummary.java`
- 功能: 组合汇总视图对象
- 字段:
  - `totalAsset` - 总资产
  - `todayProfit` - 今日收益
  - `todayProfitRate` - 今日收益率
  - `totalProfit` - 总收益
  - `totalProfitRate` - 总收益率
  - `fundCount` - 基金数量

---

### 4.4 定时任务模块 (Scheduler)

#### 模块职责

每日净值同步、交易日字段重置等自动化任务。

#### 关键类说明

**NetValueSyncScheduler.java**
- 位置: `backend/src/main/java/com/fund/scheduler/NetValueSyncScheduler.java`
- 功能: 净值同步定时任务
- 执行时间:
  - 每个工作日 15:05 - 净值同步
  - 每个工作日 09:00 - 字段重置

---

## 5. 数据库设计

### 5.1 数据库概览

- 数据库名: `jiangcai_fund`
- 字符集: `utf8mb4`
- 存储引擎: `InnoDB`

### 5.2 表结构

#### 用户表 (user)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 邮箱 |
| password | VARCHAR(255) | NOT NULL | 密码（加密） |
| nickname | VARCHAR(50) | | 昵称 |
| status | TINYINT | DEFAULT 1 | 状态：1正常 0禁用 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

索引: `idx_email`

#### 基金信息表 (fund)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| fund_name | VARCHAR(50) | | 基金名称 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 添加时间 |

索引: `uk_user_fund(user_id, fund_code)`, `idx_user_id`

#### 用户基金持仓表 (user_fund)

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | ID |
| user_id | BIGINT | NOT NULL | 用户ID |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| fund_name | VARCHAR(50) | | 基金名称 |
| hold_share | DECIMAL(10,2) | DEFAULT 0 | 持有份额 |
| hold_amount | DECIMAL(10,2) | DEFAULT 0 | 投入金额 |
| cost_price | DECIMAL(10,4) | DEFAULT 0 | 成本价 |
| buy_date | DATE | | 买入日期 |
| today_buy_share | DECIMAL(10,2) | DEFAULT 0 | 当日买入份额 |
| today_sell_share | DECIMAL(10,2) | DEFAULT 0 | 当日卖出份额 |
| yesterday_share | DECIMAL(10,2) | DEFAULT 0 | 昨日收盘份额 |
| yesterday_net_value | DECIMAL(10,4) | DEFAULT 0 | 昨日确认净值 |
| profit_status | TINYINT | DEFAULT 0 | 收益状态：0-估算 1-已确认 |
| last_sync_time | DATETIME | | 最后同步时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

索引: `uk_user_fund_code(user_id, fund_code)`, `idx_user_id`

### 5.3 实体类映射

**User.java**
```java
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
```

**Fund.java**
```java
@Data
public class Fund {
    private Long id;
    private Long userId;
    private String fundCode;
    private String fundName;
    private Date createTime;
}
```

**UserFund.java**
```java
@Data
public class UserFund {
    private Long id;
    private Long userId;
    private String fundCode;
    private String fundName;
    private BigDecimal holdShare;
    private BigDecimal holdAmount;
    private BigDecimal costPrice;
    private Date buyDate;
    private BigDecimal todayBuyShare;
    private BigDecimal todaySellShare;
    private BigDecimal yesterdayShare;
    private BigDecimal yesterdayNetValue;
    private Integer profitStatus;
    private Date lastSyncTime;
    private Date createTime;
    private Date updateTime;
}
```

---

## 6. API接口文档

### 6.1 认证接口

#### 发送验证码
- **POST** `/api/auth/send-verify-code`
- **Request Body:**
```json
{
  "email": "user@example.com"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 用户注册
- **POST** `/api/auth/register`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "123456",
  "confirmPassword": "123456",
  "nickname": "用户名",
  "verifyCode": "123456"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "用户名"
    },
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

#### 用户登录
- **POST** `/api/auth/login`
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "123456"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "nickname": "用户名"
    },
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

#### 获取当前用户
- **GET** `/api/auth/me`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "用户名"
  }
}
```

### 6.2 基金接口

#### 搜索基金
- **GET** `/api/fund/search?keyword=半导体`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "fundCode": "001284",
      "fundName": "银河创新成长混合A",
      "fundType": "混合型"
    }
  ]
}
```

#### 获取基金数据
- **GET** `/api/fund/data?code=001284`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fundCode": "001284",
    "fundName": "银河创新成长混合A",
    "unitNetValue": "3.4567",
    "estimatedNetValue": "3.5123",
    "estimatedChange": 1.56,
    "valuationTime": "2024-01-15 14:30",
    "holdings": [
      {
        "stockCode": "600519",
        "stockName": "贵州茅台",
        "weight": "10.5%",
        "change": 2.35
      }
    ],
    "historyTrend": [
      {
        "date": "1705276800000",
        "netValue": 3.4567,
        "dailyChange": 1.25
      }
    ],
    "compareIndices": [
      {
        "date": "2024-01-01",
        "szzs": 0.0,
        "tysm": 0.0
      }
    ]
  }
}
```

#### 获取基金业绩走势
- **GET** `/api/fund/performance?code=001284&period=6month`
- **Period参数:** `1month`, `3month`, `6month`, `1year`, `3year`, `all`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fundCode": "001284",
    "fundName": "银河创新成长混合A",
    "period": "6month",
    "periodReturn": 12.56,
    "netWorthTrend": [...],
    "compareIndices": [...]
  }
}
```

#### 获取基金列表
- **GET** `/api/fund/list`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "fundCode": "001284",
      "fundName": "银河创新成长混合A",
      "createTime": "2024-01-01 10:00:00"
    }
  ]
}
```

#### 添加基金
- **POST** `/api/fund/add`
- **Headers:** `Authorization: Bearer {token}`
- **Request Body:**
```json
{
  "fundCode": "001284",
  "fundName": "银河创新成长混合A"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fundCode": "001284",
    "fundName": "银河创新成长混合A"
  }
}
```

#### 更新持仓
- **POST** `/api/fund/holding/update`
- **Headers:** `Authorization: Bearer {token}`
- **Request Body:**
```json
{
  "fundCode": "001284",
  "holdShare": 1000.00,
  "costPrice": 3.2000,
  "buyDate": "2024-01-01"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 删除基金
- **POST** `/api/fund/delete`
- **Headers:** `Authorization: Bearer {token}`
- **Request Body:**
```json
{
  "fundCode": "001284"
}
```
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 获取持仓列表
- **GET** `/api/fund/holding/list`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "fundCode": "001284",
      "fundName": "银河创新成长混合A",
      "holdShare": 1000.00,
      "costPrice": 3.2000,
      "currentNetValue": "3.4567",
      "currentValue": "3456.70",
      "todayProfit": "256.70",
      "profitRate": 8.02
    }
  ]
}
```

#### 获取组合汇总
- **GET** `/api/fund/portfolio/summary`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalAsset": 34567.00,
    "todayProfit": 1234.56,
    "todayProfitRate": 3.70,
    "totalProfit": 4567.00,
    "totalProfitRate": 15.22,
    "fundCount": 5
  }
}
```

### 6.3 行情接口

#### 获取主要指数数据
- **GET** `/api/market/index`
- **Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "name": "上证指数",
      "code": "000001",
      "currentPoint": "3150.25",
      "change": 25.36,
      "changePercent": 0.81
    }
  ]
}
```

---

## 7. 第三方数据源

### 7.1 数据源列表

| 数据源 | 用途 | URL |
|--------|------|-----|
| 天天基金 | 实时净值估值 | `https://fundgz.1234567.com.cn` |
| 东方财富 | 持仓股票、历史走势 | `https://fundf10.eastmoney.com` |
| 腾讯财经 | 股票行情 | `https://qt.gtimg.cn` |

### 7.2 数据获取策略

**FundDataService.java** 中的数据获取方法：

```java
// 1. 天天基金 - 实时估值
private void fetchBasicInfo(String fundCode, FundData fundData) {
    String url = String.format(TIANTIAN_FUND_URL, fundCode, System.currentTimeMillis());
    // 解析JSONP格式返回
}

// 2. 东方财富 - 持仓股票
private void fetchHoldingsAndEnrich(String fundCode, FundData fundData) {
    String url = String.format(EASTMONEY_HOLDINGS_URL, fundCode, System.currentTimeMillis());
    // 解析HTML表格
}

// 3. 腾讯财经 - 股票涨跌幅
private void enrichStockChange(List<FundHolding> holdings) {
    // 批量查询股票行情
}

// 4. 东方财富 - 历史走势
private void fetchHistoryTrend(String fundCode, FundData fundData) {
    String url = String.format(EASTMONEY_TREND_URL, fundCode, System.currentTimeMillis());
    // 解析JavaScript变量
}
```

---

## 8. 配置说明

### 8.1 应用配置 (application.yml)

```yaml
server:
  port: 8080  # 服务端口

spring:
  application:
    name: fund-management
  datasource:
    url: jdbc:mysql://localhost:3306/jiangcai_fund?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
    database: 0
  mail:
    host: smtp.qq.com
    port: 587
    username: your_email@qq.com
    password: your_auth_code

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.fund.entity
  configuration:
    map-underscore-to-camel-case: true

okhttp:
  connect-timeout: 5000
  read-timeout: 5000
  write-timeout: 5000

cors:
  allowed-origins: "*"

jwt:
  secret: your-secret-key-for-hs256-algorithm
  expiration: 86400000  # 24小时
```

### 8.2 CORS配置 (CorsConfig.java)

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(false);
        // ... 更多配置
    }
}
```

### 8.3 安全配置 (SecurityConfig.java)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/fund/search").permitAll()
                .antMatchers("/api/fund/data").permitAll()
                .anyRequest().authenticated();
        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
```

---

## 9. 项目运行指南

### 9.1 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 8+ |
| Maven | 3.6+ |
| Node.js | 16+ |
| MySQL | 8.0+ |
| Redis | 可选（用于缓存） |

### 9.2 数据库初始化

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 9.3 启动后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 9.4 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

### 9.5 Docker部署（可选）

```dockerfile
# 后端 Dockerfile
FROM maven:3.8-openjdk-8
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests
CMD ["java", "-jar", "target/fund-management-1.0.0.jar"]
```

---

## 10. 核心业务逻辑

### 10.1 收益计算算法

**FundHoldingService.java** 中的 `calculateProfit` 方法：

```java
// 1. 确定用于计算今日收益的份额
BigDecimal shareForToday = holdShare.subtract(todayBuyShare).add(todaySellShare);

// 2. 确定当前净值
String currentNetValue = determineCurrentNetValue(fundData, postClose);

// 3. 计算当前市值
BigDecimal currentValue = shareForToday.multiply(currentNetValueBD);

// 4. 计算今日收益
if (yesterdayNetValue != null) {
    todayProfit = shareForToday.multiply(currentNetValue.subtract(yesterdayNetValue));
} else {
    todayProfit = currentValue * estimatedChange / 100;
}

// 5. 计算总收益和收益率
BigDecimal profit = currentValue - (holdShare * costPrice);
BigDecimal profitRate = profit / (holdShare * costPrice) * 100;
```

### 10.2 交易日判断逻辑

**TradingDayChecker.java** 和 **isTradingDay()** 方法：

```java
private boolean isTradingDay() {
    Calendar cal = Calendar.getInstance();
    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
    
    // 判断是否为周末
    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
        return false;
    }
    
    // 判断是否在交易时间内 (9:30 - 15:00)
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minute = cal.get(Calendar.MINUTE);
    int currentTime = hour * 60 + minute;
    int tradingStart = 9 * 60 + 30;
    int tradingEnd = 15 * 60;
    
    return currentTime >= tradingStart && currentTime <= tradingEnd;
}
```

### 10.3 定时任务调度

**NetValueSyncScheduler.java**：

```java
// 每日收盘后同步净值
@Scheduled(cron = "0 5 15 * * MON-FRI")
public void syncNetValue() {
    // 遍历所有用户的持仓基金
    // 获取最新净值
    // 更新昨日净值和收益状态
}

// 每日开盘前重置交易字段
@Scheduled(cron = "0 0 9 * * MON-FRI")
public void resetTradingFields() {
    // 重置当日买卖份额
    // 重置收益状态为估算
}
```

---

## 11. 前端组件说明

### 11.1 组件结构

```
frontend/src/components/
├── Login.vue              # 登录页面
├── Register.vue           # 注册页面
├── FundList.vue           # 基金列表（主页面）
├── SearchFund.vue         # 基金搜索组件
├── FundCard.vue           # 基金卡片组件
├── FundDetailModal.vue    # 基金详情弹窗
├── HoldingList.vue        # 持仓列表
├── EditHoldingModal.vue   # 编辑持仓弹窗
├── PortfolioSummary.vue    # 组合汇总卡片
├── MarketIndex.vue        # 行情指数展示
└── PerformanceChart.vue   # 业绩走势图（ECharts）
```

### 11.2 主要组件功能

#### FundList.vue
- 功能: 展示用户的基金列表
- 状态管理: 用户基金数据、持仓信息
- 交互: 添加基金、查看详情、删除基金

#### PerformanceChart.vue
- 功能: 使用ECharts展示基金历史业绩
- 特性: 支持多周期切换 (1月/3月/6月/1年/3年/全部)
- 数据: 基金净值走势 + 对比指数

#### PortfolioSummary.vue
- 功能: 展示投资组合汇总
- 数据: 总资产、今日收益、总收益、基金数量
- 图表: 持仓分布饼图

---

## 12. 依赖关系图

### 12.1 Maven依赖

```
backend/pom.xml
│
├── org.springframework.boot:spring-boot-starter-web
│   └── 内嵌Tomcat
│
├── org.mybatis.spring.boot:mybatis-spring-boot-starter:2.3.2
│   └── MyBatis核心
│
├── com.mysql:mysql-connector-j
│   └── MySQL驱动
│
├── com.squareup.okhttp3:okhttp:4.12.0
│   └── HTTP客户端
│
├── com.alibaba.fastjson2:fastjson2:2.0.47
│   └── JSON解析
│
├── org.jsoup:jsoup:1.17.2
│   └── HTML解析
│
├── org.springframework.boot:spring-boot-starter-security
│   └── 安全框架
│
├── io.jsonwebtoken:jjwt:0.11.5
│   └── JWT实现
│
├── org.springframework.boot:spring-boot-starter-data-redis
│   └── Redis缓存
│
└── org.springframework.boot:spring-boot-starter-mail
    └── 邮件服务
```

### 12.2 NPM依赖

```
frontend/package.json
│
├── vue: ^3.5.32
│   └── 前端框架
│
├── axios: ^1.15.0
│   └── HTTP客户端
│
├── echarts: ^6.0.0
│   └── 数据可视化
│
├── @vitejs/plugin-vue: ^6.0.6
│   └── Vite Vue插件
│
└── vite: ^8.0.8
    └── 构建工具
```

---

## 13. 错误处理

### 13.1 统一响应格式

**ApiResponse.java**：

```java
@Data
public class ApiResponse<T> {
    private int code;      // 状态码
    private String message; // 消息
    private T data;        // 数据
    
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}
```

### 13.2 异常处理

- **业务异常**: 在Service层抛出 `RuntimeException`
- **全局异常**: 通过 `@ControllerAdvice` 统一处理
- **第三方API异常**: 在 `FundDataService` 中捕获并记录日志

---

## 14. 安全注意事项

1. **密码加密**: 使用BCryptPasswordEncoder加密存储
2. **JWT令牌**: 使用HS256算法，设置24小时过期时间
3. **敏感信息**: 邮箱密码等配置信息不应提交到版本控制
4. **跨域限制**: 生产环境应配置具体的允许来源
5. **验证码机制**: 注册时使用邮件验证码防止恶意注册

---

## 15. 注意事项

1. 第三方接口为公开接口，可能存在调用频率限制
2. 盘后净值同步依赖第三方接口数据更新，存在一定延迟
3. 本系统仅供个人学习使用，不构成投资建议
4. 投资有风险，入市需谨慎

---

*本文档由Code Wiki自动生成，最后更新时间: 2024*

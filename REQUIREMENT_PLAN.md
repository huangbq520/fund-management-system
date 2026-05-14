# 用户每日收益统计功能 — 需求分析与实现计划

## 一、需求概述

为"酱菜养基"系统开发用户每日收益统计模块，对用户每一只持仓基金单独记录每日收益数据，自动定时生成，供前端绘制收益曲线图表。

---

## 二、需求详细分析

### 2.1 业务需求

| 编号 | 需求描述 | 优先级 |
|------|----------|--------|
| R1 | 按用户 + 基金 + 日期维度，记录每日收益数据 | P0 |
| R2 | 记录字段包括：当日盈亏金额、当日收益率、当日单位净值、持有份额、持有市值 | P0 |
| R3 | 每日凌晨自动执行统计任务（建议 00:10），统计上一交易日数据 | P0 |
| R4 | 同一天、同一用户、同一只基金只生成一条记录，不可重复 | P0 |
| R5 | 提供接口：查询用户整体每日收益汇总（所有基金合计，按日期） | P0 |
| R6 | 提供接口：查询指定单只基金每日收益明细（按日期） | P0 |
| R7 | 数据结构适配前端 ECharts 折线图展示（日期为 X 轴，收益/收益率为 Y 轴） | P0 |
| R8 | 不改动现有业务表（`fund`、`user_fund`），新增独立统计表 | P0 |
| R9 | 当用户新增持仓基金时，能够回填该基金历史收益数据 | P2 |

### 2.2 数据流分析

```
每日 00:10 定时触发
    │
    ▼
┌─────────────────────────────────────────┐
│  DailyProfitScheduler                   │
│  遍历所有 user_fund 记录                 │
│  对每条记录：                           │
│    1. 获取 fund_code + user_id          │
│    2. 查询当日净值（东方财富历史API）     │
│    3. 计算当日收益 = holdShare ×         │
│       (todayNAV - yesterdayNAV)         │
│    4. 计算收益率 = (todayNAV -           │
│       yesterdayNAV) / yesterdayNAV × 100│
│    5. INSERT INTO fund_daily_profit     │
│       ON DUPLICATE KEY skip             │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│  fund_daily_profit 表                   │
│  唯一约束: (user_id, fund_code,          │
│             record_date)                │
└─────────────────────────────────────────┘
    │
    ├──► GET /api/fund/daily-profit/overall
    │    用户整体每日收益（折线图）          │
    │
    └──► GET /api/fund/daily-profit/{fundCode}
         单只基金每日收益（折线图）          │
```

### 2.3 收益计算公式

```
当日净值（todayNAV）    = 东方财富历史净值 API → Data_netWorthTrend 最后一条
昨日净值（yesterdayNAV） = Data_netWorthTrend 倒数第二条
当日收益（dailyProfit）  = holdShare × (todayNAV - yesterdayNAV)
当日收益率（dailyReturn）= (todayNAV - yesterdayNAV) / yesterdayNAV × 100
持有市值（holdAmount）   = holdShare × todayNAV
```

---

## 三、数据库设计

### 3.1 新建表 `fund_daily_profit`

```sql
CREATE TABLE IF NOT EXISTS `fund_daily_profit` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `fund_code` VARCHAR(10) NOT NULL COMMENT '基金代码',
    `fund_name` VARCHAR(50) COMMENT '基金名称（冗余，方便查询）',
    `record_date` DATE NOT NULL COMMENT '统计日期（净值日期）',
    `daily_profit` DECIMAL(10, 2) DEFAULT 0 COMMENT '当日收益金额',
    `daily_return_rate` DECIMAL(10, 4) DEFAULT 0 COMMENT '当日收益率（%）',
    `net_value` DECIMAL(10, 4) DEFAULT 0 COMMENT '当日单位净值',
    `hold_share` DECIMAL(10, 2) DEFAULT 0 COMMENT '当日持有份额',
    `hold_amount` DECIMAL(10, 2) DEFAULT 0 COMMENT '当日持有市值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_fund_date` (`user_id`, `fund_code`, `record_date`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_fund` (`user_id`, `fund_code`),
    INDEX `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金每日收益统计表';
```

### 3.2 设计要点

| 要点 | 说明 |
|------|------|
| **唯一约束** | `uk_user_fund_date (user_id, fund_code, record_date)` 保证不重复 |
| **冗余字段** | `fund_name` 和 `hold_share` 冗余存储，避免查询时 JOIN，提升查询速度 |
| **不修改旧表** | 完全独立于 `fund` / `user_fund`，零侵入 |
| **索引设计** | `idx_user_id` 用于整体查询，`idx_user_fund` 用于单基金查询 |

---

## 四、后端设计

### 4.1 新增文件清单

```
backend/src/main/java/com/fund/
├── entity/FundDailyProfit.java          # 实体类
├── mapper/FundDailyProfitMapper.java    # MyBatis Mapper
├── service/DailyProfitService.java      # 业务服务
├── scheduler/DailyProfitScheduler.java  # 定时任务（追加到现有 Scheduler）
└── controller/DailyProfitController.java # REST 接口
```

### 4.2 实体类 FundDailyProfit

```java
@Data
public class FundDailyProfit {
    private Long id;
    private Long userId;
    private String fundCode;
    private String fundName;
    private Date recordDate;
    private BigDecimal dailyProfit;
    private BigDecimal dailyReturnRate;
    private BigDecimal netValue;
    private BigDecimal holdShare;
    private BigDecimal holdAmount;
    private Date createTime;
}
```

### 4.3 Mapper FundDailyProfitMapper

```java
@Mapper
public interface FundDailyProfitMapper {
    // 插入（防重复）
    @Insert("INSERT IGNORE INTO fund_daily_profit (user_id, fund_code, fund_name, record_date, " +
            "daily_profit, daily_return_rate, net_value, hold_share, hold_amount, create_time) " +
            "VALUES (#{userId}, #{fundCode}, #{fundName}, #{recordDate}, #{dailyProfit}, " +
            "#{dailyReturnRate}, #{netValue}, #{holdShare}, #{holdAmount}, NOW())")
    int insert(FundDailyProfit record);

    // 查询用户整体每日收益（所有基金合计）
    @Select("SELECT record_date, SUM(daily_profit) as daily_profit, " +
            "SUM(hold_amount) as hold_amount, " +
            "AVG(daily_return_rate) as daily_return_rate " +
            "FROM fund_daily_profit WHERE user_id = #{userId} " +
            "GROUP BY record_date ORDER BY record_date ASC")
    List<FundDailyProfit> selectOverallByUserId(@Param("userId") Long userId);

    // 查询单只基金每日收益
    @Select("SELECT * FROM fund_daily_profit " +
            "WHERE user_id = #{userId} AND fund_code = #{fundCode} " +
            "ORDER BY record_date ASC")
    List<FundDailyProfit> selectByUserAndFund(
        @Param("userId") Long userId,
        @Param("fundCode") String fundCode
    );
}
```

### 4.4 Service DailyProfitService

核心方法：

```java
@Service
public class DailyProfitService {

    // 定时任务调用：统计所有用户持仓基金的昨日收益
    public void calculateDailyProfit() {
        // 1. 查询所有 user_fund 记录（hold_share > 0）
        // 2. 判断昨日是否为交易日（调用 TradingDayChecker）
        // 3. 对每条记录：
        //    a. 调用 FundDataService 获取历史净值
        //    b. 取最后两条：todayNAV（最新）、yesterdayNAV（次新）
        //    c. 计算收益和收益率
        //    d. INSERT IGNORE 入库
        // 4. 记录执行日志
    }

    // 查询用户整体每日收益
    public List<DailyProfitVO> getOverallDailyProfit(Long userId) { ... }

    // 查询单基金每日收益
    public List<FundDailyProfit> getFundDailyProfit(Long userId, String fundCode) { ... }
}
```

### 4.5 API 接口设计

#### 接口 1：用户整体每日收益

```
GET /api/fund/daily-profit/overall?period=3month
Headers: Authorization: Bearer {token}

Query:
│ period │ 时间周期：1month / 3month / 6month / 1year / 3year / all（默认 6month）│

Response:
{
  "code": 200,
  "data": {
    "summary": {
      "totalProfit": 567.80,
      "avgDailyProfit": 12.34,
      "maxDailyProfit": 89.00,
      "maxDailyLoss": -45.00,
      "maxProfitDate": "2026-05-01",
      "maxLossDate": "2026-04-15"
    },
    "dailyList": [
      {
        "recordDate": "2026-05-01",
        "dailyProfit": 156.80,
        "dailyReturnRate": 1.23,
        "holdAmount": 12800.00
      }
    ]
  }
}
```

#### 接口 2：单只基金每日收益（含累计收益曲线数据）

```
GET /api/fund/daily-profit/{fundCode}?period=3month
Headers: Authorization: Bearer {token}

Query:
│ period │ 时间周期：1month / 3month / 6month / 1year / 3year / all（默认 6month）│

Response:
{
  "code": 200,
  "data": {
    "fundCode": "002207",
    "fundName": "前海开源金银珠宝混合C",
    "holdShare": 1494.49,
    "costPrice": 3.4002,
    "summary": {
      "totalProfit": 234.56,
      "totalReturnRate": 12.35,
      "avgDailyProfit": 5.10,
      "maxDailyProfit": 45.30,
      "maxDailyLoss": -38.85,
      "tradingDays": 46
    },
    "profitCurve": [
      {
        "recordDate": "2026-03-15",
        "dailyProfit": 12.50,
        "cumulativeProfit": 0.00,
        "dailyReturnRate": 0.85,
        "netValue": 2.9530
      }
    ],
    "detailList": [
      {
        "recordDate": "2026-05-13",
        "dailyProfit": -38.85,
        "dailyReturnRate": -1.26,
        "netValue": 3.0530,
        "holdShare": 1494.49,
        "holdAmount": 4562.68
      }
    ]
  }
}
```

> **字段说明**：
> - `profitCurve` 用于绘制收益曲线，每条记录包含 `cumulativeProfit`（从起始日累计至今的收益）
> - `detailList` 用于下方明细列表，倒序排列（最新在前），展示每日详细数据
> - `summary` 提供统计摘要：累计收益、累计收益率、日均收益、最大单日盈亏

### 4.6 定时任务 Cron 表达式

```
每天凌晨 00:10 执行（避开 00:00 高峰）：
0 10 0 * * ?
```

仅在交易日执行（周一至周五）：

```java
@Scheduled(cron = "0 10 0 * * MON-FRI")
public void syncDailyProfit() {
    if (tradingDayChecker.isYesterdayTradingDay()) {
        dailyProfitService.calculateDailyProfit();
    }
}
```

---

## 五、分阶段实施计划

### 阶段 1：数据库建表（P0）

| 任务 | 文件 | 说明 |
|------|------|------|
| 1.1 | `schema.sql` 追加 DDL | 新增 `fund_daily_profit` 建表语句 |
| 1.2 | 执行建表 SQL | 在 MySQL 中创建表 |

**验证**：`SHOW CREATE TABLE fund_daily_profit` 确认表结构

---

### 阶段 2：后端实体与 Mapper（P0）

| 任务 | 文件 | 说明 |
|------|------|------|
| 2.1 | `entity/FundDailyProfit.java` | 新建实体类 |
| 2.2 | `mapper/FundDailyProfitMapper.java` | 新建 Mapper，含 insert / selectOverall / selectByFund |
| 2.3 | `vo/DailyProfitVO.java` | 前端视图对象（可选，也可复用实体） |

**验证**：编写简单单元测试或直接调用 Mapper 验证 CRUD

---

### 阶段 3：业务服务 DailyProfitService（P0）

| 任务 | 文件 | 说明 |
|------|------|------|
| 3.1 | `service/DailyProfitService.java` | 核心服务：收益计算 + 统计入库 |
| 3.2 | 实现 `calculateDailyProfit()` | 批量遍历 user_fund，调用净值 API，计算并入库 |
| 3.3 | 实现 `getOverallDailyProfit()` | 用户整体聚合查询 |
| 3.4 | 实现 `getFundDailyProfit()` | 单基金明细查询 |

**验证**：手动调用 `calculateDailyProfit()`，检查数据库是否正确插入记录

---

### 阶段 4：定时任务（P0）

| 任务 | 文件 | 说明 |
|------|------|------|
| 4.1 | `scheduler/DailyProfitScheduler.java` | 新建定时任务类 |
| 4.2 | 配置 Cron: `0 10 0 * * MON-FRI` | 交易日 00:10 执行 |
| 4.3 | 交易日判断 | 调用 TradingDayChecker 验证昨日是否为交易日 |

**验证**：调整 Cron 为每分钟执行，观察日志输出和数据库写入

---

### 阶段 5：REST API 接口（P0）

| 任务 | 文件 | 说明 |
|------|------|------|
| 5.1 | `controller/DailyProfitController.java` | 新建控制器 |
| 5.2 | `GET /api/fund/daily-profit/overall` | 用户整体每日收益，支持 period 参数筛选，返回累计曲线+摘要 |
| 5.3 | `GET /api/fund/daily-profit/{fundCode}` | 单基金每日收益，支持 period 参数筛选，返回 profitCurve + detailList + summary |
| 5.4 | `vo/DailyProfitVO.java` | 视图对象：DailyProfitVO.Summary、DailyProfitVO.CurvePoint、DailyProfitVO.DetailItem |

**验证**：用 curl 调用接口，检查返回的 JSON 数据结构包含所有必要字段

---

### 阶段 6：前端「我的收益」标签页（P1）

**位置**：基金详情弹窗 [FundDetailModal.vue](frontend/src/components/FundDetailModal.vue) 中，在现有 "业绩走势" 标签旁新增 "我的收益" 标签。

#### 6.1 标签结构

```
┌──────────────────────────────────────────────────────┐
│  [基本信息]  [持仓股票]  [业绩走势]  [我的收益]        │  ← 我的收益 与 业绩走势 并列
└──────────────────────────────────────────────────────┘
```

#### 6.2 页面布局（「我的收益」标签页内部）

```
┌──────────────────────────────────────────────────────┐
│  收益摘要卡片                                         │
│  ┌──────────┬──────────┬──────────┬──────────┐      │
│  │ 累计收益  │ 累计收益率 │ 日均收益  │最大单日收益│      │
│  │ +234.56  │ +12.35%  │  +5.10   │ +45.30   │      │
│  └──────────┴──────────┴──────────┴──────────┘      │
│                                                       │
│  时间周期筛选： [近1月] [近3月] [近6月] [近1年] [近3年] [全部] │
│                                                       │
│  累计收益曲线（ECharts 折线图）                        │
│  ┌────────────────────────────────────────────┐      │
│  │  📈 累计收益曲线                            │      │
│  │     /‾‾‾\                                   │      │
│  │    /      \    /‾‾‾‾\                       │      │
│  │   /        \__/      \___                   │      │
│  │  ─────────────────────────────              │      │
│  │  X轴: 日期   Y轴: 累计收益（元）              │      │
│  └────────────────────────────────────────────┘      │
│                                                       │
│  每日收益明细列表（倒序，最新在前）                     │
│  ┌────────────────────────────────────────────┐      │
│  │ 日期       净值      收益       收益率      │      │
│  │ 05-13     3.0530   -38.85     -1.26%      │      │
│  │ 05-12     3.0790    +8.50     +0.58%      │      │
│  │ 05-09     3.0690   +12.30     +0.84%      │      │
│  │ ...                                       │      │
│  └────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────┘
```

#### 6.3 新建 / 修改文件

| 任务 | 文件 | 说明 |
|------|------|------|
| 6.1 | `api/index.js` | 新增 `getFundDailyProfit(code, period)` / `getOverallDailyProfit(period)` |
| 6.2 | `stores/fundStore.js` | 新增 `dailyProfitData{}` state + `fetchFundDailyProfit(code, period)` action |
| 6.3 | `components/MyProfitTab.vue` | **新建**「我的收益」标签页组件，包含：摘要卡片 + 时间周期筛选 + 累计收益曲线 + 明细列表 |
| 6.4 | `components/FundDetailModal.vue` | 修改标签栏，新增「我的收益」标签，切换时渲染 `MyProfitTab` |

#### 6.4 MyProfitTab.vue 组件设计

```
Props:
  fundCode: String    — 基金代码

内部状态:
  period: '6month'    — 当前时间周期
  chartData: null     — API 返回的 profitCurve + summary + detailList
  loading: false

生命周期:
  onMounted → fetchData()
  watch(period) → fetchData()

子组件区:
  1. ProfitSummaryCards   — 4 个摘要指标卡片
  2. PeriodTabs           — 6 个时间周期切换按钮
  3. CumulativeProfitChart — ECharts 累计收益曲线（window.echarts）
  4. DailyDetailTable     — 每日收益明细表格（倒序）

图表配置:
  - X 轴: 日期（formatDate→MM-DD）
  - Y 轴: 累计收益（元），带 +/− 符号
  - 曲线颜色: 正收益 #ef4444（红），负收益 #22c55e（绿）
  - Tooltip: 日期 + 当日收益 + 累计收益 + 收益率
  - 平滑曲线: smooth 0.4
  - 基准线: y=0 水平虚线
  - 面积填充: 正收益区域浅红，负收益区域浅绿
```

#### 6.5 数据流

```
用户点击「我的收益」标签
  │
  ▼
MyProfitTab onMounted
  │
  ▼
fundStore.fetchFundDailyProfit(fundCode, '6month')
  │
  ▼
GET /api/fund/daily-profit/{fundCode}?period=6month
  │
  ▼
返回 { summary, profitCurve, detailList }
  │
  ├──► ProfitSummaryCards ← summary
  ├──► CumulativeProfitChart ← profitCurve（绘制 ECharts）
  └──► DailyDetailTable ← detailList
```

#### 6.6 与现有「业绩走势」标签的区分

| 对比维度 | 业绩走势（现有） | 我的收益（新增） |
|----------|-----------------|-----------------|
| 数据来源 | 基金公开历史净值（东方财富 API） | 用户持仓收益记录（fund_daily_profit 表） |
| 展示内容 | 基金净值增长率 vs 沪深300 vs 同类平均 | 用户实际累计收益金额 |
| Y 轴含义 | 相对基准的涨跌幅 % | 累计盈亏金额（元） |
| 基准线 | 无（以起始日为 0%） | y=0 水平线（盈亏分界） |
| 意义 | 评价基金本身表现 | 评价用户在这只基金上赚了多少钱 |

**两个标签页互补：**
- 「业绩走势」看基金好不好
- 「我的收益」看用户赚没赚



---

### 阶段 7：历史数据回填（P2，可选）

| 任务 | 说明 |
|------|------|
| 7.1 | 编写一次性回填脚本，遍历所有历史交易日，补充缺失的每日收益记录 |
| 7.2 | 新增基金时，自动回填该基金近 30 天收益数据 |

---

## 六、优先级排序

```
P0（必须，本迭代完成）：
  阶段 1 → 阶段 2 → 阶段 3 → 阶段 4 → 阶段 5

P1（重要，本迭代完成）：
  阶段 6（前端图表）

P2（后续迭代）：
  阶段 7（历史回填）
```

---

## 七、风险与注意事项

| 风险 | 应对措施 |
|------|----------|
| 外部 API（东方财富）超时或不可用 | 单条失败不阻断整体，记录日志，下次定时任务自动补全 |
| 历史净值数据不足 2 条（新基金） | 跳过该基金，等待后续数据积累 |
| 定时任务执行时间过长 | 使用线程池并行处理，设置总超时 30 分钟 |
| 数据重复 | `INSERT IGNORE` + 唯一约束双重保证 |
| 非交易日误统计 | 调用 TradingDayChecker 判断昨日是否为交易日 |

---

## 八、前端交互示意

```
基金详情弹窗 —「我的收益」标签页
┌──────────────────────────────────────────────────────────┐
│  [基本信息]  [持仓股票]  [业绩走势]  [我的收益]            │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌───────────┬───────────┬───────────┬───────────┐       │
│  │  📊 累计收益│  📈 累计收益│  📐 日均收益│  🏆 最大单日  │       │
│  │  +234.56  │  +12.35%  │  +5.10    │  +45.30    │       │
│  └───────────┴───────────┴───────────┴───────────┘       │
│                                                           │
│  时间周期：[近1月] [近3月] [近6月]● [近1年] [近3年] [全部] │
│                                                           │
│  累计收益曲线                                              │
│  ┌──────────────────────────────────────────────────┐    │
│  │  +200                                    ╱        │    │
│  │  +150                              ╱───╱         │    │
│  │  +100                    ╱────────╱              │    │
│  │   +50          ╱────────╱                        │    │
│  │     0  ───────╱─────────────────────────────    │    │
│  │   -50    ╲───╱                                    │    │
│  │         ╲──╱                                      │    │
│  │  ┌──────┬──────┬──────┬──────┬──────┬──────┐    │    │
│  │  03-15  04-01  04-15  05-01  05-13              │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
│  每日收益明细（46 个交易日）                                │
│  ┌──────────────────────────────────────────────────┐    │
│  │  日期       净值      当日收益    收益率    市值    │    │
│  │  05-13     3.0530    -38.85    -1.26%   4562.68  │    │
│  │  05-12     3.0790     +8.50    +0.58%   4601.53  │    │
│  │  05-09     3.0690    +12.30    +0.84%   4586.59  │    │
│  │  05-08     3.0560    +25.10    +1.72%   4567.89  │    │
│  │  ...                                              │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

### 「我的收益」与「业绩走势」对比

```
┌─────────────────────────────────────────────────────────┐
│                  业绩走势 │ 我的收益                      │
│  ────────────────────────┼──────────────────────────    │
│  数据：基金公开历史净值     │ 用户持仓实际收益             │
│   Y轴：相对基准涨跌幅 %     │ 累计盈亏金额（元）           │
│  曲线：本基金 vs 沪深300   │ 累计收益曲线                 │
│  意义：评价基金好坏         │ 反映实际赚钱与否             │
└─────────────────────────────────────────────────────────┘
```

---

*文档版本: v1.1 | 更新日期: 2026-05-13 | 更新内容: 细化前端「我的收益」标签页设计*

# 基金持仓编辑 — 需求分析与设计文档

> 版本 v1.0 | 2026-05-14

## 一、需求概述

重构基金持仓编辑功能，聚焦核心刚需：新建、加仓、减仓、清仓，支持两种录入模式双向联动，以及基础定投管理。不做批量、分组、归档等冗余功能。

---

## 二、功能清单

### 2.1 基础持仓操作

| 操作 | 描述 | 触发方式 |
|------|------|----------|
| **新建持仓** | 首次为一只基金录入持仓数据 | 添加基金后，点击"编辑持仓" |
| **加仓** | 增加持有份额，更新平均成本价 | 持仓列表行内操作 |
| **减仓** | 减少持有份额（部分卖出），更新成本 | 持仓列表行内操作 |
| **清仓** | 全部卖出，份额归零，保留历史收益记录 | 持仓列表行内操作 |

### 2.2 双模式录入（核心设计）

#### 模式一：份额 + 成本价录入

```
用户输入：
  - 持有份额（holdShare）
  - 平均成本价（costPrice）
  - 买入日期（buyDate）

系统自动计算：
  - 持仓金额 = holdShare × costPrice
  - 当前市值 = holdShare × 实时净值
  - 持仓收益 = 当前市值 - 持仓金额
  - 持仓收益率 = 持仓收益 / 持仓金额 × 100%
```

#### 模式二：金额 + 盈亏录入

```
用户输入：
  - 持仓总金额（holdAmount）
  - 持仓盈亏 或 持仓收益率（二选一，联动换算）
  - 买入日期（buyDate）

系统自动反推：
  - 当前市值 = holdAmount（模式二中视为当前市值）
  - 实时净值 = API 获取
  - 持有份额 = 当前市值 / 实时净值
  - 成本金额 = 当前市值 - 持仓盈亏
  - 平均成本价 = 成本金额 / 持有份额
```

#### 双向联动规则

```
切换 模式一 → 模式二：
  自动回填：
    holdAmount ← 当前市值（holdShare × 实时净值）
    profit ← 当前市值 - holdShare × costPrice
    profitRate ← profit / (holdShare × costPrice) × 100

切换 模式二 → 模式一：
  自动回填：
    holdShare ← 反推份额（holdAmount / 实时净值）
    costPrice ← 反推成本价（成本金额 / holdShare）
```

```
┌─────────────────────────────────────┐
│  [份额+成本价]  [金额+盈亏]          │  ← 模式切换 Tab
├─────────────────────────────────────┤
│                                     │
│  模式一：                           │
│  持有份额: [________] 份            │
│  成本价:   [________] 元            │
│  ─────────────────────              │
│  持仓金额: ¥5,081.56  (自动计算)    │
│                                     │
│  模式二：                           │
│  持仓总金额: [________] 元          │
│  持仓盈亏:   [________] 元          │
│    或 收益率: [________] %          │
│  ─────────────────────              │
│  持有份额: 1494.49 份  (自动反推)   │
│  成本价:   3.4002 元   (自动反推)   │
└─────────────────────────────────────┘
```

### 2.3 加仓 / 减仓

加仓和减仓是修改持仓的特殊场景，通过调整份额和成本来实现。

#### 加仓公式

```
新份额 = 原份额 + 加仓份额
新成本价 = (原成本 × 原份额 + 加仓成本 × 加仓份额) / 新份额
新持仓金额 = 新份额 × 新成本价
```

#### 减仓公式

```
新份额 = 原份额 - 减仓份额（新份额 > 0 则为减仓，= 0 则提示使用清仓）
新成本价 = 原成本价（不变，卖出不影响剩余份额的成本基础）
新持仓金额 = 新份额 × 新成本价
```

#### 清仓

```
新份额 = 0
新成本价 = 0
新持仓金额 = 0
保留 fund_daily_profit 历史收益记录不删除
保留 fund 表记录（可重新买入）
```

### 2.4 定投计划（基础版）

| 操作 | 描述 |
|------|------|
| **新增计划** | 设置：定投基金、每期金额、定投周期（每周/每两周/每月）、定投日期 |
| **暂停/恢复** | 暂停后不再自动执行，恢复后继续 |
| **份额并入** | 定投执行后，自动将买入份额并入持仓（更新 holdShare 和 costPrice） |

```
定投计划表（新表 fund_investment_plan）：
  - 基金代码
  - 每期金额
  - 定投周期（WEEKLY / BIWEEKLY / MONTHLY）
  - 定投日期（周几 或 每月几号）
  - 状态（ACTIVE / PAUSED / STOPPED）
  - 下次执行日期
  - 累计定投期数
  - 累计定投金额
```

> 定投执行由 DailyProfitScheduler 在 22:00 顺带处理：检查到期定投计划 → 模拟买入 → 自动并入持仓 → 刷新收益

---

## 三、数据流设计

### 3.1 新建/编辑持仓 → 联动更新

```
用户保存持仓
  │
  ▼
POST /api/fund/holding/update
  │ mode=SHARES 或 mode=AMOUNT
  │ 写入 user_fund: holdShare, costPrice, holdAmount
  │
  ▼
返回更新后的 FundHoldingVO（含实时 currentValue / todayProfit / profitRate）
  │
  ├──► 前端更新持仓列表行
  ├──► 前端刷新 PortfolioSummary
  └──► 后端触发该基金每日收益重算（当天 record_date 覆盖写入 fund_daily_profit）
```

### 3.2 加仓 / 减仓 → 联动更新

```
POST /api/fund/holding/adjust
  │ type=BUY（加仓）或 SELL（减仓）
  │ 传入 adjustShare, adjustCost（加仓时必填成本）
  │
  ▼
计算新份额 / 新成本价 → 写入 user_fund
  │
  ▼
返回更新后的 FundHoldingVO
  │
  ├──► 前端更新持仓列表
  └──► 记录操作日志到 fund_holding_log（新表，可选）
```

### 3.3 清仓

```
POST /api/fund/holding/clear
  │ fundCode
  │
  ▼
holdShare = 0, costPrice = 0, holdAmount = 0 → 写入 user_fund
fund 表保留（fund 表不删除）
fund_daily_profit 历史记录保留
```

---

## 四、API 设计

### 4.1 新增 / 修改接口

#### 编辑持仓（统一入口，支持双模式）

```
POST /api/fund/holding/update
Authorization: Bearer {token}

Request:
{
  "fundCode": "002207",
  "mode": "SHARES" | "AMOUNT",
  
  // 模式一 SHARES
  "holdShare": 1494.49,
  "costPrice": 3.4002,
  
  // 模式二 AMOUNT
  "holdAmount": 5081.56,
  "profit": 234.56,           // 盈亏金额（与 profitRate 二选一）
  "profitRate": 4.84,         // 收益率 %（与 profit 二选一）
  
  "buyDate": "2026-01-15"
}

Response:
{
  "code": 200,
  "data": {
    "fundCode": "002207",
    "fundName": "前海开源金银珠宝混合C",
    "holdShare": 1494.49,
    "costPrice": 3.4002,
    "holdAmount": 5081.56,
    "currentValue": 4562.68,
    "todayProfit": -38.85,
    "profitRate": -9.74
  }
}
```

#### 加仓 / 减仓

```
POST /api/fund/holding/adjust
Authorization: Bearer {token}

Request:
{
  "fundCode": "002207",
  "type": "BUY" | "SELL",
  
  // 加仓
  "adjustShare": 100.00,     // 加仓份额
  "adjustCost": 3.5000,      // 加仓成本价
  
  // 减仓
  "adjustShare": 50.00,      // 减仓份额
  "adjustDate": "2026-05-14"
}

Response:
{
  "code": 200,
  "data": { ...FundHoldingVO }
}
```

#### 清仓

```
POST /api/fund/holding/clear
Authorization: Bearer {token}

Request:
{
  "fundCode": "002207"
}

Response:
{
  "code": 200,
  "message": "清仓成功"
}
```

### 4.2 定投接口

```
POST   /api/fund/plan/create    — 创建定投计划
POST   /api/fund/plan/pause     — 暂停
POST   /api/fund/plan/resume    — 恢复
DELETE /api/fund/plan/{id}      — 删除
GET    /api/fund/plan/list      — 定投计划列表
```

---

## 五、数据库变更

### 5.1 user_fund 表（不变）

现有字段已经足够：`hold_share`、`cost_price`、`hold_amount`、`buy_date`。

### 5.2 新建表：fund_holding_log（可选，P2）

```sql
CREATE TABLE IF NOT EXISTS `fund_holding_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `fund_code` VARCHAR(10) NOT NULL,
    `type` VARCHAR(10) NOT NULL COMMENT 'BUY/SELL/CLEAR/MANUAL',
    `adjust_share` DECIMAL(10,2) COMMENT '变动份额（正=加仓，负=减仓）',
    `adjust_cost` DECIMAL(10,4) COMMENT '操作时成本价',
    `before_share` DECIMAL(10,2) COMMENT '操作前份额',
    `after_share` DECIMAL(10,2) COMMENT '操作后份额',
    `before_cost` DECIMAL(10,4) COMMENT '操作前成本价',
    `after_cost` DECIMAL(10,4) COMMENT '操作后成本价',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_fund` (`user_id`, `fund_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓操作日志';
```

### 5.3 新建表：fund_investment_plan

```sql
CREATE TABLE IF NOT EXISTS `fund_investment_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `fund_code` VARCHAR(10) NOT NULL,
    `amount_per_period` DECIMAL(10,2) NOT NULL COMMENT '每期定投金额',
    `cycle` VARCHAR(10) NOT NULL COMMENT 'WEEKLY/BIWEEKLY/MONTHLY',
    `cycle_day` INT NOT NULL COMMENT '周几(1-7) 或 每月几号(1-28)',
    `status` VARCHAR(10) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/STOPPED',
    `next_execute_date` DATE COMMENT '下次执行日期',
    `total_periods` INT DEFAULT 0 COMMENT '累计已执行期数',
    `total_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '累计定投金额',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user` (`user_id`),
    UNIQUE KEY `uk_user_fund` (`user_id`, `fund_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定投计划表';
```

---

## 六、后端文件变更

### 新建文件

| 文件 | 说明 |
|------|------|
| `entity/FundHoldingLog.java` | 操作日志实体 |
| `entity/FundInvestmentPlan.java` | 定投计划实体 |
| `mapper/FundHoldingLogMapper.java` | 操作日志 Mapper |
| `mapper/FundInvestmentPlanMapper.java` | 定投计划 Mapper |
| `controller/HoldingController.java` | 持仓操作控制器（新建/编辑/加仓/减仓/清仓） |
| `controller/InvestmentPlanController.java` | 定投控制器 |
| `service/InvestmentPlanService.java` | 定投业务逻辑 |

### 修改文件

| 文件 | 变更 |
|------|------|
| `FundHoldingService.java` | 新增模式二反推计算、加仓/减仓/清仓方法 |
| `FundDataService.java` | 无需改动 |
| `schema.sql` | 追加新表 DDL |
| `DailyProfitScheduler.java` | 追加定投执行逻辑 |

---

## 七、前端 UI 设计原则

### 7.1 核心原则

- **一个弹窗完成所有操作**：编辑、加仓、减仓共用一个弹窗，通过顶部 Segmented Control 切换场景
- **模式切换零闪烁**：份额/金额两种模式用 CSS transition 平滑过渡，不重新挂载组件
- **操作列最多 2 个按钮**：主按钮"编辑"直接打开弹窗，次级"更多"下拉收起减仓/清仓
- **计算结果即时可见**：输入框失焦即触发计算，不用多点一次"计算"按钮

### 7.2 弹窗结构

```
┌──────────────────────────────────────────┐
│  002207 前海开源金银珠宝混合C        [×]  │
├──────────────────────────────────────────┤
│                                          │
│  ○ 编辑持仓   ○ 加仓   ○ 减仓            │  ← Segmented 场景切换
│                                          │
│  实时净值 3.0530  估值 05-13 15:00       │  ← 只读信息栏
│                                          │
│  ┌─ 切换开关 ───────────────────────┐   │
│  │ 份额+成本价  ◉────────○  金额+盈亏 │   │  ← Toggle 滑块
│  └──────────────────────────────────┘   │
│                                          │
│  ┌─ 模式一（份额+成本价）───────────┐   │
│  │                                  │   │
│  │ 持有份额  [1494.49    ] 份       │   │
│  │ 成本价    [3.4002     ] 元       │   │
│  │ 买入日期  [2026-01-15 ]          │   │
│  │ ───────────────────────          │   │
│  │ 持仓金额  ¥5,081.56              │   │
│  │ 当前市值  ¥4,562.68   -10.21%    │   │
│  │                                  │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ┌─ 模式二（金额+盈亏）───────────┐   │
│  │                                  │   │
│  │ 持仓金额  [4562.68    ] 元       │   │
│  │ 盈亏金额  [-518.88    ] 元       │   │
│  │   或切换  [ 收益率输入 ]         │   │
│  │ 买入日期  [2026-01-15 ]          │   │
│  │ ───────────────────────          │   │
│  │ → 份额 1494.49  成本 3.4002     │   │
│  │                                  │   │
│  └──────────────────────────────────┘   │
│                                          │
│                       [取消]  [保存]     │
└──────────────────────────────────────────┘
```

### 7.3 持仓列表操作列

```
当前操作列（只有"编辑"按钮）：

  操作
  ─────
  [编辑]          ← 点击打开弹窗，默认"编辑持仓"场景

弹窗内 Segmented Control 可切换：
  ○ 编辑持仓  → 修改份额/成本/金额
  ○ 加仓      → 输入加仓份额+成本
  ○ 减仓      → 输入减仓份额

"清仓"放在减仓场景底部，一个轻量文本按钮：
  减仓份额输入框下方 → 「清空全部持仓」灰色文字链接
```

### 7.4 前端文件变更

| 文件 | 变更 |
|------|------|
| `components/EditHoldingModal.vue` | **重写**：Segmented 场景切换 + Toggle 模式切换 + 加仓/减仓/清仓合一 |
| `components/InvestmentPlanPanel.vue` | 定投面板（独立组件，在持仓列表下方折叠展开） |
| `HoldingList.vue` | 操作列保持简洁（1 个编辑按钮），不再额外增加按钮 |
| `api/index.js` | 新增 adjust / clear / plan API |
| `stores/fundStore.js` | 新增 adjustPosition / clearPosition actions |

> **删除文件**：不创建独立的 `AdjustPositionModal.vue`，加仓/减仓统一在 EditHoldingModal 中处理。

---

## 八、分阶段实施计划

### 阶段 1：后端 — 模式二反推计算（P0）

| 任务 | 说明 |
|------|------|
| 1.1 `FundHoldingService` | 新增模式二反推逻辑：holdAmount + profit/profitRate → holdShare + costPrice |
| 1.2 `POST /holding/update` | 扩展支持 `mode` 参数，模式二自动反推后写入 user_fund |

### 阶段 2：后端 — 加仓/减仓/清仓（P0）

| 任务 | 说明 |
|------|------|
| 2.1 `POST /holding/adjust` | 加仓：加权平均成本；减仓：份额递减 |
| 2.2 `POST /holding/clear` | 份额归零，保留历史 |
| 2.3 `HoldingController` | 新建控制器或扩展现有 FundController |

### 阶段 3：前端 — 统一编辑弹窗（P0）

| 任务 | 说明 |
|------|------|
| 3.1 `EditHoldingModal.vue` 重写 | 单弹窗三场景 + Toggle 模式切换 + CSS transition 动画 |
| 3.2 `HoldingList.vue` | 操作列简化，只保留"编辑"按钮 |

### 阶段 4：联动刷新（P0）

| 任务 | 说明 |
|------|------|
| 4.1 保存后自动刷新持仓列表 + 组合汇总 |
| 4.2 持仓变更后触发 `fund_daily_profit` 当天记录重算 |

### 阶段 5：定投计划（P1）

| 任务 | 说明 |
|------|------|
| 5.1 数据库建表 `fund_investment_plan` |
| 5.2 后端 CRUD API |
| 5.3 定时执行逻辑（22:00 与每日收益一起） |
| 5.4 前端折叠面板（持仓列表下方） |

---

*文档版本: v1.0 | 创建日期: 2026-05-14*

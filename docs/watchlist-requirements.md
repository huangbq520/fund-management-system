# 用户自选功能需求文档

> 版本：v1.0  
> 日期：2026-06-15  
> 状态：待评审

---

## 1. 功能概述

在现有"我的持仓"基础上，新增**用户自选**功能。自选池与持仓**完全独立**——用户可以自选一只基金但不持有，也可以持有一只基金但不在自选列表中。自选是纯粹的"关注/观察"行为。

---

## 2. 核心规则

| 规则 | 说明 |
|------|------|
| **自选 ≠ 持仓** | 添加自选不产生持仓记录，清仓不影响自选列表 |
| **可重叠** | 同一只基金可以同时存在于自选和持仓中 |
| **自选不参与盈亏计算** | 盈亏汇总、每日收益等仅统计持仓，自选仅供观察 |
| **数据实时刷新** | 交易时段内，自选列表的估算涨幅等字段与持仓列表同步刷新（30s） |

---

## 3. UI 交互设计

### 3.1 Tab 切换

在"我的持仓"标题右侧增加 Tab 切换组件：

```
┌──────────────────────────────────────────────┐
│  我的持仓  │  自选                            │
├──────────────────────────────────────────────┤
│  （下方列表区域随 Tab 切换对应内容）            │
└──────────────────────────────────────────────┘
```

- 默认选中"我的持仓"
- 切换到"自选"时，持仓相关的汇总卡片（资产概览、盈亏汇总、饼图）保持不变，仅下方列表区域切换
- 自选 Tab 上显示当前自选数量角标

### 3.2 自选列表（空态）

当用户尚未添加任何自选基金时，显示空态占位：

```
┌──────────────────────────────────────────────┐
│          📋 还没有自选基金                     │
│   使用顶部搜索栏搜索基金，点击"加入自选"即可    │
└──────────────────────────────────────────────┘
```

### 3.3 自选列表（有数据）

| 列 | 说明 | 排序 | 备注 |
|----|------|------|------|
| ☐ | 多选复选框 | — | 用于对比和批量操作 |
| 基金代码 + 名称 | 代码和名称上下排列或同行展示 | — | 点击名称可打开基金详情弹窗 |
| 关联板块 | 基金关联的行业/概念板块 | 否 | **暂无数据源**，先显示"—"占位 |
| 最新净值 | 最近一个交易日确认净值 | 可排序 | |
| 最新涨幅 | 基于最新确认净值的日涨跌幅 | 可排序 | 红涨绿跌 |
| 估算涨幅 | 盘中实时估算涨跌幅 | 可排序 | 红涨绿跌，非交易时段显示"—" |
| 自选以来 | 自添加以来的累计收益率 | 可排序 | 公式：`(最新净值 - 添加日净值) / 添加日净值 × 100%` |
| 近1周 | 近1周收益率 | 可排序 | |
| 近1月 | 近1月收益率 | 可排序 | |
| 近3月 | 近3月收益率 | 可排序 | |
| 近6月 | 近6月收益率 | 可排序 | |
| 近1年 | 近1年收益率 | 可排序 | |
| 操作 | 移除自选按钮 | — | |

### 3.4 分组管理

在自选列表上方增加分组筛选栏：

```
┌─────────────────────────────────────────────────────┐
│  [全部] [科技赛道] [稳健理财] [消费板块] [+ 新建分组]  │
│                                                     │
│  [🔍 多选] [📊 对比选中] [🗑 批量移除]               │
└─────────────────────────────────────────────────────┘
```

- **全部** — 显示所有自选基金（默认选中）
- **分组标签** — 点击切换筛选
- **+ 新建分组** — 弹出输入框，输入分组名称后创建
- **分组管理** — 长按/右键分组标签可重命名或删除
- **拖拽入组** — 支持将列表中基金拖拽到分组标签上进行归类（后期优化项）

### 3.5 多维度排序

- 点击列头即按该列排序（升序 → 降序 → 取消）
- 默认按"添加时间"倒序排列
- 当前排序状态以列头三角箭头标识

### 3.6 自选基金对比

操作流程：
1. 勾选 2~5 只自选基金
2. 点击"对比选中"按钮
3. 弹出对比弹窗，以表格形式横向对比：

```
┌──────────────────────────────────────────────────────┐
│  基金对比                                  [✕ 关闭]  │
├──────────────────────────────────────────────────────┤
│ 指标          │ 基金A      │ 基金B      │ 基金C      │
├───────────────┼────────────┼────────────┼────────────┤
│ 最新净值      │ 1.2345     │ 2.3456     │ 0.9876     │
│ 估算涨幅      │ +1.23%     │ -0.56%     │ +0.78%     │
│ 自选以来      │ +5.67%     │ -2.34%     │ +12.34%    │
│ 近1周         │ +0.5%      │ -0.3%      │ +1.2%      │
│ 近1月         │ +3.2%      │ -1.5%      │ +5.6%      │
│ 近3月         │ +8.9%      │ -4.2%      │ +15.3%     │
│ 近6月         │ +12.1%     │ -8.7%      │ +22.8%     │
│ 近1年         │ +25.3%     │ -15.2%     │ +35.6%     │
│ 基金类型      │ 股票型      │ 混合型      │ 债券型      │
└──────────────────────────────────────────────────────┘
```

---

## 4. 数据模型设计

### 4.1 新增表：`user_watchlist`（自选列表）

```sql
CREATE TABLE user_watchlist (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    fund_code       VARCHAR(10)  NOT NULL COMMENT '基金代码',
    fund_name       VARCHAR(100) NOT NULL COMMENT '基金名称',
    group_id        BIGINT       NULL     COMMENT '分组ID，NULL表示未分组',
    add_net_value   DECIMAL(10,4) NULL    COMMENT '加入自选时的净值（用于计算自选以来收益）',
    add_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入自选的时间',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序权重，越大越靠前',
    notes           VARCHAR(200) NULL     COMMENT '用户备注',
    UNIQUE KEY uk_user_fund (user_id, fund_code),
    INDEX idx_user_group (user_id, group_id)
) COMMENT '用户自选基金列表';
```

### 4.2 新增表：`watchlist_group`（自选分组）

```sql
CREATE TABLE watchlist_group (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    group_name  VARCHAR(50)  NOT NULL COMMENT '分组名称',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序权重',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) COMMENT '自选分组';
```

### 4.3 与现有表的关系

```
user ──┬── user_fund (持仓)     ← 现有，不变
       │
       ├── fund (跟踪列表)      ← 现有，需评估是否仍有存在价值
       │
       ├── user_watchlist (自选) ← 新增
       │
       └── watchlist_group      ← 新增
```

> **待讨论**：现有 `fund` 表是否保留？建议后续将 `fund` 表逻辑迁移到 `user_watchlist`，避免概念重叠。

---

## 5. API 设计

### 5.1 自选 CRUD

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/watchlist/list` | 获取自选列表（含实时行情） |
| `POST` | `/api/watchlist/add` | 添加自选基金 |
| `POST` | `/api/watchlist/remove` | 移除自选基金 |
| `POST` | `/api/watchlist/batch-add` | 批量添加自选 |
| `POST` | `/api/watchlist/batch-remove` | 批量移除自选 |

### 5.2 分组管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/watchlist/groups` | 获取用户分组列表 |
| `POST` | `/api/watchlist/groups` | 创建分组 |
| `PUT` | `/api/watchlist/groups/{id}` | 修改分组名称 |
| `DELETE` | `/api/watchlist/groups/{id}` | 删除分组（组内基金变为未分组） |
| `PUT` | `/api/watchlist/{id}/group` | 将自选基金移入/移出分组 |

### 5.3 对比

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/watchlist/compare` | 传入基金代码数组，返回对比数据 |

### 5.4 请求/响应示例

#### 添加自选

```
POST /api/watchlist/add
Body: { "fundCode": "000001", "fundName": "华夏成长混合" }

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "fundCode": "000001",
    "fundName": "华夏成长混合",
    "addNetValue": 1.2345,
    "addTime": "2026-06-15 14:30:00"
  }
}
```

#### 获取自选列表

```
GET /api/watchlist/list?groupId=1&sortBy=estimateChange&sortDir=desc

Response:
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "fundCode": "000001",
      "fundName": "华夏成长混合",
      "groupId": 1,
      "groupName": "科技赛道",
      "netValue": 1.2500,           // 最新净值
      "estimateNav": 1.2543,        // 估算净值(盘中)
      "latestChange": 1.25,         // 最新涨幅 %
      "estimateChange": 1.68,       // 估算涨幅 %
      "addNetValue": 1.2000,        // 自选时净值
      "sinceAddReturn": 4.17,       // 自选以来 %
      "return1w": 0.50,
      "return1m": 3.20,
      "return3m": 8.90,
      "return6m": 12.10,
      "return1y": 25.30,
      "addTime": "2026-06-15 14:30:00",
      "notes": null
    }
  ]
}
```

#### 对比接口

```
POST /api/watchlist/compare
Body: { "fundCodes": ["000001", "000002", "000003"] }

Response:
{
  "code": 200,
  "data": {
    "funds": [...],           // 各基金基本信息和行情
    "comparison": { ... }     // 按指标组织的对比数据
  }
}
```

---

## 6. 前端改动范围

### 6.1 新建文件

| 文件 | 说明 |
|------|------|
| `src/components/WatchlistTable.vue` | 自选列表表格组件 |
| `src/components/WatchlistGroupBar.vue` | 分组筛选栏组件 |
| `src/composables/useWatchlist.js` | 自选相关组合式函数 |
| `src/api/watchlist.js` | 自选 API 请求层 |

### 6.2 修改文件

| 文件 | 改动 |
|------|------|
| `src/views/DashboardView.vue` | 增加 Tab 切换逻辑，集成自选组件 |
| `src/components/SearchFund.vue` | 搜索结果增加"加入自选"按钮（与"添加持仓"并列） |
| `src/stores/fundStore.js` | 增加自选相关状态（或新建 `watchlistStore.js`） |
| `src/router/index.js` | 评估是否需要新增路由 |

### 6.3 状态管理

新增 `watchlistStore`（Pinia）：

```js
{
  // 状态
  watchlist: [],           // 自选列表
  groups: [],              // 分组列表
  activeGroupId: null,     // 当前激活的分组筛选
  sortBy: 'addTime',       // 当前排序列
  sortDir: 'desc',         // 排序方向
  selectedIds: [],         // 勾选的基金ID（用于对比和批量操作）
  loading: false,

  // 操作
  fetchWatchlist(),
  addToWatchlist(fundCode, fundName),
  removeFromWatchlist(id),
  batchRemove(ids),
  fetchGroups(),
  createGroup(name),
  deleteGroup(id),
  assignToGroup(watchlistId, groupId),
  compareFunds(codes),
}
```

---

## 7. 搜索流程变更

当前搜索基金后只有一个"添加"按钮。变更后：

```
┌─────────────────────────────────┐
│  搜索结果：华夏成长混合 (000001)  │
│  [加入自选]  [添加持仓]          │
└─────────────────────────────────┘
```

- **加入自选**：仅写入 `user_watchlist` 表，弹出 toast "已加入自选"
- **添加持仓**：走现有流程，写入 `fund` + `user_fund` 表
- 两个按钮互相独立，互不影响

---

## 8. 已确认决策

| # | 问题 | 决策 |
|---|------|------|
| 1 | **关联板块数据源** | 暂无数据源，先显示"—"，后续接入数据源再补齐 |
| 2 | **现有 `fund` 表去留** | 新建 `user_watchlist` 表，旧的 `fund` 表保持不动，仅用于持仓关联，两表独立运作 |
| 3 | **清盘/退市基金处理** | 先不做特殊处理，遇到再解决 |
| 4 | **自选数量上限** | 限制 200 只，与天天基金一致。添加时若已达上限返回错误提示 |

---

## 9. 实现优先级

| 优先级 | 功能模块 | 说明 |
|--------|----------|------|
| P0 | 自选列表基础 CRUD | 添加、移除、列表展示、实时行情 |
| P0 | Tab 切换 UI | 持仓/自选列表切换 |
| P0 | 搜索流程改造 | 搜索→加入自选 |
| P1 | 多维度排序 | 列头点击排序 |
| P1 | 分组管理 | 创建/删除分组、基金归类 |
| P2 | 自选基金对比 | 勾选→对比弹窗 |
| P3 | 拖拽排序/入组 | 体验优化项 |
| P3 | 自选导入/导出 | CSV 批量操作 |
| P3 | 关联板块数据 | 需要先解决数据源 |

---

> 本文档待用户评审确认后进入开发阶段。

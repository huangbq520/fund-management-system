# 大盘指数行情展示弹窗 — 设计文档

## 1. 概述

用户点击首页 MarketIndex 组件中的任意一张指数卡片（上证指数/上证50/深证成指/创业板指/沪深300）时，弹出全屏遮罩弹窗，上半部分展示该指数的基础行情数据，下半部分展示 ECharts 蜡烛K线图（主图）+ 联动成交量柱状图（副图），支持切换K线周期。

---

## 2. 交互流程

```
DashboardView
  └─ MarketIndex (5张指数卡片, 已有)
       └─ 点击卡片 → emit('select', index)
            └─ DashboardView 接收事件 → 打开 IndexQuoteModal
                 └─ IndexQuoteModal (新组件)
                      ├─ 弹窗头部：指数名称 + 代码 + 关闭按钮
                      ├─ 行情概览区：最新价格/涨跌幅 + 6项关键指标卡片
                      ├─ 周期切换条：分钟/日/周/月 等 10 个周期按钮
                      └─ 图表区：
                           ├─ 主图：ECharts K线图 (candlestick)
                           └─ 副图：ECharts 成交量柱状图 (bar, 与主图X轴联动)
```

---

## 3. 后端改动

### 3.1 接口改造：`GET /api/market/kline`

**新增参数 `klt`**（K线周期），当前仅支持 `101`（日线），需扩展支持全部周期。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 指数代码，如 `sh000001` |
| startDate | String | 是 | 开始日期 `yyyyMMdd` |
| endDate | String | 是 | 结束日期 `yyyyMMdd` |
| klt | String | 否 | K线周期，默认 `101`（日线） |

**klt 取值表：**

| klt | 含义 | klt | 含义 |
|-----|------|-----|------|
| 1 | 1分钟 | 60 | 60分钟 |
| 5 | 5分钟 | 101 | 日线 (默认) |
| 15 | 15分钟 | 102 | 周K |
| 30 | 30分钟 | 103 | 月K |
| — | — | 104 | 季K |
| — | — | 105 | 年K |

**改动文件：**
- `MarketIndexService.java` — `getIndexKline()` 方法签名增加 `klt` 参数，URL 模板中加入 `&klt=%s`
- `MarketIndexController.java` — `getIndexKline()` 增加 `@RequestParam(defaultValue = "101") String klt`

### 3.2 新增接口：`GET /api/market/realtime`

返回单只指数的实时行情快照（用于弹窗顶部的概览数据）。复用现有腾讯接口数据，单条解析返回。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 指数代码 |

**响应结构：**
```json
{
  "code": 200,
  "data": {
    "code": "sh000001",
    "name": "上证指数",
    "price": 3350.80,
    "change": 50.30,
    "changePercent": 1.52,
    "open": 3300.50,
    "high": 3360.20,
    "low": 3290.10,
    "volume": 123456789,
    "amount": 9876543210,
    "updateTime": "2026-06-03 15:00:00"
  }
}
```

> 注意：腾讯接口返回的实时数据字段中，`fields[3]`=当前价、`fields[31]`=涨跌额、`fields[32]`=涨跌幅，但开盘价/最高/最低/成交量等字段需要确认腾讯接口字段索引。如果腾讯接口不提供足够字段，可从K线数据的最新一条中提取。

**改动文件：**
- `MarketIndexService.java` — 新增 `getIndexRealtime(String code)` 方法
- `MarketIndexController.java` — 新增 `GET /api/market/realtime` 端点

---

## 4. 前端改动

### 4.1 新增组件：`IndexQuoteModal.vue`

**路径：** `frontend/src/components/IndexQuoteModal.vue`

**Props：**

| prop | 类型 | 说明 |
|------|------|------|
| indexCode | String | 指数代码，如 `sh000001` |
| indexName | String | 指数名称，如 `上证指数` |
| visible | Boolean | 是否显示 |
| initialPrice | Number | 首页卡片上的当前价格 |
| initialChange | Number | 首页卡片上的涨跌额 |
| initialChangePercent | Number | 首页卡片上的涨跌幅 |

**Emits：**
| event | 参数 | 说明 |
|-------|------|------|
| close | — | 关闭弹窗 |

**组件内部结构：**

```
<div class="quote-overlay">           <!-- 遮罩层，点击空白关闭 -->
  <div class="quote-panel">            <!-- 弹窗主体 -->
    
    <!-- 1. 头部 -->
    <div class="quote-header">
      <div class="header-left">
        <h2>{{ indexName }}</h2>
        <span class="header-code">{{ indexCode }}</span>
      </div>
      <button class="close-btn" @click="emit('close')">×</button>
    </div>

    <!-- 2. 行情概览区 -->
    <div class="quote-summary">
      <div class="price-section">
        <span class="current-price" :class="trendClass">{{ realtime.price }}</span>
        <span class="price-change" :class="trendClass">
          {{ realtime.change }}  {{ realtime.changePercent }}%
        </span>
      </div>
      <div class="metrics-grid">
        <div class="metric-item">
          <span class="metric-label">开盘价</span>
          <span class="metric-value">{{ realtime.open }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">最高价</span>
          <span class="metric-value up">{{ realtime.high }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">最低价</span>
          <span class="metric-value down">{{ realtime.low }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">成交量</span>
          <span class="metric-value">{{ realtime.volume }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">成交额</span>
          <span class="metric-value">{{ realtime.amount }}</span>
        </div>
        <div class="metric-item">
          <span class="metric-label">振幅</span>
          <span class="metric-value">{{ realtime.amplitude }}%</span>
        </div>
      </div>
    </div>

    <!-- 3. 周期选择条 -->
    <div class="period-bar">
      <button v-for="p in periods" :key="p.value"
        :class="['period-btn', { active: currentKlt === p.value }]"
        @click="switchPeriod(p.value)">
        {{ p.label }}
      </button>
    </div>

    <!-- 4. 图表区 -->
    <div class="chart-area">
      <div ref="mainChartRef" class="main-chart"></div>   <!-- K线主图 -->
      <div ref="volChartRef" class="vol-chart"></div>      <!-- 成交量副图 -->
    </div>

  </div>
</div>
```

### 4.2 K线图 ECharts 配置要点

**主图（candlestick）：**
- `type: 'candlestick'` — K线蜡烛图
- 涨为红色 `#e53935`，跌为绿色 `#009e5f`（与中国股市习惯一致）
- 数据格式：`[open, close, low, high]`
- 启用 `dataZoom`（底部滑块缩放）方便查看历史
- 十字线 tooltip 显示：日期 / 开/高/低/收 / 涨跌幅

**副图（成交量柱状图）：**
- `type: 'bar'` — 成交量柱状图
- 颜色与K线颜色联动：收盘≥开盘为红色，反之为绿色
- 与主图共用 X 轴（`grid` 上下排列），同步 dataZoom
- Y 轴显示成交量（自动单位换算：万/亿）

**网格布局：**
```
┌──────────────────────────────┐
│  grid[0]: top:  10%          │  ← 主图 K线 (candlestick)
│           height: 55%        │
├──────────────────────────────┤
│  grid[1]: top:  72%          │  ← 副图 成交量 (bar)
│           height: 18%        │
└──────────────────────────────┘
```

**dataZoom 联动：** 两个图表使用同一个 `dataZoom` 配置，或者使用 `group` 属性关联，确保缩放/平移时上下图同步。

### 4.3 周期切换逻辑

```
switchPeriod(klt) →
  1. currentKlt = klt
  2. 根据 klt 计算合适的 startDate（往前推合理范围）
     - 分钟级别 (1/5/15/30/60): 往前推约 200 根K线
     - 日线 (101): 默认近 6 个月
     - 周/月 (102/103): 默认近 2 年
     - 季/年 (104/105): 默认近 5 年
  3. endDate = 今天
  4. 调用 API 获取新数据
  5. 重新渲染图表
```

### 4.4 修改现有文件

**`MarketIndex.vue`：**
- 每个 `.index-item` 添加 `@click="emit('select', index)"`
- script 中添加 `const emit = defineEmits(['select'])`

**`DashboardView.vue`：**
- 引入 `IndexQuoteModal` 组件
- 维护响应式变量：`selectedIndex` (null / index对象)、`showQuoteModal`
- 监听 `MarketIndex` 的 `@select` 事件，设置 selectedIndex 并打开弹窗
- 弹窗关闭时清空 selectedIndex

**`api/index.js`：**
- 新增 `getMarketKline(code, startDate, endDate, klt)` 方法
- 新增 `getMarketRealtime(code)` 方法

**`stores/marketStore.js`（可选）：**
- 如需跨组件共享K线数据，可新增 `fetchKline()` action

### 4.5 分钟级数据的日期格式

东方财富接口对分钟级K线的日期要求：
- 日线及以上：`yyyyMMdd`（如 `20260101`）
- 分钟级：参数 `beg` / `end` 可能需要时间戳或 `yyyyMMddHHmmss` 格式

> 需要在开发阶段实测确认。如果分钟级不传日期参数，接口默认返回最近约250根K线，此时前端可不传 startDate/endDate。

---

## 5. 数据流

```
用户点击指数卡片
  ↓
DashboardView 打开 IndexQuoteModal
  ↓
IndexQuoteModal onMounted:
  ├─ fetchRealtime(code)  → 获取实时快照填充概览区
  └─ fetchKline(code, start, end, '101') → 获取日K数据渲染图表
  ↓
用户切换周期（如点击"周K"）
  ↓
fetchKline(code, '20240101', '20260603', '102') → 重新渲染图表
  ↓
用户点击关闭
  ↓
emit('close') → DashboardView 隐藏弹窗
```

---

## 6. 文件清单

### 新建文件
| 文件 | 说明 |
|------|------|
| `frontend/src/components/IndexQuoteModal.vue` | 行情弹窗主组件，含概览信息 + K线图 + 成交量图 + 周期选择 |

### 修改文件
| 文件 | 改动内容 |
|------|----------|
| `backend/.../service/MarketIndexService.java` | `getIndexKline()` 增加 `klt` 参数；新增 `getIndexRealtime()` 方法 |
| `backend/.../controller/MarketIndexController.java` | kline 端点增加 `klt` 参数；新增 realtime 端点 |
| `frontend/src/components/MarketIndex.vue` | 添加 `@click` 事件和 `emit('select')` |
| `frontend/src/views/DashboardView.vue` | 引入 IndexQuoteModal，处理打开/关闭逻辑 |
| `frontend/src/api/index.js` | 新增 `getMarketKline()` / `getMarketRealtime()` |

---

## 7. 样式参考

沿用现有设计语言：
- 弹窗遮罩：`rgba(0,0,0,0.6)` + `backdrop-filter: blur(4px)` (与 FundDetailModal 一致)
- 弹窗面板：白色背景、`border-radius: 20px`、阴影 `0 25px 60px rgba(0,0,0,0.25)`
- 上涨色：`#e53935`（红色）、下跌色：`#009e5f`（绿色）
- 周期按钮：圆角胶囊样式，选中态白色+阴影 (与 FundTrendChart 的 period-tab 一致)
- 信息卡片：`background: #fafbfc`、`border-radius: 16px`、顶部彩色装饰条
- 弹窗宽度：`max-width: 1000px`、`width: 95%`
- 图表高度：主图约 400px，副图约 120px

---

## 8. 开发顺序建议

1. **后端** — klt 参数扩展 + realtime 接口
2. **API 层** — api/index.js 新增方法
3. **IndexQuoteModal** — 新建组件（先做日线 K线图 + 成交量图）
4. **MarketIndex** — 添加点击事件
5. **DashboardView** — 集成弹窗
6. **周期切换** — 完善各周期数据加载
7. **概览区** — 接入 realtime 接口
8. **分钟级调通** — 联调分钟级K线参数

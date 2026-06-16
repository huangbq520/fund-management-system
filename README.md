# 酱菜养基 — 基金持仓管理与实时估值系统

一个基于 **Spring Boot + Vue 3** 的个人基金投资管理工具。聚合天天基金、东方财富、腾讯财经等公开数据源，提供基金搜索、实时估值、持仓管理、收益计算、自选对比、OCR 识别等一站式功能。

## ✨ 功能特性

### 📊 持仓管理
- **基金搜索与添加**：输入关键词搜索公募基金，一键添加到个人持仓
- **持仓编辑**：支持按份额/按金额两种模式编辑持仓，自动计算成本价和持仓金额
- **买入/卖出同步**：支持指定日期的加仓减仓操作，自动加权平均成本
- **批量操作**：批量添加、批量删除基金
- **金额隐藏**：一键切换隐私模式，隐藏敏感金额

### 📈 实时估值与收益
- **实时估值**：展示基金最新单位净值、估算净值、估算涨跌幅及估值时间
- **当日收益**：基于估算涨幅实时计算当日浮动收益
- **昨日收益**：基于历史净值精确计算昨日已确认收益
- **持仓收益率**：实时展示每只基金及整体组合的持仓收益率
- **周期涨跌幅**：近一周/近一月/近三月/近六月/近一年涨跌幅一目了然
- **组合汇总**：总资产、当日收益、总收益、总收益率实时汇总

### 🏦 大盘行情
- **五大指数**：上证指数、上证50、深证成指、创业板指、沪深300 实时行情
- **K 线图表**：点击指数查看交互式日 K 线图，支持缩放拖拽
- **实时刷新**：交易时段自动刷新行情数据

### ⭐ 自选基金
- **自选列表**：独立于持仓的自选基金管理，支持分组归类
- **基金对比**：选择 2-5 只基金，对比各项关键指标（净值、收益率、最大回撤等）
- **分组管理**：创建/编辑/删除分组，基金自由分配
- **排序筛选**：支持按涨跌幅、收益率等多维度排序

### 📸 OCR 识别
- 上传持仓截图（支付宝/天天基金等），自动识别基金代码和名称
- 基于百度 OCR API，支持批量提取

### 📈 历史数据与图表
- **业绩走势图**：ECharts 折线图展示基金历史净值走势，支持 1月/3月/6月/1年/3年/全部 周期切换
- **沪深300对比**：业绩走势叠加沪深300和同类平均对比基准线
- **基金详情弹窗**：查看基金重仓股票及实时涨跌
- **每日收益**：查看每只基金及整体的历史每日收益明细和趋势图

### 🔄 自动化任务
- **盘后净值同步**：每个工作日 15:00-22:00 每 30 分钟自动同步确认净值
- **交易日字段重置**：每个工作日 9:00 自动重置当日交易字段
- **每日收益统计**：每个工作日 22:00 自动计算当日收益并写入历史表

### 🔐 用户认证
- 邮箱注册 + 验证码验证
- JWT 令牌认证，24 小时有效
- BCrypt 密码加密
- 多用户数据隔离

### ⚡ 性能优化
- **Redis 多级缓存**：基金数据缓存（交易时段 60s，非交易时段 20min）、验证码缓存、OCR Token 缓存
- **前端静默刷新**：交易时段 30s 自动刷新，原地合并数据避免全量重渲染
- **本地状态更新**：增删改操作直接更新前端状态，无需重新请求全量列表

---

## 🛠 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 应用框架 |
| MyBatis | 2.3.2 | ORM / 数据访问 |
| MySQL | 8.0 | 数据库 |
| Spring Security | 5.7 | 认证与授权 |
| JWT (jjwt) | 0.11.5 | 令牌认证 |
| OkHttp | 4.12 | HTTP 客户端（外部 API 调用） |
| Jsoup | 1.17 | HTML 解析（持仓表格） |
| FastJSON2 | 2.0 | JSON 解析 |
| Redis (Spring Data) | - | 缓存层 |
| Spring Mail | - | 邮件验证码发送 |
| Lombok | - | 代码简化 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5 | 前端框架 |
| Vite | 8.0 | 构建工具 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| Axios | 1.15 | HTTP 请求 |
| ECharts | 6.0 | 数据可视化 |

---

## 📁 项目结构

```
fund-management-system/
├── backend/
│   ├── src/main/java/com/fund/
│   │   ├── controller/          # REST API 控制器
│   │   │   ├── AuthController.java          # 认证接口
│   │   │   ├── FundController.java          # 基金/持仓核心接口
│   │   │   ├── DailyProfitController.java   # 每日收益接口
│   │   │   ├── MarketIndexController.java   # 大盘行情接口
│   │   │   ├── WatchlistController.java     # 自选基金接口
│   │   │   └── OcrController.java           # OCR 识别接口
│   │   ├── service/             # 业务逻辑层
│   │   │   ├── FundDataService.java         # 基金数据聚合（含 Redis 缓存）
│   │   │   ├── FundHoldingService.java      # 持仓计算与收益引擎
│   │   │   ├── FundSearchService.java       # 基金搜索
│   │   │   ├── DailyProfitService.java      # 每日收益统计
│   │   │   ├── MarketIndexService.java      # 大盘指数行情
│   │   │   ├── WatchlistService.java        # 自选管理
│   │   │   ├── OcrService.java             # OCR 识别
│   │   │   └── AuthService.java            # 用户认证
│   │   ├── mapper/              # MyBatis 数据访问层（6 个 Mapper）
│   │   ├── entity/              # 实体类（6 个 Entity）
│   │   ├── vo/                  # 值对象 / 视图模型（17 个 VO）
│   │   ├── dto/                 # 请求 DTO
│   │   ├── config/              # 配置类
│   │   │   ├── SecurityConfig.java          # Spring Security 配置
│   │   │   ├── JwtAuthenticationFilter.java # JWT 认证过滤器
│   │   │   ├── RedisConfig.java            # Redis 配置
│   │   │   ├── CorsConfig.java             # CORS 跨域配置
│   │   │   └── OkHttpConfig.java           # OkHttp 客户端配置
│   │   ├── scheduler/           # 定时任务
│   │   │   ├── NetValueSyncScheduler.java   # 净值同步 & 字段重置
│   │   │   └── DailyProfitScheduler.java    # 每日收益计算
│   │   └── util/                # 工具类
│   │       ├── HttpUtil.java               # HTTP 请求封装（含重试）
│   │       ├── JwtUtil.java                # JWT 工具
│   │       ├── BaiduOcrUtil.java           # 百度 OCR API
│   │       └── TradingDayChecker.java      # 交易日判断
│   └── src/main/resources/
│       ├── mapper/              # MyBatis XML 映射文件
│       ├── application.yml      # 应用配置
│       └── schema.sql           # 数据库初始化脚本
│
├── frontend/
│   ├── src/
│   │   ├── api/                 # API 请求封装（fund / auth / market / watchlist / ocr）
│   │   ├── components/          # Vue 组件（20 个）
│   │   │   ├── HoldingList.vue              # 持仓列表（固定列、批量删除）
│   │   │   ├── EditHoldingModal.vue         # 编辑持仓弹窗
│   │   │   ├── PortfolioSummary.vue         # 组合汇总卡片
│   │   │   ├── MarketIndex.vue              # 大盘指数行情条
│   │   │   ├── IndexQuoteModal.vue          # 指数 K 线弹窗
│   │   │   ├── FundDetailModal.vue          # 基金详情弹窗（重仓股）
│   │   │   ├── FundTrendChart.vue           # 业绩走势图表
│   │   │   ├── MyProfitTab.vue              # 每日收益标签页
│   │   │   ├── SearchFund.vue               # 基金搜索框
│   │   │   ├── WatchlistTable.vue           # 自选列表
│   │   │   ├── WatchlistGroupBar.vue        # 自选分组栏
│   │   │   ├── WatchlistCompareModal.vue    # 基金对比弹窗
│   │   │   ├── AllocationPieChart.vue       # 资产配置饼图
│   │   │   ├── OcrModal.vue                 # OCR 识别弹窗
│   │   │   ├── ScrollDatePicker.vue         # 滚动日期选择器
│   │   │   ├── Login.vue                    # 登录表单
│   │   │   ├── Register.vue                 # 注册表单
│   │   │   ├── UserMenu.vue                 # 用户菜单
│   │   │   ├── ToastContainer.vue           # 消息提示
│   │   │   └── SiteFooter.vue               # 页脚
│   │   ├── views/               # 页面视图
│   │   │   ├── DashboardView.vue            # 主仪表盘
│   │   │   └── AuthView.vue                 # 认证页面
│   │   ├── stores/              # Pinia 状态管理（4 个 Store）
│   │   ├── composables/         # 组合式函数（格式、刷新、Toast）
│   │   ├── router/              # 路由配置
│   │   ├── App.vue              # 根组件
│   │   └── main.js              # 入口文件
│   ├── vite.config.js
│   └── package.json
│
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- **JDK** 8+
- **Maven** 3.6+
- **Node.js** 16+
- **MySQL** 8.0+
- **Redis** 7.0+（必要，用于基金数据缓存、验证码、OCR Token）

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 2. 配置应用

编辑 `backend/src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jiangcai_fund
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
  mail:                              # 可选，注册验证码功能需要
    host: smtp.qq.com
    port: 587
    username: your_email@qq.com
    password: your_smtp_password

# 可选：百度 OCR（OCR 识别功能需要）
baidu:
  ocr:
    app-id: your_app_id
    api-key: your_api_key
    secret-key: your_secret_key
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务运行在 http://localhost:8080

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务运行在 http://localhost:3000，API 请求自动代理到后端 8080 端口。

---

## 📡 API 接口

### 认证接口 `/api/auth`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/send-verify-code` | 否 | 发送邮箱验证码（Redis 缓存，10 分钟有效） |
| POST | `/api/auth/register` | 否 | 用户注册 |
| POST | `/api/auth/login` | 否 | 用户登录，返回 JWT 令牌 |
| GET | `/api/auth/me` | 是 | 获取当前用户信息 |

### 基金接口 `/api/fund`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/fund/search?keyword=` | 否 | 搜索公募基金 |
| GET | `/api/fund/data?code=` | 否 | 获取基金完整数据（含估值、持仓、历史走势） |
| GET | `/api/fund/performance?code=&period=` | 否 | 获取基金业绩走势（period: 1month~3year/all） |
| GET | `/api/fund/nav-at?code=&date=` | 否 | 查询指定日期历史净值 |
| GET | `/api/fund/list` | 是 | 获取用户已添加基金列表 |
| GET | `/api/fund/holding/list` | 是 | 获取持仓列表（含实时盈亏计算） |
| GET | `/api/fund/portfolio/summary` | 是 | 获取投资组合汇总 |
| POST | `/api/fund/add` | 是 | 添加基金 → 返回完整 `FundHoldingVO` |
| POST | `/api/fund/add/batch` | 是 | 批量添加基金 |
| POST | `/api/fund/delete` | 是 | 删除基金 |
| POST | `/api/fund/delete/batch` | 是 | 批量删除基金 |
| POST | `/api/fund/holding/update` | 是 | 编辑持仓（支持按份额/按金额两种模式） |
| POST | `/api/fund/holding/adjust` | 是 | 买入/卖出调整持仓（`type`: BUY/SELL） |
| POST | `/api/fund/holding/clear` | 是 | 清仓（份额→0） |

### 每日收益接口 `/api/fund/daily-profit`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/fund/daily-profit/overall?period=` | 是 | 整体每日收益汇总 |
| GET | `/api/fund/daily-profit/{fundCode}?period=` | 是 | 单基金每日收益 |
| POST | `/api/fund/daily-profit/calculate` | 是 | 手动触发每日收益计算 |
| POST | `/api/fund/daily-profit/recalculate` | 是 | 重新计算所有每日收益 |

### 大盘行情接口 `/api/market`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/market/indices` | 否 | 获取五大指数实时行情 |
| GET | `/api/market/kline?code=&startDate=&endDate=&klt=` | 否 | K 线数据（klt: 101=日K） |
| GET | `/api/market/realtime?code=` | 否 | 单个指数实时行情 |

### 自选基金接口 `/api/watchlist`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/watchlist/list` | 是 | 获取自选列表（含实时行情） |
| POST | `/api/watchlist/add` | 是 | 加入自选（上限 200 只） |
| POST | `/api/watchlist/remove` | 是 | 移除自选 |
| POST | `/api/watchlist/batch-add` | 是 | 批量添加自选 |
| POST | `/api/watchlist/batch-remove` | 是 | 批量移除自选 |
| PUT | `/api/watchlist/{id}/group` | 是 | 分配分组 |
| GET | `/api/watchlist/groups` | 是 | 获取分组列表 |
| POST | `/api/watchlist/groups` | 是 | 创建分组 |
| PUT | `/api/watchlist/groups/{id}` | 是 | 修改分组名称 |
| DELETE | `/api/watchlist/groups/{id}` | 是 | 删除分组 |
| POST | `/api/watchlist/compare` | 是 | 基金对比（2-5 只） |

### OCR 识别接口 `/api/ocr`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/ocr/recognize` | 否 | 上传图片识别基金代码（Multipart, 最大 4MB） |

---

## 🗄 数据库表结构

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `user` | 用户表 | id, email, password（BCrypt）, nickname, status |
| `fund` | 用户基金列表 | id, user_id, fund_code, fund_name, create_time |
| `user_fund` | 持仓详情 | id, user_id, fund_code, hold_share, hold_amount, cost_price, buy_date, today_buy/sell_share, profit_status, confirmed_net_value/profit, cached_estimated_* |
| `fund_daily_profit` | 每日收益记录 | id, user_id, fund_code, record_date, daily_profit, daily_return_rate, net_value, hold_share |
| `user_watchlist` | 自选列表 | id, user_id, fund_code, fund_name, group_id, add_net_value, notes |
| `watchlist_group` | 自选分组 | id, user_id, group_name, sort_order |

---

## 🏗 核心架构

### 数据流

```
┌─────────────────────────────────────────────────────────┐
│                       前端 (Vue 3)                       │
│  DashboardView → HoldingList / WatchlistTable / etc.    │
│  Pinia Stores (fund → holding → summary 自动联动)       │
└─────────────────────┬───────────────────────────────────┘
                      │  Axios (代理到 localhost:8080)
┌─────────────────────▼───────────────────────────────────┐
│                  后端 (Spring Boot)                      │
│  Controller → Service → Mapper → MySQL                  │
│                    │                                     │
│              Redis 缓存层                                │
│         fund:data:{code} (60s/1200s TTL)                │
│         baidu:access_token (29天)                        │
│         verify:code:{email} (10分钟)                     │
└─────────────────────┬───────────────────────────────────┘
                      │  OkHttp
┌─────────────────────▼───────────────────────────────────┐
│                  第三方数据源                             │
│  天天基金 API   东方财富 API    腾讯股票 API   百度 OCR  │
│  (实时估值)     (持仓/走势/K线)  (股票行情)   (图片识别) │
└─────────────────────────────────────────────────────────┘
```

### 缓存策略

| 缓存项 | Redis Key | TTL | 说明 |
|--------|-----------|-----|------|
| 基金数据 | `fund:data:{code}` | 60s（交易时段）/ 1200s（非交易） | `FundData` 完整 JSON，消除重复外部 API 调用 |
| 验证码 | `verify:code:{email}` | 10 min | 注册/登录验证码 |
| OCR Token | `baidu:access_token` | 29 天 | 百度 OCR access_token |
| 估算缓存 | `user_fund.cached_estimated_*` | DB 字段 | 盘后估算数据回退 |

### 定时任务

| 任务 | Cron | 说明 |
|------|------|------|
| 净值同步 | `0 */30 15-22 * * MON-FRI` | 盘后每 30 分钟同步确认净值 |
| 字段重置 | `0 0 9 * * MON-FRI` | 开盘前重置当日交易字段 |
| 每日收益 | `0 0 22 * * MON-FRI` | 收盘后计算当日收益 |

### 前端性能优化

- **原地合并**：静默刷新时对比新旧数据，仅修改变化属性，避免整表重渲染
- **本地状态更新**：增删改操作直接在 Pinia store 中更新，不发起额外 API 请求
- **自动刷新**：交易时段每 30s 静默刷新，非交易时段自动停止
- **本地汇总**：`fetchHoldings` 后直接 `recalcSummary`，减少 `/portfolio/summary` 调用

---

## 📊 第三方数据源

| 数据源 | 用途 | 接口地址 |
|--------|------|----------|
| 天天基金 | 实时净值估值、基本信息 | `https://fundgz.1234567.com.cn/js/{code}.js` |
| 东方财富 | 基金持仓股票 | `https://fundf10.eastmoney.com/FundArchivesDatas.aspx` |
| 东方财富 | 历史净值走势、对比指数 | `https://fund.eastmoney.com/pingzhongdata/{code}.js` |
| 东方财富 | K 线数据 | `https://push2his.eastmoney.com/api/qt/stock/kline/get` |
| 东方财富 | 基金搜索 | `https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx` |
| 腾讯财经 | 股票实时行情 | `https://qt.gtimg.cn/q={codes}` |
| 百度 OCR | 图片文字识别 | `https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic` |

---

## ⚠️ 注意事项

1. **第三方接口限制**：所有数据源均为公开接口，可能存在调用频率限制。系统已通过 Redis 缓存大幅降低调用频率
2. **盘后净值延迟**：确认净值通常在交易日 19:00-22:00 之间陆续发布，定时任务在此期间每 30 分钟轮询
3. **估算数据时效**：天天基金的估算净值在交易日 9:30-15:00 期间更新，盘后清空
4. **OCR 准确性**：识别结果取决于截图清晰度，建议使用标准截图
5. **免责声明**：本系统仅供个人学习与投资辅助使用，**不构成任何投资建议**。投资有风险，入市需谨慎

---

## 📄 License

MIT License

Copyright (c) 2024-present huangbq

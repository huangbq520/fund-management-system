# 基金实时估值与持仓管理系统

一个基于 Spring Boot + Vue 3 的个人基金投资管理工具，聚合天天基金、东方财富、腾讯财经等公开数据源，为投资者提供基金搜索、实时估值、持仓管理、收益计算等一站式服务。

## 项目解决的核心痛点

### 1. 基金数据分散、信息获取效率低
投资者通常需要在多个平台之间来回切换才能获取完整的基金信息。该系统通过数据聚合技术，将多个数据源整合到一个系统中，用户只需一次操作即可获取基金的全部关键信息。

### 2. 持仓收益手动计算繁琐
传统投资管理中，用户需要手动记录每只基金的持仓份额、成本价，并手动计算收益。系统实现了持仓信息的自动记录和收益的自动计算，用户只需输入基础数据，系统即可实时计算持仓金额和收益率。

### 3. 净值数据更新不及时
系统通过定时任务技术，在每日收盘后自动同步所有持仓基金的净值数据，并在每个交易日开盘前自动重置当日交易字段，确保用户始终看到最新的净值信息。

### 4. 缺乏统一的投资组合管理
当用户持有多只基金时，难以直观了解整体投资组合的状况。系统提供了组合汇总功能，帮助用户全面掌握总资产、总收益和持仓分布。

## 技术栈

### 后端
- Java 8 + Spring Boot 2.7
- MyBatis + MySQL
- OkHttp（第三方接口调用）
- Jsoup（HTML解析）
- FastJSON2（数据解析）
- Spring Security + JWT（用户认证）
- Redis（数据缓存）
- Spring Mail（邮件服务）

### 前端
- Vue 3 + Vite
- Axios
- ECharts（数据可视化）

## 项目结构

```
fund-management-system/
├── backend/                 # 后端项目
│   ├── src/main/java/com/fund/
│   │   ├── controller/      # REST API控制器
│   │   ├── service/         # 业务逻辑层
│   │   ├── mapper/          # 数据访问层
│   │   ├── entity/          # 实体类
│   │   ├── vo/              # 值对象
│   │   ├── dto/             # 数据传输对象
│   │   ├── config/          # 配置类
│   │   ├── scheduler/       # 定时任务
│   │   └── util/            # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/          # MyBatis映射文件
│   │   ├── application.yml  # 应用配置
│   │   └── schema.sql       # 数据库脚本
│   └── pom.xml
│
└── frontend/                # 前端项目
    ├── src/
    │   ├── api/             # API请求封装
    │   ├── components/      # Vue组件
    │   ├── App.vue          # 主组件
    │   ├── main.js          # 入口文件
    │   └── style.css        # 全局样式
    ├── index.html
    ├── vite.config.js
    └── package.json
```

## 核心逻辑流

### 1. 基金数据获取的长链聚合

当用户请求基金数据时，系统依次调用多个外部API完成数据聚合：

```
请求基金数据
    ├── 天天基金API → 获取实时净值估值和基本信息
    ├── 东方财富API → 获取基金持仓股票信息
    ├── 腾讯股票API → 获取持仓股票的实时涨跌幅
    └── 东方财富API → 获取基金历史净值走势
```

### 2. 定时任务自动化

| 任务 | 执行时间 | 功能 |
|------|----------|------|
| 净值同步 | 每个工作日 15:05 | 遍历所有持仓基金，获取最新净值，更新昨日份额和收益状态 |
| 字段重置 | 每个工作日 09:00 | 重置当日买入/卖出份额和收益状态，为新交易日做准备 |

### 3. 持仓管理与收益计算

用户查看持仓时，系统会查询用户所有持仓记录，获取实时净值和相关信息，根据持仓份额和成本价计算持仓金额、收益金额和收益率。

### 4. 用户认证流程

采用 JWT 令牌机制，用户登录后获得令牌，后续请求通过令牌验证用户身份，确保每个用户只能访问自己的基金数据。

## 功能特性

- **基金搜索**：输入关键词搜索公募基金，获取基金代码和名称
- **实时估值**：展示基金的最新净值、估值涨跌幅和估值时间
- **持仓管理**：管理个人基金持仓，记录份额、成本价和买入日期
- **收益计算**：实时计算持仓金额、收益金额和收益率
- **持仓分布**：可视化展示各基金的持仓占比
- **历史走势**：ECharts折线图展示近90天业绩走势
- **持仓详情**：查看基金的重仓股票及其涨跌情况
- **自动同步**：每日收盘后自动更新净值数据

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis（可选，用于缓存）

### 1. 初始化数据库

```sql
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 2. 配置数据库连接

编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jiangcai_fund
    username: root
    password: your_password
```

### 3. 启动后端服务

```bash
cd backend
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 4. 启动前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 http://localhost:3000 启动

## API接口

### 认证接口

| 方法 | 地址 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/send-code | 发送验证码 |

### 基金接口

| 方法 | 地址 | 参数 | 说明 |
|------|------|------|------|
| GET | /api/fund/search | keyword | 搜索基金 |
| GET | /api/fund/data | code | 获取基金完整数据 |
| GET | /api/fund/list | - | 获取基金列表 |
| POST | /api/fund/add | fundCode, fundName | 添加基金 |
| POST | /api/fund/delete | fundCode | 删除基金 |
| GET | /api/fund/holding/list | - | 获取持仓列表 |
| POST | /api/fund/holding/update | - | 更新持仓信息 |
| GET | /api/fund/portfolio/summary | - | 获取组合汇总 |

### 行情接口

| 方法 | 地址 | 说明 |
|------|------|------|
| GET | /api/market/index | 获取主要指数数据 |

## 数据库表结构

| 表名 | 说明 |
|------|------|
| user | 用户表（邮箱、密码、昵称） |
| fund | 用户基金列表 |
| user_fund | 用户基金持仓详情（份额、金额、成本价等） |
| market_index_config | 指数配置表 |

## 第三方数据源

| 数据源 | 用途 | 地址 |
|--------|------|------|
| 天天基金 | 实时净值估值 | https://fundgz.1234567.com.cn |
| 东方财富 | 持仓股票、历史走势 | https://fundf10.eastmoney.com |
| 腾讯财经 | 股票行情 | https://qt.gtimg.cn |

## 注意事项

1. 第三方接口为公开接口，可能存在调用频率限制
2. 盘后净值同步依赖第三方接口数据更新，存在一定延迟
3. 本系统仅供个人学习使用，不构成投资建议
4. 投资有风险，入市需谨慎

## License

MIT License

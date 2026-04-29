# 基金实时估值与持仓管理系统（酱菜养基）

基于天天基金、东方财富、腾讯财经公开接口的基金估值与持仓管理工具，支持邮箱验证码注册、JWT 登录、持仓成本维护与组合汇总。

## 功能概览

- 邮箱验证码注册/登录
- 基金搜索与添加
- 持仓管理（份额、成本价、买入日期）
- 组合汇总与收益概览
- 基金详情：估值、净值、近 90 天走势、前 10 持仓
- 大盘指数行情展示

## 技术栈

- 后端：Spring Boot 2.7、MyBatis、MySQL、Redis、Spring Security + JWT、OkHttp、FastJSON2、Jsoup、JavaMail
- 前端：Vue 3、Vite、Axios、ECharts

## 运行环境

- JDK 8
- Maven 3.6+
- Node.js 16+
- MySQL 8+
- Redis 6+
- 可用的 SMTP 邮箱（用于验证码发送）

## 快速开始

### 1. 初始化数据库

```sql
mysql -u root -p < backend/src/main/resources/schema.sql
```

默认库名为 `jiangcai_fund`。

### 2. 配置后端

编辑 `backend/src/main/resources/application.yml`，更新数据库、Redis、SMTP 与 JWT 配置：

- `spring.datasource.*`
- `spring.redis.*`
- `spring.mail.*`
- `jwt.secret` / `jwt.expiration`

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:3000`，并在 `vite.config.js` 中将 `/api` 代理到后端。

## API 概览

### 认证接口（无需登录）

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | /api/auth/send-verify-code | 发送邮箱验证码 |
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/auth/me | 获取当前用户 |

### 业务接口（需 `Authorization: Bearer <token>`）

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | /api/fund/search?keyword= | 基金搜索 |
| GET | /api/fund/data?code= | 基金详情（估值/走势/持仓） |
| GET | /api/fund/list | 基金列表 |
| POST | /api/fund/add | 添加基金 |
| POST | /api/fund/delete | 删除基金 |
| GET | /api/fund/holding/list | 持仓列表 |
| POST | /api/fund/holding/update | 更新持仓 |
| GET | /api/fund/portfolio/summary | 组合汇总 |

### 市场数据（无需登录）

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | /api/market/indices | 大盘指数 |

## 数据来源

- 天天基金：`fundgz.1234567.com.cn`
- 东方财富：`fundf10.eastmoney.com`、`fund.eastmoney.com`
- 腾讯财经：`qt.gtimg.cn`
- 东方财富基金搜索：`fundsuggest.eastmoney.com`

## 项目结构

```
fund-management-system/
├── backend/                Spring Boot 服务
│   ├── src/main/java/com/fund
│   │   ├── config           安全/JWT/Redis/OkHttp 配置
│   │   ├── controller       API 控制器
│   │   ├── service          业务逻辑与三方数据聚合
│   │   ├── mapper           MyBatis Mapper
│   │   ├── entity           实体模型
│   │   └── vo/dto           数据传输对象
│   └── src/main/resources
│       ├── mapper           MyBatis XML
│       ├── application.yml  配置文件
│       └── schema.sql       初始化脚本
└── frontend/               Vue 3 + Vite 前端
    ├── src/api              API 封装
    ├── src/components       业务组件
    └── vite.config.js       代理配置
```

## 注意事项

- 三方接口为公开数据源，可能存在频率或稳定性限制。
- 邮箱验证码依赖 Redis 与 SMTP 配置，请确保相关服务可用。
- 部署前请更换 JWT 密钥及数据库/邮箱密码。

# 基金实时估值与持仓管理系统

基于天天基金、腾讯财经、东方财富三方公开接口的基金查询与管理工具。

## 技术栈

### 后端
- Java + Spring Boot
- MyBatis + MySQL
- OkHttp (第三方接口调用)
- FastJSON2 (数据解析)

### 前端
- Vue3 + Vite
- Axios
- ECharts

## 项目结构

```
fund-management/
├── backend/                 # 后端项目
│   ├── src/main/java/com/fund/
│   │   ├── controller/      # REST API控制器
│   │   ├── service/         # 业务逻辑层
│   │   ├── mapper/          # 数据访问层
│   │   ├── entity/          # 实体类
│   │   ├── vo/              # 值对象
│   │   ├── config/          # 配置类
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

## 快速开始

### 1. 后端启动

#### 准备MySQL数据库
```sql
-- 执行 schema.sql 创建数据库和表
mysql -u root -p < backend/src/main/resources/schema.sql
```

#### 修改数据库配置
编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fund_db
    username: root
    password: your_password
```

#### 启动后端
```bash
cd backend
mvn spring-boot:run
```
后端服务将在 http://localhost:8080 启动

### 2. 前端启动

```bash
cd frontend
npm install
npm run dev
```
前端服务将在 http://localhost:3000 启动

## API接口

| 方法 | 地址 | 参数 | 说明 |
|------|------|------|------|
| GET | /api/fund/search | code | 搜索基金，获取实时基础信息 |
| GET | /api/fund/list | - | 获取用户已添加的所有基金 |
| GET | /api/fund/detail | code | 获取基金详情（含持仓、走势） |
| POST | /api/fund/add | fundCode, fundName | 添加基金到列表 |
| POST | /api/fund/delete | fundCode | 删除基金 |

## 功能特性

- 基金搜索：输入基金代码，查询实时估值信息
- 基金列表：展示已添加基金的实时数据，每30秒自动刷新
- 基金详情：查看基金基本信息、业绩走势图表、持仓股票
- 数据可视化：ECharts折线图展示近90天业绩走势

## 第三方数据源

- 天天基金：https://fundgz.1234567.com.cn
- 腾讯财经：https://qt.gtimg.cn
- 东方财富：https://fundf10.eastmoney.com

## 注意事项

1. 第三方接口为公开接口，可能存在调用限制
2. MVP阶段不支持用户登录，仅实现单用户本地数据持久化
3. 接口调用失败时会有兜底逻辑，确保系统可用性
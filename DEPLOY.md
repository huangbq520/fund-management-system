# 酱菜养基 — 系统部署安装说明

本文档描述如何从零开始在本地或服务器上部署「酱菜养基」基金持仓管理系统。

---

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 安装基础软件](#2-安装基础软件)
  - [2.1 Windows](#21-windows)
  - [2.2 Linux (Ubuntu/Debian)](#22-linux-ubuntudebian)
  - [2.3 macOS](#23-macos)
- [3. 初始化数据库](#3-初始化数据库)
- [4. 配置后端](#4-配置后端)
- [5. 启动后端](#5-启动后端)
- [6. 配置与启动前端](#6-配置与启动前端)
- [7. 生产环境部署](#7-生产环境部署)
- [8. 验证部署](#8-验证部署)
- [9. 可选功能配置](#9-可选功能配置)
- [10. 常见问题](#10-常见问题)

---

## 1. 环境要求

| 软件 | 最低版本 | 用途 | 必须？ |
|------|----------|------|--------|
| JDK | 8+ | 运行 Spring Boot 后端 | ✅ 必须 |
| Maven | 3.6+ | 构建后端项目 | ✅ 必须 |
| MySQL | 8.0+ | 数据持久化存储 | ✅ 必须 |
| Redis | 7.0+ | 缓存层，提升性能 | ✅ 必须 |
| Node.js | 16+ | 构建前端项目 | ✅ 必须 |
| npm | 8+ | 管理前端依赖 | ✅ 必须 |
| Nginx | 1.18+ | 生产环境反向代理 | 🔧 生产推荐 |

> **说明**：Redis 在系统中用于基金数据缓存（大幅减少外部 API 调用）、邮箱验证码存储、OCR Token 缓存，属于必需品而非可选。

---

## 2. 安装基础软件

### 2.1 Windows

#### JDK 8+
下载并安装 [Adoptium JDK 8](https://adoptium.net/download/) 或 [Oracle JDK 8](https://www.oracle.com/java/technologies/downloads/)。

安装后验证：
```powershell
java -version
# 输出示例: openjdk version "1.8.0_402"
```

#### Maven
下载 [Apache Maven](https://maven.apache.org/download.cgi)，解压到 `D:\Program Files\`，添加 `bin` 目录到系统 PATH。

```powershell
mvn -version
# Apache Maven 3.9.x
```

#### MySQL 8.0+
下载 [MySQL Community Server](https://dev.mysql.com/downloads/mysql/8.0.html)，使用 MSI 安装程序安装。

安装时记住 `root` 密码。安装后登录验证：
```powershell
mysql -u root -p
```

#### Redis
下载 [Redis for Windows](https://github.com/redis-windows/redis-windows/releases)（推荐使用 Cygwin 版本或通过 WSL 安装）。

以服务方式安装：
```powershell
redis-server --service-install
redis-server --service-start
```

验证：
```powershell
redis-cli ping
# 返回 PONG 表示正常运行
```

#### Node.js
下载 [Node.js LTS](https://nodejs.org/)，使用安装程序安装（npm 随 Node.js 一起安装）。

```powershell
node -v
# v18.x.x 或更高
npm -v
# 9.x.x 或更高
```

---

### 2.2 Linux (Ubuntu/Debian)

```bash
# 更新包索引
sudo apt update

# JDK 8
sudo apt install openjdk-8-jdk -y

# Maven
sudo apt install maven -y

# MySQL 8.0
sudo apt install mysql-server -y
sudo systemctl enable mysql
sudo systemctl start mysql

# Redis
sudo apt install redis-server -y
sudo systemctl enable redis-server
sudo systemctl start redis-server

# Node.js (使用 NodeSource 安装 LTS 版本)
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs -y

# 验证
java -version
mvn -version
mysql --version
redis-cli ping
node -v
npm -v
```

---

### 2.3 macOS

```bash
# 使用 Homebrew 一键安装
brew install openjdk@8
brew install maven
brew install mysql
brew install redis
brew install node@18

# 启动服务
brew services start mysql
brew services start redis

# 验证
java -version
mvn -version
redis-cli ping
node -v
```

---

## 3. 初始化数据库

### 3.1 登录 MySQL

```bash
mysql -u root -p
```

### 3.2 执行建表脚本

项目根目录的 `backend/src/main/resources/schema.sql` 包含完整的建表语句：

```sql
-- 在 MySQL 中执行
SOURCE /path/to/backend/src/main/resources/schema.sql;
```

或者直接在命令行执行：

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

### 3.3 验证数据库

```sql
USE jiangcai_fund;
SHOW TABLES;
```

应该看到 **6 张表**：

| 表名 | 说明 |
|------|------|
| `user` | 用户表（邮箱、密码、昵称） |
| `fund` | 用户基金列表 |
| `user_fund` | 持仓详情（份额、成本价、收益确认） |
| `fund_daily_profit` | 每日收益记录 |
| `user_watchlist` | 自选基金列表 |
| `watchlist_group` | 自选分组 |

---

## 4. 配置后端

编辑 `backend/src/main/resources/application.yml`：

### 4.1 必改配置

```yaml
spring:
  # ====== 数据库连接（必改）======
  datasource:
    url: jdbc:mysql://localhost:3306/jiangcai_fund?useUnicode=true&characterEncoding=utf8
    username: root              # ← 改成你的 MySQL 用户名
    password: your_password     # ← 改成你的 MySQL 密码

  # ====== Redis 连接 ======
  redis:
    host: localhost             # ← Redis 地址（默认本机）
    port: 6379                  # ← Redis 端口（默认 6379）
    database: 0                 # 使用第 0 号数据库
```

### 4.2 可选配置

```yaml
server:
  port: 8080                    # 后端服务端口，默认 8080

jwt:
  secret: your-secret-key       # JWT 签名密钥（生产环境请更换为随机长字符串）
  expiration: 86400000          # Token 有效期（毫秒），默认 24 小时
```

### 4.3 启用/禁用日志

在 `application.yml` 中：

```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 开发环境：打印 SQL
    # log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl  # 生产环境：关闭 SQL 日志
```

---

## 5. 启动后端

### 5.1 开发模式

```bash
cd backend

# 方式一：Maven 直接运行
mvn spring-boot:run

# 方式二：先打包再运行（适合反复测试）
mvn clean package -DskipTests
java -jar target/fund-management-1.0.0.jar
```

启动成功后会看到：

```
Started FundApplication in X.XXX seconds
```

服务运行在 `http://localhost:8080`。

### 5.2 后台运行（Linux）

```bash
nohup java -jar target/fund-management-1.0.0.jar > app.log 2>&1 &
```

### 5.3 注册为系统服务（Linux systemd）

创建服务文件 `/etc/systemd/system/fund-management.service`：

```ini
[Unit]
Description=Fund Management System
After=network.target mysql.service redis.service

[Service]
Type=simple
User=your_user
WorkingDirectory=/opt/fund-management
ExecStart=/usr/bin/java -jar /opt/fund-management/fund-management-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用并启动：
```bash
sudo systemctl daemon-reload
sudo systemctl enable fund-management
sudo systemctl start fund-management
sudo systemctl status fund-management
```

---

## 6. 配置与启动前端

### 6.1 安装依赖

```bash
cd frontend
npm install
```

### 6.2 开发模式

```bash
npm run dev
```

前端开发服务器运行在 `http://localhost:3000`，API 请求自动代理到后端 `http://localhost:8080`。

代理配置在 `frontend/vite.config.js` 中：

```js
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

> **注意**：如果后端运行在其他地址或端口，请修改 `target` 配置。

### 6.3 生产构建

```bash
npm run build
```

构建产物在 `frontend/dist/` 目录下，是一组静态文件（HTML、JS、CSS），可直接部署到 Nginx 或其他 Web 服务器。

---

## 7. 生产环境部署

### 7.1 推荐架构

```
浏览器 (HTTPS)
    │
    ▼
Nginx (:443)
    ├── /api/*  →  反向代理到  Java (:8080)
    └── /*      →  静态文件   frontend/dist/
```

### 7.2 Nginx 配置

```nginx
server {
    listen 80;
    server_name your-domain.com;          # ← 改成你的域名

    # 前端静态文件
    root /opt/fund-management/frontend/dist;
    index index.html;

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 60s;
    }

    # Vue Router History 模式：所有非 API 路径回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

部署并重载：
```bash
sudo nginx -t                     # 检查配置语法
sudo systemctl reload nginx       # 重载配置
```

### 7.3 HTTPS 配置（推荐）

使用 Let's Encrypt 获取免费 SSL 证书：

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
# 按提示完成配置，certbot 会自动修改 Nginx 配置
```

### 7.4 部署脚本

在项目根目录创建一键部署脚本 `deploy.sh`：

```bash
#!/bin/bash
set -e

echo "=== 1. 构建后端 ==="
cd backend
mvn clean package -DskipTests
cd ..

echo "=== 2. 构建前端 ==="
cd frontend
npm install
npm run build
cd ..

echo "=== 3. 停止旧服务 ==="
sudo systemctl stop fund-management 2>/dev/null || true

echo "=== 4. 部署后端 ==="
sudo cp backend/target/fund-management-1.0.0.jar /opt/fund-management/
sudo systemctl start fund-management

echo "=== 5. 部署前端 ==="
sudo rm -rf /var/www/fund-management/*
sudo cp -r frontend/dist/* /var/www/fund-management/

echo "=== 6. 重载 Nginx ==="
sudo nginx -t && sudo systemctl reload nginx

echo "✅ 部署完成！"
```

---

## 8. 验证部署

### 8.1 后端验证

```bash
# 公开接口测试
curl http://localhost:8080/api/market/indices
# 应返回 JSON 格式的五大指数数据

# 搜索基金测试
curl "http://localhost:8080/api/fund/search?keyword=沪深300"
```

### 8.2 前端验证

1. 浏览器打开 `http://localhost:3000`（开发模式）或你的域名
2. 页面应显示大盘指数行情条
3. 点击右上角用户菜单 → 注册账号
4. 登录后搜索基金并添加到持仓

### 8.3 Redis 缓存验证

添加基金后查看 Redis 中是否有缓存键：

```bash
redis-cli
> KEYS fund:data:*
# 应返回已缓存的基金数据键，如 "fund:data:000001"
> TTL fund:data:000001
# 显示剩余 TTL 秒数
```

---

## 9. 可选功能配置

### 9.1 邮箱验证码

在 `application.yml` 中配置 QQ 邮箱（或其他 SMTP 服务）：

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: your_email@qq.com        # ← 改成你的 QQ 邮箱
    password: your_smtp_authorization_code  # ← QQ 邮箱 SMTP 授权码（非登录密码）
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

> **获取 QQ 邮箱 SMTP 授权码**：QQ 邮箱 → 设置 → 账户 → POP3/SMTP 服务 → 开启 → 获取授权码。

不配置邮箱将无法使用注册功能（可手动执行 SQL 插入用户）。

### 9.2 百度 OCR 识别

在 [百度智能云控制台](https://console.bce.baidu.com/ai/#/ai/ocr/overview/index) 创建应用，获取 API Key 和 Secret Key。

```yaml
baidu:
  ocr:
    app-id: your_app_id               # ← 百度 OCR 应用 ID
    api-key: your_api_key             # ← API Key
    secret-key: your_secret_key       # ← Secret Key
```

不配置此项将无法使用 OCR 截图识别功能。

---

## 10. 常见问题

### Q1: MySQL 连接失败 `Communications link failure`

- 确认 MySQL 服务已启动
- 确认 `application.yml` 中的用户名和密码正确
- 确认数据库 `jiangcai_fund` 已创建

### Q2: Redis 连接失败 `Unable to connect to Redis`

- 确认 Redis 服务已启动：`redis-cli ping`
- 确认 `application.yml` 中 Redis 地址和端口正确

### Q3: 前端页面空白 / API 请求 404

- 开发模式下，确认 `vite.config.js` 中代理配置正确
- 确认后端服务在 `localhost:8080` 运行
- 生产部署时，确认 Nginx 配置中 `proxy_pass` 指向正确的地址

### Q4: 添加基金后数据加载慢

这是初次请求的正常现象——系统需要从第三方数据源（天天基金、东方财富）拉取基金数据。首只基金加载后，后续请求会命中 Redis 缓存，速度大幅提升。

### Q5: 登录后很快被踢出 / Token 过期

- 默认 Token 有效期 24 小时
- 检查 `application.yml` 中 `jwt.secret` 是否与部署时一致（更换会导致已签发 Token 全部失效）

### Q6: 定时任务没有执行

- 定时任务依赖系统时间，确认服务器时区为 `Asia/Shanghai`
- 净值同步在工作日 15:00-22:00 执行，非交易日不执行
- 可手动触发：`POST /api/fund/daily-profit/calculate`

### Q7: 端口被占用

检查端口占用情况：

```bash
# Windows
netstat -ano | findstr :8080

# Linux / macOS
lsof -i :8080
```

修改 `application.yml` 中的 `server.port` 或 `vite.config.js` 中的 `port` 即可更换端口。

### Q8: 如何备份数据库

```bash
mysqldump -u root -p jiangcai_fund > backup_$(date +%Y%m%d).sql
```

恢复：
```bash
mysql -u root -p jiangcai_fund < backup_20260616.sql
```

---

## 附录 A：所需端口一览

| 端口 | 服务 | 说明 |
|------|------|------|
| 3306 | MySQL | 数据库连接 |
| 6379 | Redis | 缓存服务 |
| 8080 | Java 后端 | Spring Boot API 服务 |
| 3000 | Vite 开发服务器 | 仅开发模式 |
| 80/443 | Nginx | 生产模式 Web 服务 |

> 防火墙需放行：3306（内部）、6379（内部）、8080（内部）、80/443（外部）。

## 附录 B：最小硬件配置

| 环境 | CPU | 内存 | 磁盘 | 适用 |
|------|-----|------|------|------|
| 最小 | 2 核 | 2 GB | 10 GB | 个人开发测试 |
| 推荐 | 4 核 | 4 GB | 20 GB | 生产部署 |

# 钱迹 · 个人理财管理系统（PC Web）

软件工程课程设计项目。前后端分离单体交付：Spring Boot 3 + MyBatis 提供 REST API，Vue 3 + Element Plus + ECharts 构建 PC Web 界面，MySQL 8 持久化六张业务表。

## 1. 功能概览

| 模块 | 能力 |
| --- | --- |
| 账号 | 注册、登录、退出、Session 30 分钟、安全问题重置密码、修改密码、昵称与安全问答 |
| 账户 | 多账户（现金/银行卡/支付宝/微信/其他）、期初余额、允许透支开关、余额原子扣减、一键对账修复 |
| 分类 | 12 个预设分类、自定义分类、启停、重命名、被引用保护 |
| 流水 | 增删改查、分页筛选、收支类型、跨账户/跨类型修改自动迁移余额、来源与去重指纹 |
| 预算 | 月度总预算 + 分类预算、Upsert、执行率、红黄绿分级、预警阈值、每日可花额度 |
| 统计 | 月度收支结余、分类占比（尾差归入最大项）、按日/按月趋势、饼图下钻、异常大额支出、固定订阅识别 |
| 目标 | 手工记录储蓄、进度、预计达成日期、达成状态 |
| 报表 | Excel 流水导出（EasyExcel）、PDF 财务报表（OpenPDF + 中文字体）、JSON 备份导出/导入 |
| 导入 | CSV 模板下载、UTF-8/GBK 解析、自动分类、逐行纠错、去重导入 |
| 智能简报 | 本地聚合 → 白名单脱敏 → Agnes（agnes-3.0-flash）调用 → 规则降级 |
| 管理端 | 管理员用户列表、启用/禁用普通用户 |
| 演示 | demo profile 下的事务回滚演示接口 |

## 2. 目录结构

```
.
├── backend/                     # Spring Boot 后端
│   ├── src/main/java/com/example/finance/
│   │   ├── account/ category/ record/ budget/ stats/ goal/
│   │   ├── user/ backup/ brief/ insight/ onboarding/ demo/
│   │   ├── reportexport/ web/ common/ config/ interceptor/
│   │   └── FinanceApplication.java
│   ├── src/main/resources/
│   │   ├── mapper/*.xml         # MyBatis SQL
│   │   ├── fonts/simhei.ttf     # PDF 中文字体
│   │   ├── static/              # 前端构建产物（由 npm run build 生成）
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-demo.yml
│   └── pom.xml
├── frontend/                    # Vue 3 前端
│   ├── src/views/               # 15 个页面
│   ├── src/api/  stores/  router/  layout/  components/
│   ├── vite.config.js           # 构建输出到 backend/src/main/resources/static
│   └── package.json
├── docs/sql/schema.sql          # 建库建表脚本
├── docs/sql/check_consistency.sql
├── 理财管理系统_方案总结_v3.0.md
└── 理财管理系统_开发方案_v1.0.md
```

## 3. 环境要求

- JDK 17+（本机验证 JDK 21）
- Maven 3.8+
- Node.js 18+（本机验证 Node 24）
- MySQL 8.0（本机验证 8.0.44）

## 4. 初始化数据库

```powershell
mysql -uroot -p -e "source docs/sql/schema.sql"
```

数据库名 `finance_db`，字符集 `utf8mb4`。连接信息默认如下，密码必须通过环境变量提供，不写入仓库：

| 配置 | 默认值 |
| --- | --- |
| DB_HOST | localhost |
| DB_PORT | 3306 |
| DB_NAME | finance_db |
| DB_USERNAME | root |
| DB_PASSWORD | 无默认值，需设置环境变量 |

## 5. 运行

先设置数据库密码环境变量（同时用于启动与后续构建）：

```powershell
$env:DB_PASSWORD='你的 MySQL 密码'
```

### 5.1 首次运行（先构建前端）

```powershell
cd frontend
npm install
npm run build          # 输出到 backend/src/main/resources/static

cd ../backend
mvn -DskipTests package
java -jar target\finance.jar --spring.profiles.active=demo
```

访问 <http://localhost:8080>。

- `demo` profile：开启事务回滚演示接口，适合课程答辩。
- `dev` profile：常规开发，演示接口关闭。

### 5.2 前端开发模式

```powershell
cd frontend
npm install
npm run dev
```

访问 <http://localhost:5173>，`/api` 自动代理到 8080。

### 5.3 重新构建

```powershell
cd frontend
npm run build          # 输出到 backend/src/main/resources/static

cd ../backend
mvn -DskipTests package
```

## 6. 演示账号

| 账号 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| demo | 123456 | 普通用户 | 首次启动自动生成 4 个月演示数据 |
| admin | admin123 | 管理员 | 可进入「用户管理」 |

新注册账号会自动创建 12 个预设分类，并进入新手引导。

## 7. 智能简报配置

仓库不包含任何 API Key。需要 AI 简报时自行申请 Agnes 密钥并设置环境变量，未配置时会自动使用规则引擎降级：

```powershell
$env:AGNES_API_KEY='sk-xxxx'
$env:AGNES_API_BASE='https://api.agnes-ai.cn/v1'   # 可选
```

请求只发送聚合后的白名单数字，不发送账号、账户名与备注原文；超时或输出校验失败会自动降级，并在响应中返回 `source=RULE`、`degraded=true`。

## 8. 演示脚本建议

1. 使用 demo/123456 登录，查看看板 4 张统计卡、预算执行与 6 个月趋势。
2. 在「收支流水」记一笔支出，观察账户余额即时变化。
3. 在「账户与对账」点击「检查对账」→「一键修复」，查看差异被消除。
4. 在「预算管理」切换月份并调整分类预算，观察执行率与红黄绿分级。
5. 在「统计分析」点击饼图扇区，下钻到对应分类流水。
6. 在「报表与备份」导出 Excel、PDF，再导出 JSON 备份。
7. 在「CSV 导入」下载模板 → 上传 → 修改账户/分类 → 确认导入，重复数据自动跳过。
8. 在「智能简报」生成月报，查看 AI 生成内容与来源标识（AI / 规则降级）。
9. 在「报表与备份」运行事务一致性演示，验证余额不足时流水与余额均不变。
10. 退出后用 admin/admin123 登录，进入「用户管理」禁用/启用账号。

## 9. 关键实现约定

- 登录态使用 HttpSession + HttpOnly/SameSite=Lax Cookie，30 分钟过期。
- 所有业务查询强制携带 Session 中的 `user_id`，前端无法指定归属；越权统一 404。
- 账户余额只允许由流水或「对账修复」改变，支出使用条件 UPDATE 原子扣减，余额不足整笔回滚。
- 预算唯一键为 `(user_id, month, category_id)`，`category_id=0` 表示月度总预算。
- 目标储蓄只更新 `goal.saved_amt`，不写流水、不改余额、不进收支统计。
- 备份导入：账户按名称复用、分类按（类型,名称）复用、流水按指纹去重、目标按名称跳过、导入后重算余额。

## 10. 常用校验

```powershell
# 健康检查（未登录应返回 401）
curl.exe -i http://localhost:8080/api/auth/me

# 登录
curl.exe -c cookies.txt -H "Content-Type: application/json" -d "{\"username\":\"demo\",\"password\":\"123456\"}" http://localhost:8080/api/auth/login

# 对账
curl.exe -b cookies.txt http://localhost:8080/api/accounts/reconcile
```

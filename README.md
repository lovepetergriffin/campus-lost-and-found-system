# 校园失物招领系统

面向校园场景的失物与招领信息平台。项目依据 `docs/S0` 至 `docs/S3` 中的立项、需求、规模度量和概要设计文档实现，覆盖信息发布、管理员审核、组合查询、认领申请、认领处理、通知和业务统计等核心流程。

## 功能范围

- 游客：浏览已审核信息，按关键词、类型、分类和地点筛选，查看物品详情。
- 普通用户：注册登录，发布失物/招领信息，编辑或关闭自己的发布，提交认领申请，处理收到的认领申请，查看通知。
- 管理员：审核待发布信息，维护物品分类，查看用户数、信息数、认领率等统计指标。
- 安全与规则：BCrypt 密码加密、Bearer 签名令牌、接口鉴权、角色权限、重复认领校验、物品状态流转和统一异常响应。

## 技术栈

- 后端：Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus 3.5、Maven
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Element Plus
- 数据库：默认 H2（开箱运行），支持 MySQL 8
- 测试：JUnit 5、Spring Boot Test、MockMvc

## 目录结构

```text
.
├── backend/                 # Spring Boot REST API
│   ├── src/main/java/       # controller/service/mapper/domain/security
│   ├── src/main/resources/  # H2/MySQL 配置与建表脚本
│   └── src/test/            # 核心业务链集成测试
├── frontend/                # Vue 3 单页应用
│   └── src/                 # 页面、路由、状态、API 与样式
└── docs/                    # S0–S3 课程阶段文档
```

## 快速启动

需要 JDK 17+、Maven 3.9+、Node.js 20.19+ 和 pnpm。

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认使用文件型 H2 数据库，数据保存在 `backend/.data`；接口地址为 `http://localhost:8080`，H2 控制台地址为 `http://localhost:8080/h2-console`。

### 2. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

打开 `http://localhost:5173`。Vite 会把 `/api` 请求代理到后端。

### 3. 演示管理员

- 用户名：`admin`
- 密码：`admin123`

该账号只用于本地演示。部署前必须修改默认密码，并通过环境变量 `TOKEN_SECRET` 设置足够长的随机令牌密钥。

## 使用 MySQL 8

先创建数据库：

```sql
CREATE DATABASE campus_lost_found CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后设置连接信息并启用 `mysql` profile：

```bash
cd backend
DB_URL='jdbc:mysql://localhost:3306/campus_lost_found?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=your_password \
TOKEN_SECRET=replace_with_a_long_random_secret \
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

`schema.sql` 会创建需求文档中定义的六类业务表（用户表在实现中命名为 `sys_user`，以规避数据库保留字冲突）。

## 核心 API

| 模块 | 方法与路径 | 说明 |
|---|---|---|
| 认证 | `POST /api/auth/register` | 用户注册 |
| 认证 | `POST /api/auth/login` | 登录并获取 Bearer 令牌 |
| 物品 | `GET /api/items` | 公开信息分页与组合查询 |
| 物品 | `POST /api/items` | 发布失物或招领信息 |
| 物品 | `GET /api/items/mine` | 我的发布记录 |
| 认领 | `POST /api/claims/items/{itemId}` | 提交认领申请 |
| 认领 | `PATCH /api/claims/{id}/decision` | 发布者处理认领申请 |
| 通知 | `GET /api/notifications` | 我的通知 |
| 管理 | `POST /api/admin/items/{id}/review` | 管理员审核信息 |
| 管理 | `GET /api/admin/statistics` | 业务统计 |

除公开查询、注册和登录外，请添加请求头：

```text
Authorization: Bearer <token>
```

## 构建与测试

```bash
cd backend && mvn test
cd ../frontend && pnpm build
```

后端集成测试覆盖“注册 → 发布 → 审核 → 查询 → 认领 → 确认 → 通知”的完整主流程，以及未授权访问拦截。

## 分支与提交规范

- 分支：`main`、`develop`、`feature/*`
- 提交前缀：`feat`、`fix`、`docs`、`test`、`refactor`、`chore`

# 陈哥厨房 (Der Kitchen) - 后端服务

一个私人家庭点餐小程序的后端服务，基于 Spring Boot 3.2 构建，采用 Maven 多模块架构。

## 技术栈

- **Java 17** + Spring Boot 3.2.5
- **MyBatis-Plus 3.5.6** (ORM)
- **PostgreSQL 15** (数据库)
- **Flyway**（空库单一 V1 基线）
- **MinIO** (图片存储)
- **Sa-Token 1.45.0**（认证与角色鉴权）
- **Knife4j 4.4** (API 文档)
- **Docker Compose** (部署)

---

## 项目结构

```
server/
├── pom.xml                          # 父 POM（版本统一管理）
├── Dockerfile
├── docker-compose.yml
│
├── der-kitchen-common/              # 公共模块（统一响应/异常/枚举/MyBatis-Plus配置）
├── der-kitchen-file/                # 文件服务模块（MinIO封装/图片压缩）
└── der-kitchen-app/                 # 启动模块（业务代码 + Spring Boot 入口）
    └── src/main/java/com/der/kitchen/
        ├── DerKitchenApplication.java
        ├── config/                  # AppConfig / WebConfig / CorsConfig / OpenApiConfig
        ├── auth/                    # 认证（微信/账号登录、Sa-Token角色数据源）
        ├── user/                    # 用户模块
        ├── category/                # 分类模块
        ├── dish/                    # 菜品模块
        ├── order/                   # 订单模块
        ├── notification/            # 订单事件、站内通知与微信异步投递
        ├── favorite/                # 收藏模块
        └── stats/                   # 统计模块
```

### 模块依赖关系

```
der-kitchen-app
  ├── der-kitchen-common
  ├── der-kitchen-file
  │     └── der-kitchen-common
```

---

## 本地开发

### 前置条件

- JDK 17+
- Maven 3.9+
- PostgreSQL 15+（或使用 Docker）
- MinIO（或使用 Docker）

### 快速启动（Docker）

```bash
# 在 server/ 目录执行
docker-compose up -d postgres minio

# 编译并启动应用
mvn clean install -DskipTests
cd der-kitchen-app
mvn spring-boot:run
```

### 仅 IDE 启动

在 `server/` 目录打开（识别为多模块 Maven 工程），直接运行：

```
com.der.kitchen.DerKitchenApplication
```

### 环境变量

复制 `.env.example` 为 `.env` 并修改配置：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| DB_HOST | 数据库地址 | localhost |
| DB_PORT | 数据库端口 | 5432 |
| DB_NAME | 数据库名 | der_kitchen |
| DB_USERNAME | 数据库用户 | postgres |
| DB_PASSWORD | 数据库密码 | postgres |
| MINIO_ENDPOINT | MinIO 地址 | http://localhost:9000 |
| MINIO_ACCESS_KEY | MinIO 用户 | minioadmin |
| MINIO_SECRET_KEY | MinIO 密码 | minioadmin |
| WX_APP_ID | 微信小程序 AppID | - |
| WX_APP_SECRET | 微信小程序 Secret | - |
| WX_SUBSCRIBE_ENABLED | 是否启用微信订阅消息投递 | false |
| WX_SUBSCRIBE_TEMPLATE_ID | 微信订阅消息模板 ID | - |
| WX_SUBSCRIBE_MAX_ATTEMPTS | 微信投递最大尝试次数 | 5 |
| ADMIN_OPENID | 管理员 OpenID | - |
| WIFE_OPENID | 用户 OpenID | - |
| ADMIN_USERNAME | 初始管理端用户名 | admin |
| ADMIN_PASSWORD_HASH | 初始管理端密码的 BCrypt 哈希 | - |
| CORS_ALLOWED_ORIGINS | 允许携带凭据访问 API 的前端来源，逗号分隔 | http://localhost:5173,http://127.0.0.1:5173 |

开发环境默认管理端账号为 `admin / Abc@1234`。配置文件和 V1 中保存的是 BCrypt 哈希；生产环境必须通过 `ADMIN_PASSWORD_HASH` 替换，禁止沿用开发密码。

`update_time/create_by/update_by` 由 MyBatis-Plus `AutoFillHandler` 维护，不依赖数据库触发器。登录成功后会把用户名写入 Sa-Token Session，审计字段记录用户名；无登录后台任务记录 `system`。

### API 文档

启动后访问：http://localhost:8080/doc.html

- **用户端接口**：`/api/v1/**`（除 admin）
- **管理端接口**：`/api/v1/admin/**`

生产 profile 默认关闭 Springdoc/Knife4j；Actuator 仅暴露不含详情的 `/actuator/health`。

### 接口认证

只有 `/api/v1/auth/wx-login`、`/api/v1/auth/admin-login` 无需登录；其余 `/api/**` 接口需要在 Header 携带：

```
Authorization: Bearer <token>
```

`/api/v1/admin/**` 和 `/api/common/file/**` 还要求当前用户具有 `admin` 角色。当前按单实例部署，Sa-Token 使用默认内存会话；服务重启后 token 失效。需要多实例或会话跨重启保留时，再引入 Redis 持久层。

### 数据库迁移

数据库结构由应用启动时的 Flyway 统一管理，唯一初始脚本为 `der-kitchen-app/src/main/resources/db/migration/V1__init.sql`。Docker Compose 不直接执行迁移脚本。旧数据库与当前单一基线不兼容；确认无需保留数据后，应重建空数据库再启动应用。

### HTTP 错误语义

错误响应的 HTTP 状态与响应体 `code` 保持一致。常用状态包括：参数错误 400、未登录 401、无权限 403、资源不存在 404、数据约束冲突 409、上传过大 413、外部服务异常 502/503、系统异常 500。具体约定见 [后端基础设施基线](../docs/backend-infrastructure.md)。

### 通知、统计与 API 契约

订单事件与通知在同一事务落库，微信消息由可重试后台任务异步投递；统计统一采用上海业务日期并排除取消订单。分页固定使用 `pageNum/pageSize`，响应为 `records/total/pageNum/pageSize`。完整约定见 [通知、统计与 API 契约基线](../docs/notification-stats-api-contract.md)。

### 菜品图片契约

菜品只持久化 `imageFileId`，对应 `dish_info.image_file_id → sys_file.id`；不保存 URL 或 MinIO 对象键。管理端先通过 `POST /api/common/file/upload` 上传 `files`，再把返回的 `fileId` 作为 `imageFileId` 提交给菜品接口。响应中的 `imageUrl`/`thumbnailUrl` 是动态生成的短期地址。完整规则见 [分类、菜品、文件与收藏闭环基线](../docs/category-dish-file-favorite.md)。

---

## 生产部署

```bash
# 在 server/ 目录

# 1. 编译打包
mvn clean package -DskipTests

# 2. 构建镜像并启动所有服务
docker-compose up -d --build
```

产物路径：`der-kitchen-app/target/der-kitchen-app.jar`

服务默认端口：
- 应用：8080
- PostgreSQL：5432
- MinIO Console：9001

---

## 模块说明

### der-kitchen-common

底层公共库，无业务依赖，可独立复用：

| 包 | 内容 |
|----|------|
| `aspect` | 接口日志切面 |
| `config` | MyBatisPlusConfig、AutoFillHandler |
| `entity` | `BaseEntity`（审计字段基类） |
| `enums` | OrderStatus（含状态机）、DishStatus、MealType、UserRole |
| `exception` | BizException、GlobalExceptionHandler |
| `result` | `R<T>`、PageResult |
| `util` | `SecurityUtils`（基于 Sa-Token 获取当前用户） |

### der-kitchen-file

MinIO 文件服务，依赖 common：

| 类 | 职责 |
|----|------|
| `MinioConfiguration` / `MinioProperties` | MinioClient Bean + 校验后的属性绑定 |
| `FileService` / `MinioTemplate` | 上传、压缩、预签名访问与删除 |
| `FileController` | `/api/common/file/**`（管理员鉴权） |

### der-kitchen-app

Spring Boot 启动入口，包含全部业务模块。
配置文件位于 `der-kitchen-app/src/main/resources/`。

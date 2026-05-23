# 陈哥厨房 (Der Kitchen) - 后端服务

一个私人家庭点餐小程序的后端服务，基于 Spring Boot 3.2 构建。

## 技术栈

- **Java 17** + Spring Boot 3.2.5
- **MyBatis-Plus 3.5.6** (ORM)
- **PostgreSQL 15** (数据库)
- **MinIO** (图片存储)
- **JWT** (认证)
- **Knife4j 4.4** (API 文档)
- **Docker Compose** (部署)

## 本地开发

### 前置条件

- JDK 17+
- PostgreSQL 15+ (或使用 Docker)
- MinIO (或使用 Docker)

### 快速启动（Docker）

```bash
# 启动 PostgreSQL + MinIO
docker-compose up -d postgres minio

# 运行应用
./mvnw spring-boot:run
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
| JWT_SECRET | JWT 签名密钥 | - |
| ADMIN_OPENID | 管理员 OpenID | - |
| WIFE_OPENID | 用户 OpenID | - |

### API 文档

启动后访问：http://localhost:8080/doc.html

- **用户端接口**：`/api/v1/**`（除 admin）
- **管理端接口**：`/api/v1/admin/**`

### 接口认证

除 `/api/v1/auth/*` 外，所有接口需要在 Header 携带：

```
Authorization: Bearer <token>
```

## 生产部署

```bash
# 构建镜像并启动所有服务
docker-compose up -d --build
```

服务默认端口：
- 应用：8080
- PostgreSQL：5432
- MinIO Console：9001

## 项目结构

```
src/main/java/com/der/kitchen/
├── auth/           # 认证模块（登录、JWT过滤器）
├── user/           # 用户模块
├── category/       # 分类模块
├── dish/           # 菜品模块
├── order/          # 订单模块
├── favorite/       # 收藏模块
├── file/           # 文件上传模块
├── stats/          # 统计模块
└── common/         # 公共模块
    ├── annotation/ # 自定义注解
    ├── aspect/     # AOP切面
    ├── config/     # 配置类
    ├── entity/     # 基类
    ├── enums/      # 枚举
    ├── exception/  # 异常处理
    ├── result/     # 统一响应
    └── util/       # 工具类
```

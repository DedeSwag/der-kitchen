# 陈哥厨房 后端模块化重构设计文档

> 编写日期：2026-07-11  
> 版本：V2.0  
> 参考项目：smart-om-server（智慧运维多模块架构）  
> 设计目标：将单体 Spring Boot 项目拆分为 Maven 多模块结构

> 2026-08-07 补充决策：认证基线改为 Sa-Token 1.45.0 标准有状态会话；本文中原 JWT 自实现方案已按当前实现修订。

---

## 一、重构动机

### 1.1 当前问题

当前 `der-kitchen-server` 是一个单 Maven 模块项目，所有代码（common、file、auth、业务模块）耦合在同一 artifact 中：

```
server/
├── pom.xml                          ← 单体 pom，所有依赖混合
└── src/main/java/com/der/kitchen/
    ├── common/                      ← 通用代码（可独立复用）
    ├── file/                        ← 文件服务（可独立复用）
    ├── auth/                        ← 认证模块
    ├── user/                        ← 用户模块
    ├── dish/                        ← 菜品模块
    ├── category/                    ← 分类模块
    ├── order/                       ← 订单模块
    ├── favorite/                    ← 收藏模块
    └── stats/                       ← 统计模块
```

**痛点：**
- 依赖膨胀：业务模块强依赖 MinIO SDK 等非必要依赖
- 无法独立复用：common/file 等底层模块无法被其他项目引用
- 编译耦合：修改 file 模块需要重新编译整个项目
- 职责混乱：application.yml 混合了所有模块的配置

### 1.2 目标架构

参考 smart-om-server 的多模块设计，拆分为 **父 POM + 3 个子模块**：

```
server/
├── pom.xml                          ← 父 POM（dependencyManagement 统一版本）
├── docker-compose.yml
├── Dockerfile
├── der-kitchen-common/              ← 公共模块
├── der-kitchen-file/                ← 文件服务模块
└── der-kitchen-app/                 ← 启动模块（聚合所有依赖）
```

---

## 二、模块职责划分

### 2.1 模块依赖关系

```
der-kitchen-app
  ├── der-kitchen-common
  ├── der-kitchen-file
  │     └── der-kitchen-common
```

> app 显式依赖 common 和 file，file 依赖 common，依赖关系保持单向；不依靠传递依赖表达 app 对 common 的直接使用。

### 2.2 各模块职责

| 模块 | artifactId | 职责 | 打包方式 |
|------|-----------|------|---------|
| 父 POM | der-kitchen-server | 统一版本管理、插件管理 | pom |
| 公共模块 | der-kitchen-common | 统一响应、异常处理、BaseEntity、枚举、工具类、MyBatis-Plus 配置 | jar |
| 文件模块 | der-kitchen-file | MinIO 配置、文件上传/压缩/删除服务 | jar |
| 启动模块 | der-kitchen-app | Spring Boot 入口、业务模块代码、配置文件、数据库驱动、API 文档 | jar (executable) |

---

## 三、详细模块设计

### 3.1 父 POM (`server/pom.xml`)

```xml
<groupId>com.der</groupId>
<artifactId>der-kitchen-server</artifactId>
<version>1.0.0</version>
<packaging>pom</packaging>

<modules>
    <module>der-kitchen-common</module>
    <module>der-kitchen-file</module>
    <module>der-kitchen-app</module>
</modules>
```

**职责：**
- 继承 `spring-boot-starter-parent`
- `<properties>` 统一管理所有第三方依赖版本号
- `<dependencyManagement>` 声明子模块间的依赖坐标
- `<pluginManagement>` 统一 maven-compiler-plugin、spring-boot-maven-plugin 等

**版本清单（properties）：**

| 依赖 | 版本 |
|------|------|
| mybatis-plus | 3.5.6 |
| knife4j | 4.4.0 |
| minio | 8.5.9 |
| sa-token | 1.45.0 |
| flyway | Spring Boot 依赖管理 |
| thumbnailator | 0.4.20 |
| hutool | 5.8.26 |

---

### 3.2 公共模块 (`der-kitchen-common`)

**包结构：**

```
com.der.kitchen.common/
├── aspect/
│   └── ApiLogAspect.java             # 接口日志切面
├── config/
│   ├── MyBatisPlusConfig.java        # 分页插件配置
│   ├── AutoFillHandler.java          # 审计字段自动填充
├── entity/
│   └── BaseEntity.java              # 实体基类
├── enums/
│   ├── OrderStatus.java
│   ├── DishStatus.java
│   ├── MealType.java
│   └── UserRole.java
├── exception/
│   ├── BizException.java            # 业务异常
│   └── GlobalExceptionHandler.java  # 全局异常处理器
├── result/
│   ├── R.java                       # 统一响应体
│   └── PageResult.java              # 分页响应
└── util/
    └── SecurityUtils.java            # Sa-Token 当前用户访问入口
```

**依赖（pom.xml）：**
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-aop`
- `mybatis-plus-spring-boot3-starter`
- `sa-token-core`（`SecurityUtils` 编译期需要）
- `lombok`（provided）

> 上述框架依赖在 common 中仅作为 optional 编译依赖，不向 app 传递运行时；app 必须显式声明自身使用的框架能力。CORS 和鉴权 WebConfig 含应用配置，归 app 模块。

---

### 3.3 文件模块 (`der-kitchen-file`)

**包结构：**

```
com.der.kitchen.file/
├── config/
│   ├── MinioConfiguration.java       # MinIO 客户端配置
│   └── MinioProperties.java          # 带校验的属性绑定
├── minio/MinioTemplate.java          # 对象存储操作封装
├── service/FileService.java          # 上传/访问/删除抽象
└── controller/
    └── FileController.java          # 文件上传接口
```

**依赖（pom.xml）：**
- `der-kitchen-common`（继承公共基础设施）
- `spring-boot-starter-web`、`spring-boot-starter-validation`、MyBatis-Plus（optional 编译依赖）
- `hutool-all`（文件实现直接使用）
- `minio`（MinIO SDK）
- `thumbnailator`（图片压缩）
- `swagger-annotations-jakarta`（API 注解，可选）

---

### 3.4 启动模块 (`der-kitchen-app`)

**包结构：**

```
com.der.kitchen/
├── DerKitchenApplication.java        # Spring Boot 主入口
├── auth/                             # 认证模块
│   ├── controller/AuthController.java
│   ├── service/StpInterfaceImpl.java # Sa-Token 角色数据源
│   └── dto/
├── user/                             # 用户模块
├── dish/                             # 菜品模块
├── category/                         # 分类模块
├── order/                            # 订单模块
├── favorite/                         # 收藏模块
└── stats/                            # 统计模块
```

**依赖（pom.xml）：**
- `der-kitchen-common`
- `der-kitchen-file`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-aop`
- `mybatis-plus-spring-boot3-starter`
- `hutool-all`
- `sa-token-spring-boot3-starter`
- `postgresql`（runtime）
- `knife4j-openapi3-jakarta-spring-boot-starter`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`（test）

**配置文件位置：** `der-kitchen-app/src/main/resources/`
- `application.yml`（主配置）
- `application-dev.yml`（开发环境）
- `application-prod.yml`（生产环境）

**Spring Boot 打包插件**仅在此模块配置。

---

## 四、配置管理策略

### 4.1 配置归属

| 配置项 | 归属模块 | 说明 |
|--------|---------|------|
| server.port | app | 仅启动模块关心 |
| spring.datasource.* | app | 数据源由运行层提供 |
| mybatis-plus.* | app | 可由 common 提供默认值，app 覆盖 |
| minio.* | app（file 模块读取） | 配置在 app 中，file 模块通过 `@ConfigurationProperties` 读取 |
| app.wx.* / app.auth.* | app | 业务配置 |
| springdoc.* / knife4j.* | app | API 文档配置 |
| logging.* | app | 日志配置 |

### 4.2 自动装配

- `der-kitchen-common` 和 `der-kitchen-file` 中的 `@Configuration` / `@Component` 类通过包扫描被 app 模块自动发现
- app 模块的 `@SpringBootApplication` 所在包为 `com.der.kitchen`，自动扫描所有子包
- 无需额外配置 `spring.factories` 或 `AutoConfiguration`

---

## 五、迁移映射表

| 原路径 | 目标模块 | 目标路径 |
|--------|---------|---------|
| `common/annotation/` | common | `common/annotation/` |
| `common/aspect/` | common | `common/aspect/` |
| `common/config/AutoFillHandler.java` | common | `common/config/AutoFillHandler.java` |
| `common/config/MyBatisPlusConfig.java` | common | `common/config/MyBatisPlusConfig.java` |
| `common/config/CorsConfig.java` | **app** | `config/CorsConfig.java` |
| `common/config/WebConfig.java` | **app** | `config/WebConfig.java` |
| `common/config/MinioConfig.java` | **file** | `file/config/MinioConfiguration.java` + `MinioProperties.java` |
| `common/config/AppConfig.java` | **app** | `config/AppConfig.java` |
| `common/config/OpenApiConfig.java` | **app** | `config/OpenApiConfig.java` |
| `common/entity/` | common | `common/entity/` |
| `common/enums/` | common | `common/enums/` |
| `common/exception/` | common | `common/exception/` |
| `common/result/` | common | `common/result/` |
| `common/util/` | common | `common/util/` |
| `file/controller/` | **file** | `file/controller/` |
| `file/service/` | **file** | `file/service/` |
| `auth/` | **app** | `auth/` |
| `user/` | **app** | `user/` |
| `dish/` | **app** | `dish/` |
| `category/` | **app** | `category/` |
| `order/` | **app** | `order/` |
| `favorite/` | **app** | `favorite/` |
| `stats/` | **app** | `stats/` |
| `DerKitchenApplication.java` | **app** | `DerKitchenApplication.java` |

---

## 六、包名设计

| 模块 | 顶层包名 |
|------|---------|
| common | `com.der.kitchen.common` |
| file | `com.der.kitchen.file` |
| app | `com.der.kitchen` |

> app 模块的 `@SpringBootApplication` 在 `com.der.kitchen` 下，确保所有子包（common/file/auth/user/...）均被自动扫描。

---

## 七、构建与运行

### 7.1 本地开发

```bash
# 在 server/ 根目录执行
mvn clean install -DskipTests    # 编译安装所有模块到本地仓库

# 启动应用（方式一：Maven）
cd der-kitchen-app
mvn spring-boot:run

# 启动应用（方式二：IDE）
# 直接运行 DerKitchenApplication.java 即可
```

### 7.2 Docker 部署

```bash
# 构建 jar
mvn clean package -DskipTests

# 构建镜像
docker build -t der-kitchen-server .

# 完整启动
docker-compose up -d
```

**Dockerfile 修改：**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY der-kitchen-app/target/der-kitchen-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 八、与参考项目 smart-om 的差异决策

| 对比项 | smart-om | der-kitchen（本次） | 原因 |
|--------|---------|-------------------|------|
| 认证框架 | Sa-Token | Sa-Token 标准会话 | 统一处理登录态、踢下线、角色鉴权和多端扩展，删除自研 JWT 链路 |
| AutoFillHandler 获取用户 | StpUtil.getSession() | 登录时写入 Session，AutoFillHandler 读取 `username` | 审计字段记录可读用户名，无登录任务记录 `system` |
| 业务模块粒度 | 每个业务域独立模块 | 业务模块合并在 app 中 | 项目规模小，6 个业务模块独立过重 |
| 文件模块 | 独立数据库表记录文件 | `sys_file` 元数据 + MinIO 对象 | 数据库存稳定对象键和归属，访问时生成预签名 URL |
| 工具库 | hutool-all + easyexcel | hutool-all | 无导出需求 |
| ID 策略 | 雪花算法 (ASSIGN_ID) | 自增 (AUTO) | PostgreSQL BIGSERIAL，保持原设计 |

---

## 九、风险与注意事项

1. **包扫描路径**：app 模块的 Application 类必须在 `com.der.kitchen` 包下，否则无法扫描到 common/file 模块的 Bean
2. **resources 冲突**：同名配置文件（如 application.yml）仅 app 模块有效，common/file 不放 application.yml
3. **循环依赖**：common 不能引用 file 或 app 的类，file 不能引用 app 的类
4. **测试配置**：集成测试在 app 模块中运行，单元测试各模块独立
5. **IDE 支持**：IntelliJ IDEA 需要在 server/ 目录打开项目，识别为多模块 Maven 工程

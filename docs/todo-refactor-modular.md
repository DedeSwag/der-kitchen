# 后端模块化重构 TODO 清单

> 本文是重构前的历史执行清单，不再作为当前任务依据，其中 JWT、`UserContextHolder` 和 `@RequireRole` 方案均已废弃。当前整改顺序见 [backend-remediation-plan.md](./backend-remediation-plan.md)。

> 关联设计文档：[refactor-modular-design.md](./refactor-modular-design.md)  
> 基准：当前单体结构 `server/` 目录  
> 目标：拆分为 `der-kitchen-common` / `der-kitchen-file` / `der-kitchen-app` 三模块

---

## 阶段一：父 POM 与模块骨架搭建

- [ ] 1.1 备份当前 `server/pom.xml` 为 `server/pom.xml.bak`
- [ ] 1.2 重写 `server/pom.xml` 为父 POM：
  - packaging 改为 `pom`
  - 保留 `spring-boot-starter-parent` 作为 parent
  - 声明 `<modules>` 包含三个子模块
  - 将原有 `<properties>` 中的版本号保留
  - 添加 `<dependencyManagement>` 管理子模块坐标和第三方版本
  - 移除 `<dependencies>`（拆到子模块）
  - 移除 `spring-boot-maven-plugin`（仅 app 模块需要）
  - 添加全局 `lombok` 依赖（provided）
- [ ] 1.3 创建 `server/der-kitchen-common/` 目录结构：
  ```
  der-kitchen-common/
  ├── pom.xml
  └── src/main/java/com/der/kitchen/common/
  ```
- [ ] 1.4 创建 `server/der-kitchen-file/` 目录结构：
  ```
  der-kitchen-file/
  ├── pom.xml
  └── src/main/java/com/der/kitchen/file/
  ```
- [ ] 1.5 创建 `server/der-kitchen-app/` 目录结构：
  ```
  der-kitchen-app/
  ├── pom.xml
  └── src/
      ├── main/java/com/der/kitchen/
      └── main/resources/
  ```
- [ ] 1.6 编写 `der-kitchen-common/pom.xml`
- [ ] 1.7 编写 `der-kitchen-file/pom.xml`
- [ ] 1.8 编写 `der-kitchen-app/pom.xml`（含 spring-boot-maven-plugin）
- [ ] 1.9 验证 `mvn clean install -DskipTests` 空项目能正常编译

---

## 阶段二：迁移 common 模块

- [ ] 2.1 移动 `common/entity/BaseEntity.java` → `der-kitchen-common`
- [ ] 2.2 移动 `common/result/R.java` → `der-kitchen-common`
- [ ] 2.3 移动 `common/result/PageResult.java` → `der-kitchen-common`
- [ ] 2.4 移动 `common/exception/BizException.java` → `der-kitchen-common`
- [ ] 2.5 移动 `common/exception/GlobalExceptionHandler.java` → `der-kitchen-common`
- [ ] 2.6 移动 `common/enums/` 全部枚举 → `der-kitchen-common`
  - `OrderStatus.java`
  - `DishStatus.java`
  - `MealType.java`
  - `UserRole.java`
- [ ] 2.7 移动 `common/annotation/RequireRole.java` → `der-kitchen-common`
- [ ] 2.8 移动 `common/aspect/RoleCheckAspect.java` → `der-kitchen-common`
- [ ] 2.9 移动 `common/aspect/ApiLogAspect.java` → `der-kitchen-common`
- [ ] 2.10 移动 `common/config/MyBatisPlusConfig.java` → `der-kitchen-common`
- [ ] 2.11 移动 `common/config/AutoFillHandler.java` → `der-kitchen-common`
- [ ] 2.12 移动 `common/config/CorsConfig.java` → `der-kitchen-common`
- [ ] 2.13 移动 `common/config/WebConfig.java` → `der-kitchen-common`
- [ ] 2.14 移动 `common/util/JwtUtil.java` → `der-kitchen-common`
- [ ] 2.15 移动 `common/util/WxUtil.java` → `der-kitchen-common`
- [ ] 2.16 移动 `common/util/UserContext.java` → `der-kitchen-common`
- [ ] 2.17 移动 `common/util/UserContextHolder.java` → `der-kitchen-common`
- [ ] 2.18 确认所有文件包名仍为 `com.der.kitchen.common.*`（无需修改）
- [ ] 2.19 验证 `der-kitchen-common` 模块独立编译通过

---

## 阶段三：迁移 file 模块

- [ ] 3.1 移动 `common/config/MinioConfig.java` → `der-kitchen-file/src/.../file/config/`
  - 修改包名 `com.der.kitchen.common.config` → `com.der.kitchen.file.config`
- [ ] 3.2 移动 `file/service/MinioService.java` → `der-kitchen-file/src/.../file/service/`
  - 修改 import `com.der.kitchen.common.config.MinioConfig` → `com.der.kitchen.file.config.MinioConfig`
- [ ] 3.3 移动 `file/controller/FileController.java` → `der-kitchen-file/src/.../file/controller/`
- [ ] 3.4 确认 file 模块对 common 模块的依赖已在 pom.xml 中声明
- [ ] 3.5 验证 `der-kitchen-file` 模块独立编译通过

---

## 阶段四：迁移 app 模块（业务代码）

- [ ] 4.1 移动 `DerKitchenApplication.java` → `der-kitchen-app`
- [ ] 4.2 移动 `auth/` 全部文件 → `der-kitchen-app`
  - `controller/AuthController.java`
  - `filter/JwtAuthFilter.java`
  - `dto/WxLoginRequest.java`
  - `dto/AdminLoginRequest.java`
  - `dto/LoginResponse.java`
- [ ] 4.3 移动 `user/` 全部文件 → `der-kitchen-app`
  - `entity/User.java`
  - `mapper/UserMapper.java`
  - `service/UserService.java`
  - `controller/UserController.java`
- [ ] 4.4 移动 `category/` 全部文件 → `der-kitchen-app`
  - `entity/Category.java`
  - `mapper/CategoryMapper.java`
  - `service/CategoryService.java`
  - `controller/CategoryController.java`
  - `controller/AdminCategoryController.java`
  - `dto/CategoryRequest.java`
- [ ] 4.5 移动 `dish/` 全部文件 → `der-kitchen-app`
  - `entity/Dish.java`
  - `mapper/DishMapper.java`
  - `service/DishService.java`
  - `controller/DishController.java`
  - `controller/AdminDishController.java`
  - `dto/DishCreateDTO.java`
  - `dto/DishUpdateDTO.java`
  - `dto/DishQueryDTO.java`
- [ ] 4.6 移动 `order/` 全部文件 → `der-kitchen-app`
  - `entity/Order.java`
  - `entity/OrderItem.java`
  - `mapper/OrderMapper.java`
  - `mapper/OrderItemMapper.java`
  - `service/OrderService.java`
  - `controller/OrderController.java`
  - `controller/AdminOrderController.java`
  - `dto/OrderCreateDTO.java`
  - `dto/OrderQueryDTO.java`
  - `dto/OrderAddItemDTO.java`
  - `vo/OrderDetailVO.java`
- [ ] 4.7 移动 `favorite/` 全部文件 → `der-kitchen-app`
  - `entity/Favorite.java`
  - `mapper/FavoriteMapper.java`
  - `service/FavoriteService.java`
  - `controller/FavoriteController.java`
- [ ] 4.8 移动 `stats/` 全部文件 → `der-kitchen-app`
  - `service/StatsService.java`
  - `controller/StatsController.java`
- [ ] 4.9 移动 `common/config/AppConfig.java` → `der-kitchen-app` 的 `config/` 下
  - 修改包名为 `com.der.kitchen.config`
- [ ] 4.10 移动 `common/config/OpenApiConfig.java` → `der-kitchen-app` 的 `config/` 下
  - 修改包名为 `com.der.kitchen.config`
- [ ] 4.11 移动配置文件到 `der-kitchen-app/src/main/resources/`
  - `application.yml`
  - `application-dev.yml`
  - `application-prod.yml`
  - `db/migration/V1__init.sql`（如有）
- [ ] 4.12 移动 Docker 相关文件保持在 `server/` 根目录
- [ ] 4.13 检查所有 Java 文件的 import 语句，修正因模块拆分导致的包名变更
- [ ] 4.14 验证 `der-kitchen-app` 模块编译通过

---

## 阶段五：全量验证

- [ ] 5.1 在 `server/` 根目录执行 `mvn clean package -DskipTests`，确认构建成功
- [ ] 5.2 确认生成 `der-kitchen-app/target/der-kitchen-app.jar`
- [ ] 5.3 启动应用 `java -jar der-kitchen-app/target/der-kitchen-app.jar`
  - 确认 Spring Boot 正常启动
  - 确认数据库连接正常
  - 确认 MinIO 连接正常
- [ ] 5.4 访问 `http://localhost:8080/doc.html` 确认 Knife4j 文档正常
- [ ] 5.5 使用 Postman/Knife4j 测试核心接口：
  - 登录接口 `POST /api/v1/auth/wx-login`
  - 菜品列表 `GET /api/v1/dishes`
  - 文件上传 `POST /api/v1/admin/files/upload`
  - 创建订单 `POST /api/v1/orders`
- [ ] 5.6 确认 Docker 构建流程正常：
  - 修改 `Dockerfile` 指向新的 jar 路径
  - `docker-compose up -d --build` 验证
- [ ] 5.7 清理旧代码：
  - 删除 `server/src/` 目录（已全部迁移到子模块）
  - 删除 `server/pom.xml.bak`

---

## 阶段六：文档更新

- [ ] 6.1 更新 `server/README.md`：
  - 修改项目结构图为多模块结构
  - 更新构建命令
  - 更新 Docker 部署说明
- [ ] 6.2 各子模块添加简要 README（可选）
- [ ] 6.3 更新 `.gitignore`（如需排除新的 target 目录）

---

## 附录：最终目录结构预览

```
server/
├── pom.xml                                    # 父 POM
├── Dockerfile
├── docker-compose.yml
├── .env.example
│
├── der-kitchen-common/                        # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/der/kitchen/common/
│       ├── annotation/
│       │   └── RequireRole.java
│       ├── aspect/
│       │   ├── RoleCheckAspect.java
│       │   └── ApiLogAspect.java
│       ├── config/
│       │   ├── MyBatisPlusConfig.java
│       │   ├── AutoFillHandler.java
│       │   ├── CorsConfig.java
│       │   └── WebConfig.java
│       ├── entity/
│       │   └── BaseEntity.java
│       ├── enums/
│       │   ├── OrderStatus.java
│       │   ├── DishStatus.java
│       │   ├── MealType.java
│       │   └── UserRole.java
│       ├── exception/
│       │   ├── BizException.java
│       │   └── GlobalExceptionHandler.java
│       ├── result/
│       │   ├── R.java
│       │   └── PageResult.java
│       └── util/
│           ├── JwtUtil.java
│           ├── WxUtil.java
│           ├── UserContext.java
│           └── UserContextHolder.java
│
├── der-kitchen-file/                          # 文件服务模块
│   ├── pom.xml
│   └── src/main/java/com/der/kitchen/file/
│       ├── config/
│       │   └── MinioConfig.java
│       ├── service/
│       │   └── MinioService.java
│       └── controller/
│           └── FileController.java
│
└── der-kitchen-app/                           # 启动模块（含全部业务代码）
    ├── pom.xml
    └── src/
        ├── main/java/com/der/kitchen/
        │   ├── DerKitchenApplication.java
        │   ├── config/
        │   │   ├── AppConfig.java
        │   │   └── OpenApiConfig.java
        │   ├── auth/
        │   │   ├── controller/AuthController.java
        │   │   ├── filter/JwtAuthFilter.java
        │   │   └── dto/
        │   ├── user/
        │   │   ├── entity/User.java
        │   │   ├── mapper/UserMapper.java
        │   │   ├── service/UserService.java
        │   │   └── controller/UserController.java
        │   ├── category/
        │   │   ├── entity/Category.java
        │   │   ├── mapper/CategoryMapper.java
        │   │   ├── service/CategoryService.java
        │   │   ├── controller/CategoryController.java
        │   │   ├── controller/AdminCategoryController.java
        │   │   └── dto/CategoryRequest.java
        │   ├── dish/
        │   │   ├── entity/Dish.java
        │   │   ├── mapper/DishMapper.java
        │   │   ├── service/DishService.java
        │   │   ├── controller/DishController.java
        │   │   ├── controller/AdminDishController.java
        │   │   └── dto/
        │   ├── order/
        │   │   ├── entity/
        │   │   ├── mapper/
        │   │   ├── service/OrderService.java
        │   │   ├── controller/OrderController.java
        │   │   ├── controller/AdminOrderController.java
        │   │   ├── dto/
        │   │   └── vo/
        │   ├── favorite/
        │   │   ├── entity/Favorite.java
        │   │   ├── mapper/FavoriteMapper.java
        │   │   ├── service/FavoriteService.java
        │   │   └── controller/FavoriteController.java
        │   └── stats/
        │       ├── service/StatsService.java
        │       └── controller/StatsController.java
        └── main/resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            └── db/migration/V1__init.sql
```

---

## 时间估算

| 阶段 | 预估耗时 | 说明 |
|------|---------|------|
| 阶段一：骨架搭建 | 1h | POM 编写与目录创建 |
| 阶段二：迁移 common | 0.5h | 文件移动，无代码变更 |
| 阶段三：迁移 file | 0.5h | 少量包名修改 |
| 阶段四：迁移 app | 1h | 主要是文件移动和 import 修正 |
| 阶段五：验证 | 1h | 编译、启动、接口测试 |
| 阶段六：文档 | 0.5h | README 更新 |
| **合计** | **~4.5h** | — |

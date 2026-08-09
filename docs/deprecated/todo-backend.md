# 后端搭建待办清单

> 技术栈：Java 17 + Spring Boot 3.2 + MyBatis-Plus + PostgreSQL 15 + MinIO  
> 包名：com.der.kitchen

---

## 阶段一：工程搭建与基础配置（0.5天）

- [ ] 1.1 使用 Spring Initializr 创建项目，选择依赖：
  - Spring Web
  - Spring Boot Actuator
  - PostgreSQL Driver
  - Lombok
- [ ] 1.2 pom.xml 补充依赖：
  - mybatis-plus-spring-boot3-starter 3.5.x
  - knife4j-openapi3-jakarta-spring-boot-starter 4.x
  - minio 8.x（官方 SDK）
  - java-jwt / jjwt
  - caffeine（本地缓存）
  - thumbnailator（图片压缩）
- [ ] 1.3 创建工程目录结构（common/config/exception/result/enums/util + 各业务模块）
- [ ] 1.4 编写 application.yml / application-dev.yml / application-prod.yml
- [ ] 1.5 编写 docker-compose.yml（app + postgres + minio 三个服务）
- [ ] 1.6 编写 Dockerfile
- [ ] 1.7 启动 docker-compose，确认 PostgreSQL 和 MinIO 可访问
- [ ] 1.8 执行 DDL 初始化脚本（V1__init.sql），创建全部表 + 触发器
- [ ] 1.9 MinIO 创建 bucket（dishes / avatars），设置 public-read 策略

---

## 阶段二：通用模块开发（0.5天）

- [ ] 2.1 统一响应体 `R<T>` 和 `PageResult<T>`
- [ ] 2.2 业务异常类 `BizException`
- [ ] 2.3 全局异常处理器 `GlobalExceptionHandler`（BizException / Validation / 兜底Exception）
- [ ] 2.4 枚举定义：`UserRole`、`OrderStatus`（含状态机校验方法）、`DishStatus`、`MealType`
- [ ] 2.5 MyBatis-Plus 配置（分页插件、逻辑删除、驼峰映射）
- [ ] 2.6 CORS 配置（允许管理端域名 + 小程序域名）
- [ ] 2.7 Knife4j 配置（API 分组：用户端 / 管理端）
- [ ] 2.8 接口日志 AOP（记录请求路径、参数、响应耗时）
- [ ] 2.9 公共字段自动填充（MetaObjectHandler：create_time/update_time/create_by/update_by）

---

## 阶段三：认证模块（0.5天）

- [ ] 3.1 微信工具类 `WxUtil`：code2session 接口调用，换取 openid
- [ ] 3.2 JWT 工具类 `JwtUtil`：签发 / 解析 / 刷新 token
- [ ] 3.3 用户上下文 `UserContextHolder`（ThreadLocal 存储当前用户信息）
- [ ] 3.4 JWT 认证过滤器 `JwtAuthFilter`：解析 Header → 注入 UserContext
- [ ] 3.5 权限注解 `@RequireRole` + AOP 切面 `RoleCheckAspect`
- [ ] 3.6 白名单配置（application.yml 中 allowed-openids 列表）
- [ ] 3.7 登录接口 `POST /api/v1/auth/wx-login`：
  - 调用微信换取 openid
  - 校验白名单
  - 查询/创建用户
  - 签发 JWT 返回
- [ ] 3.8 登录接口联调测试（使用微信开发者工具获取 code 测试）

---

## 阶段四：用户模块（0.5天）

- [ ] 4.1 User 实体类 + UserMapper
- [ ] 4.2 UserService：根据 openid 查询、创建用户、更新信息
- [ ] 4.3 UserController：`GET /api/v1/user/info`（获取当前用户信息）

---

## 阶段五：分类模块（0.5天）

- [ ] 5.1 Category 实体类 + CategoryMapper
- [ ] 5.2 CategoryService：CRUD + 批量排序
- [ ] 5.3 用户端接口 `GET /api/v1/categories`（仅返回 status=active，按 sort_order 排序）
- [ ] 5.4 管理端接口：
  - `GET /api/v1/admin/categories`（含 hidden）
  - `POST /api/v1/admin/categories`
  - `PUT /api/v1/admin/categories/{id}`
  - `DELETE /api/v1/admin/categories/{id}`（校验有无关联菜品）
  - `PUT /api/v1/admin/categories/sort`（批量更新排序）

---

## 阶段六：菜品模块（1天）

- [ ] 6.1 Dish 实体类 + DishMapper
- [ ] 6.2 DishService：CRUD + 上下架 + 状态切换 + 批量上下架
- [ ] 6.3 DishCreateDTO / DishUpdateDTO / DishVO 定义（含参数校验注解）
- [ ] 6.4 用户端接口：
  - `GET /api/v1/dishes`（is_listed=true & deleted=false，附带 isFavorited 字段）
  - `GET /api/v1/dishes/{id}`
- [ ] 6.5 管理端接口：
  - `GET /api/v1/admin/dishes`（支持 categoryId / status / isListed 筛选 + 分页）
  - `POST /api/v1/admin/dishes`
  - `PUT /api/v1/admin/dishes/{id}`
  - `DELETE /api/v1/admin/dishes/{id}`（软删除）
  - `PUT /api/v1/admin/dishes/{id}/status`
  - `PUT /api/v1/admin/dishes/{id}/listing`
  - `PUT /api/v1/admin/dishes/batch-listing`

---

## 阶段七：收藏模块（0.5天）

- [ ] 7.1 Favorite 实体类 + FavoriteMapper
- [ ] 7.2 FavoriteService：添加 / 取消 / 列表查询
- [ ] 7.3 用户端接口：
  - `GET /api/v1/favorites`（返回菜品详情列表）
  - `POST /api/v1/favorites`
  - `DELETE /api/v1/favorites/{dishId}`

---

## 阶段八：订单模块（1.5天）

- [ ] 8.1 Order 实体类 + OrderMapper
- [ ] 8.2 OrderItem 实体类 + OrderItemMapper
- [ ] 8.3 OrderCreateDTO / OrderVO / OrderDetailVO 定义
- [ ] 8.4 OrderService 核心逻辑：
  - 创建订单（校验菜品可用性 → 创建 order → 批量创建 order_items）
  - 取消订单（校验 pending 状态）
  - 追加菜品（校验 pending/preparing → 新增 order_items，is_extra=true）
  - 状态流转（状态机校验 canTransitTo）
- [ ] 8.5 用户端接口：
  - `POST /api/v1/orders`（创建订单）
  - `GET /api/v1/orders`（分页列表，支持状态筛选）
  - `GET /api/v1/orders/{id}`（详情含 items）
  - `POST /api/v1/orders/{id}/cancel`
  - `POST /api/v1/orders/{id}/items`（加菜）
- [ ] 8.6 管理端接口：
  - `GET /api/v1/admin/orders`（支持状态/日期/餐次筛选 + 分页）
  - `GET /api/v1/admin/orders/{id}`
  - `PUT /api/v1/admin/orders/{id}/status`（状态流转）
  - `GET /api/v1/admin/orders/pending-count`（轮询用）
- [ ] 8.7 下单后触发微信订阅消息推送（调用微信 subscribeMessage.send）

---

## 阶段九：文件上传模块（0.5天）

- [ ] 9.1 MinioConfig 配置类（注入 MinioClient Bean）
- [ ] 9.2 MinioService：上传文件、生成访问 URL、删除文件
- [ ] 9.3 图片压缩逻辑（Thumbnailator，超过 500KB 自动压缩）
- [ ] 9.4 管理端接口 `POST /api/v1/admin/files/upload`：
  - 校验后缀白名单（jpg/png/webp）
  - 校验文件大小 ≤ 5MB
  - 压缩 → 上传 MinIO → 返回 URL

---

## 阶段十：统计模块（0.5天）

- [ ] 10.1 StatsService：高频菜品统计 SQL、每日汇总 SQL、概览数据
- [ ] 10.2 管理端接口：
  - `GET /api/v1/admin/stats/overview`（工作台数据卡片）
  - `GET /api/v1/admin/stats/top-dishes?startDate=&endDate=`
  - `GET /api/v1/admin/stats/daily?startDate=&endDate=`

---

## 阶段十一：联调与收尾（1天）

- [ ] 11.1 Knife4j 接口文档检查，确保所有接口可正常调试
- [ ] 11.2 补充接口参数校验（@Valid + DTO 注解）
- [ ] 11.3 SQL 日志检查（开发环境打印完整 SQL）
- [ ] 11.4 Actuator 健康检查配置 `/actuator/health`
- [ ] 11.5 Logback 日志配置（按天滚动，保留30天）
- [ ] 11.6 编写 README.md（启动方式、环境变量、接口文档地址）
- [ ] 11.7 生产环境 docker-compose 部署验证

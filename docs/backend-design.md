# 陈哥厨房 后端设计文档

> 编写日期：2026-05-22  
> 版本：V1.0  
> 技术栈：Java 17 + Spring Boot 3 + PostgreSQL + MinIO  
> 设计原则：简单实用，快速交付，预留扩展

---

## 一、技术选型

| 层面 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | LTS 版本 |
| 框架 | Spring Boot | 3.2+ | 主流生态，开发效率高 |
| ORM | MyBatis-Plus | 3.5+ | 轻量灵活，适合简单业务 |
| 数据库 | PostgreSQL | 15+ | 稳定可靠，JSON 支持好 |
| 文件存储 | MinIO | 最新 | 自建对象存储，兼容 S3 协议 |
| 缓存 | 本地 Caffeine | 3.x | 用户量小，无需 Redis |
| 认证 | 微信小程序登录 + JWT | — | 轻量鉴权 |
| API 文档 | Knife4j (Swagger) | 4.x | 接口调试 |
| 构建 | Maven | 3.9+ | — |
| 部署 | Docker + docker-compose | — | 一键部署 |

---

## 二、工程结构

```
server/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/main/java/com/der/kitchen/
│   ├── DerKitchenApplication.java
│   ├── common/                     # 通用模块
│   │   ├── config/                 # 配置类
│   │   │   ├── WebConfig.java
│   │   │   ├── MinioConfig.java
│   │   │   └── CorsConfig.java
│   │   ├── exception/              # 异常处理
│   │   │   ├── BizException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── result/                 # 统一响应
│   │   │   ├── R.java
│   │   │   └── PageResult.java
│   │   ├── enums/                  # 枚举
│   │   │   ├── OrderStatus.java
│   │   │   ├── DishStatus.java
│   │   │   ├── MealType.java
│   │   │   └── UserRole.java
│   │   └── util/                   # 工具类
│   │       ├── JwtUtil.java
│   │       └── WxUtil.java
│   ├── auth/                       # 认证模块
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── filter/JwtAuthFilter.java
│   │   └── dto/WxLoginRequest.java
│   ├── user/                       # 用户模块
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── mapper/UserMapper.java
│   │   └── entity/User.java
│   ├── dish/                       # 菜品模块
│   │   ├── controller/
│   │   │   ├── DishController.java       # 用户端
│   │   │   └── AdminDishController.java  # 管理端
│   │   ├── service/DishService.java
│   │   ├── mapper/DishMapper.java
│   │   ├── entity/Dish.java
│   │   └── dto/
│   │       ├── DishCreateDTO.java
│   │       ├── DishUpdateDTO.java
│   │       └── DishVO.java
│   ├── category/                   # 分类模块
│   │   ├── controller/AdminCategoryController.java
│   │   ├── service/CategoryService.java
│   │   ├── mapper/CategoryMapper.java
│   │   ├── entity/Category.java
│   │   └── dto/CategoryDTO.java
│   ├── order/                      # 订单模块
│   │   ├── controller/
│   │   │   ├── OrderController.java       # 用户端
│   │   │   └── AdminOrderController.java  # 管理端
│   │   ├── service/OrderService.java
│   │   ├── mapper/
│   │   │   ├── OrderMapper.java
│   │   │   └── OrderItemMapper.java
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   └── OrderItem.java
│   │   └── dto/
│   │       ├── OrderCreateDTO.java
│   │       ├── OrderVO.java
│   │       └── OrderDetailVO.java
│   ├── favorite/                   # 收藏模块
│   │   ├── controller/FavoriteController.java
│   │   ├── service/FavoriteService.java
│   │   ├── mapper/FavoriteMapper.java
│   │   └── entity/Favorite.java
│   ├── stats/                      # 统计模块
│   │   ├── controller/AdminStatsController.java
│   │   ├── service/StatsService.java
│   │   └── vo/StatsVO.java
│   └── file/                       # 文件模块
│       ├── controller/FileController.java
│       └── service/MinioService.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/
│       └── migration/              # 数据库脚本
│           └── V1__init.sql
└── src/test/
```

---

## 三、数据库设计

### 3.1 ER 关系

```
users 1───N orders
users 1───N favorites
categories 1───N dishes
dishes 1───N order_items
dishes 1───N favorites
orders 1───N order_items
```

### 3.2 公共字段约定

所有业务表统一包含以下审计字段：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| create_time | TIMESTAMP | NOW() | 创建时间 |
| update_time | TIMESTAMP | NOW() | 最后更新时间（UPDATE 时自动更新） |
| create_by | VARCHAR(32) | NULL | 创建人名称 |
| update_by | VARCHAR(32) | NULL | 最后更新人名称 |
| deleted | BOOLEAN | FALSE | 软删除标记，TRUE 表示已删除 |

> `update_time` 通过 PostgreSQL 触发器自动维护，业务代码无需手动赋值。  
> `create_by` / `update_by` 由业务层从 UserContext 中取当前用户昵称写入。  
> MyBatis-Plus 全局配置 `logic-delete-field: deleted`，查询自动过滤已删除记录。

```sql
-- 自动更新 update_time 的触发器函数（全局复用）
CREATE OR REPLACE FUNCTION set_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 3.3 DDL

```sql
-- =============================================
-- 用户表
-- =============================================
CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    openid      VARCHAR(64)     NOT NULL UNIQUE,
    nickname    VARCHAR(50),
    avatar_url  VARCHAR(500),
    role        VARCHAR(10)     NOT NULL DEFAULT 'user',    -- user / admin
    status      VARCHAR(10)     NOT NULL DEFAULT 'active',  -- active / disabled
    -- 审计字段
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_openid   ON users(openid);
CREATE INDEX idx_users_deleted  ON users(deleted);

CREATE TRIGGER trg_users_update_time
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 菜品分类表
-- =============================================
CREATE TABLE categories (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(20)     NOT NULL,
    sort_order  INT             NOT NULL DEFAULT 0,
    status      VARCHAR(10)     NOT NULL DEFAULT 'active',  -- active / hidden
    -- 审计字段
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_categories_deleted ON categories(deleted);

CREATE TRIGGER trg_categories_update_time
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 菜品表
-- =============================================
CREATE TABLE dishes (
    id            BIGSERIAL       PRIMARY KEY,
    name          VARCHAR(50)     NOT NULL,
    description   VARCHAR(500),
    image_url     VARCHAR(500),
    category_id   BIGINT          NOT NULL REFERENCES categories(id),
    cooking_time  INT,                                              -- 分钟
    status        VARCHAR(15)     NOT NULL DEFAULT 'normal',        -- normal / out_of_stock / unavailable
    is_listed     BOOLEAN         NOT NULL DEFAULT TRUE,
    -- 审计字段
    create_time   TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by     VARCHAR(32),
    update_by     VARCHAR(32),
    deleted       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dishes_category ON dishes(category_id);
CREATE INDEX idx_dishes_listed   ON dishes(is_listed, deleted);
CREATE INDEX idx_dishes_deleted  ON dishes(deleted);

CREATE TRIGGER trg_dishes_update_time
    BEFORE UPDATE ON dishes
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 订单表
-- =============================================
CREATE TABLE orders (
    id                BIGSERIAL       PRIMARY KEY,
    user_id           BIGINT          NOT NULL REFERENCES users(id),
    meal_type         VARCHAR(10)     NOT NULL,    -- breakfast / lunch / dinner
    meal_date         DATE            NOT NULL,
    status            VARCHAR(15)     NOT NULL DEFAULT 'pending',
                                                   -- pending / preparing / cooking / completed / cancelled
    taste_tags        JSONB,                        -- ["微辣","少油"]
    dietary_notes     VARCHAR(500),
    special_requests  VARCHAR(500),
    -- 审计字段
    create_time       TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by         VARCHAR(32),
    update_by         VARCHAR(32),
    deleted           BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_orders_user      ON orders(user_id);
CREATE INDEX idx_orders_status    ON orders(status);
CREATE INDEX idx_orders_meal_date ON orders(meal_date);
CREATE INDEX idx_orders_deleted   ON orders(deleted);

CREATE TRIGGER trg_orders_update_time
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 订单菜品明细表
-- =============================================
CREATE TABLE order_items (
    id          BIGSERIAL       PRIMARY KEY,
    order_id    BIGINT          NOT NULL REFERENCES orders(id),
    dish_id     BIGINT          NOT NULL REFERENCES dishes(id),
    dish_name   VARCHAR(50)     NOT NULL,           -- 冗余快照，防菜品改名影响历史
    quantity    INT             NOT NULL DEFAULT 1,
    is_extra    BOOLEAN         NOT NULL DEFAULT FALSE,  -- 是否加菜
    -- 审计字段
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_order_items_order   ON order_items(order_id);
CREATE INDEX idx_order_items_deleted ON order_items(deleted);

CREATE TRIGGER trg_order_items_update_time
    BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION set_update_time();

-- =============================================
-- 收藏表
-- =============================================
CREATE TABLE favorites (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users(id),
    dish_id     BIGINT          NOT NULL REFERENCES dishes(id),
    -- 审计字段
    create_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP       NOT NULL DEFAULT NOW(),
    create_by   VARCHAR(32),
    update_by   VARCHAR(32),
    deleted     BOOLEAN         NOT NULL DEFAULT FALSE,
    -- 同一用户对同一菜品只能有一条有效收藏
    UNIQUE(user_id, dish_id)
);

CREATE INDEX idx_favorites_user    ON favorites(user_id);
CREATE INDEX idx_favorites_deleted ON favorites(deleted);

CREATE TRIGGER trg_favorites_update_time
    BEFORE UPDATE ON favorites
    FOR EACH ROW EXECUTE FUNCTION set_update_time();
```

---

## 四、接口设计

### 4.1 通用约定

- Base URL: `/api/v1`
- 认证方式：Header `Authorization: Bearer {token}`
- 响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 分页参数：`page`（从1开始）、`size`（默认20）
- 错误码：

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务异常 |

---

### 4.2 认证接口

#### POST `/api/v1/auth/wx-login`

微信小程序登录，code 换取 token。

**请求：**
```json
{
  "code": "wx_login_code",
  "nickname": "老婆",
  "avatarUrl": "https://..."
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGci...",
    "role": "user",
    "userId": 1,
    "nickname": "老婆"
  }
}
```

**逻辑：**
1. 用 code 调用微信接口换取 openid
2. 查询 users 表，存在则更新信息，不存在则检查白名单
3. 一期仅白名单 openid 可登录，非白名单返回 403
4. 签发 JWT（有效期7天）

---

### 4.3 用户端接口

#### GET `/api/v1/categories`

获取所有可见分类（status=active），按 sort_order 排序。

**响应：**
```json
{
  "data": [
    { "id": 1, "name": "家常菜" },
    { "id": 2, "name": "汤羹" }
  ]
}
```

---

#### GET `/api/v1/dishes`

获取菜品列表。

**参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Long | 否 | 分类筛选 |

**响应：**
```json
{
  "data": [
    {
      "id": 1,
      "name": "红烧肉",
      "imageUrl": "http://minio/dishes/1.jpg",
      "categoryId": 1,
      "cookingTime": 30,
      "status": "normal",
      "isFavorited": true
    }
  ]
}
```

**逻辑：** 仅返回 `is_listed=true AND is_deleted=false` 的菜品。

---

#### GET `/api/v1/dishes/{id}`

获取菜品详情。

---

#### GET `/api/v1/favorites`

获取当前用户收藏列表。

---

#### POST `/api/v1/favorites`

添加收藏。

**请求：**
```json
{ "dishId": 1 }
```

---

#### DELETE `/api/v1/favorites/{dishId}`

取消收藏。

---

#### POST `/api/v1/orders`

创建订单。

**请求：**
```json
{
  "mealType": "dinner",
  "mealDate": "2026-05-22",
  "items": [
    { "dishId": 1, "quantity": 2 },
    { "dishId": 3, "quantity": 1 }
  ],
  "tasteTags": ["微辣", "少油"],
  "dietaryNotes": "不要香菜",
  "specialRequests": "红烧肉要软烂"
}
```

**校验：**
- items 不能为空
- dishId 对应菜品必须 is_listed=true 且 status=normal
- mealDate 不能超过明天

**逻辑：**
1. 校验菜品可用性
2. 创建 order 记录
3. 批量创建 order_items
4. 触发新订单通知（推送给管理员）

---

#### GET `/api/v1/orders`

获取当前用户订单列表。

**参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | String | 否 | 状态筛选，多个逗号分隔 |
| page | Int | 否 | 默认1 |
| size | Int | 否 | 默认20 |

---

#### GET `/api/v1/orders/{id}`

获取订单详情，含菜品明细和备注。

---

#### POST `/api/v1/orders/{id}/cancel`

用户取消订单。仅 `pending` 状态可取消。

---

#### POST `/api/v1/orders/{id}/items`

追加菜品（加菜）。仅 `pending` 或 `preparing` 状态可加菜。

**请求：**
```json
{
  "items": [
    { "dishId": 5, "quantity": 1 }
  ]
}
```

**逻辑：** 新增 order_items，标记 `is_extra=true`。

---

### 4.4 管理端接口

> 所有 `/api/v1/admin/**` 接口需校验 `role=admin`。

#### CRUD `/api/v1/admin/categories`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/categories | 获取所有分类（含隐藏） |
| POST | /admin/categories | 新增分类 |
| PUT | /admin/categories/{id} | 编辑分类 |
| DELETE | /admin/categories/{id} | 删除分类（需无关联菜品） |
| PUT | /admin/categories/sort | 批量更新排序 |

---

#### CRUD `/api/v1/admin/dishes`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/dishes | 菜品列表（支持分类/状态/上架筛选） |
| POST | /admin/dishes | 新增菜品 |
| PUT | /admin/dishes/{id} | 编辑菜品 |
| DELETE | /admin/dishes/{id} | 软删除菜品 |
| PUT | /admin/dishes/{id}/status | 修改状态（normal/out_of_stock/unavailable） |
| PUT | /admin/dishes/{id}/listing | 上下架切换 |
| PUT | /admin/dishes/batch-listing | 批量上下架（快速上下架页面用） |

**批量上下架请求：**
```json
{
  "items": [
    { "dishId": 1, "isListed": true },
    { "dishId": 2, "isListed": false }
  ]
}
```

---

#### 订单管理 `/api/v1/admin/orders`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/orders | 订单列表（支持状态/日期/餐次筛选） |
| GET | /admin/orders/{id} | 订单详情 |
| PUT | /admin/orders/{id}/status | 更新订单状态 |
| GET | /admin/orders/pending-count | 获取待处理订单数量（轮询用） |

**状态更新请求：**
```json
{ "status": "preparing" }
```

**状态流转校验：**
| 当前状态 | 可转为 |
|---------|--------|
| pending | preparing / cancelled |
| preparing | cooking / cancelled |
| cooking | completed / cancelled |
| completed | — |
| cancelled | — |

---

#### 统计 `/api/v1/admin/stats`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /admin/stats/top-dishes | 高频菜品 Top10 |
| GET | /admin/stats/daily | 每日汇总 |
| GET | /admin/stats/overview | 工作台概览数据 |

**高频菜品参数：** `startDate`, `endDate`

**高频菜品响应：**
```json
{
  "data": [
    { "dishId": 1, "dishName": "红烧肉", "count": 16 },
    { "dishId": 3, "dishName": "番茄蛋汤", "count": 12 }
  ]
}
```

**工作台概览响应：**
```json
{
  "data": {
    "pendingCount": 2,
    "processingCount": 1,
    "todayCompletedCount": 3,
    "todayTotalCount": 6
  }
}
```

---

#### 文件上传 `/api/v1/admin/files`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /admin/files/upload | 上传图片，返回访问URL |

**请求：** `multipart/form-data`，字段名 `file`

**校验：** 仅允许 jpg/png/webp，大小 ≤ 5MB

**响应：**
```json
{
  "data": {
    "url": "http://minio-host:9000/dishes/2026/05/22/abc123.jpg"
  }
}
```

---

## 五、核心业务逻辑

### 5.1 认证与鉴权

```
请求 → JwtAuthFilter → 解析Token → 注入UserContext → Controller
                     ↘ 无Token/过期 → 401
                     
AdminController → @RequireRole("admin") → 校验角色 → 403
```

**JWT Payload：**
```json
{
  "userId": 1,
  "role": "admin",
  "exp": 1716422400
}
```

**白名单机制（一期）：**
- 配置文件中维护允许登录的 openid 列表
- 登录时校验，不在白名单内返回 403

```yaml
app:
  wx:
    app-id: wx_xxx
    app-secret: xxx
  auth:
    allowed-openids:
      - oXXXX_wife_openid
      - oXXXX_chenge_openid
    jwt-secret: your-secret-key
    jwt-expire-days: 7
```

---

### 5.2 订单状态机

```java
public enum OrderStatus {
    PENDING("pending"),
    PREPARING("preparing"),
    COOKING("cooking"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    // 允许的状态转移
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        PENDING, Set.of(PREPARING, CANCELLED),
        PREPARING, Set.of(COOKING, CANCELLED),
        COOKING, Set.of(COMPLETED, CANCELLED),
        COMPLETED, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
```

---

### 5.3 新订单通知

由于用户量极小（仅1人），通知机制采用简单方案：

**小程序端（管理页）：**
- 前端每 5 秒轮询 `/admin/orders/pending-count`
- 数量变化时播放提示音 + 震动

**Web 端：**
- 同样轮询（间隔 5 秒），数量变化时触发浏览器 Notification

**微信订阅消息（补充）：**
- 下单时调用微信订阅消息接口推送给管理员
- 需管理员预先订阅消息模板

> 💡 用户量只有1人，轮询完全够用，无需引入 WebSocket 增加复杂度。

---

### 5.4 文件上传流程

```
客户端 → 上传图片 → 后端接收 → 压缩(≤500KB) → 写入MinIO → 返回URL
```

**MinIO Bucket 规划：**

| Bucket | 用途 | 访问权限 |
|--------|------|---------|
| dishes | 菜品图片 | public-read |
| avatars | 用户头像 | public-read |

**文件命名：** `{bucket}/{yyyy/MM/dd}/{uuid}.{ext}`

---

## 六、配置文件

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/der_kitchen
    username: postgres
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
  servlet:
    multipart:
      max-file-size: 5MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted        # 对应所有表的 deleted 字段
      logic-delete-value: true
      logic-not-delete-value: false

minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket-name: dishes

app:
  wx:
    app-id: ${WX_APP_ID}
    app-secret: ${WX_APP_SECRET}
  auth:
    allowed-openids:
      - ${WIFE_OPENID}
      - ${ADMIN_OPENID}
    jwt-secret: ${JWT_SECRET}
    jwt-expire-days: 7
```

---

## 七、部署方案

### docker-compose.yml

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_PASSWORD=your_password
      - MINIO_ACCESS_KEY=minioadmin
      - MINIO_SECRET_KEY=minioadmin
      - WX_APP_ID=xxx
      - WX_APP_SECRET=xxx
      - JWT_SECRET=xxx
      - WIFE_OPENID=xxx
      - ADMIN_OPENID=xxx
    depends_on:
      - postgres
      - minio

  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: der_kitchen
      POSTGRES_PASSWORD: your_password
    volumes:
      - pg_data:/var/lib/postgresql/data

  minio:
    image: minio/minio
    ports:
      - "9000:9000"
      - "9001:9001"
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data

volumes:
  pg_data:
  minio_data:
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/der-kitchen-server.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 八、关键类设计

### 8.1 统一响应

```java
@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

### 8.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<?> handleBiz(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return R.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统异常，请稍后重试");
    }
}
```

### 8.3 权限注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String value();
}

@Aspect
@Component
public class RoleCheckAspect {
    @Before("@annotation(requireRole)")
    public void check(RequireRole requireRole) {
        UserContext current = UserContextHolder.get();
        if (!requireRole.value().equals(current.getRole())) {
            throw new BizException(403, "无权限访问");
        }
    }
}
```

---

## 九、安全设计

| 安全点 | 方案 |
|--------|------|
| 认证 | 微信 code 登录 + JWT |
| 鉴权 | 角色注解 + AOP 拦截 |
| 数据隔离 | 用户端接口自动带入当前 userId |
| SQL 注入 | MyBatis-Plus 参数化查询 |
| 文件上传 | 后缀白名单 + 文件大小限制 + Content-Type 校验 |
| 敏感配置 | 环境变量注入，不写入代码 |
| CORS | 仅允许小程序域名和管理端域名 |

---

## 十、监控与日志

| 内容 | 方案 |
|------|------|
| 接口日志 | AOP 统一记录请求/响应/耗时 |
| 异常日志 | GlobalExceptionHandler 记录 |
| 日志输出 | Logback，按天滚动，保留30天 |
| 健康检查 | Spring Boot Actuator `/actuator/health` |

> 用户量小，无需引入 ELK、Prometheus 等重量级方案。出问题直接查日志文件即可。

---

## 十一、开发计划对应

| 后端任务 | 预估工时 | 说明 |
|---------|---------|------|
| 工程搭建 + 基础配置 | 0.5天 | 脚手架、Docker、数据库初始化 |
| 认证模块 | 0.5天 | 微信登录、JWT、白名单 |
| 菜品模块（CRUD + 上下架） | 1天 | |
| 分类模块 | 0.5天 | |
| 订单模块（创建/查询/状态机） | 1.5天 | 核心逻辑 |
| 收藏模块 | 0.5天 | |
| 文件上传（MinIO） | 0.5天 | |
| 统计接口 | 0.5天 | |
| 联调 + Bug修复 | 1天 | |
| **合计** | **约6.5天** | |

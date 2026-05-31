# 陈哥厨房 - 微信小程序端

> 家庭私人点餐系统 · 用户端 + 管理员轻量接单  
> 版本：V1.0.0  
> 技术栈：uni-app + Vue 3 + TypeScript + Pinia + Vite

---

## 一、项目简介

「陈哥厨房」是一款极简私密的家庭点菜微信小程序。老婆线上轻松点菜，陈哥后台管理菜品、接收订单、掌控做菜进度，替代口头沟通。

**核心能力：**
- 用户端：浏览菜品 → 选菜加购 → 确认下单 → 订单跟踪
- 管理员端：接单工作台 → 订单状态流转 → 快速上下架

---

## 二、目录结构

```
mp/
├── index.html                # H5 入口
├── package.json              # 依赖管理
├── vite.config.ts            # Vite 配置
├── tsconfig.json             # TypeScript 配置
└── src/
    ├── App.vue               # 应用入口组件（静默登录）
    ├── main.ts               # 应用创建（Pinia 注入）
    ├── pages.json            # 路由 & tabBar 配置
    ├── manifest.json         # 小程序 appid、权限声明
    ├── uni.scss              # uni-app 全局变量
    ├── api/                  # API 接口层
    │   ├── auth.ts           # 登录/用户信息接口
    │   ├── category.ts       # 分类接口
    │   ├── dish.ts           # 菜品接口（用户端 + 管理端）
    │   ├── favorite.ts       # 收藏接口
    │   └── order.ts          # 订单接口（用户端 + 管理端）
    ├── components/           # 公共组件
    │   ├── CartBar.vue       # 底部购物车浮层
    │   ├── Confetti.vue      # 下单成功撒花动效
    │   ├── DishCard.vue      # 菜品卡片（双列布局）
    │   ├── Empty.vue         # 空状态占位
    │   ├── NetworkBar.vue    # 网络状态提示
    │   ├── OrderCard.vue     # 订单卡片
    │   └── Skeleton.vue      # 骨架屏
    ├── composables/          # 组合式函数
    │   ├── useAuth.ts        # 登录守卫 / 权限检查
    │   └── useLoadMore.ts    # 分页加载 + 下拉刷新
    ├── pages/                # 页面
    │   ├── home/index.vue    # 首页（菜品浏览 + 快速点菜）
    │   ├── dish/detail.vue   # 菜品详情页
    │   ├── order/
    │   │   ├── confirm.vue   # 确认下单页
    │   │   ├── list.vue      # 订单列表页
    │   │   └── detail.vue    # 订单详情页
    │   ├── mine/
    │   │   ├── index.vue     # 个人中心
    │   │   └── favorites.vue # 我的收藏
    │   └── admin/
    │       ├── workbench.vue # 接单工作台
    │       └── listing.vue   # 快速上下架
    ├── stores/               # Pinia 状态管理
    │   ├── user.ts           # 用户 token、角色、登录/登出
    │   ├── cart.ts           # 购物车（加减菜、加菜模式、复刻下单）
    │   └── app.ts            # 全局状态（用餐时段、待处理数）
    ├── styles/
    │   └── global.scss       # 全局 CSS 变量 & 重置样式
    ├── types/
    │   └── index.ts          # TypeScript 类型定义
    ├── utils/
    │   └── request.ts        # HTTP 请求封装（自动鉴权、401 重登）
    └── static/tab/           # tabBar 图标资源
```

---

## 三、页面路由

| 路径 | 页面名称 | 说明 |
|------|----------|------|
| `/pages/home/index` | 首页 | 菜品分类浏览 + 选菜，默认 Tab 页 |
| `/pages/dish/detail` | 菜品详情 | 菜品大图、简介、加购 |
| `/pages/order/confirm` | 确认订单 | 选时段、备注口味、提交 |
| `/pages/order/list` | 订单列表 | 进行中/历史，Tab 页 |
| `/pages/order/detail` | 订单详情 | 状态进度、菜品明细、操作按钮 |
| `/pages/mine/index` | 个人中心 | 头像、收藏入口，Tab 页 |
| `/pages/mine/favorites` | 我的收藏 | 收藏列表快速加购 |
| `/pages/admin/workbench` | 接单工作台 | 管理员接单/推进状态 |
| `/pages/admin/listing` | 快速上下架 | 管理员批量上下架菜品 |

**底部 TabBar（用户端）：**
| Tab | 图标 | 页面 |
|-----|------|------|
| 点菜 | home | `/pages/home/index` |
| 订单 | order | `/pages/order/list` |
| 我的 | mine | `/pages/mine/index` |

> 管理员登录后，第二个 Tab 动态切换为「接单」（跳转 workbench）。

---

## 四、核心技术方案

### 4.1 登录认证

- 采用微信 `wx.login()` 静默登录，用户无感知
- App.vue `onLaunch` 自动触发登录
- Token 存储在 `uni.Storage` + Pinia
- 请求拦截器自动带 `Authorization: Bearer <token>`
- 401 时自动尝试重新登录

### 4.2 状态管理（Pinia）

| Store | 职责 |
|-------|------|
| `user` | token、用户信息、角色判断、登录/登出、tabBar 切换 |
| `cart` | 购物车菜品列表、数量增减、加菜模式、批量添加（复刻） |
| `app` | 当前用餐时段（智能默认）、待处理订单数 |

### 4.3 请求封装

- 基础地址：`http://localhost:8080/api/v1`
- 自动附加 Token
- 统一错误处理（toast 提示）
- 401 自动重登录
- 便捷方法：`http.get / http.post / http.put / http.delete`

### 4.4 数据类型

```typescript
UserInfo { id, nickname, avatarUrl, role }
Category { id, name, sortOrder, status }
Dish { id, name, categoryId, imageUrl, description, cookingTime, status, isListed }
CartItem { dish: Dish, quantity }
Order { id, orderNo, mealType, status, flavorTags, avoidNote, specialNote, items, createTime }
OrderItem { id, dishId, dishName, dishImage, quantity, isExtra }
```

### 4.5 订单状态流转

```
pending（待接单） → preparing（备菜中） → cooking（烹饪中） → completed（已完成）
    ↓
cancelled（已取消）
```

### 4.6 设计规范

| 属性 | 值 |
|------|-----|
| 主色 | `#FF6B35` |
| 辅色 | `#2EC4B6` |
| 背景 | `#F8F8F8` |
| 卡片圆角 | `12rpx` |
| 正文字号 | `28rpx` |
| 字体 | PingFang SC / -apple-system |

---

## 五、API 接口清单

### 用户端

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/wx-login` | 微信登录 |
| GET | `/auth/me` | 获取当前用户信息 |
| GET | `/categories` | 获取菜品分类列表 |
| GET | `/dishes` | 获取菜品列表（支持 categoryId 筛选、分页） |
| GET | `/dishes/:id` | 获取菜品详情 |
| GET | `/favorites` | 获取收藏列表 |
| POST | `/favorites` | 添加收藏 |
| DELETE | `/favorites/:dishId` | 取消收藏 |
| POST | `/orders` | 创建订单 |
| GET | `/orders` | 获取订单列表（支持 status 筛选、分页） |
| GET | `/orders/:id` | 获取订单详情 |
| POST | `/orders/:id/items` | 临时加菜 |
| PUT | `/orders/:id/cancel` | 取消订单 |

### 管理端

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/dishes` | 获取全部菜品（含下架） |
| PUT | `/admin/dishes/:id/listing` | 单个菜品上下架 |
| PUT | `/admin/dishes/batch-listing` | 批量上下架 |
| GET | `/admin/orders` | 管理端订单列表 |
| PUT | `/admin/orders/:id/transit` | 推进订单状态 |
| GET | `/admin/orders/pending-count` | 获取待处理订单数 |

---

## 六、环境要求

| 工具 | 版本要求 |
|------|----------|
| Node.js | >= 18.x |
| pnpm / npm | pnpm >= 8.x 或 npm >= 9.x |
| 微信开发者工具 | 最新稳定版 |
| 后端服务 | Java Spring Boot（`server/` 目录） |

---

## 七、如何测试运行

### 7.1 安装依赖

```bash
cd mp
npm install
# 或使用 pnpm
pnpm install
```

### 7.2 H5 模式运行（浏览器快速调试）

```bash
npm run dev:h5
```

启动后浏览器访问 `http://localhost:12001`，可直接在浏览器中预览页面和交互。

> ⚠️ H5 模式下微信登录 API 不可用，需要后端提供 mock 或跳过登录。

### 7.3 微信小程序模式运行（推荐）

```bash
npm run dev:mp-weixin
```

编译产物输出到 `dist/dev/mp-weixin/` 目录，然后：

1. 打开 **微信开发者工具**
2. 选择「导入项目」
3. 项目目录选择：`mp/dist/dev/mp-weixin`
4. AppID 填写：`wx5101c76e51193c94`（或使用测试号）
5. 点击确定，即可在模拟器中预览

### 7.4 后端服务启动

小程序依赖后端 API 服务（`server/` 目录），需要先启动后端：

```bash
cd server
# 确保已安装 Java 17+ 和 Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端默认运行在 `http://localhost:8080`。

> 如果后端未启动，小程序页面可以加载但数据为空/请求报错。

### 7.5 修改 API 地址

如果后端部署地址变更，修改 `src/utils/request.ts` 中的 `BASE_URL`：

```typescript
const BASE_URL = 'http://你的服务器地址:8080/api/v1'
```

微信小程序真机调试需配置合法域名（`mp-weixin` → 开发设置 → 服务器域名），或在开发者工具中勾选「不校验合法域名」。

### 7.6 生产构建

```bash
# H5 生产包
npm run build:h5

# 微信小程序生产包
npm run build:mp-weixin
```

微信小程序生产包位于 `dist/build/mp-weixin/`，通过微信开发者工具上传发布。

### 7.7 类型检查

```bash
npm run type-check
```

---

## 八、业务流程

### 8.1 用户点菜流程

```
首页浏览菜品 → 点击 [+] 加入购物车 → 点击 CartBar「去下单」
→ 确认订单页（选时段 / 口味 / 备注） → 提交订单
→ 跳转订单详情页，实时查看状态
```

### 8.2 临时加菜流程

```
订单详情页 → 点击「临时加菜」 → 购物车进入加菜模式（关联 orderId）
→ 首页选菜 → 提交加菜 → 返回订单详情
```

### 8.3 历史订单复刻

```
订单列表/详情 → 点击「再来一单」 → 历史菜品批量加入购物车
→ 跳转确认下单页 → 修改/提交
```

### 8.4 管理员接单流程

```
接单工作台 → 查看新订单 → 接单（待接单→备菜中）
→ 开始烹饪（备菜中→烹饪中） → 完成（烹饪中→已完成）
```

---

## 九、注意事项

1. **微信 AppID**：`manifest.json` 中已配置 `wx5101c76e51193c94`，如使用自己的 AppID 需修改
2. **静默登录**：首次使用需在微信开发者工具中模拟微信环境，或使用 H5 模式配合后端 mock
3. **图片上传**：菜品图片由管理端 Web 上传，小程序端仅展示
4. **角色区分**：同一小程序内根据用户角色（`admin`/`user`）动态切换 tabBar 和可访问页面
5. **网络请求**：开发阶段需在微信开发者工具「详情 → 本地设置」勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」

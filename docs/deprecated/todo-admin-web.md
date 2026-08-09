# 管理后台前端搭建待办清单

> 技术栈：Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router  
> 定位：Web 电脑端管理后台（菜品管理、订单管理、数据统计）

---

## 阶段一：工程搭建（0.5天）

- [ ] 1.1 使用 `npm create vite@latest der-kitchen-admin -- --template vue-ts` 初始化项目
- [ ] 1.2 安装核心依赖：
  ```
  element-plus @element-plus/icons-vue
  vue-router pinia @vueuse/core
  axios
  echarts（数据统计图表）
  ```
- [ ] 1.3 安装开发依赖：
  ```
  sass unplugin-auto-import unplugin-vue-components
  eslint prettier
  ```
- [ ] 1.4 配置 vite.config.ts：
  - API 代理（/api → 后端地址）
  - Element Plus 自动导入
  - 路径别名 `@/`
- [ ] 1.5 配置 tsconfig.json 路径别名
- [ ] 1.6 创建项目目录结构：
  ```
  src/
  ├── api/          # 接口请求
  ├── assets/       # 静态资源
  ├── components/   # 公共组件
  ├── composables/  # 组合式函数
  ├── layouts/      # 布局组件
  ├── router/       # 路由配置
  ├── stores/       # Pinia 状态管理
  ├── styles/       # 全局样式
  ├── types/        # TS 类型定义
  ├── utils/        # 工具函数
  └── views/        # 页面组件
  ```

---

## 阶段二：基础框架搭建（1天）

### 布局

- [ ] 2.1 主布局 `AdminLayout.vue`：
  - 顶部导航栏（Logo + 新订单铃铛 + 用户头像下拉）
  - 左侧菜单（可折叠）
  - 右侧内容区（`<router-view>`）
- [ ] 2.2 左侧菜单组件（递归菜单项，支持折叠/展开）
- [ ] 2.3 面包屑导航组件

### 路由

- [ ] 2.4 路由配置 `router/index.ts`：
  ```
  /login            → 登录页
  /                 → AdminLayout
    /dashboard      → 工作台
    /dishes         → 菜品列表
    /dishes/create  → 新增菜品
    /dishes/:id     → 编辑菜品
    /categories     → 分类管理
    /orders         → 订单管理
    /orders/:id     → 订单详情
    /stats          → 数据统计
    /settings       → 设置（权限管理预留）
  ```
- [ ] 2.5 路由守卫（未登录跳 /login，无权限跳 403）

### 状态管理

- [ ] 2.6 `stores/user.ts`：用户信息、token、登录/登出
- [ ] 2.7 `stores/app.ts`：侧栏折叠状态、新订单计数

### 请求封装

- [ ] 2.8 `utils/request.ts`：Axios 封装
  - 请求拦截器：自动带 Authorization Header
  - 响应拦截器：统一处理 code !== 200、401 跳登录、错误 toast
- [ ] 2.9 `api/` 目录按模块拆分：auth.ts / dish.ts / category.ts / order.ts / stats.ts / file.ts

### 全局样式

- [ ] 2.10 CSS 变量定义（色彩规范：主色 #FF6B35、侧栏 #1F2937 等）
- [ ] 2.11 Element Plus 主题色覆盖
- [ ] 2.12 全局 reset + 基础排版样式

---

## 阶段三：登录页（0.5天）

- [ ] 3.1 登录页 UI（居中卡片，Logo + 账号密码表单）
  > 一期管理端使用 openid 白名单，Web 端可先用简单账号密码模式或扫码登录
- [ ] 3.2 登录逻辑：调用 `/api/v1/auth/wx-login` 或临时管理员登录接口
- [ ] 3.3 登录成功后存储 token 到 localStorage + Pinia
- [ ] 3.4 登出功能（清除 token，跳转登录页）

---

## 阶段四：工作台页面（1天）

- [ ] 4.1 统计数据卡片组件（待接单 / 进行中 / 今日完成 / 今日总单）
- [ ] 4.2 调用 `GET /api/v1/admin/stats/overview` 获取数据
- [ ] 4.3 待处理订单列表（卡片形式，显示菜品+备注摘要）
- [ ] 4.4 订单操作按钮（接单 / 开始烹饪 / 完成上菜）
- [ ] 4.5 本周热门菜品展示
- [ ] 4.6 新订单轮询（每5秒调用 pending-count，数量变化时：
  - 铃铛角标更新
  - 浏览器 Notification 弹窗
  - 播放提示音
  ）
- [ ] 4.7 轮询逻辑封装为 composable `useOrderPolling`

---

## 阶段五：菜品管理页面（1.5天）

### 菜品列表

- [ ] 5.1 菜品列表页 `views/dishes/index.vue`
- [ ] 5.2 筛选栏：分类下拉 + 状态下拉 + 上架下拉 + 搜索框
- [ ] 5.3 表格列：图片缩略图 / 菜品名称 / 分类 / 时长 / 状态 / 上架开关 / 操作
- [ ] 5.4 上架开关组件（el-switch，实时调用 listing 接口）
- [ ] 5.5 状态切换（点击弹出 el-select：正常/缺货/暂不做）
- [ ] 5.6 删除操作（二次确认 → 软删除）
- [ ] 5.7 分页组件

### 新增/编辑菜品

- [ ] 5.8 菜品表单页 `views/dishes/form.vue`（或右侧抽屉 Drawer）
- [ ] 5.9 表单字段：菜品名称 / 分类选择 / 图片上传 / 简介 / 烹饪时长 / 状态 / 上架
- [ ] 5.10 图片上传组件（调用 `/api/v1/admin/files/upload`，预览 + 删除）
- [ ] 5.11 表单校验（名称必填1-20字、分类必选、图片必传）
- [ ] 5.12 提交逻辑（新增 POST / 编辑 PUT）

---

## 阶段六：分类管理页面（0.5天）

- [ ] 6.1 分类管理页 `views/categories/index.vue`
- [ ] 6.2 可拖拽排序列表（使用 vuedraggable 或 Element Plus Sortable）
- [ ] 6.3 每行展示：排序手柄 / 分类名 / 菜品数量 / 编辑按钮 / 隐藏按钮 / 删除按钮
- [ ] 6.4 新增分类弹窗（el-dialog，输入分类名）
- [ ] 6.5 编辑分类弹窗
- [ ] 6.6 删除校验（有关联菜品时提示）
- [ ] 6.7 拖拽松手后调用批量排序接口

---

## 阶段七：订单管理页面（1天）

### 订单列表

- [ ] 7.1 订单管理页 `views/orders/index.vue`
- [ ] 7.2 筛选栏：状态下拉 + 日期范围选择 + 餐次下拉 + 查询按钮
- [ ] 7.3 表格列：订单号 / 餐次 / 菜品摘要 / 备注(查看) / 状态标签 / 操作按钮
- [ ] 7.4 操作按钮逻辑（根据当前状态展示不同按钮）
- [ ] 7.5 备注查看弹窗（口味 / 忌口 / 特殊要求）
- [ ] 7.6 分页

### 订单详情

- [ ] 7.7 订单详情页 `views/orders/detail.vue`（或弹窗）
- [ ] 7.8 展示：状态、用餐时段、下单时间、菜品明细（区分加菜）、备注

---

## 阶段八：数据统计页面（1天）

- [ ] 8.1 数据统计页 `views/stats/index.vue`
- [ ] 8.2 时间范围选择器（本周/本月/自定义日期）
- [ ] 8.3 高频菜品 Top10 横向条形图（ECharts）
- [ ] 8.4 每日点餐趋势折线图（ECharts）
- [ ] 8.5 调用 `/api/v1/admin/stats/top-dishes` 和 `/api/v1/admin/stats/daily`
- [ ] 8.6 图表响应式适配

---

## 阶段九：设置页面（0.5天）

- [ ] 9.1 权限管理页 `views/settings/index.vue`（预留）
- [ ] 9.2 当前用户展示卡片
- [ ] 9.3 "添加家庭成员"按钮（置灰 + tooltip "二期开放"）

---

## 阶段十：收尾（0.5天）

- [ ] 10.1 响应式适配：
  - ≥ 1440px 正常布局
  - 1024-1440px 侧栏收起
  - < 1024px 提示使用小程序
- [ ] 10.2 全局 loading 状态
- [ ] 10.3 404 页面
- [ ] 10.4 页面 title 动态设置
- [ ] 10.5 浏览器 Notification 权限申请
- [ ] 10.6 构建优化（分包、gzip）
- [ ] 10.7 环境变量配置（.env.development / .env.production）
- [ ] 10.8 编写 README.md（启动命令、构建部署、环境变量说明）

---

## 预估总工时：约 8 天

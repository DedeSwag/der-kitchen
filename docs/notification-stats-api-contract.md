# 通知、统计与 API 契约基线

> 生效日期：2026-08-08  
> 覆盖整改阶段：6、7，以及阶段 8 的“路径、字段、分页、错误码统一”。

## 1. 订单事件与通知闭环

订单域只产生持久化事件，不在订单事务中调用微信。`OrderEventService` 使用 `Propagation.MANDATORY`，因此订单写入、`biz_order_event` 和 `sys_notification` 必须在同一个事务内成功或回滚。

| 订单动作 | 事件类型 | 站内通知 | 微信订阅消息 |
|---|---|---|---|
| 用户创建订单 | `order_created` | `new_order` | 创建待投递记录 |
| 用户加菜 | `items_added` | `items_added` | 不发送 |
| 用户取消订单 | `user_cancelled` | `order_cancelled` | 不发送 |
| 管理员流转状态 | `status_changed` | 不发送 | 不发送 |

`biz_order_event` 保存操作者、前后状态、菜品数量和说明；`sys_notification` 同时承载站内通知与微信投递记录，并通过 `(event_id, recipient_id, channel)` 唯一约束防止重复创建。

站内通知直接标记为 `delivered`。管理端和管理员小程序每 3 秒使用 `afterId` 增量轮询；收到创建、加菜或取消通知后刷新订单。待接单角标仍调用 `/api/v1/admin/orders/pending-count`，其唯一口径是未删除且 `status = pending` 的订单数。

微信消息使用以下状态机：

```text
pending/failed -> processing -> sent
                        \-> failed -> processing
                        \-> dead（达到最大尝试次数）
```

后台任务用 `FOR UPDATE SKIP LOCKED` 原子领取记录，使用数据库 `clock_timestamp()` 判断到期时间并恢复超时的 `processing` 记录。失败采用指数退避，最长 1 小时；投递状态并发变化时拒绝覆盖。管理员需在小程序工作台点击“开启一次提醒”调用微信订阅授权，微信的一次性订阅额度由微信平台管理。

相关配置：

| 环境变量 | 默认值 | 说明 |
|---|---:|---|
| `WX_SUBSCRIBE_ENABLED` | `false` | 是否启用外部微信投递 |
| `WX_SUBSCRIBE_TEMPLATE_ID` | 空 | 微信订阅消息模板 ID |
| `WX_SUBSCRIBE_PAGE` | `pages/order/detail?id={orderId}` | 消息跳转页 |
| `WX_SUBSCRIBE_MEAL_KEY` | `thing1` | 餐次模板字段 |
| `WX_SUBSCRIBE_SUMMARY_KEY` | `thing2` | 摘要模板字段 |
| `WX_SUBSCRIBE_TIME_KEY` | `time3` | 时间模板字段 |
| `WX_SUBSCRIBE_MAX_ATTEMPTS` | `5` | 最大投递次数 |
| `WX_SUBSCRIBE_RETRY_BASE_SECONDS` | `30` | 退避基数 |
| `WX_SUBSCRIBE_PROCESSING_TIMEOUT_SECONDS` | `300` | 领取超时恢复时间 |
| `WX_SUBSCRIBE_WORKER_DELAY_MILLIS` | `3000` | 任务轮询间隔 |

## 2. 统计唯一口径

统计只查询最终表 `biz_order`、`biz_order_item`、`dish_info`，业务日期统一为 `Asia/Shanghai`，日期区间的起止日均包含，单次查询最多 366 个自然日。

- 所有订单数、菜品数和排行都排除 `cancelled` 订单及逻辑删除记录。
- `todayOrders`：餐期为上海当日的非取消订单数。
- `pendingOrders`：所有日期中状态严格为 `pending` 的订单数，与待接单接口一致。
- `totalDishes`：当前未删除且已上架菜品数。
- `weekOrders`：上海当周周一至周日的非取消订单数。
- 热门菜品按 `dish_id` 聚合 `quantity`，菜名取该菜品最新一条非取消订单快照，避免同名菜或改名后拆分统计。
- 每日汇总使用日期序列补齐无订单日期；`orderCount` 统计订单，`dishCount` 汇总明细数量。
- 当前需求和表结构没有价格模型，因此不伪造金额统计；以后引入价格时必须同时增加订单金额与明细单价快照。

## 3. 通用响应与错误码

所有 JSON API 都使用统一响应壳：

```json
{ "code": 200, "message": "success", "data": {} }
```

失败时 HTTP 状态和响应体 `code` 必须相等，前端保留真实 `code` 与 HTTP `status`，不再把所有异常折叠成普通 `Error`。

| HTTP / `code` | 语义 |
|---:|---|
| 400 | 参数缺失、格式或业务输入不合法 |
| 401 | 未登录、登录失效 |
| 403 | 角色无权访问或账号被禁用 |
| 404 | 资源不存在或对当前用户不可见 |
| 405 | 请求方法不支持 |
| 409 | 状态迁移、并发更新、唯一或关联约束冲突 |
| 413 | 上传内容超过限制 |
| 415 | 请求内容类型不支持 |
| 502 / 503 | 外部服务失败或暂不可用 |
| 500 | 未处理的服务端异常 |

## 4. 登录、分页和字段

登录与 `/auth/me` 都返回扁平用户结构，不使用嵌套 `user`：

```json
{
  "token": "仅登录响应返回",
  "userId": 1,
  "nickname": "陈哥",
  "avatarUrl": null,
  "role": "admin"
}
```

所有分页请求统一使用 `pageNum`、`pageSize`，默认 `1`、`20`，`pageSize` 范围为 1～100。分页响应固定为：

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20
}
```

对外返回 VO，不返回实体审计字段和逻辑删除字段。菜品写入字段使用 `imageFileId`；`imageUrl` 和 `thumbnailUrl` 只作为可空的短期读取字段。订单统一使用 `mealDate`、`mealType`、`dietaryNotes`、`specialRequests`，订单明细返回菜名快照且不暴露冗余 `orderId`。

## 5. 最终路径

前端基础地址可以不同，但拼接后的最终路径必须与下表一致。小程序默认基础地址为 `http://localhost:8080/api/v1`，可通过 `VITE_API_BASE_URL` 覆盖；Web 使用完整 `/api/v1/...` 路径。

| 域 | 路径 |
|---|---|
| 登录 | `/api/v1/auth/wx-login`、`/api/v1/auth/admin-login`、`/api/v1/auth/me`、`/api/v1/auth/logout`、`/api/v1/auth/change-password` |
| 用户分类/菜品 | `/api/v1/categories`、`/api/v1/dishes`、`/api/v1/dishes/{id}` |
| 收藏 | `/api/v1/favorites`、`/api/v1/favorites/{dishId}` |
| 用户订单 | `/api/v1/orders`、`/api/v1/orders/{id}`、`/api/v1/orders/{id}/items`、`/api/v1/orders/{id}/cancel` |
| 管理分类 | `/api/v1/admin/categories...` |
| 管理菜品 | `/api/v1/admin/dishes...` |
| 管理订单 | `/api/v1/admin/orders...`、`/api/v1/admin/orders/pending-count` |
| 管理通知 | `/api/v1/admin/notifications`、`/api/v1/admin/notifications/unread-count`、`/api/v1/admin/notifications/subscription-config`、`/api/v1/admin/notifications/{id}/read`、`/api/v1/admin/notifications/read-all` |
| 管理统计 | `/api/v1/admin/stats/overview`、`/api/v1/admin/stats/top-dishes`、`/api/v1/admin/stats/daily` |
| 文件 | `/api/common/file/upload`、`/api/common/file/{fileId}` |

不再保留重复的 `/api/v1/user/info`；当前用户统一从 `/api/v1/auth/me` 获取。

## 6. 自动化验收

真实 PostgreSQL/Testcontainers 数据集覆盖：事件与通知同事务、新增/加菜/取消触发、通知读取、微信领取与失败退避、取消订单过滤、菜品 ID 聚合、空日期补齐、扁平登录、分页字段、VO 脱敏以及 400/404 错误码一致性。

# 订单核心闭环

本文档记录第 5 阶段完成后的订单唯一业务口径。订单提醒、事件表和微信异步投递属于第 6 阶段，不在本阶段事务中直接调用外部服务。

## 1. 请求与响应契约

- 创建订单提交 `mealType`、`mealDate`、`items`、`tasteTags`、`dietaryNotes`、`specialRequests`。
- `mealType` 只保存 `breakfast/lunch/dinner`，日期单独保存到 `mealDate`；`today_lunch` 等仅是小程序界面选项，不进入后端和数据库。
- 预约日期只能是今天或明天；单次创建/加菜最多 50 个不同菜品，每个菜品数量为 1～20。同一次请求出现重复 `dishId` 时返回 400，由调用端合并数量。
- 创建响应、用户订单列表、管理端订单列表和详情统一返回订单 VO。列表包含菜品快照明细，管理端同时获得下单人昵称，避免再按订单逐条请求详情。
- 列表 `status` 支持单个状态、逗号分隔状态，以及用户端快捷口径 `ongoing`（pending/preparing/cooking）和 `history`（completed/cancelled）。

## 2. 创建与菜品快照

创建订单时先完成日期、重复菜品和所有菜品可点性校验，全部通过后才写入订单主表和明细表。任一菜品不存在、已下架、分类隐藏、缺货或暂不做时，主单与明细均不落库。

`biz_order_item` 保存下单时的 `dish_id`、`dish_name`、`quantity` 和 `is_extra`。历史订单展示 `dish_name` 快照，菜品后续改名、下架或软删除不会改变历史含义。当前需求没有售价和订单金额模型，因此加菜只原子追加明细，不虚构金额字段；如后续引入价格，应同时增加单价/金额快照并纳入同一事务。

## 3. 唯一状态机

允许的状态迁移只由 `OrderStatus` 定义：

| 当前状态 | 可迁移到 |
|---|---|
| pending | preparing / cancelled |
| preparing | cooking / cancelled |
| cooking | completed / cancelled |
| completed | 无 |
| cancelled | 无 |

- 管理员状态接口和用户取消接口最终都调用同一个迁移方法。
- 用户只能取消本人且仍为 `pending` 的订单；管理员可按状态机取消任意未完成订单。
- 用户只能给本人且处于 `pending/preparing` 的订单加菜。
- 不合法或已被并发请求抢先变更的状态操作返回 HTTP 409。

## 4. 事务、归属与并发

- 创建订单、用户取消、加菜、管理员状态迁移均在事务内执行。
- 详情、取消和加菜都检查订单归属；其他用户访问返回 403，订单不存在返回 404。
- 取消、加菜和管理员流转先对 `biz_order` 主记录执行 `SELECT ... FOR UPDATE`。同一订单的写操作串行执行，因此不会发生“管理员完成订单的同时用户又成功加菜”或两个状态更新互相覆盖。
- `biz_order_item` 在数据库层限制数量 1～20、快照菜名非空；`taste_tags` 必须是 JSON 数组且最多 10 项。

## 5. 前端闭环

- 小程序确认页将“今天/明天 + 餐次”转换为 `mealDate + mealType`，口味和备注字段与后端统一。
- 用户取消使用 `POST /orders/{id}/cancel`；管理端状态流转使用 `PUT /admin/orders/{id}/status` 并直接提交目标状态。
- 小程序订单列表、详情、轻量接单页以及 Web 订单列表/详情均覆盖 `pending → preparing → cooking → completed`，管理端可以取消未完成订单。
- 小程序对 409 等非 2xx 响应读取后端业务消息，不再只显示笼统的 HTTP 状态码。

## 6. 验证基线

`DatabaseBaselineIntegrationTest` 使用真实 PostgreSQL 15 和 MinIO，覆盖：

- 创建、列表菜品摘要、详情、加菜与完整状态迁移；
- 用户取消与加菜状态边界、管理员非法跳转；
- 菜品改名后的历史名称快照；
- 不存在菜品、重复菜品、超期预约在主单落库前失败；
- 两个并发状态请求只有一个成功，最终状态不被覆盖。

小程序执行 `npm run type-check`，Web 管理端执行 `npm run build`。

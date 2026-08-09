# 后端模块边界与通用基础设施基线

> 更新日期：2026-08-08

## 1. 模块依赖

依赖方向固定为 `app → common + file`、`file → common`。`common` 和 `file` 为底层库，其 Spring/MyBatis 编译依赖标记为 optional；`app` 显式声明 Web、Validation、AOP、MyBatis-Plus、Hutool 等运行时能力。`common` 不引用业务或文件模块，`file` 不引用 app 业务代码。

## 2. HTTP 与错误契约

成功响应仍为 `R<T>`，业务码为 200。错误响应同时使用真实 HTTP 状态和相同的响应体 code：

| HTTP/code | 场景 |
|---|---|
| 400 | 参数校验、格式或普通业务规则错误 |
| 401 | 未登录、token 过期或被踢下线 |
| 403 | 角色不足、账号禁用或资源越权 |
| 404 | 路径或资源不存在 |
| 405 | 请求方法不支持 |
| 409 | 唯一键、外键等数据库约束冲突 |
| 413 | 上传请求超过限制 |
| 415 | Content-Type 不支持 |
| 502/503 | 微信、MinIO 等外部服务异常或未配置 |
| 500 | 未处理的系统异常；响应不暴露堆栈和内部消息 |

控制器不再接收弱类型 `Map` 请求。ID、分页、状态枚举、文本长度、日期与嵌套订单明细均在入口校验，数据库约束作为最后防线。

## 3. 安全与暴露面

- CORS 仅匹配 `/api/**`，启用凭据时只接受 `CORS_ALLOWED_ORIGINS` 中的明确来源，禁止 `*`。
- OpenAPI 默认声明 Bearer/Sa-Token；两个登录接口明确标记为无需安全方案。
- 生产环境关闭 Springdoc/Knife4j，只暴露 Actuator `health` 且不显示详情。
- `User.password` 和 `User.openid` 使用序列化忽略作兜底，业务接口优先返回专用 DTO/VO。
- 微信响应、OpenID、session_key、密码和 MinIO 内部错误不会写入客户端响应或接口日志。

## 4. 配置和日志

基础配置提供本地开发默认值；生产 profile 的数据库、MinIO、微信、账号初始化及 CORS 来源必须由环境变量提供。开发环境可输出 SQL 和健康详情，生产环境关闭 SQL 输出并使用日志滚动策略。

接口日志只记录 HTTP 方法、路径、控制器方法、耗时及异常类型，不记录请求体、token 或异常消息。外部服务在服务端保留必要的结构化诊断信息，对客户端返回稳定的通用错误。

## 5. 测试门禁

`DatabaseBaselineIntegrationTest` 在真实 PostgreSQL 15 容器中启动完整 Spring 上下文，同时验证：

- 空库 Flyway 基线与核心 Mapper 写入；
- 未登录 401、普通用户访问管理端 403、禁用账号 403；
- 退出登录与修改密码后旧 token 失效；
- 用户不能读取其他用户订单，管理员仍可读取；
- 参数错误、未知资源和错误方法分别返回 400/404/405。

若本机没有 Docker，Testcontainers 用例会明确标记为 skipped；交付验证必须在 Docker 可用环境执行，不能以跳过结果代替通过。

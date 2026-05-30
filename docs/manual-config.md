# 小程序 - 需要手动配置的内容

> 以下内容需要你本人操作完成，代码层面已预留好对应位置。

---

## 1. 微信 AppID

**文件位置：** `mp/src/manifest.json`

将 `"appid": "wx_your_appid_here"` 替换为你的微信小程序实际 AppID。

> 获取方式：[微信公众平台](https://mp.weixin.qq.com/) → 开发管理 → 开发设置 → AppID

---

## 2. Tab 栏图标（共8个）

**文件位置：** `mp/src/static/tab/`

需要替换以下空文件为实际的 PNG 图标：

| 文件名 | 用途 | 规格 |
|--------|------|------|
| home.png | 点菜Tab（未选中） | 81×81 px, 透明背景 |
| home-active.png | 点菜Tab（选中） | 81×81 px, 主色#FF6B35 |
| order.png | 订单Tab（未选中） | 81×81 px, 透明背景 |
| order-active.png | 订单Tab（选中） | 81×81 px, 主色#FF6B35 |
| mine.png | 我的Tab（未选中） | 81×81 px, 透明背景 |
| mine-active.png | 我的Tab（选中） | 81×81 px, 主色#FF6B35 |
| admin.png | 接单Tab（未选中，管理员专用） | 81×81 px, 透明背景 |
| admin-active.png | 接单Tab（选中，管理员专用） | 81×81 px, 主色#FF6B35 |

**推荐图标风格：**
- home: 锅铲/餐具
- order: 订单/清单
- mine: 人物
- admin: 厨师帽

> 推荐工具：[iconfont](https://www.iconfont.cn/) 下载 PNG 图标，或用 Figma 导出。

---

## 3. 后端接口地址

**文件位置：** `mp/src/utils/request.ts`

当前设置为本地开发地址：
```ts
const BASE_URL = 'http://localhost:8080/api/v1'
```

部署时替换为实际服务器域名（需在微信后台配置合法域名）：
```ts
const BASE_URL = 'https://your-domain.com/api/v1'
```

---

## 4. 后端微信配置

**文件位置：** `server/src/main/resources/application.yml`

确保以下配置与你的微信小程序对应：
```yaml
app:
  wx:
    app-id: 你的AppID
    app-secret: 你的AppSecret
```

---

## 5. 默认头像图片（可选）

**文件位置：** `mp/src/static/default-avatar.png`

放一张默认头像图片，用于用户未登录或未设置头像时显示。

---

## 6. 微信开发者工具配置

开发时在微信开发者工具中导入项目：
- 项目目录选择：`mp/dist/dev/mp-weixin`（开发模式）或 `mp/dist/build/mp-weixin`（生产模式）
- AppID 填写实际值
- 勾选"不校验合法域名"（开发阶段）

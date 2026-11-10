# CampusMart 接口文档

| 项目 | 内容 |
|---|---|
| 项目名称 | CampusMart 校园自营商城 |
| 文档版本 | V1.0 |
| 适用后端版本 | Spring Boot 4.0.8 |
| 编写日期 | 2026-10-21 |
| 覆盖范围 | 用户模块（注册、登录） |

---

## 一、基础约定

### 1.1 服务地址

| 环境 | 地址 |
|---|---|
| 本地开发 | `http://localhost:8080` |
| 接口前缀 | `/api` |

### 1.2 请求格式

- 请求体统一使用 `application/json`
- 字符编码统一 `UTF-8`
- 时间格式统一 `yyyy-MM-dd HH:mm:ss`
- 金额单位为元，保留两位小数

### 1.3 统一响应结构

所有接口返回以下结构，不会出现其他格式：

```json
{
  "code": 0,
  "msg": "ok",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | `0` 表示成功，非 `0` 表示失败 |
| msg | string | 提示信息，失败时可直接展示给用户 |
| data | any | 业务数据，失败时为 `null` |

**重要**：业务失败时 HTTP 状态码仍为 `200`，请通过 `code` 判断结果，不要依赖 HTTP 状态码。

### 1.4 鉴权方式

需要登录的接口，在请求头携带 token：

```
Authorization: Bearer <token>
```

- token 由登录接口下发，有效期 7 天
- token 放在响应体中，由前端存储（推荐 `localStorage`）
- 不使用 Cookie，避免跨域携带问题

**无需鉴权的接口**（白名单）：

| 接口 | 说明 |
|---|---|
| `POST /api/user/register` | 注册 |
| `POST /api/user/login` | 登录 |
| `GET /api/ping` | 健康检查 |

其余接口均需携带 token。未携带、过期或非法时返回 `401`。

### 1.5 错误码规范

| 区间 | 含义 |
|---|---|
| `0` | 成功 |
| `400` | 参数校验失败 |
| `401` | 未登录或 token 失效 |
| `403` | 无权限（访问他人资源） |
| `404` | 资源不存在 |
| `409` | 重复操作或唯一约束冲突 |
| `429` | 请求过于频繁 |
| `500` | 系统异常 |
| `1xxxx` | 用户业务错误 |
| `2xxxx` | 商品业务错误 |
| `3xxxx` | 购物车业务错误 |
| `4xxxx` | 订单业务错误 |
| `5xxxx` | 支付业务错误 |
| `6xxxx` | 优惠券业务错误 |

用户模块完整错误码：

| code | 消息 | 触发场景 |
|---|---|---|
| 400 | 手机号格式不正确 / 密码长度需在 6-20 位 | 参数校验失败 |
| 10001 | 该手机号已注册 | 注册时手机号重复 |
| 10002 | 手机号或密码错误 | 登录失败（不区分具体原因） |
| 10003 | 用户不存在 | token 中的 userId 在库中已失效 |
| 10004 | 账号已被禁用 | 用户 status = 0 |

---

## 二、用户模块

### 2.1 用户注册

**基本信息**

| 项 | 内容 |
|---|---|
| 接口路径 | `POST /api/user/register` |
| 是否需要登录 | 否 |
| Content-Type | `application/json` |

**请求参数**

| 字段 | 类型 | 必填 | 规则 | 说明 |
|---|---|---|---|---|
| phone | string | 是 | 11 位，`^1[3-9]\d{9}$` | 登录账号，全库唯一 |
| password | string | 是 | 长度 6-20 | 密码明文传输（HTTPS 保障），服务端加密存储 |
| nickname | string | 否 | 最长 50 字符 | 不传时默认取手机号后 4 位 |

**请求示例**

```json
{
  "phone": "13800138001",
  "password": "123456",
  "nickname": "小明"
}
```

**成功响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": 2
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data | long | 新用户的用户 id |

**失败响应**

```json
{ "code": 10001, "msg": "该手机号已注册", "data": null }
```

```json
{ "code": 400, "msg": "手机号格式不正确", "data": null }
```

**业务说明**

1. 手机号唯一性由数据库唯一索引 `uk_phone` 保证，并发注册时由索引兜底。
2. **注册成功不返回 token**，用户需再调用登录接口获取凭证。注册与登录职责分离，便于后续为注册单独增加短信验证等逻辑。
3. 密码在服务端使用 BCrypt 加密后存储，数据库中不出现明文。

---

### 2.2 用户登录

**基本信息**

| 项 | 内容 |
|---|---|
| 接口路径 | `POST /api/user/login` |
| 是否需要登录 | 否 |
| Content-Type | `application/json` |

**请求参数**

| 字段 | 类型 | 必填 | 规则 |
|---|---|---|---|
| phone | string | 是 | 11 位 |
| password | string | 是 | 6-20 位 |

**请求示例**

```json
{
  "phone": "13800138001",
  "password": "123456"
}
```

**成功响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwicm9sZSI6IlVTRVIiLCJleHAiOjE3NjE1NTAwMDB9.xxxxx",
    "userId": 2,
    "nickname": "小明"
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| data.token | string | JWT，有效期 7 天 |
| data.userId | long | 用户 id |
| data.nickname | string | 昵称，用于前端右上角展示 |

**失败响应**

```json
{ "code": 10002, "msg": "手机号或密码错误", "data": null }
```

```json
{ "code": 10004, "msg": "账号已被禁用", "data": null }
```

**业务说明**

1. **手机号不存在与密码错误返回同一个错误码 `10002`、同一条提示**。这是有意的安全设计：若两者提示不同，攻击者可利用返回码差异批量探测哪些手机号已注册。
2. token 载荷包含 `sub`（用户 id）、`role`（USER）、`exp`（过期时间）。
3. 登录成功后前端应保存 `token` 与 `userId`，并在后续请求中自动携带。

**JWT 载荷示例（解码后）**

```json
{
  "sub": "2",
  "role": "USER",
  "iat": 1760947200,
  "exp": 1761552000
}
```

---

### 2.3 获取当前用户信息（V1.1 预留）

> 本版本暂不实现，接口规格先行约定，供后续开发参考。

| 项 | 内容 |
|---|---|
| 接口路径 | `GET /api/user/info` |
| 是否需要登录 | 是 |
| 请求参数 | 无（userId 从 token 解析） |

**成功响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "id": 2,
    "phone": "13800138001",
    "nickname": "小明",
    "avatar": null,
    "createdAt": "2026-10-21 10:00:00"
  }
}
```

**注意**：响应中**绝不包含 password 字段**，返回对象为 `UserVO` 而非实体 `User`。

---

## 三、客户端联调说明

### 3.1 推荐调用流程

```
用户填写注册表单
        │
        ▼
POST /api/user/register
        │
        ├─ code=0 ──► 注册成功，跳到登录页（或自动调用登录）
        │
        └─ code=10001 ──► 提示"该手机号已注册"
        │
        ▼
POST /api/user/login
        │
        ├─ code=0 ──► 保存 token 与 userId 到 localStorage，跳首页
        │
        └─ code=10002 ──► 提示"手机号或密码错误"
```

### 3.2 前端 token 处理约定

```js
// 请求拦截：自动带上 token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：统一处理业务码
axios.interceptors.response.use(res => {
  const { code, msg, data } = res.data
  if (code === 0) return data
  if (code === 401) {
    localStorage.removeItem('token')
    location.href = '/login'
  }
  return Promise.reject(new Error(msg))
})
```

### 3.3 自测清单

注册接口：

- [ ] 正常注册 → `code=0`，数据库中新增一条记录
- [ ] 重复手机号 → `code=10001`
- [ ] 手机号 10 位 → `code=400`，提示手机号格式不正确
- [ ] 密码 3 位 → `code=400`，提示密码长度
- [ ] 不传 nickname → 注册成功，昵称为手机号后 4 位

登录接口：

- [ ] 密码正确 → 返回 token 与用户信息
- [ ] 密码错误 → `code=10002`
- [ ] 手机号不存在 → `code=10002`（与上一条完全相同）
- [ ] 使用返回的 token 访问受保护接口 → 正常返回
- [ ] 不带 token 访问受保护接口 → `code=401`
- [ ] 篡改 token 后访问 → `code=401`

---

## 四、变更记录

| 版本 | 日期 | 变更内容 |
|---|---|---|
| V1.0 | 2026-10-21 | 初版，包含注册与登录接口，预留获取用户信息接口 |

---

## 五、后续规划

| 版本 | 计划内容 |
|---|---|
| V1.1 | 获取/修改用户信息、头像上传、收货地址 CRUD |
| V1.2 | 商品分类树、商品列表、商品详情 |
| V1.3 | 购物车、结算预览、下单 |
| V2.0 | 支付、订单超时取消、优惠券 |

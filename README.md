# CampusMart 校园商城

一个前后端分离的校园电商系统，覆盖「浏览商品 → 加购 → 下单 → 支付 → 取消/超时关闭」全流程，
重点在**库存并发安全**、**订单状态机**和**缓存设计**上的工程实践。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 4.0.8 / JDK 17 / MyBatis-Plus 3.5.17 |
| 存储 | MySQL 8（InnoDB）/ Redis 7 |
| 认证 | JWT（jjwt 0.12.7）+ 拦截器 + ThreadLocal |
| 前端 | Vue 3 / Vite / Element Plus / Vue Router / Axios |
| 部署 | Docker Compose（MySQL + Redis + 后端 + Nginx 前端） |

---

## 功能

| 模块 | 能力 |
|---|---|
| 用户 | 注册、登录、JWT 鉴权、个人信息 |
| 分类 | 树形分类，两级联动筛选商品 |
| 商品 | SPU / SKU 两级结构、列表（分类 + 排序 + 分页 + 关键词搜索）、详情 |
| 购物车 | 加购合并、改数量、删除、失效商品标记、金额汇总 |
| 订单 | 立即购买、购物车下单、模拟支付、取消、超时自动关闭、列表、详情 |
| 库存 | 条件扣减防超卖、变动流水、幂等归还 |
| 缓存 | 商品详情 Redis 缓存，防穿透 / 击穿 / 雪崩 |

---

## 核心设计

### 1. 库存并发安全

**问题**：高并发下单时，`select` 查库存再 `update` 扣减，两步之间有窗口，会超卖。

**方案**：把判断压进 SQL，让数据库在行锁内完成「判断 + 修改」。

```sql
UPDATE t_sku SET stock = stock - #{num}
WHERE id = #{skuId} AND stock >= #{num}
```

返回受影响行数 `rows`：`1` 表示扣减成功，`0` 表示库存不足。应用层只判断 `rows`，不做任何「先查后改」。

> 实测：20 线程并发下单同一 SKU，成功 1 笔、失败 19 笔，库存精确扣减，无超卖。

### 2. 库存流水表（幂等归还）

`t_sku.stock` 只是一个数字，看不出它是怎么变来的。增加 `t_stock_log` 记录每一次变动：

```sql
UNIQUE KEY uk_biz (biz_no, biz_type, sku_id)
```

同一订单、同一业务类型、同一 SKU 只允许一条记录。归还库存时先插流水，**唯一键冲突就说明已经还过了**，直接跳过，天然幂等。

> 实测：20 线程并发取消同一订单，该单流水恒为 2 条（1 扣 + 1 还），库存只归还一次。

### 3. 订单状态机

```
        支付
  10 ──────────→ 20（已支付）
  │
  ├── 用户取消 ──→ 50（已取消）
  └── 超时关闭 ──→ 60（已关闭）
```

所有状态流转都用条件更新：`UPDATE ... WHERE id = ? AND status = 10`。并发下只有一个请求能改成功，其余拿到 `rows = 0`，据此判断「状态已变更」。

### 4. 超时关单

`@Scheduled` 每分钟扫描一批过期订单（`LIMIT 200`），逐条条件更新后再归还库存：

- **先查后改**：批量操作拿不到 id 列表，必须先 `SELECT id` 才能查明细还库存
- **单条保底**：每条仍带 `WHERE status = 10`，防止查出来之后用户刚好支付
- **幂等**：`rows = 0` 直接跳过，不还库存

### 5. Redis 缓存（Cache Aside）

商品详情是典型的高频读、低频写场景，接入缓存并处理三个经典问题：

| 问题 | 场景 | 方案 |
|---|---|---|
| 缓存穿透 | 反复请求不存在的 id | 缓存空值（TTL 2 分钟） |
| 缓存击穿 | 热点 key 过期瞬间被打爆 | `SETNX` 互斥锁 + 双重检查 |
| 缓存雪崩 | 大量 key 同时过期 | TTL 加 0~300 秒随机偏移 |

> 实测：并发 50 压测 10 秒、11275 次请求，**数据库 SELECT 次数为 0**；单请求响应从 10.8ms 降至 3.4ms。

### 6. 列表接口避免 N+1

订单列表要显示每单的首件商品图。逐条订单查明细是 `1 + N` 次数据库往返，改为：

1. 分页查主表 → 得到本页 10 个 `orderId`
2. `IN (...)` 一次查回所有明细（**共 2 次查询**）
3. `Collectors.groupingBy` 在内存里按订单分组

---

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
git clone <repo-url>
cd MyselfCampusMart
docker compose up -d --build
```

首次启动会自动执行 `sql/init.sql` 建表并写入基础数据（分类 / 商品 / 测试账号）。

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost:8081 |
| 后端 API | http://localhost:8082/api |
| MySQL | localhost:3307（root / 123456） |
| Redis | localhost:6380（db 1） |

> 端口特意避开了常用的 3306 / 6379 / 8080，不会和本机已装的服务冲突。

**测试账号**：`13800138005` / `123456`

### 方式二：本地开发

**后端**

```bash
cd CampusMart
# 确保本地 MySQL(3306) 和 Redis(6379) 已启动
# 首次需要执行 sql/init.sql
mvn spring-boot:run
```

**前端**

```bash
cd campusmart-web
npm install
npm run dev        # http://localhost:5173，已配置 /api 代理到 8080
```

---

## 目录结构

```
MyselfCampusMart/
├── CampusMart/                 后端
│   ├── sql/init.sql            初始化脚本（建表 + 基础数据）
│   ├── src/main/java/com/campusmart/
│   │   ├── common/             Result、错误码、常量、上下文
│   │   ├── config/             MyBatis-Plus、拦截器注册
│   │   ├── controller/         REST 接口
│   │   ├── handler/            全局异常处理
│   │   ├── interceptor/        JWT 拦截器
│   │   ├── mapper/             数据访问
│   │   ├── pojo/               entity / dto / vo
│   │   ├── service/            业务逻辑
│   │   └── task/               定时任务
│   └── Dockerfile
├── campusmart-web/             前端
│   ├── src/api/                接口封装
│   ├── src/views/              页面
│   ├── nginx.conf              生产环境 nginx 配置
│   └── Dockerfile
└── docker-compose.yml
```

---

## 接口概览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/user/register` | 注册 |
| POST | `/api/user/login` | 登录 |
| GET | `/api/user/info` | 当前用户 |
| GET | `/api/category/tree` | 分类树 |
| GET | `/api/product/list` | 商品列表（page/size/categoryId/sort/keyword） |
| GET | `/api/product/{spuId}` | 商品详情 |
| POST | `/api/cart` | 加入购物车 |
| GET | `/api/cart` | 购物车列表 |
| PUT | `/api/cart/{id}` | 修改数量 |
| DELETE | `/api/cart/{id}` | 删除 |
| POST | `/api/order` | 立即购买 |
| POST | `/api/order/from-cart` | 购物车下单 |
| POST | `/api/order/{id}/pay` | 模拟支付 |
| POST | `/api/order/{id}/cancel` | 取消订单 |
| GET | `/api/order/list` | 订单列表 |
| GET | `/api/order/{id}` | 订单详情 |

统一响应格式：

```json
{ "code": 0, "data": {}, "msg": "ok" }
```

`code = 0` 成功；`401` 未登录；其余为业务错误码（如 `30002` 库存不足）。

---

## 压测数据

环境：本机 Windows，MySQL 与 Redis 同机，数据量较小（5 个 SPU / 12 个 SKU）。

| 场景 | 结果 |
|---|---|
| 20 线程并发下单同一 SKU | 成功 1 / 失败 19，无超卖 |
| 20 线程并发取消同一订单 | 成功 1 / 失败 19，库存仅归还 1 次 |
| 商品详情缓存命中（并发 50 × 10s） | 11275 请求，数据库 SELECT **0 次** |
| 商品详情缓存命中 vs 查库（串行单请求） | 3.4ms vs 10.8ms |

> 说明：本地 MySQL 就在同机、数据量小，QPS 差异会被压测客户端本身限制，因此更准确的衡量指标是**数据库查询次数**。

---

## 待办

- [ ] 收货地址簿
- [ ] 优惠券
- [ ] 接入真实支付（支付宝沙箱）
- [ ] RabbitMQ 延迟消息替代定时扫描关单
- [ ] 接口限流
- [ ] 后台管理端

---

## License

仅用于学习与求职展示，非真实交易系统。

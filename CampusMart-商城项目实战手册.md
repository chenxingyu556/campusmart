# CampusMart · 校园自营商城实战手册

> 面向已学完 JavaSE / JavaWeb / Redis / Linux / 数据结构，正在补 Spring Boot 的学习者。
> 目标：做一个**业务复杂度高于苍穹外卖**的电商项目，可部署、可讲透、简历不撞车。

---

## 一、项目定位

**校园优选 CampusMart** —— 面向校内学生的自营电商平台，卖零食、文具、数码配件。

- **前台（用户端）**：浏览 → 搜索 → 购物车 → 下单 → 支付 → 收货 → 评价
- **后台（管理端）**：商品管理、订单履约、优惠券、数据看板

### 为什么不直接做苍穹外卖

| 维度 | 苍穹外卖 | CampusMart |
|---|---|---|
| 商品模型 | 菜品，单规格 | SPU + SKU，多规格组合 |
| 库存 | 无真实库存 | 有，且需防超卖 |
| 购物车 | 单商品临时表 | 多商品 + 选中态 + 实时计价 |
| 优惠 | 无 | 优惠券并发领取 + 门槛 + 过期 |
| 订单 | 状态简单 | 完整状态机 + 超时取消 + 退款 |
| 支付 | 微信支付 | 模拟支付 + 回调幂等 + 验签设计 |
| 技术难点 | 缓存、来单提醒 | **并发扣库存、延迟任务、优惠券超发** |

外卖项目面试官已经看腻了。而「你怎么防超卖」「订单 30 分钟未支付怎么自动取消」这两个问题，是电商岗的高频题，做过就有得聊。

---

## 二、技术选型

| 层次 | 选型 | 版本建议 | 备注 |
|---|---|---|---|
| JDK | OpenJDK | **17** | 你本地已有 `C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot` |
| 框架 | Spring Boot | **4.0.8** | 不要选 4.1.x / SNAPSHOT / M2 |
| ORM | MyBatis-Plus | **3.5.17**（`mybatis-plus-spring-boot4-starter`） | 必须配 boot4 专用坐标 + 额外引入 `mybatis-plus-jsqlparser` |
| 数据库 | MySQL | 8.0 | 用 InnoDB，注意隔离级别 |
| 缓存 | Redis | 7.x | 库存预扣 / 延迟队列 / 缓存 |
| 认证 | JWT (jjwt) | 0.12.x | 前台用户与后台管理员分开两套 token |
| 接口调试 | Apifox / Postman | — | Knife4j 在 Boot 4 下不可用 |
| 对象存储 | 阿里云 OSS | 3.17.4 | 复用你 web-ai-project02 的 bucket |
| 前端（前台） | Vue3 + Element Plus | — | 页面不用多，够走通流程即可 |
| 前端（后台） | 开源 admin 模板 | — | 别自己写后台，浪费时间 |
| 部署 | Linux + Nginx + systemd | — | 或 Docker Compose |

**关于版本的说明（2026-10 修正）**：早期建议退到 3.3.x，理由是生态适配不稳。但 start.spring.io 现已不再提供 3.x（最低 4.0.8），且 **MyBatis-Plus 自 3.5.15 起已提供 `mybatis-plus-spring-boot4-starter` 正式兼容 Boot 4**。因此本项目直接使用 4.0.8，不再降级。

两个必须注意的点：

1. **starter 坐标必须换**。用 `mybatis-plus-spring-boot3-starter` 在 Boot 4 下会自动配置不生效，启动时报 `No qualifying bean of type 'XxxMapper'`。
2. **分页要额外引依赖**。自 3.5.9 起 `PaginationInnerInterceptor` 已从核心包拆出，必须单独引入 `mybatis-plus-jsqlparser`，否则分页不可用。

另外 Boot 4 已升级到 **Jackson 3**，核心包名从 `com.fasterxml.jackson` 变为 `tools.jackson`。涉及 JSON 序列化的地方要注意 import。

---

## 三、动手前的 1 周准备

你还没系统学 Spring Boot，但不需要先看完一整套课程。只学下面六件事，够撑起整个项目：

| 顺序 | 内容 | 学完能做什么 | 建议耗时 |
|---|---|---|---|
| 1 | 自动装配、starter、`application.yml` | 能建一个能跑的项目 | 半天 |
| 2 | `@RestController` / `@Service` / `@Mapper` | 能写接口 | 半天 |
| 3 | MyBatis-Plus：`BaseMapper`、`LambdaQueryWrapper`、分页插件 | 能写 CRUD 和分页 | 1 天 |
| 4 | 全局异常处理 `@RestControllerAdvice` + 统一响应 | 接口返回格式统一 | 半天 |
| 5 | 拦截器 `HandlerInterceptor` + JWT 校验 | 能做登录态 | 1 天 |
| 6 | AOP `@Aspect`（做操作日志） | 后台操作留痕 | 半天 |

**不要学的**（现在用不上，学了会忘）：Spring Cloud、消息队列、Seata、ES、Docker 深入、响应式编程。

学习方式建议：不要看完视频再做。**边做边查**，遇到问题搜关键字。你已经有 JavaWeb 底子，Spring Boot 本质就是"把 Servlet 那套用注解包了一层"，理解起来会很快。

---

## 四、数据库设计

### 4.1 核心表清单（13 张）

| 表名 | 作用 | 关键点 |
|---|---|---|
| `t_user` | 前台用户 | 手机号唯一索引 |
| `t_admin` | 后台管理员 | 与前台用户分离 |
| `t_category` | 商品分类 | 一级/二级，`parent_id` |
| `t_spu` | 商品（标准产品单位） | 名称、主图、分类、描述 |
| `t_sku` | 规格（库存量单位） | 价格、库存、规格值组合、`version` |
| `t_spu_attr` | 商品属性模板 | 颜色/尺寸等可选项 |
| `t_cart` | 购物车 | `(user_id, sku_id)` 唯一 |
| `t_address` | 收货地址 | 默认地址标记 |
| `t_order` | 订单主表 | 订单号、状态、金额、支付时间 |
| `t_order_item` | 订单明细 | 下单时的**商品快照** |
| `t_coupon` | 优惠券模板 | 面额、门槛、库存、有效期 |
| `t_user_coupon` | 用户持有的券 | `(user_id, coupon_id)` 唯一 |
| `t_comment` | 商品评价 | 关联订单明细 |

### 4.2 关键 DDL

```sql
CREATE DATABASE campusmart DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusmart;

CREATE TABLE t_user (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  phone      VARCHAR(20)  NOT NULL,
  password   VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  nickname   VARCHAR(50)  DEFAULT NULL,
  avatar     VARCHAR(500) DEFAULT NULL,
  status     TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB;

CREATE TABLE t_spu (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(200) NOT NULL,
  sub_title   VARCHAR(500) DEFAULT NULL,
  main_image  VARCHAR(500) DEFAULT NULL,
  detail      TEXT,
  price_low   DECIMAL(10,2) NOT NULL COMMENT '最低价，冗余用于列表展示',
  sales       INT NOT NULL DEFAULT 0 COMMENT '销量',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_category_status (category_id, status),
  KEY idx_sales (sales DESC)
) ENGINE=InnoDB;

CREATE TABLE t_sku (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  spu_id     BIGINT UNSIGNED NOT NULL,
  sku_name   VARCHAR(200) NOT NULL COMMENT '如 红色-XL',
  specs      JSON DEFAULT NULL COMMENT '{"颜色":"红","尺寸":"XL"}',
  price      DECIMAL(10,2) NOT NULL,
  stock      INT NOT NULL DEFAULT 0,
  version    INT NOT NULL DEFAULT 0,
  image      VARCHAR(500) DEFAULT NULL,
  status     TINYINT NOT NULL DEFAULT 1,
  KEY idx_spu (spu_id)
) ENGINE=InnoDB;

CREATE TABLE t_cart (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT UNSIGNED NOT NULL,
  sku_id     BIGINT UNSIGNED NOT NULL,
  quantity   INT NOT NULL DEFAULT 1,
  checked    TINYINT NOT NULL DEFAULT 1 COMMENT '选中态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB;

CREATE TABLE t_order (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  order_no       CHAR(32) NOT NULL,
  user_id        BIGINT UNSIGNED NOT NULL,
  total_amount   DECIMAL(10,2) NOT NULL COMMENT '商品总额',
  pay_amount     DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  coupon_id      BIGINT UNSIGNED DEFAULT NULL,
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1待发货 2待收货 3已完成 4已取消 5退款中',
  receiver_name  VARCHAR(50) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  receiver_addr  VARCHAR(300) NOT NULL,
  pay_time       DATETIME DEFAULT NULL,
  expire_time    DATETIME NOT NULL COMMENT '超时时间 = 创建时间 + 30min',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_status (user_id, status),
  KEY idx_status_expire (status, expire_time) COMMENT '扫表兜底用'
) ENGINE=InnoDB;

CREATE TABLE t_order_item (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  order_id    BIGINT UNSIGNED NOT NULL,
  sku_id      BIGINT UNSIGNED NOT NULL,
  spu_name    VARCHAR(200) NOT NULL COMMENT '快照',
  sku_name    VARCHAR(200) NOT NULL COMMENT '快照',
  sku_image   VARCHAR(500) DEFAULT NULL,
  price       DECIMAL(10,2) NOT NULL COMMENT '下单时单价，快照',
  quantity    INT NOT NULL,
  KEY idx_order (order_id)
) ENGINE=InnoDB;

CREATE TABLE t_coupon (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  amount       DECIMAL(10,2) NOT NULL COMMENT '面额',
  threshold    DECIMAL(10,2) NOT NULL COMMENT '使用门槛',
  total        INT NOT NULL COMMENT '发行总量',
  remain       INT NOT NULL COMMENT '剩余量',
  begin_time   DATETIME NOT NULL,
  end_time     DATETIME NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE t_user_coupon (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT UNSIGNED NOT NULL,
  coupon_id  BIGINT UNSIGNED NOT NULL,
  status     TINYINT NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  order_id   BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_coupon (user_id, coupon_id),
  KEY idx_user_status (user_id, status)
) ENGINE=InnoDB;
```

### 4.3 三个必须理解的设计点

1. **`t_order_item` 存的是快照，不是外键引用**
   下单那一刻的商品名、图、价格全部复制一份存进明细表。之后商家改价、改图，历史订单不能跟着变。这是电商系统的基本功 —— 面试常问"为什么订单明细要冗余商品信息"。

2. **`t_spu.price_low` 是冗余字段**
   列表页要显示"￥9.9 起"，如果没有这个字段，每次都要 `JOIN t_sku` 取 `MIN(price)`。用冗余换查询性能，代价是 SKU 改价时要同步更新 SPU。

3. **`t_sku.specs` 用 JSON 类型**
   MySQL 5.7+ 支持 JSON。规格组合如 `{"颜色":"红","尺寸":"XL"}`，比"规格1/规格2/规格3"三列硬编码灵活。查询时可用 `JSON_EXTRACT`，但注意 JSON 字段建索引要用生成列。

---

## 五、模块划分

### 5.1 前台（用户端）`/api/user/**`

| 模块 | 功能 |
|---|---|
| 用户 | 注册、登录、个人信息、头像上传 |
| 商品 | 分类树、商品列表（分页/排序/分类筛选）、搜索、详情（SPU + 全部 SKU） |
| 购物车 | 加入、修改数量、勾选、删除、清空 |
| 地址 | CRUD、设默认 |
| 订单 | 结算预览、提交订单、支付、取消、确认收货、订单列表/详情 |
| 优惠券 | 可领列表、领取、我的券 |
| 评价 | 发表评价、商品评价列表 |

### 5.2 后台（管理端）`/api/admin/**`

| 模块 | 功能 |
|---|---|
| 管理员 | 登录、JWT |
| 分类 | CRUD |
| 商品 | SPU/SKU CRUD、上下架、图片上传 |
| 订单 | 列表、详情、发货、退款审核 |
| 优惠券 | 创建、停发 |
| 看板 | 今日订单数、销售额、热销商品 Top10 |

### 5.3 工程结构

```
campusmart/
├── campusmart-common/        通用：Result、异常、常量、工具
├── campusmart-pojo/          实体、DTO、VO
├── campusmart-server/        主应用
│   ├── config/               MP配置、Redis配置、拦截器注册
│   ├── controller/
│   │   ├── user/             前台接口
│   │   └── admin/            后台接口
│   ├── service/impl/
│   ├── mapper/
│   ├── interceptor/          JwtInterceptor
│   ├── handler/              GlobalExceptionHandler
│   ├── task/                 OrderTimeoutTask
│   └── aspect/               LogAspect
└── campusmart-web/           前端（可独立仓库）
```

---

## 六、12 周推进计划

### 第 0 周：Spring Boot 速通

- [ ] 建多模块 Maven 工程，跑通一个 `/api/ping`
- [ ] 集成 MyBatis-Plus，写一个 `BaseMapper` 查询
- [ ] 用 Apifox 或 Postman 调通一个查询接口（Boot 4 下不装 Knife4j）
- [ ] 写 `Result` + `GlobalExceptionHandler`

**验收**：能在接口文档里点通一个查询接口。

### 第 1-2 周：用户与认证

- [ ] BCrypt 加密密码（`BCryptPasswordEncoder`，不要自己写 md5+salt）
- [ ] 注册、登录，签发 JWT
- [ ] `JwtInterceptor` 校验 token，把 userId 放进 `UserContext`（ThreadLocal）
- [ ] 前台/后台两套拦截器与两套密钥
- [ ] 全局异常捕获 `TokenExpiredException` 返回 401

**验收**：未带 token 访问受保护接口返回 401，带 token 正常返回。

### 第 3-4 周：商品体系（重点）

- [ ] 分类树：`parent_id` 递归组装成树，一次查库内存组装（不要 N+1）
- [ ] SPU/SKU 新增：一个表单提交 `spu + List<sku>`，事务写入两张表
- [ ] 商品列表：分页 + 分类筛选 + 价格/销量排序
- [ ] 商品详情：SPU + 全部 SKU，前端根据规格选择定位到具体 SKU
- [ ] 图片上传接 OSS（`MultipartFile` → OSS，返回 URL）

**验收**：后台能发布一个"有 3 种颜色 × 2 种尺寸 = 6 个 SKU"的商品，前台能看到规格选择。

### 第 5 周：购物车与地址

- [ ] 加入购物车：存在则数量累加（`ON DUPLICATE KEY UPDATE` 或先查后改）
- [ ] 购物车列表：`JOIN t_sku` 与 `t_spu` 实时取价与库存
- [ ] **价格不存快照**：购物车显示的价格永远是当前价，只有订单才做快照
- [ ] 地址 CRUD，只能有一条默认地址

**验收**：购物车能勾选、能改数量、总价实时变化。

### 第 6-7 周：下单与库存（核心）

- [ ] 结算预览接口：算商品总额、优惠、运费、实付
- [ ] 下单接口：校验 → 扣库存 → 建订单 → 建明细 → 核销券 → 清购物车，**同一个事务**
- [ ] 库存扣减用 `UPDATE t_sku SET stock = stock - ? WHERE id = ? AND stock >= ?`
- [ ] Redis 预扣作为第一道闸（详见 7.1）
- [ ] 订单状态机（详见 7.4）

**验收**：用 JMeter 100 线程并发抢 10 件库存，最终库存为 0 且绝不出现负数。

### 第 8 周：支付与超时取消

- [ ] 模拟支付接口：把订单从 0 改为 1，写 `pay_time`
- [ ] 支付回调接口：**幂等**（已支付直接返回成功，不重复处理）
- [ ] 延迟任务：Redis ZSet 存到期时间戳，定时轮询（详见 7.2）
- [ ] 兜底扫表：每分钟扫一次 `status=0 AND expire_time < NOW()`

**验收**：下单后不支付，30 分钟后自动变已取消且库存回滚。

### 第 9 周：优惠券

- [ ] 领券：Redis 扣 remain + Lua（详见 7.3）
- [ ] 数据库 `remain > 0` 兜底 + 唯一索引防重复领
- [ ] 下单时选券：校验门槛、有效期、状态
- [ ] 取消订单时退券

**验收**：1000 人抢 100 张券，最终正好发出 100 张，无超发。

### 第 10 周：后台管理

- [ ] 订单列表、详情、发货（填单号）
- [ ] 退款审核流程
- [ ] 数据看板：今日订单数、销售额、Top10 热销
- [ ] AOP 记录后台操作日志

### 第 11 周：缓存与性能

- [ ] 商品详情缓存：`item:{spuId}`，TTL 30 分钟，更新时双删
- [ ] 分类树缓存：几乎不变，永久缓存 + 后台改动时刷新
- [ ] 缓存穿透：空值缓存 5 分钟
- [ ] 缓存雪崩：TTL 加随机偏移
- [ ] 压测并记录 QPS、P99

### 第 12 周：部署与总结

- [ ] Docker Compose 起 MySQL + Redis + 应用（或 systemd）
- [ ] Nginx 反代 + 静态资源 + `client_max_body_size`
- [ ] 前端打包部署
- [ ] 写 README：架构图、接口文档、压测数据、踩坑记录

---

## 七、六个核心难点

### 7.1 防超卖：三层防线

**第一层 · Redis 预扣（挡住绝大部分流量）**

```lua
-- KEYS[1] = stock:sku:{skuId}
-- ARGV[1] = 本次购买数量
local stock = tonumber(redis.call('GET', KEYS[1]))
local need  = tonumber(ARGV[1])
if stock == nil then return -1 end        -- 缓存未预热
if stock < need then return 0 end         -- 库存不足
redis.call('DECRBY', KEYS[1], need)
return 1
```

**第二层 · 数据库条件更新（真正的正确性保证）**

```sql
UPDATE t_sku
SET stock = stock - #{num}, sales = sales + #{num}
WHERE id = #{skuId} AND stock >= #{num}
```

影响行数为 0 就说明扣失败，抛异常回滚整个事务。**注意这里不需要 `version` 字段** —— 库存扣减是减法操作，`stock >= num` 这个条件本身就是安全的，加了 version 反而增加无谓的失败重试。`version` 适合"读取-计算-写回"的场景（比如余额、积分），不适合纯减法。

**第三层 · 兜底**

- 下单失败时**把 Redis 预扣的库存加回去**（在 `catch` 里 `INCRBY`，或记录日志由补偿任务处理）
- 定时任务校正：`Redis 库存 = DB 库存 - 未支付订单占用`

**为什么不能只用 Redis**：Redis 挂了或数据丢了，就超卖了。为什么不能只用数据库：所有请求打到 MySQL，热点 SKU 的行锁会把数据库拖死。

### 7.2 订单超时取消：Redis ZSet 延迟队列

```java
public static final String DELAY_KEY = "order:delay";

// 下单成功后投递
public void push(Long orderId, long delayMillis) {
    redisTemplate.opsForZSet().add(DELAY_KEY, String.valueOf(orderId),
            System.currentTimeMillis() + delayMillis);
}

// 定时任务轮询（每 5 秒一次）
@Scheduled(fixedDelay = 5000)
public void consume() {
    long now = System.currentTimeMillis();
    Set<String> due = redisTemplate.opsForZSet()
            .rangeByScore(DELAY_KEY, 0, now, 0, 100);
    if (due == null || due.isEmpty()) return;

    for (String orderId : due) {
        // 先移除，谁移除成功谁负责处理，避免多实例重复消费
        Long removed = redisTemplate.opsForZSet().remove(DELAY_KEY, orderId);
        if (removed == null || removed == 0) continue;
        try {
            orderService.cancelIfUnpaid(Long.valueOf(orderId));
        } catch (Exception e) {
            log.error("取消订单失败 {}", orderId, e);
        }
    }
}
```

**关键点**：

- `ZREM` 的返回值是抢占凭证，多实例部署也不会重复处理
- `cancelIfUnpaid` 内部要先 `UPDATE t_order SET status=4 WHERE id=? AND status=0`，影响行数为 0 说明已经被支付了，直接返回 —— **这是与支付回调的竞争处理**
- ZSet 方案简单够用，但有精度问题（轮询间隔）。进阶可换 RabbitMQ 死信队列或 Redisson 的 `RDelayedQueue`

### 7.3 优惠券防超发

```java
// 领券接口
public void receive(Long userId, Long couponId) {
    // 1. 唯一索引兜底重复领取（t_user_coupon 的 uk_user_coupon）
    //    先查一次，快速失败
    if (userCouponMapper.exists(userId, couponId)) {
        throw new BizException("已领取过");
    }

    // 2. Redis 原子扣减剩余量
    Long r = redisTemplate.execute(RECEIVE_SCRIPT,
            Collections.singletonList("coupon:remain:" + couponId));
    if (r == null || r == 0) throw new BizException("已被领完");

    try {
        // 3. 数据库扣减 + 插入用户券，同一事务
        int rows = couponMapper.decreaseRemain(couponId);
        if (rows == 0) throw new BizException("已被领完");
        userCouponMapper.insert(userId, couponId);
    } catch (DuplicateKeyException e) {
        // 并发下两个请求同时通过了第 1 步，唯一索引拦住一个
        redisTemplate.opsForValue().increment("coupon:remain:" + couponId);
        throw new BizException("已领取过");
    } catch (Exception e) {
        redisTemplate.opsForValue().increment("coupon:remain:" + couponId);
        throw e;
    }
}
```

三层防护：**Redis 挡流量 → 数据库条件更新保正确 → 唯一索引防重复**。任何一层失效，其余两层还能兜住。

### 7.4 订单状态机

```
0 待支付 ──支付──> 1 待发货 ──发货──> 2 待收货 ──确认──> 3 已完成
   │                    │
   └──超时/取消──> 4 已取消       └──申请退款──> 5 退款中 ──> 4 / 3
```

不要用一堆 `if` 判断状态。写一个枚举 + 允许的流转表：

```java
public enum OrderStatus {
    UNPAID(0), UNSHIPPED(1), SHIPPED(2), FINISHED(3), CANCELLED(4), REFUNDING(5);
}

private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
    UNPAID,    Set.of(UNSHIPPED, CANCELLED),
    UNSHIPPED, Set.of(SHIPPED, REFUNDING, CANCELLED),
    SHIPPED,   Set.of(FINISHED, REFUNDING),
    REFUNDING, Set.of(CANCELLED, FINISHED),
    FINISHED,  Set.of(),
    CANCELLED, Set.of()
);

public void transfer(Order order, OrderStatus target) {
    if (!ALLOWED.get(order.getStatus()).contains(target)) {
        throw new BizException("订单状态不允许该操作");
    }
    // UPDATE ... WHERE id=? AND status=#{原状态}  用条件更新防并发
}
```

**所有状态变更都用 `WHERE status = 原状态` 的条件更新**，这样两个并发请求只有一个能成功。这比 `synchronized` 或分布式锁都简洁。

### 7.5 购物车价格：不存快照

购物车**永远实时查价格**，因为：
- 商品可能降价，用户应该看到新价
- 商品可能下架，购物车里要提示"已失效"

而订单**必须存快照**，因为：
- 用户下单时的价格是双方约定，不能事后更改

这个对比是面试很好的切入点：**"什么时候冗余，什么时候实时查"取决于数据是否具有法律/契约意义。**

### 7.6 SPU / SKU 模型

一个"iPhone 保护壳"（SPU）有多个规格组合（SKU）：

```
SPU: 磨砂保护壳  ￥19.9 起
 ├─ SKU: 黑色 · iPhone 15     ￥19.9  库存 100
 ├─ SKU: 黑色 · iPhone 15 Pro ￥22.9  库存 50
 ├─ SKU: 白色 · iPhone 15     ￥19.9  库存 0
 └─ SKU: 白色 · iPhone 15 Pro ￥22.9  库存 30
```

前端交互：用户点"黑色"再点"15 Pro" → 前端根据规格组合找到对应 SKU id → 加购时传 skuId。

**不是"选规格再算价格"，而是"选规格定位到 SKU"**。价格、库存都挂在 SKU 上。理解这一点，商品模块就通了。

---

## 八、避坑清单

| 坑 | 表现 | 正确做法 |
|---|---|---|
| 金额用 double | 算出来 0.1 + 0.2 = 0.30000000000000004 | 一律 `BigDecimal`，或存"分"用 Long |
| 下单忘记加事务 | 订单建了但库存没扣 | `@Transactional(rollbackFor = Exception.class)` |
| 事务注解失效 | 同类内部调用 `this.method()` 不生效 | 拆到另一个 Bean，或注入自身代理 |
| 分页插件没配 | `selectPage` 返回全部数据 | 注册 `MybatisPlusInterceptor` + `PaginationInnerInterceptor` |
| N+1 查询 | 列表页几百条 SQL | 批量 `IN` 查询后在内存组装 |
| JWT 无过期 | token 永久有效 | `setExpiration` + 前端 401 后跳登录 |
| 库存回滚漏了 | Redis 库存越用越少 | 下单异常时 `INCRBY` 补回 |
| 定时任务多实例重复跑 | 订单被取消两次 | ZSet `ZREM` 抢占 或加分布式锁 |
| 图片直接存数据库 | 数据库暴涨 | 存 OSS，库里存 URL |
| 循环里查数据库 | 接口 3 秒才返回 | 先收集 id，一次 `IN` 查完 |

---

## 九、简历怎么写

> **CampusMart 校园自营商城**（个人项目｜Spring Boot 3 + MyBatis-Plus + MySQL + Redis + OSS）
> - 独立完成前后台全链路开发，覆盖用户、商品（SPU/SKU 多规格）、购物车、订单、优惠券、评价六大模块
> - 设计三级防超卖方案：Redis Lua 预扣 → 数据库条件更新（`WHERE stock >= n`）→ 定时校正，压测 100 并发抢 10 件库存，最终库存准确为 0，无超卖
> - 基于 Redis ZSet 实现订单延迟队列，30 分钟未支付自动取消并回滚库存与优惠券；通过 `ZREM` 抢占 + 条件更新解决多实例重复消费与支付回调竞争
> - 优惠券领取采用 Redis 原子扣减 + 数据库唯一索引兜底，1000 并发抢 100 张券无超发
> - 商品详情缓存，配合空值缓存与 TTL 随机偏移解决穿透与雪崩，接口 P99 从 __ms 降至 __ms
> - 部署于 Linux（Nginx + Docker Compose），完成 OSS 图片存储与后台操作日志切面

**面试前必须准备好的四个回答**：

1. 为什么库存扣减不用 `version` 乐观锁？→ 因为纯减法是幂等的条件更新，`stock >= n` 已是原子校验；version 适合读-改-写场景。
2. Redis 预扣了但数据库扣失败怎么办？→ 异常分支 `INCRBY` 补回 + 定时任务按 `DB 库存 - 未支付占用` 校正。
3. 延迟队列为什么不用 `Thread.sleep` 或定时扫表？→ 扫表有精度和性能问题（全表扫描），`sleep` 无法持久化、重启就丢。
4. 支付回调和超时任务同时到达怎么办？→ 都走 `UPDATE ... WHERE status=0`，谁先成功谁生效，另一个影响行数为 0 直接返回。

---

## 十、可选加分项

按性价比排序：

1. **搜索**：MySQL 全文索引（`FULLTEXT` + `ngram` 分词）起步，进阶接 Elasticsearch
2. **商品评价图片**：复用 OSS
3. **库存预警**：库存低于阈值时后台标红
4. **订单导出**：Apache POI 导出 Excel
5. **接口限流**：Redis 令牌桶（你在网盘方案里已经写过，可直接复用）
6. **分布式锁**：Redisson，用于后台改价等低频写操作

---

## 十一、开工 checklist

- [ ] JDK 17 已就位（`C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot`）
- [ ] Spring Boot 锁定 **4.0.8**，MyBatis-Plus 用 `mybatis-plus-spring-boot4-starter` + `mybatis-plus-jsqlparser`
- [ ] MySQL 8 + Redis 7 本地跑起来
- [ ] 建 Git 仓库，每天提交
- [ ] 建 `NOTES.md`，记录每天的坑 —— 写总结时就是素材
- [ ] 先做前台，再做后台；先做单规格商品，跑通下单后再加 SKU 多规格

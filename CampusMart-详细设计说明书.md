# CampusMart 校园自营商城 —— 详细设计说明书

| 项目 | 内容 |
|---|---|
| 项目名称 | CampusMart 校园自营商城 |
| 文档名称 | 详细设计说明书 |
| 版本 | V1.0 |
| 编制 | 星宇 |
| 日期 | 2026-10-19 |
| 依据 | 《CampusMart 概要设计说明书》V1.0 |

---

## 1 引言

### 1.1 编写目的

本文档在概要设计的基础上，细化到可编码的粒度：给出包结构、类清单、方法签名、核心算法、数据库字段定义、接口报文样例、前端页面结构。开发者可依据本文档直接进入编码，无需再做设计决策。

### 1.2 阅读约定

- 方法签名使用 Java 语法，省略 `public` 修饰符时表示接口方法。
- `BizException(code, msg)` 为项目自定义业务异常，见 3.2 节。
- 所有金额类型为 `BigDecimal`，数据库中为 `DECIMAL(10,2)`。
- 所有时间类型为 `LocalDateTime`，数据库为 `DATETIME`。
- 伪代码中 `//` 开头为注释，`...` 表示省略的非关键代码。

---

## 2 系统结构

### 2.1 工程结构

```
campusmart/
├── pom.xml                          父 POM，统一依赖版本
├── campusmart-common/               通用模块（无业务依赖）
│   └── src/main/java/com/campusmart/common/
│       ├── result/
│       │   ├── Result.java
│       │   └── PageResult.java
│       ├── exception/
│       │   ├── BizException.java
│       │   ├── ErrorCode.java
│       │   └── TokenException.java
│       ├── constant/
│       │   ├── RedisKey.java
│       │   ├── OrderStatus.java
│       │   └── CouponStatus.java
│       └── util/
│           ├── JwtUtil.java
│           ├── UserContext.java
│           └── MoneyUtil.java
├── campusmart-pojo/                 实体与传输对象
│   └── src/main/java/com/campusmart/pojo/
│       ├── entity/                  @TableName 实体
│       ├── dto/                     入参
│       ├── vo/                      出参
│       └── query/                   查询条件
├── campusmart-server/               主应用
│   ├── config/
│   ├── controller/user/
│   ├── controller/admin/
│   ├── service/
│   ├── service/impl/
│   ├── mapper/
│   ├── interceptor/
│   ├── handler/
│   ├── aspect/
│   ├── task/
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── mapper/*.xml
└── campusmart-web/                  前端（Vue3 + Vite）
```

### 2.2 分层调用规则

| 规则 | 说明 |
|---|---|
| R-01 | Controller 不得直接调用 Mapper，必须经过 Service |
| R-02 | Service 之间可以互相调用，但不得形成循环依赖 |
| R-03 | Mapper 只做数据访问，不含业务判断 |
| R-04 | Entity 不出现在 Controller 的入参与返回值中，必须转为 DTO/VO |
| R-05 | 事务注解只加在 Service 的 public 方法上 |
| R-06 | 同类内部方法调用不走代理，事务注解会失效；需要事务时必须跨 Bean 调用 |
| R-07 | 所有跨模块编排集中在 OrderService，其他 Service 保持单一职责 |

### 2.3 包命名规范

`com.campusmart.<层>.<模块>.<类型>`，例如 `com.campusmart.service.order.OrderService`。

---

## 3 公共组件详细设计

### 3.1 统一响应 Result

```java
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String  msg;
    private T       data;

    public static <T> Result<T> ok(T data) {
        return build(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return build(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return build(code, msg, null);
    }

    private static <T> Result<T> build(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
```

| 方法 | 入参 | 出参 | 说明 |
|---|---|---|---|
| ok(T data) | 业务数据 | Result | 成功带数据 |
| ok() | 无 | Result | 成功无数据 |
| fail(int, String) | 码与提示 | Result | 失败 |

### 3.2 异常体系

```java
public class ErrorCode {
    public static final int PARAM_ERROR   = 400;
    public static final int UNAUTHORIZED  = 401;
    public static final int FORBIDDEN     = 403;
    public static final int NOT_FOUND     = 404;
    public static final int CONFLICT      = 409;
    public static final int TOO_MANY      = 429;
    public static final int SYSTEM_ERROR  = 500;

    // 用户
    public static final int PHONE_EXISTS      = 10001;
    public static final int LOGIN_FAILED      = 10002;
    public static final int USER_NOT_FOUND    = 10003;
    public static final int ADDRESS_NOT_FOUND = 10004;

    // 商品
    public static final int PRODUCT_OFF_SHELF = 20001;
    public static final int SKU_NOT_FOUND     = 20002;

    // 购物车
    public static final int CART_EMPTY        = 30001;
    public static final int STOCK_NOT_ENOUGH  = 30002;

    // 订单
    public static final int ORDER_NOT_FOUND   = 40001;
    public static final int ORDER_STATUS_ERR  = 40002;
    public static final int ORDER_EXPIRED     = 40003;
    public static final int DUPLICATE_SUBMIT  = 40009;

    // 支付
    public static final int PAY_ALREADY_DONE  = 50001;
    public static final int SIGN_INVALID      = 50002;

    // 优惠券
    public static final int COUPON_SOLD_OUT   = 60001;
    public static final int COUPON_RECEIVED   = 60002;
    public static final int COUPON_UNUSABLE   = 60003;
}

public class BizException extends RuntimeException {
    private final int code;
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
    public int getCode() { return code; }
}
```

**设计要点**：`BizException` 继承 `RuntimeException`（非受检），避免每层 `throws` 污染签名；由全局异常处理器统一转成 `Result`。

### 3.3 JWT 工具

```java
@Component
public class JwtUtil {

    @Value("${jwt.user-secret}")
    private String userSecret;

    @Value("${jwt.admin-secret}")
    private String adminSecret;

    @Value("${jwt.expire-ms}")
    private long expireMs;

    /** 签发 token */
    public String create(Long userId, String role, boolean admin) {
        SecretKey key = Keys.hmacShaKeyFor(
                (admin ? adminSecret : userSecret).getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }

    /** 解析 token，失败抛 TokenException */
    public Claims parse(String token, boolean admin) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    (admin ? adminSecret : userSecret).getBytes(StandardCharsets.UTF_8));
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenException("登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException("无效的登录凭证");
        }
    }
}
```

| 方法 | 入参 | 出参 | 异常 |
|---|---|---|---|
| create | userId, role, admin | token 字符串 | 无 |
| parse | token, admin | Claims | TokenException |

**安全要点**：前台与后台使用不同密钥。若共用密钥，普通用户伪造 `role=ADMIN` 即可越权访问后台接口。

### 3.4 用户上下文

```java
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getUserId() {
        Long id = USER_ID.get();
        if (id == null) throw new BizException(ErrorCode.UNAUTHORIZED, "未登录");
        return id;
    }

    public static Long getUserIdOrNull() { return USER_ID.get(); }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
```

**强制约束**：拦截器 `afterCompletion` 中必须调用 `clear()`。Tomcat 线程池会复用线程，不清理会导致下一个请求读到上一个用户的 id —— 这是线上事故级问题。

### 3.5 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    public Result<Void> handleToken(TokenException e) {
        return Result.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(FieldError::getDefaultMessage).orElse("参数错误");
        return Result.fail(ErrorCode.PARAM_ERROR, msg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一约束冲突", e);
        return Result.fail(ErrorCode.CONFLICT, "请勿重复操作");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
```

### 3.6 MyBatis-Plus 配置

```java
@Configuration
@MapperScan("com.campusmart.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mpInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor()); // 禁止全表 update/delete
        return interceptor;
    }
}
```

**注意**：不配置分页插件时，`selectPage` 会返回全部数据且不报错，是隐性故障。

**Boot 4 额外注意**：`PaginationInnerInterceptor` 自 MyBatis-Plus 3.5.9 起已从核心包拆分到 `mybatis-plus-jsqlparser`，pom 中必须单独引入该依赖（版本与 starter 一致），否则分页功能不可用。

### 3.7 拦截器设计

| 拦截器 | 路径 | 职责 |
|---|---|---|
| UserJwtInterceptor | `/api/**` 排除 `/api/user/register`、`/api/user/login`、`/api/category/**`、`/api/product/**`、`/api/pay/notify` | 校验用户 token，注入 UserContext |
| AdminJwtInterceptor | `/api/admin/**` 排除 `/api/admin/login` | 校验管理员 token，注入 UserContext |

```java
@Override
public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
    if (!(handler instanceof HandlerMethod)) return true;

    String auth = req.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
        throw new TokenException("请先登录");
    }
    Claims claims = jwtUtil.parse(auth.substring(7), isAdmin);
    UserContext.set(Long.valueOf(claims.getSubject()), (String) claims.get("role"));
    return true;
}

@Override
public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                            Object handler, Exception ex) {
    UserContext.clear();   // 必须，否则线程复用会串号
}
```

---

## 4 数据库详细设计

### 4.1 枚举字典

| 枚举 | 值 | 含义 |
|---|---|---|
| 订单状态 OrderStatus | 0 | 待支付 |
| | 1 | 待发货 |
| | 2 | 待收货 |
| | 3 | 已完成 |
| | 4 | 已取消 |
| | 5 | 退款中 |
| 用户券状态 CouponStatus | 0 | 未使用 |
| | 1 | 已使用 |
| | 2 | 已过期 |
| 商品状态 | 0 | 下架 |
| | 1 | 上架 |
| 支付状态 | 0 | 待支付 |
| | 1 | 已支付 |
| | 2 | 已退款 |
| 通用启用状态 | 0 | 禁用/删除 |
| | 1 | 正常 |

### 4.2 建表语句

```sql
CREATE DATABASE IF NOT EXISTS campusmart
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusmart;

-- 1. 前台用户
CREATE TABLE t_user (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  phone       VARCHAR(20)  NOT NULL              COMMENT '手机号，登录账号',
  password    VARCHAR(100) NOT NULL              COMMENT 'BCrypt 哈希',
  nickname    VARCHAR(50)  DEFAULT NULL          COMMENT '昵称',
  avatar      VARCHAR(500) DEFAULT NULL          COMMENT '头像 URL',
  status      TINYINT      NOT NULL DEFAULT 1    COMMENT '1正常 0禁用',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB COMMENT='前台用户';

-- 2. 后台管理员
CREATE TABLE t_admin (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL,
  password    VARCHAR(100) NOT NULL              COMMENT 'BCrypt 哈希',
  real_name   VARCHAR(50)  DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='后台管理员';

-- 3. 商品分类
CREATE TABLE t_category (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  parent_id   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 表示一级分类',
  name        VARCHAR(50)  NOT NULL,
  icon        VARCHAR(500) DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0    COMMENT '排序，越小越前',
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_parent (parent_id, status, sort)
) ENGINE=InnoDB COMMENT='商品分类';

-- 4. 商品 SPU
CREATE TABLE t_spu (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(200) NOT NULL,
  sub_title   VARCHAR(500) DEFAULT NULL          COMMENT '副标题/卖点',
  main_image  VARCHAR(500) DEFAULT NULL,
  images      JSON         DEFAULT NULL          COMMENT '轮播图 URL 数组',
  detail      TEXT                               COMMENT '富文本详情',
  price_low   DECIMAL(10,2) NOT NULL             COMMENT '最低 SKU 价，冗余',
  sales       INT          NOT NULL DEFAULT 0    COMMENT '累计销量，冗余',
  status      TINYINT      NOT NULL DEFAULT 1    COMMENT '1上架 0下架',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_category_status (category_id, status),
  KEY idx_sales (sales DESC),
  KEY idx_name (name(64))
) ENGINE=InnoDB COMMENT='商品 SPU';

-- 5. 商品 SKU
CREATE TABLE t_sku (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  spu_id      BIGINT UNSIGNED NOT NULL,
  sku_name    VARCHAR(200) NOT NULL              COMMENT '如 黑色-iPhone15',
  specs       JSON         DEFAULT NULL          COMMENT '{"颜色":"黑","型号":"15"}',
  price       DECIMAL(10,2) NOT NULL,
  stock       INT          NOT NULL DEFAULT 0,
  version     INT          NOT NULL DEFAULT 0    COMMENT '备用，非库存扣减必需',
  image       VARCHAR(500) DEFAULT NULL,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_spu (spu_id, status),
  KEY idx_stock (stock)
) ENGINE=InnoDB COMMENT='商品 SKU';

-- 6. 购物车
CREATE TABLE t_cart (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT UNSIGNED NOT NULL,
  sku_id      BIGINT UNSIGNED NOT NULL,
  quantity    INT          NOT NULL DEFAULT 1,
  checked     TINYINT      NOT NULL DEFAULT 1    COMMENT '1选中 0未选',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_sku (user_id, sku_id),
  KEY idx_user_checked (user_id, checked)
) ENGINE=InnoDB COMMENT='购物车';

-- 7. 收货地址
CREATE TABLE t_address (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT UNSIGNED NOT NULL,
  receiver    VARCHAR(50)  NOT NULL              COMMENT '收货人',
  phone       VARCHAR(20)  NOT NULL,
  province    VARCHAR(50)  NOT NULL,
  city        VARCHAR(50)  NOT NULL,
  district    VARCHAR(50)  NOT NULL,
  detail      VARCHAR(200) NOT NULL              COMMENT '详细地址',
  is_default  TINYINT      NOT NULL DEFAULT 0,
  status      TINYINT      NOT NULL DEFAULT 1,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id, status)
) ENGINE=InnoDB COMMENT='收货地址';

-- 8. 订单主表
CREATE TABLE t_order (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  order_no        CHAR(32)      NOT NULL         COMMENT '业务订单号',
  user_id         BIGINT UNSIGNED NOT NULL,
  total_amount    DECIMAL(10,2) NOT NULL         COMMENT '商品总额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  freight         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '运费',
  pay_amount      DECIMAL(10,2) NOT NULL         COMMENT '实付金额',
  user_coupon_id  BIGINT UNSIGNED DEFAULT NULL   COMMENT '使用的用户券 id',
  status          TINYINT       NOT NULL DEFAULT 0 COMMENT '见订单状态枚举',
  receiver        VARCHAR(50)   NOT NULL,
  receiver_phone  VARCHAR(20)   NOT NULL,
  receiver_addr   VARCHAR(300)  NOT NULL         COMMENT '省市区+详细，快照',
  remark          VARCHAR(200)  DEFAULT NULL,
  tracking_no     VARCHAR(50)   DEFAULT NULL     COMMENT '物流单号',
  pay_time        DATETIME      DEFAULT NULL,
  ship_time       DATETIME      DEFAULT NULL,
  finish_time     DATETIME      DEFAULT NULL,
  cancel_time     DATETIME      DEFAULT NULL,
  cancel_reason   VARCHAR(100)  DEFAULT NULL,
  expire_time     DATETIME      NOT NULL         COMMENT '创建时间 + 30 分钟',
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_status (user_id, status, created_at DESC),
  KEY idx_status_expire (status, expire_time)
) ENGINE=InnoDB COMMENT='订单主表';

-- 9. 订单明细
CREATE TABLE t_order_item (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  order_id   BIGINT UNSIGNED NOT NULL,
  order_no   CHAR(32)      NOT NULL              COMMENT '冗余，便于按单号查',
  spu_id     BIGINT UNSIGNED NOT NULL,
  sku_id     BIGINT UNSIGNED NOT NULL,
  spu_name   VARCHAR(200)  NOT NULL              COMMENT '快照',
  sku_name   VARCHAR(200)  NOT NULL              COMMENT '快照',
  sku_image  VARCHAR(500)  DEFAULT NULL          COMMENT '快照',
  specs      JSON          DEFAULT NULL          COMMENT '快照',
  price      DECIMAL(10,2) NOT NULL              COMMENT '下单时单价，快照',
  quantity   INT           NOT NULL,
  subtotal   DECIMAL(10,2) NOT NULL              COMMENT 'price * quantity',
  KEY idx_order (order_id),
  KEY idx_order_no (order_no),
  KEY idx_spu (spu_id)
) ENGINE=InnoDB COMMENT='订单明细';

-- 10. 优惠券模板
CREATE TABLE t_coupon (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  type        TINYINT       NOT NULL DEFAULT 1  COMMENT '1满减 2无门槛',
  amount      DECIMAL(10,2) NOT NULL            COMMENT '面额',
  threshold   DECIMAL(10,2) NOT NULL DEFAULT 0  COMMENT '使用门槛',
  total       INT           NOT NULL            COMMENT '发行总量',
  remain      INT           NOT NULL            COMMENT '剩余量',
  begin_time  DATETIME      NOT NULL,
  end_time    DATETIME      NOT NULL,
  status      TINYINT       NOT NULL DEFAULT 1  COMMENT '1进行中 0停止发放',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_time (begin_time, end_time, status)
) ENGINE=InnoDB COMMENT='优惠券模板';

-- 11. 用户优惠券
CREATE TABLE t_user_coupon (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT UNSIGNED NOT NULL,
  coupon_id  BIGINT UNSIGNED NOT NULL,
  status     TINYINT      NOT NULL DEFAULT 0    COMMENT '0未使用 1已使用 2已过期',
  order_no   CHAR(32)     DEFAULT NULL          COMMENT '核销订单号',
  used_time  DATETIME     DEFAULT NULL,
  expire_time DATETIME    NOT NULL              COMMENT '冗余券模板结束时间，便于查询',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_coupon (user_id, coupon_id),
  KEY idx_user_status (user_id, status, expire_time)
) ENGINE=InnoDB COMMENT='用户优惠券';

-- 12. 商品评价
CREATE TABLE t_comment (
  id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT UNSIGNED NOT NULL,
  spu_id     BIGINT UNSIGNED NOT NULL,
  sku_id     BIGINT UNSIGNED NOT NULL,
  order_no   CHAR(32)      NOT NULL,
  content    VARCHAR(1000) NOT NULL,
  images     JSON          DEFAULT NULL,
  score      TINYINT       NOT NULL DEFAULT 5  COMMENT '1~5 星',
  status     TINYINT       NOT NULL DEFAULT 1  COMMENT '1正常 0隐藏',
  created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_sku (order_no, sku_id) COMMENT '同一订单同一SKU只能评一次',
  KEY idx_spu (spu_id, status, created_at DESC)
) ENGINE=InnoDB COMMENT='商品评价';

-- 13. 支付流水
CREATE TABLE t_payment (
  id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  pay_no       CHAR(32)      NOT NULL,
  order_no     CHAR(32)      NOT NULL,
  user_id      BIGINT UNSIGNED NOT NULL,
  amount       DECIMAL(10,2) NOT NULL,
  channel      VARCHAR(20)   NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
  status       TINYINT       NOT NULL DEFAULT 0      COMMENT '0待支付 1已支付 2已退款',
  pay_time     DATETIME      DEFAULT NULL,
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_order_no (order_no)
) ENGINE=InnoDB COMMENT='支付流水';
```

### 4.3 字段设计说明

| 表.字段 | 设计说明 |
|---|---|
| `t_order.order_no` | CHAR(32)，格式 `年月日时分秒(14) + 用户id后6位 + 随机8位`，可读且基本唯一，最终由唯一索引保证 |
| `t_order.receiver_addr` | 将省市区与详细地址拼接后存一条，避免订单表关联地址表——地址可能被用户修改或删除，订单必须留快照 |
| `t_order.expire_time` | 建单时直接算出，供扫表兜底使用；ZSet 中的 score 使用同一时间戳 |
| `t_order_item.subtotal` | 冗余计算列，避免每次查询时 `price * quantity` 计算，也避免浮点误差 |
| `t_user_coupon.expire_time` | 冗余券模板的结束时间，查询"我的可用券"时无需 JOIN 券模板 |
| `t_spu.images` | JSON 数组，避免为了存多张图再加一张图片表 |
| `t_comment.uk_order_sku` | 用唯一索引实现"一单一评"，而不是靠代码判断，避免并发重复评价 |

### 4.4 索引使用验证

| 查询场景 | SQL 条件 | 使用索引 |
|---|---|---|
| 手机号登录 | `WHERE phone = ?` | `uk_phone` |
| 分类下商品列表 | `WHERE category_id = ? AND status = 1 ORDER BY sales DESC` | `idx_category_status` + 排序需额外 filesort，可在数据量增大后加 `(category_id, status, sales)` |
| 商品详情取 SKU | `WHERE spu_id = ? AND status = 1` | `idx_spu` |
| 用户订单列表 | `WHERE user_id = ? AND status = ? ORDER BY created_at DESC` | `idx_user_status` |
| 超时扫表 | `WHERE status = 0 AND expire_time < NOW()` | `idx_status_expire` |
| 我的优惠券 | `WHERE user_id = ? AND status = ? AND expire_time > NOW()` | `idx_user_status` |

**注意**：`ORDER BY sales DESC` 与 `WHERE category_id=?` 组合，索引 `(category_id, status)` 只能过滤不能排序，会产生 filesort。数据量小时可接受；若商品量超过万级，应改为 `(category_id, status, sales DESC)`。

---

## 5 模块详细设计

### 5.1 用户模块

#### 5.1.1 类清单

| 类 | 类型 | 职责 |
|---|---|---|
| `UserController` | Controller | 注册、登录、信息查询修改、头像上传 |
| `UserService` | 接口 | 用户业务 |
| `UserServiceImpl` | 实现 | 同上 |
| `UserMapper` | Mapper | t_user 数据访问 |
| `AddressController` | Controller | 地址 CRUD |
| `AddressService` | 接口 | 地址业务 |
| `AddressMapper` | Mapper | t_address 数据访问 |

#### 5.1.2 方法清单

```java
public interface UserService {
    /** 注册，返回用户 id */
    Long register(RegisterDTO dto);

    /** 登录，返回 token */
    String login(LoginDTO dto);

    /** 查询当前用户信息 */
    UserVO info(Long userId);

    /** 修改昵称与头像 */
    void updateInfo(Long userId, UserUpdateDTO dto);
}

public interface AddressService {
    List<AddressVO> list(Long userId);
    Long add(Long userId, AddressDTO dto);
    void update(Long userId, Long id, AddressDTO dto);
    void delete(Long userId, Long id);
    Address getOwned(Long userId, Long id);   // 校验归属，越权抛 403
}
```

#### 5.1.3 注册流程

```
接收 RegisterDTO(phone, password, nickname)
  │
  ├─ 参数校验：手机号格式 11 位、密码 6~20 位
  │
  ├─ 查 t_user WHERE phone=?  ──存在──► 抛 10001 手机号已注册
  │
  ├─ BCrypt 加密
  │
  ├─ 插入 t_user（唯一索引兜底并发注册）
  │
  └─ 返回 userId
```

**并发注册处理**：先查再插存在竞态，两个请求同时通过查询。由 `uk_phone` 唯一索引兜底，捕获 `DuplicateKeyException` 转为 10001。

#### 5.1.4 登录流程

```java
public String login(LoginDTO dto) {
    User user = userMapper.selectOne(
        new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
    if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
        throw new BizException(ErrorCode.LOGIN_FAILED, "手机号或密码错误");
    }
    if (user.getStatus() == 0) {
        throw new BizException(ErrorCode.USER_NOT_FOUND, "账号已被禁用");
    }
    return jwtUtil.create(user.getId(), "USER", false);
}
```

**安全要点**：手机号不存在与密码错误返回同一提示，避免被用来枚举已注册手机号。

### 5.2 商品模块

#### 5.2.1 类清单

| 类 | 类型 | 职责 |
|---|---|---|
| `CategoryController` | Controller | 分类树查询 |
| `ProductController` | Controller | 列表、搜索、详情 |
| `AdminProductController` | Controller | 商品与分类的后台管理 |
| `CategoryService` | 接口 | 分类树组装、缓存 |
| `ProductService` | 接口 | 商品查询、SPU/SKU 保存 |
| `SpuMapper` / `SkuMapper` / `CategoryMapper` | Mapper | 数据访问 |

#### 5.2.2 方法清单

```java
public interface CategoryService {
    List<CategoryTreeVO> tree();                 // 带缓存
    List<Category> listAll();
    Long save(CategoryDTO dto);
    void update(Long id, CategoryDTO dto);
    void delete(Long id);                        // 有子分类或有商品时禁止删除
}

public interface ProductService {
    PageResult<ProductListVO> page(ProductQuery query);
    ProductDetailVO detail(Long spuId);
    Long saveSpu(SpuSaveDTO dto);                // 事务：SPU + 批量 SKU
    void updateSpu(Long spuId, SpuSaveDTO dto);
    void updateStatus(Long spuId, Integer status);
}
```

#### 5.2.3 分类树组装算法

```java
public List<CategoryTreeVO> tree() {
    List<Category> all = categoryMapper.selectList(
        new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSort));
    // 一次查库，内存组装，避免递归查询造成 N+1
    Map<Long, List<Category>> byParent = all.stream()
        .collect(Collectors.groupingBy(Category::getParentId));

    return byParent.getOrDefault(0L, Collections.emptyList()).stream()
        .map(c -> {
            CategoryTreeVO vo = new CategoryTreeVO();
            BeanUtils.copyProperties(c, vo);
            vo.setChildren(byParent.getOrDefault(c.getId(), Collections.emptyList())
                .stream().map(x -> {
                    CategoryTreeVO child = new CategoryTreeVO();
                    BeanUtils.copyProperties(x, child);
                    return child;
                }).toList());
            return vo;
        }).toList();
}
```

**复杂度**：一次 SQL + O(n) 内存分组。若每个节点都查一次子分类，50 个分类就是 50 次 SQL。

#### 5.2.4 SPU/SKU 保存事务

```java
@Transactional(rollbackFor = Exception.class)
public Long saveSpu(SpuSaveDTO dto) {
    // 1. 校验分类存在
    if (categoryMapper.selectById(dto.getCategoryId()) == null) {
        throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
    }
    // 2. 校验 SKU 至少一个，且价格合法
    if (CollectionUtils.isEmpty(dto.getSkus())) {
        throw new BizException(ErrorCode.PARAM_ERROR, "至少需要一个规格");
    }

    // 3. 保存 SPU，price_low 取 SKU 最低价
    Spu spu = new Spu();
    BeanUtils.copyProperties(dto, spu);
    spu.setPriceLow(dto.getSkus().stream()
        .map(SkuDTO::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
    spu.setSales(0);
    spu.setStatus(1);
    spuMapper.insert(spu);

    // 4. 批量保存 SKU
    List<Sku> skus = dto.getSkus().stream().map(s -> {
        Sku sku = new Sku();
        BeanUtils.copyProperties(s, sku);
        sku.setSpuId(spu.getId());
        sku.setStatus(1);
        return sku;
    }).toList();
    skuMapper.insertBatch(skus);   // 自定义批量插入，避免逐条 insert

    // 5. 清缓存（新建一般无需，防御性处理）
    redis.delete(RedisKey.item(spu.getId()));
    return spu.getId();
}
```

**批量插入**：MyBatis-Plus 的 `IService.saveBatch` 默认逐条执行，需在 `application.yml` 开启 `rewriteBatchedStatements=true` 才能合并成一条多值 INSERT。

### 5.3 购物车模块

#### 5.3.1 方法清单

```java
public interface CartService {
    void add(Long userId, CartAddDTO dto);
    List<CartItemVO> list(Long userId);
    void updateQuantity(Long userId, Long cartId, Integer quantity);
    void updateChecked(Long userId, List<Long> cartIds, boolean checked);
    void updateCheckedAll(Long userId, boolean checked);
    void delete(Long userId, List<Long> cartIds);
    void clear(Long userId);
    List<CartItem> selectChecked(Long userId);   // 供订单模块调用
    void removeByIds(Long userId, List<Long> cartIds);
}
```

#### 5.3.2 加入购物车

```java
public void add(Long userId, CartAddDTO dto) {
    Sku sku = skuMapper.selectById(dto.getSkuId());
    if (sku == null || sku.getStatus() == 0) {
        throw new BizException(ErrorCode.SKU_NOT_FOUND, "商品不存在或已下架");
    }

    Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
        .eq(Cart::getUserId, userId).eq(Cart::getSkuId, dto.getSkuId()));

    int target = (exist == null ? 0 : exist.getQuantity()) + dto.getQuantity();
    if (target > sku.getStock()) {
        throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足，当前仅剩 " + sku.getStock());
    }

    if (exist == null) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setSkuId(dto.getSkuId());
        cart.setQuantity(dto.getQuantity());
        cart.setChecked(1);
        try {
            cartMapper.insert(cart);
        } catch (DuplicateKeyException e) {
            // 并发加购，唯一索引拦截后退化为更新
            cartMapper.increaseQuantity(userId, dto.getSkuId(), dto.getQuantity());
        }
    } else {
        cartMapper.updateQuantity(exist.getId(), target);
    }
}
```

#### 5.3.3 购物车列表查询

```java
public List<CartItemVO> list(Long userId) {
    List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
        .eq(Cart::getUserId, userId).orderByDesc(Cart::getCreatedAt));
    if (carts.isEmpty()) return Collections.emptyList();

    // 批量取 SKU 与 SPU，避免 N+1
    Set<Long> skuIds = carts.stream().map(Cart::getSkuId).collect(Collectors.toSet());
    Map<Long, Sku> skuMap = skuMapper.selectBatchIds(skuIds).stream()
        .collect(Collectors.toMap(Sku::getId, s -> s));

    Set<Long> spuIds = skuMap.values().stream().map(Sku::getSpuId).collect(Collectors.toSet());
    Map<Long, Spu> spuMap = spuMapper.selectBatchIds(spuIds).stream()
        .collect(Collectors.toMap(Spu::getId, s -> s));

    return carts.stream().map(c -> {
        Sku sku = skuMap.get(c.getSkuId());
        Spu spu = sku == null ? null : spuMap.get(sku.getSpuId());
        CartItemVO vo = new CartItemVO();
        vo.setCartId(c.getId());
        vo.setSkuId(c.getSkuId());
        vo.setQuantity(c.getQuantity());
        vo.setChecked(c.getChecked() == 1);

        if (sku == null || spu == null || sku.getStatus() == 0 || spu.getStatus() == 0) {
            vo.setValid(false);            // 失效：商品下架或删除
            vo.setInvalidReason("商品已下架");
        } else {
            vo.setValid(true);
            vo.setSpuName(spu.getName());
            vo.setSkuName(sku.getSkuName());
            vo.setImage(sku.getImage() != null ? sku.getImage() : spu.getMainImage());
            vo.setPrice(sku.getPrice());   // 实时价格，不存快照
            vo.setStock(sku.getStock());
            vo.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        return vo;
    }).toList();
}
```

**设计要点**：购物车价格实时取 SKU 当前价。理由见概要设计决策 D-03——购物车是"意向"，不是"契约"，用户应看到最新价。

### 5.4 订单模块（核心）

#### 5.4.1 类清单

| 类 | 类型 | 职责 |
|---|---|---|
| `OrderController` | Controller | 前台订单接口 |
| `AdminOrderController` | Controller | 后台订单接口 |
| `OrderService` | 接口 | 订单业务 |
| `OrderServiceImpl` | 实现 | 结算、下单、状态流转 |
| `OrderStateMachine` | 组件 | 状态迁移校验 |
| `StockService` | 接口 | 库存预扣、回补、DB 扣减 |
| `OrderMapper` / `OrderItemMapper` | Mapper | 数据访问 |
| `OrderTimeoutTask` | 定时任务 | 消费延迟队列、扫表兜底 |

#### 5.4.2 方法清单

```java
public interface OrderService {
    /** 结算预览：不落库 */
    OrderPreviewVO preview(Long userId, OrderPreviewDTO dto);

    /** 提交订单 */
    OrderSubmitVO submit(Long userId, OrderSubmitDTO dto);

    /** 订单分页 */
    PageResult<OrderListVO> page(Long userId, Integer status, int page, int size);

    /** 订单详情 */
    OrderDetailVO detail(Long userId, String orderNo);

    /** 用户取消（仅待支付） */
    void cancel(Long userId, String orderNo);

    /** 确认收货 */
    void confirm(Long userId, String orderNo);

    /** 供支付回调调用：标记已支付 */
    void markPaid(String orderNo, LocalDateTime payTime);

    /** 供定时任务调用：未支付则取消 */
    void cancelIfUnpaid(Long orderId);

    /** 供后台调用：发货 */
    void ship(String orderNo, String trackingNo);
}

public interface StockService {
    /** Redis 预扣，成功返回 true */
    boolean preDeduct(Long skuId, int num);

    /** Redis 回补 */
    void restoreCache(Long skuId, int num);

    /** DB 条件扣减，失败抛异常 */
    void deductDb(Long skuId, int num);

    /** DB 回补 */
    void restoreDb(Long skuId, int num);
}
```

#### 5.4.3 结算预览

```java
public OrderPreviewVO preview(Long userId, OrderPreviewDTO dto) {
    // 1. 取勾选的购物车项（若指定 cartIds 则只取这些）
    List<CartItem> items = cartMapper.selectChecked(userId, dto.getCartIds());
    if (items.isEmpty()) throw new BizException(ErrorCode.CART_EMPTY, "请先选择商品");

    // 2. 校验并计算商品总额
    BigDecimal total = BigDecimal.ZERO;
    List<OrderPreviewVO.ItemVO> itemVOs = new ArrayList<>();
    for (CartItem item : items) {
        Sku sku = skuMapper.selectById(item.getSkuId());
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (sku.getStatus() == 0 || spu.getStatus() == 0) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF, spu.getName() + " 已下架");
        }
        if (sku.getStock() < item.getQuantity()) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, spu.getName() + " 库存不足");
        }
        BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        total = total.add(subtotal);
        itemVOs.add(...);
    }

    // 3. 优惠券抵扣
    BigDecimal discount = BigDecimal.ZERO;
    if (dto.getUserCouponId() != null) {
        UserCouponVO uc = couponService.getUsable(userId, dto.getUserCouponId(), total);
        discount = uc.getAmount();
    }

    // 4. 运费：满 49 免运费，否则 6 元
    BigDecimal freight = total.compareTo(new BigDecimal("49")) >= 0
        ? BigDecimal.ZERO : new BigDecimal("6.00");

    // 5. 实付
    BigDecimal payAmount = total.subtract(discount).add(freight).max(BigDecimal.ZERO);

    // 6. 可用券列表（供前端切换）
    List<UserCouponVO> usable = couponService.listUsable(userId, total);

    return OrderPreviewVO.builder()
        .items(itemVOs)
        .totalAmount(total)
        .discountAmount(discount)
        .freight(freight)
        .payAmount(payAmount)
        .usableCoupons(usable)
        .build();
}
```

#### 5.4.4 提交订单（最核心）

```java
@Transactional(rollbackFor = Exception.class)
public OrderSubmitVO submit(Long userId, OrderSubmitDTO dto) {
    // ---------- 阶段一：幂等与校验（无副作用） ----------
    String idemKey = RedisKey.orderIdem(userId, dto.getRequestId());
    Boolean first = redis.opsForValue().setIfAbsent(idemKey, "1", 10, TimeUnit.MINUTES);
    if (!Boolean.TRUE.equals(first)) {
        throw new BizException(ErrorCode.DUPLICATE_SUBMIT, "请勿重复提交");
    }

    Address addr = addressService.getOwned(userId, dto.getAddressId());
    List<CartItem> items = cartMapper.selectChecked(userId, dto.getCartIds());
    if (items.isEmpty()) throw new BizException(ErrorCode.CART_EMPTY, "请先选择商品");

    // 重新计算金额（不信任前端传来的价格）
    BigDecimal total = BigDecimal.ZERO;
    List<Sku> skus = new ArrayList<>();
    for (CartItem item : items) {
        Sku sku = skuMapper.selectById(item.getSkuId());
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu.getStatus() == 0 || sku.getStatus() == 0) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF, spu.getName() + " 已下架");
        }
        total = total.add(sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        skus.add(sku);
    }

    // 校验优惠券并计算实付
    BigDecimal discount = BigDecimal.ZERO;
    UserCoupon uc = null;
    if (dto.getUserCouponId() != null) {
        uc = couponService.lockForUse(userId, dto.getUserCouponId(), total); // 校验+行锁
        discount = uc.getAmount();
    }
    BigDecimal freight = total.compareTo(new BigDecimal("49")) >= 0
        ? BigDecimal.ZERO : new BigDecimal("6.00");
    BigDecimal payAmount = total.subtract(discount).add(freight).max(BigDecimal.ZERO);

    // ---------- 阶段二：Redis 预扣库存（第一道防线） ----------
    List<Long> deducted = new ArrayList<>();
    try {
        for (int i = 0; i < items.size(); i++) {
            Long skuId = items.get(i).getSkuId();
            int num = items.get(i).getQuantity();
            if (!stockService.preDeduct(skuId, num)) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,
                        skus.get(i).getSkuName() + " 库存不足");
            }
            deducted.add(skuId);
        }
    } catch (BizException e) {
        // 回滚已预扣的部分
        for (Long skuId : deducted) {
            int num = findQuantity(items, skuId);
            stockService.restoreCache(skuId, num);
        }
        throw e;
    }

    try {
        // ---------- 阶段三：DB 扣库存（第二道防线，事务内） ----------
        for (int i = 0; i < items.size(); i++) {
            stockService.deductDb(items.get(i).getSkuId(), items.get(i).getQuantity());
        }

        // ---------- 阶段四：建订单 ----------
        Order order = new Order();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFreight(freight);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.UNPAID.getCode());
        order.setReceiver(addr.getReceiver());
        order.setReceiverPhone(addr.getPhone());
        order.setReceiverAddr(addr.getProvince() + addr.getCity()
                + addr.getDistrict() + addr.getDetail());
        order.setRemark(dto.getRemark());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setUserCouponId(uc == null ? null : uc.getId());
        orderMapper.insert(order);

        // ---------- 阶段五：订单明细（商品快照） ----------
        List<OrderItem> orderItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            CartItem ci = items.get(i);
            Sku sku = skus.get(i);
            Spu spu = spuMapper.selectById(sku.getSpuId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setOrderNo(order.getOrderNo());
            oi.setSpuId(spu.getId());
            oi.setSkuId(sku.getId());
            oi.setSpuName(spu.getName());          // 快照
            oi.setSkuName(sku.getSkuName());       // 快照
            oi.setSkuImage(sku.getImage() != null ? sku.getImage() : spu.getMainImage());
            oi.setSpecs(sku.getSpecs());           // 快照
            oi.setPrice(sku.getPrice());           // 快照
            oi.setQuantity(ci.getQuantity());
            oi.setSubtotal(sku.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            orderItems.add(oi);
        }
        orderItemMapper.insertBatch(orderItems);

        // ---------- 阶段六：核销优惠券 ----------
        if (uc != null) {
            couponService.use(uc.getId(), order.getOrderNo());
        }

        // ---------- 阶段七：清购物车 ----------
        cartMapper.deleteBatchIds(items.stream().map(CartItem::getCartId).toList());

        // ---------- 阶段八：事务提交后投递延迟任务 ----------
        Long orderId = order.getId();
        LocalDateTime expireTime = order.getExpireTime();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delayQueue.push(orderId, expireTime);
                }
            });

        return OrderSubmitVO.builder()
            .orderNo(order.getOrderNo())
            .payAmount(payAmount)
            .expireTime(expireTime)
            .build();

    } catch (Exception e) {
        // 事务回滚后，回补 Redis 预扣
        for (Long skuId : deducted) {
            stockService.restoreCache(skuId, findQuantity(items, skuId));
        }
        throw e;
    }
}
```

#### 5.4.5 为什么延迟任务必须在事务提交后投递

若在事务内投递，事务回滚时消息已经进了 ZSet，定时任务会去取消一个不存在的订单；更糟的情况是订单号已生成但订单未落库，取消逻辑查不到数据后静默跳过，而 Redis 库存已经被预扣，形成"幽灵占用"。

`TransactionSynchronization#afterCommit` 保证只有事务真正提交成功才投递。若投递本身失败，由 60 秒扫表任务兜底。

#### 5.4.6 订单状态机

```java
public enum OrderStatus {
    UNPAID(0), UNSHIPPED(1), SHIPPED(2), FINISHED(3), CANCELLED(4), REFUNDING(5);
    private final int code;
    OrderStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}

public final class OrderStateMachine {

    private static final Map<Integer, Set<Integer>> ALLOWED = Map.of(
        OrderStatus.UNPAID.getCode(),    Set.of(OrderStatus.UNSHIPPED.getCode(),
                                                OrderStatus.CANCELLED.getCode()),
        OrderStatus.UNSHIPPED.getCode(), Set.of(OrderStatus.SHIPPED.getCode(),
                                                OrderStatus.REFUNDING.getCode()),
        OrderStatus.SHIPPED.getCode(),   Set.of(OrderStatus.FINISHED.getCode(),
                                                OrderStatus.REFUNDING.getCode()),
        OrderStatus.REFUNDING.getCode(), Set.of(OrderStatus.CANCELLED.getCode(),
                                                OrderStatus.FINISHED.getCode()),
        OrderStatus.FINISHED.getCode(),  Set.of(),
        OrderStatus.CANCELLED.getCode(), Set.of()
    );

    public static void check(int from, int to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERR,
                "订单当前状态不允许该操作");
        }
    }
}
```

**所有状态变更的实际执行**都用条件更新，把状态校验交给数据库：

```java
int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
    .eq(Order::getId, orderId)
    .eq(Order::getStatus, OrderStatus.UNPAID.getCode())   // 关键：带上原状态
    .set(Order::getStatus, OrderStatus.UNSHIPPED.getCode())
    .set(Order::getPayTime, payTime));
if (rows == 0) {
    // 已被其他线程/任务改过（如已取消），视为已处理
    log.info("订单状态已变更，跳过 orderId={}", orderId);
    return;
}
```

#### 5.4.7 超时取消

```java
public void cancelIfUnpaid(Long orderId) {
    Order order = orderMapper.selectById(orderId);
    if (order == null) return;
    if (order.getStatus() != OrderStatus.UNPAID.getCode()) return;

    // 条件更新，与支付回调竞争
    int rows = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
        .eq(Order::getId, orderId)
        .eq(Order::getStatus, OrderStatus.UNPAID.getCode())
        .set(Order::getStatus, OrderStatus.CANCELLED.getCode())
        .set(Order::getCancelTime, LocalDateTime.now())
        .set(Order::getCancelReason, "超时未支付"));
    if (rows == 0) return;   // 已支付或已取消

    // 回滚库存（DB + Redis）
    List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
    for (OrderItem item : items) {
        stockService.restoreDb(item.getSkuId(), item.getQuantity());
        stockService.restoreCache(item.getSkuId(), item.getQuantity());
    }
    // 退还优惠券
    if (order.getUserCouponId() != null) {
        couponService.refund(order.getUserCouponId());
    }
    log.info("订单超时取消成功 orderNo={}", order.getOrderNo());
}
```

**注意**：`cancelIfUnpaid` 本身不加 `@Transactional`，因为它是被定时任务调用的入口方法；若需要事务，应拆成独立 Bean 调用。实际实现中建议给该方法加 `@Transactional` 并把库存回滚放在同一事务内。

#### 5.4.8 延迟队列实现

```java
@Component
@Slf4j
public class OrderDelayQueue {

    @Autowired private StringRedisTemplate redis;
    @Autowired private OrderService orderService;

    private static final String KEY = RedisKey.ORDER_DELAY;

    public void push(Long orderId, LocalDateTime expireTime) {
        long score = expireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redis.opsForZSet().add(KEY, String.valueOf(orderId), score);
    }

    @Scheduled(fixedDelay = 5000)
    public void consume() {
        long now = System.currentTimeMillis();
        Set<String> due = redis.opsForZSet().rangeByScore(KEY, 0, now, 0, 100);
        if (due == null || due.isEmpty()) return;

        for (String orderId : due) {
            // ZREM 返回 1 表示抢到处理权，多实例下防重复
            Long removed = redis.opsForZSet().remove(KEY, orderId);
            if (removed == null || removed == 0) continue;
            try {
                orderService.cancelIfUnpaid(Long.valueOf(orderId));
            } catch (Exception e) {
                log.error("处理超时订单失败 orderId={}", orderId, e);
                // 放回队列，5 分钟后重试
                redis.opsForZSet().add(KEY, orderId,
                    System.currentTimeMillis() + 5 * 60 * 1000);
            }
        }
    }
}
```

#### 5.4.9 扫表兜底

```java
@Scheduled(fixedDelay = 60000)
public void scanExpired() {
    List<Order> expired = orderMapper.selectList(new LambdaQueryWrapper<Order>()
        .eq(Order::getStatus, OrderStatus.UNPAID.getCode())
        .lt(Order::getExpireTime, LocalDateTime.now())
        .last("LIMIT 100"));
    for (Order o : expired) {
        try {
            orderService.cancelIfUnpaid(o.getId());
        } catch (Exception e) {
            log.error("扫表取消失败 orderNo={}", o.getOrderNo(), e);
        }
    }
}
```

### 5.5 支付模块

#### 5.5.1 方法清单

```java
public interface PaymentService {
    /** 发起支付，返回支付单号与模拟支付地址 */
    PayVO createPay(Long userId, String orderNo);

    /** 支付回调，幂等 */
    void notify(String payNo, String sign, BigDecimal amount);

    /** 查询支付状态 */
    Integer queryStatus(Long userId, String orderNo);
}
```

#### 5.5.2 回调幂等设计

```java
public void notify(String payNo, String sign, BigDecimal amount) {
    // 1. 验签
    if (!signValid(payNo, amount, sign)) {
        throw new BizException(ErrorCode.SIGN_INVALID, "签名校验失败");
    }

    Payment payment = paymentMapper.selectOne(
        new LambdaQueryWrapper<Payment>().eq(Payment::getPayNo, payNo));
    if (payment == null) throw new BizException(ErrorCode.NOT_FOUND, "支付单不存在");

    // 2. 金额校验（防止篡改）
    if (payment.getAmount().compareTo(amount) != 0) {
        throw new BizException(ErrorCode.SIGN_INVALID, "金额不一致");
    }

    // 3. 条件更新支付流水：只有待支付才能改为已支付
    int payRows = paymentMapper.update(null, new LambdaUpdateWrapper<Payment>()
        .eq(Payment::getPayNo, payNo)
        .eq(Payment::getStatus, 0)
        .set(Payment::getStatus, 1)
        .set(Payment::getPayTime, LocalDateTime.now()));
    if (payRows == 0) {
        // 已处理过，直接返回成功（幂等）
        return;
    }

    // 4. 订单转待发货
    orderService.markPaid(payment.getOrderNo(), LocalDateTime.now());
}
```

**幂等三层**：验签保证来源可信 → 金额校验保证不被篡改 → 条件更新保证重复回调不重复处理。

### 5.6 优惠券模块

#### 5.6.1 方法清单

```java
public interface CouponService {
    List<CouponVO> available(Long userId);
    void receive(Long userId, Long couponId);
    List<UserCouponVO> mine(Long userId, Integer status);
    List<UserCouponVO> listUsable(Long userId, BigDecimal amount);
    UserCouponVO getUsable(Long userId, Long userCouponId, BigDecimal amount);
    UserCoupon lockForUse(Long userId, Long userCouponId, BigDecimal amount);
    void use(Long userCouponId, String orderNo);
    void refund(Long userCouponId);
}
```

#### 5.6.2 领取实现

```java
public void receive(Long userId, Long couponId) {
    Coupon coupon = couponMapper.selectById(couponId);
    if (coupon == null || coupon.getStatus() == 0) {
        throw new BizException(ErrorCode.NOT_FOUND, "优惠券不存在");
    }
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(coupon.getBeginTime()) || now.isAfter(coupon.getEndTime())) {
        throw new BizException(ErrorCode.COUPON_UNUSABLE, "不在领取时间内");
    }

    // 第一层：快速失败，减少无效请求打到 Redis
    Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
        .eq(UserCoupon::getUserId, userId).eq(UserCoupon::getCouponId, couponId));
    if (exists > 0) throw new BizException(ErrorCode.COUPON_RECEIVED, "您已领取过该券");

    // 第二层：Redis 原子扣减
    Long r = redis.execute(RECEIVE_SCRIPT,
            Collections.singletonList(RedisKey.couponRemain(couponId)));
    if (r == null || r == 0L) {
        throw new BizException(ErrorCode.COUPON_SOLD_OUT, "手慢了，券已领完");
    }

    try {
        // 第三层：DB 条件更新 + 唯一索引兜底
        int rows = couponMapper.decreaseRemain(couponId);   // UPDATE ... WHERE remain > 0
        if (rows == 0) throw new BizException(ErrorCode.COUPON_SOLD_OUT, "券已领完");

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(CouponStatus.UNUSED.getCode());
        uc.setExpireTime(coupon.getEndTime());
        userCouponMapper.insert(uc);
    } catch (DuplicateKeyException e) {
        redis.opsForValue().increment(RedisKey.couponRemain(couponId));
        throw new BizException(ErrorCode.COUPON_RECEIVED, "您已领取过该券");
    } catch (Exception e) {
        redis.opsForValue().increment(RedisKey.couponRemain(couponId));
        throw e;
    }
}
```

Lua 脚本：

```lua
local remain = tonumber(redis.call('GET', KEYS[1]))
if remain == nil then return -1 end
if remain <= 0 then return 0 end
redis.call('DECR', KEYS[1])
return 1
```

#### 5.6.3 核销与退还

```java
public void use(Long userCouponId, String orderNo) {
    int rows = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
        .eq(UserCoupon::getId, userCouponId)
        .eq(UserCoupon::getStatus, CouponStatus.UNUSED.getCode())   // 条件更新
        .set(UserCoupon::getStatus, CouponStatus.USED.getCode())
        .set(UserCoupon::getOrderNo, orderNo)
        .set(UserCoupon::getUsedTime, LocalDateTime.now()));
    if (rows == 0) throw new BizException(ErrorCode.COUPON_UNUSABLE, "优惠券已使用或已过期");
}

public void refund(Long userCouponId) {
    UserCoupon uc = userCouponMapper.selectById(userCouponId);
    if (uc == null) return;
    // 已过期则不退回
    if (LocalDateTime.now().isAfter(uc.getExpireTime())) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
            .eq(UserCoupon::getId, userCouponId)
            .set(UserCoupon::getStatus, CouponStatus.EXPIRED.getCode()));
        return;
    }
    userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
        .eq(UserCoupon::getId, userCouponId)
        .eq(UserCoupon::getStatus, CouponStatus.USED.getCode())
        .set(UserCoupon::getStatus, CouponStatus.UNUSED.getCode())
        .set(UserCoupon::getOrderNo, null));
}
```

### 5.7 评价模块

```java
public interface CommentService {
    void add(Long userId, CommentDTO dto);
    PageResult<CommentVO> pageByProduct(Long spuId, int page, int size);
}
```

**发表评价要点**：

1. 校验订单归属与状态（必须已完成）
2. 校验 `skuId` 属于该订单明细
3. 依赖 `t_comment.uk_order_sku` 唯一索引保证一单一评，捕获 `DuplicateKeyException` 转友好提示
4. 评价后更新 `t_spu.sales` 之外的评分统计（可选，用定时任务汇总避免写热点）

### 5.8 看板模块

| 指标 | SQL 思路 |
|---|---|
| 今日订单数 | `SELECT COUNT(*) FROM t_order WHERE created_at >= CURDATE() AND status != 4` |
| 今日销售额 | `SELECT IFNULL(SUM(pay_amount),0) FROM t_order WHERE pay_time >= CURDATE() AND status IN (1,2,3)` |
| 新增用户 | `SELECT COUNT(*) FROM t_user WHERE created_at >= CURDATE()` |
| 热销 Top10 | `SELECT spu_id, SUM(quantity) q FROM t_order_item oi JOIN t_order o ON oi.order_id=o.id WHERE o.status IN (1,2,3) GROUP BY spu_id ORDER BY q DESC LIMIT 10` |
| 库存预警 | `SELECT * FROM t_sku WHERE stock < 10 AND status = 1` |

**注意**：看板查询会扫较多数据，建议加 5 分钟缓存，不要每次进首页都实时算。

---

## 6 关键算法设计

### 6.1 库存扣减三层防线

| 层 | 位置 | 作用 | 失效后果 |
|---|---|---|---|
| 第一层 | Redis Lua 预扣 | 挡住 90%+ 无效并发，减少 DB 压力 | 请求全部打到 DB，热点行锁导致超时 |
| 第二层 | DB 条件更新 | 保证最终正确性 | 出现超卖 |
| 第三层 | 定时校正 | 修复 Redis 与 DB 的偏差 | 长期运行后 Redis 库存失真 |

**第二层的核心 SQL**：

```sql
UPDATE t_sku SET stock = stock - #{num} WHERE id = #{skuId} AND stock >= #{num}
```

**为什么不用 version**：库存扣减是纯减法，`stock >= num` 已经是原子条件校验；`version` 适用于"读出 → 业务计算 → 写回"的场景（如积分、余额），对库存纯属多余的重试成本。

**为什么不用悲观锁**：`SELECT ... FOR UPDATE` 会让同一 SKU 的所有下单请求串行排队，热点商品下 QPS 会降到个位数。

### 6.2 订单号生成

```java
private String generateOrderNo(Long userId) {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String uid = String.format("%06d", userId % 1000000);
    String rand = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    return time + uid + rand;   // 14 + 6 + 6 = 26 位，补齐到 32 位不足补 0
}
```

**为什么不用自增 id**：自增 id 会暴露订单量（竞对可通过订单号推算日单量），且分库后不唯一。用"时间 + 用户 + 随机"可读性好，最终由 `uk_order_no` 保证唯一。

### 6.3 金额计算

| 规则 | 实现 |
|---|---|
| 商品总额 | Σ(sku.price × quantity) |
| 优惠抵扣 | 券面额，不超过商品总额 |
| 运费 | 商品总额 ≥ 49 元免运费，否则 6 元 |
| 实付 | max(总额 - 优惠 + 运费, 0) |

**所有中间计算使用 `BigDecimal`**，`setScale(2, RoundingMode.HALF_UP)` 统一保留两位。禁止在金额上使用 `double`。

### 6.4 缓存三大问题应对

| 问题 | 场景 | 方案 |
|---|---|---|
| 穿透 | 查询不存在的 spuId | 缓存空值 `"NULL"`，TTL 5 分钟 |
| 击穿 | 热点商品缓存过期瞬间 | 回源前用 `SETNX lock:item:{id}` 互斥，只有拿到锁的请求查库 |
| 雪崩 | 大量 key 同时过期 | TTL = 30min + `Random(0, 300)` 秒 |

```java
public ProductDetailVO detail(Long spuId) {
    String key = RedisKey.item(spuId);
    String cached = redis.opsForValue().get(key);
    if ("NULL".equals(cached)) {
        throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
    }
    if (cached != null) return JSON.parseObject(cached, ProductDetailVO.class);

    // 互斥锁防击穿
    String lockKey = "cm:lock:item:" + spuId;
    boolean locked = Boolean.TRUE.equals(
        redis.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS));
    try {
        if (!locked) {
            Thread.sleep(50);
            String retry = redis.opsForValue().get(key);
            if (retry != null && !"NULL".equals(retry)) {
                return JSON.parseObject(retry, ProductDetailVO.class);
            }
        }
        ProductDetailVO vo = loadFromDb(spuId);
        int ttl = 1800 + ThreadLocalRandom.current().nextInt(300);
        if (vo == null) {
            redis.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        redis.opsForValue().set(key, JSON.toJSONString(vo), ttl, TimeUnit.SECONDS);
        return vo;
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BizException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
    } finally {
        if (locked) redis.delete(lockKey);
    }
}
```

---

## 7 接口详细定义

### 7.1 用户登录

**请求**

```
POST /api/user/login
Content-Type: application/json

{
  "phone": "13800138000",
  "password": "123456"
}
```

**响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1001,
    "nickname": "小明"
  }
}
```

**错误**：`10002` 手机号或密码错误；`10003` 账号已禁用；`429` 登录过于频繁。

### 7.2 商品详情

**请求**

```
GET /api/product/10086
```

**响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "spuId": 10086,
    "categoryId": 3,
    "name": "磨砂手机保护壳",
    "subTitle": "防摔抗指纹，四角气囊",
    "mainImage": "https://xxx.oss-cn-beijing.aliyuncs.com/p/10086.jpg",
    "images": ["https://.../1.jpg", "https://.../2.jpg"],
    "detail": "<p>产品参数...</p>",
    "priceLow": 19.90,
    "sales": 328,
    "specs": [
      { "name": "颜色", "values": ["黑色", "白色"] },
      { "name": "型号", "values": ["iPhone 15", "iPhone 15 Pro"] }
    ],
    "skus": [
      {
        "skuId": 20001,
        "skuName": "黑色-iPhone 15",
        "specs": { "颜色": "黑色", "型号": "iPhone 15" },
        "price": 19.90,
        "stock": 100,
        "image": "https://.../black.jpg"
      },
      {
        "skuId": 20002,
        "skuName": "白色-iPhone 15 Pro",
        "specs": { "颜色": "白色", "型号": "iPhone 15 Pro" },
        "price": 22.90,
        "stock": 0,
        "image": "https://.../white.jpg"
      }
    ]
  }
}
```

**设计说明**：`specs`（规格项）用于渲染选择器，`skus`（具体组合）用于定位价格与库存。前端交互是"选规格 → 匹配到 skuId"，而不是"选规格 → 计算价格"。

### 7.3 加入购物车

**请求**

```
POST /api/cart
Authorization: Bearer <token>

{ "skuId": 20001, "quantity": 2 }
```

**响应**

```json
{ "code": 0, "msg": "ok", "data": null }
```

**错误**：`20002` 商品不存在或已下架；`30002` 库存不足（msg 中带上剩余库存）。

### 7.4 结算预览

**请求**

```
POST /api/order/preview
Authorization: Bearer <token>

{
  "cartIds": [1, 2],
  "userCouponId": 5001
}
```

**响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "items": [
      { "skuId": 20001, "spuName": "磨砂手机保护壳", "skuName": "黑色-iPhone 15",
        "image": "https://...", "price": 19.90, "quantity": 2, "subtotal": 39.80 }
    ],
    "totalAmount": 39.80,
    "discountAmount": 5.00,
    "freight": 6.00,
    "payAmount": 40.80,
    "usableCoupons": [
      { "userCouponId": 5001, "couponId": 1, "name": "满 30 减 5",
        "amount": 5.00, "threshold": 30.00, "expireTime": "2026-11-30 23:59:59", "usable": true }
    ]
  }
}
```

### 7.5 提交订单

**请求**

```
POST /api/order/submit
Authorization: Bearer <token>

{
  "requestId": "a1b2c3d4-uuid",
  "cartIds": [1, 2],
  "addressId": 88,
  "userCouponId": 5001,
  "remark": "放宿舍楼下"
}
```

**响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "orderNo": "202610191835201001234567",
    "payAmount": 40.80,
    "expireTime": "2026-10-19 19:05:20"
  }
}
```

**错误**：`40009` 重复提交；`30002` 库存不足；`20001` 商品已下架；`60003` 优惠券不可用。

`requestId` 由前端生成 UUID，同一次结算页面的多次点击携带同一个值，服务端用它做幂等键。

### 7.6 订单列表

**请求**

```
GET /api/order/list?status=0&page=1&size=10
Authorization: Bearer <token>
```

**响应**

```json
{
  "code": 0,
  "msg": "ok",
  "data": {
    "total": 12,
    "pages": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "orderNo": "202610191835201001234567",
        "status": 0,
        "statusText": "待支付",
        "payAmount": 40.80,
        "itemCount": 2,
        "items": [
          { "spuName": "磨砂手机保护壳", "skuName": "黑色-iPhone 15",
            "image": "https://...", "price": 19.90, "quantity": 2 }
        ],
        "expireTime": "2026-10-19 19:05:20",
        "remainSeconds": 1200,
        "createdAt": "2026-10-19 18:35:20"
      }
    ]
  }
}
```

`remainSeconds` 由服务端算好，前端直接用做倒计时，避免客户端时钟不准。

### 7.7 领取优惠券

**请求**

```
POST /api/coupon/5001/receive
Authorization: Bearer <token>
```

**响应**

```json
{ "code": 0, "msg": "ok", "data": null }
```

**错误**：`60001` 已领完；`60002` 已领取过；`60003` 不在领取时间。

### 7.8 支付回调

**请求**

```
POST /api/pay/notify
Content-Type: application/json

{
  "payNo": "PAY202610191840001234",
  "amount": 40.80,
  "sign": "md5(payNo + amount + secret)",
  "timestamp": 1760870400000
}
```

**响应**

```json
{ "code": 0, "msg": "ok", "data": "success" }
```

**幂等要求**：同一 `payNo` 重复回调必须返回成功，且不产生重复业务。

---

## 8 前端设计

### 8.1 页面清单

| 页面 | 路由 | 说明 |
|---|---|---|
| 首页 | `/` | 分类导航 + 推荐商品 |
| 分类页 | `/category/:id` | 商品列表、排序筛选 |
| 搜索页 | `/search?kw=` | 搜索结果 |
| 商品详情 | `/product/:spuId` | 规格选择、加购、立即购买 |
| 购物车 | `/cart` | 勾选、改数量、结算 |
| 结算页 | `/checkout` | 收货地址、优惠券、金额明细 |
| 订单列表 | `/orders` | 按状态 tab |
| 订单详情 | `/order/:orderNo` | 状态、倒计时、支付/取消 |
| 我的 | `/mine` | 个人信息、地址、优惠券 |
| 登录/注册 | `/login` `/register` | — |
| 后台-登录 | `/admin/login` | — |
| 后台-商品 | `/admin/product` | 列表、编辑、SKU 管理 |
| 后台-订单 | `/admin/order` | 列表、发货、退款 |
| 后台-看板 | `/admin/dashboard` | 数据卡片、热销榜 |

### 8.2 前端状态管理

| Store | 存放内容 |
|---|---|
| `useUserStore` | token、用户信息、登录状态 |
| `useCartStore` | 购物车条目、选中数、总价 |
| `useOrderStore` | 结算页临时数据（地址、券、金额） |

**token 存储**：`localStorage`，请求拦截器统一注入 `Authorization`。响应码 401 时清空并跳登录页。

### 8.3 请求封装约定

```js
// src/api/request.js
import axios from 'axios'

const request = axios.create({ baseURL: '/api', timeout: 10000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  res => {
    const { code, msg, data } = res.data
    if (code === 0) return data
    if (code === 401) {
      localStorage.removeItem('token')
      location.href = '/login'
    }
    return Promise.reject(new Error(msg))
  },
  err => Promise.reject(err)
)
```

### 8.4 关键交互设计

| 交互 | 要求 |
|---|---|
| 规格选择 | 选择规格项后高亮，无库存的组合置灰且不可选 |
| 加入购物车 | 成功后顶部 Toast + 购物车角标 +1，不跳转 |
| 结算倒计时 | 用服务端返回的 `remainSeconds` 本地递减，归零时刷新订单详情 |
| 支付按钮 | 点击后禁用并 loading，防止重复点击 |
| 订单列表 | 待支付订单显示倒计时；已完成订单显示"去评价" |

---

## 9 错误处理与日志

### 9.1 日志规范

| 场景 | 级别 | 内容要求 |
|---|---|---|
| 接口访问 | INFO | `method path cost userId ip` |
| 下单成功 | INFO | `orderNo userId payAmount` |
| 支付成功 | INFO | `orderNo payNo amount` |
| 订单取消 | INFO | `orderNo reason` |
| 业务异常 | WARN | `code msg` |
| 系统异常 | ERROR | 完整堆栈 + 入参摘要 |

**脱敏规则**：手机号显示为 `138****8000`，地址只保留省市区。

### 9.2 兜底补偿清单

| 场景 | 补偿任务 | 频率 |
|---|---|---|
| ZSet 消息丢失 | 扫表查 `status=0 AND expire_time<NOW()` | 60 秒 |
| Redis 库存偏差 | 按 `DB 库存 - 未支付占用` 校正 | 10 分钟 |
| 订单已取消但库存未回滚 | 检查 `status=4 且 无对应的回补记录`（可加补偿日志表） | 手动 |

---

## 10 测试设计

### 10.1 单元测试要点

| 被测对象 | 用例 |
|---|---|
| `OrderStateMachine.check` | 合法流转通过；非法流转抛异常；终态不可再变 |
| 金额计算 | 满减券、无门槛券、免运费临界值 49.00 |
| `generateOrderNo` | 长度正确、格式正确、万次生成无重复 |
| 购物车数量累加 | 超过库存时抛 30002 |

### 10.2 并发测试用例

| 用例 | 方法 | 预期 |
|---|---|---|
| 超卖测试 | JMeter 100 线程同时下单同一 SKU（库存 10） | 成功 10 单，库存 0，无负数 |
| 重复提交 | 同一 requestId 并发 10 次 | 只生成 1 个订单 |
| 领券超发 | 1000 线程抢 100 张券 | 成功 100 张，Redis 与 DB remain 均为 0 |
| 支付回调幂等 | 同一 payNo 回调 10 次 | 订单状态只变一次，无重复流水 |
| 超时与支付竞争 | 超时任务触发的同时调支付回调 | 只有一个成功，另一个影响行数为 0 |
| 缓存击穿 | 缓存过期瞬间 200 并发请求详情 | DB 只被查询一次 |

### 10.3 验收标准

- 上述并发用例全部通过
- 商品详情 P99 ≤ 200ms
- 下单 P99 ≤ 800ms
- 所有接口错误返回统一格式，无堆栈泄露

---

## 11 附录

### 11.1 依赖版本清单

> **版本说明（2026-10 更新）**：start.spring.io 已不再提供 3.x，最低可选 4.0.8。MyBatis-Plus 自 3.5.15 起提供 `mybatis-plus-spring-boot4-starter`，正式兼容 Spring Boot 4，因此本项目直接采用 Boot 4.0.8，不再降级。

| 依赖 | 版本 | 坐标说明 |
|---|---|---|
| spring-boot-starter-parent | **4.0.8** | 不要选 4.1.x / SNAPSHOT / M2 |
| mybatis-plus-spring-boot4-starter | **3.5.17** | Boot 4 专用坐标，用 boot3 版本会启动失败 |
| mybatis-plus-jsqlparser | **3.5.17** | **必须单独引入**，否则分页插件不可用（3.5.9 起拆出） |
| mysql-connector-j | 8.4.0 | — |
| spring-boot-starter-data-redis | 4.0.8 | 跟随 Boot 版本 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | — |
| lombok | 1.18.34 | — |
| hutool-all | 5.8.32 | 注意 Boot 4 使用 Jackson 3，包名为 `tools.jackson` |
| aliyun-sdk-oss | 3.17.4 | — |

**接口文档说明**：Knife4j 4.x 依赖 springdoc-openapi 2.x（面向 Boot 3），在 Boot 4 下不可用。本项目改用 **Apifox / Postman** 做接口调试，不引入文档框架。若后续需要，等 springdoc 3.x 稳定后再接入。

**pom.xml 关键片段**：

```xml
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.17</mybatis-plus.version>
</properties>

<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>

<!-- 分页拦截器依赖，缺失会导致 PaginationInnerInterceptor 无法使用 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-jsqlparser</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
```


### 11.2 Redis Key 完整清单

| Key 模板 | 类型 | TTL | 说明 |
|---|---|---|---|
| `cm:stock:sku:{skuId}` | String | 永久 | 预扣库存 |
| `cm:order:delay` | ZSet | 无 | 延迟队列 |
| `cm:order:idem:{userId}:{requestId}` | String | 10min | 下单幂等 |
| `cm:item:{spuId}` | String | 30min+随机 | 商品详情缓存 |
| `cm:category:tree` | String | 永久 | 分类树 |
| `cm:coupon:remain:{couponId}` | String | 永久 | 券剩余量 |
| `cm:lock:item:{spuId}` | String | 10s | 缓存击穿互斥锁 |

### 11.3 开发顺序建议

1. 公共模块（Result、异常、JWT、上下文、全局异常）
2. 用户模块（注册登录跑通 → 打通拦截器）
3. 分类 + 商品（含 OSS 图片上传）
4. 购物车
5. 订单（先做单商品直购，跑通后再接购物车）
6. 支付（模拟）
7. 超时取消（ZSet）
8. 优惠券
9. 评价
10. 后台
11. 缓存优化与压测
12. 部署

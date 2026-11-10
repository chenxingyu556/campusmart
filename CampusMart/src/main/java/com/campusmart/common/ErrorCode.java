package com.campusmart.common;

public final  class ErrorCode {
    private ErrorCode() {
    }

    // ========== 通用 ==========
    public static final int SUCCESS = 0;
    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int SYSTEM_ERROR = 500;

    // ========== 用户 1xxxx ==========
    public static final int PHONE_EXISTS = 10001;
    public static final int LOGIN_FAILED = 10002;
    public static final int USER_NOT_FOUND = 10003;
    public static final int USER_DISABLED = 10004;
    public static final int ADDRESS_NOT_FOUND = 10005;

    // ========== 商品 2xxxx ==========
    public static final int PRODUCT_OFF_SHELF = 20001;
    public static final int SKU_NOT_FOUND = 20002;
    public static final int CATEGORY_NOT_EMPTY = 20003;

    // ========== 购物车 3xxxx ==========
    public static final int CART_EMPTY = 30001;
    public static final int STOCK_NOT_ENOUGH = 30002;

    // ========== 订单 4xxxx ==========
    public static final int ORDER_NOT_FOUND = 40001;
    public static final int ORDER_STATUS_ERROR = 40002;
    public static final int ORDER_EXPIRED = 40003;
    public static final int DUPLICATE_SUBMIT = 40004;

    // ========== 支付 5xxxx ==========
    public static final int PAY_ALREADY_DONE = 50001;
    public static final int SIGN_INVALID = 50002;

    // ========== 优惠券 6xxxx ==========
    public static final int COUPON_SOLD_OUT = 60001;
    public static final int COUPON_RECEIVED = 60002;
    public static final int COUPON_UNUSABLE = 60003;
}

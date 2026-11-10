package com.campusmart.common;

public final class OrderStatus {

    private OrderStatus() {}

    public static final int UNPAID    = 10;   // 待支付
    public static final int PAID      = 20;   // 已支付
    public static final int SHIPPED   = 30;   // 已发货
    public static final int COMPLETED = 40;   // 已完成
    public static final int CANCELLED = 50;   // 已取消（用户主动）
    public static final int CLOSED    = 60;   // 已关闭（超时未支付）
}

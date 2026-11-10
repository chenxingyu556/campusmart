package com.campusmart.common;

public final class StockBizType {

    private StockBizType() {}

    public static final int DEDUCT  = 1;   // 下单扣减
    public static final int RESTORE = 2;   // 取消 / 超时归还
}

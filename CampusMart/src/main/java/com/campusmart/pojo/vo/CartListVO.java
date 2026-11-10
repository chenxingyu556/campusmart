package com.campusmart.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartListVO {
    private List<CartItemVO> items = new ArrayList<>();
    private Integer totalQuantity;       // 购物车总件数（所有商品数量之和）
    private BigDecimal selectedAmount;   // 已勾选商品的合计金额
}

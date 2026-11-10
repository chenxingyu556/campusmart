package com.campusmart.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long id;            // 购物车行 id —— 改数量/删除要用它
    private Long skuId;
    private String skuName;
    private Long spuId;
    private String spuName;
    private String mainImage;
    private BigDecimal price;
    private Integer quantity;
    private Integer stock;
    private Boolean selected;
    private Boolean invalid;    // 已下架 或 库存为 0
}

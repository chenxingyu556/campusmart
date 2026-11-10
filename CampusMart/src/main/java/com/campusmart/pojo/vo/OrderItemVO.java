package com.campusmart.pojo.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private Long spuId;
    private Long skuId;
    private String spuName;
    private String skuName;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
}

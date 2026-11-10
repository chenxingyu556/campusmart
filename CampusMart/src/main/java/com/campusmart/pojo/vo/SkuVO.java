package com.campusmart.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private String image;
}

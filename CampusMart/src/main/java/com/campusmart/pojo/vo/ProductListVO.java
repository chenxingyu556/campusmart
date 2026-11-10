package com.campusmart.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductListVO {
    private Long id;
    private String name;
    private String subTitle;
    private String mainImage;
    private BigDecimal priceLow;
    private Integer sales;
}

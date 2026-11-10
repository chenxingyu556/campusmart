package com.campusmart.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDetailVO {
    private Long id;
    private Long categoryId;
    private String name;
    private String subTitle;
    private String mainImage;
    private String detail;
    private BigDecimal priceLow;
    private Integer sales;

    private List<SkuVO> skus = new ArrayList<>();
}

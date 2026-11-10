package com.campusmart.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {
    private Long id;
    private String name;
    private String icon;
    private List<CategoryTreeVO> children = new ArrayList<>();
}

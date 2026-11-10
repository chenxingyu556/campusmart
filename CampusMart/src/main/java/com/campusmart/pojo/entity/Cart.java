package com.campusmart.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Boolean selected;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

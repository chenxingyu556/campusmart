package com.campusmart.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_stock_log")
public class StockLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Integer bizType;
    private String bizNo;
    private Integer changeNum;
    private LocalDateTime createdAt;
}

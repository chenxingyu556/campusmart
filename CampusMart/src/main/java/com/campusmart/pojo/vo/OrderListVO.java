package com.campusmart.pojo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListVO {
    private Long orderId;
    private String orderNo;
    private Integer status;
    private String statusDesc;
    private BigDecimal payAmount;
    private Integer totalCount;
    private String firstImage;
    private String firstSpuName;
    private LocalDateTime createdAt;
    private LocalDateTime expireTime;
}

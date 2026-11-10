package com.campusmart.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderSubmitVO {
    private Long orderId;
    private String orderNo;
    private BigDecimal payAmount;
    private LocalDateTime expireTime;
}

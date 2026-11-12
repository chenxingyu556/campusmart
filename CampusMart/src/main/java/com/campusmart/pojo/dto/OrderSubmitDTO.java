package com.campusmart.pojo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderSubmitDTO {

    @NotNull(message = "商品规格不能为空")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "最多购买 99 件")
    private Integer quantity;

    @NotNull(message = "请选择收货地址")
    private Long addressId;

    // 以下三项由后端根据 addressId 回填，前端无需传（保留字段兼容旧调用）
    @Size(max = 50, message = "收货人最长 50 字")
    private String receiverName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @Size(max = 255, message = "地址最长 255 字")
    private String receiverAddress;
}

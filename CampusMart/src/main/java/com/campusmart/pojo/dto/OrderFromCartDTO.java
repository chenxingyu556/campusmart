package com.campusmart.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderFromCartDTO {

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

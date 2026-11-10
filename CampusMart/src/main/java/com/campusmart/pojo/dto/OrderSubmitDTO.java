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

    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人最长 50 字")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "地址最长 255 字")
    private String receiverAddress;
}

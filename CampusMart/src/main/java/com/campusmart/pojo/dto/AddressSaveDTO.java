package com.campusmart.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressSaveDTO {
    @NotBlank(message = "收货人不能为空")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "省份不能为空") private String province;
    @NotBlank(message = "城市不能为空") private String city;
    @NotBlank(message = "区县不能为空") private String district;
    @NotBlank(message = "详细地址不能为空") private String detail;

    private Boolean isDefault;      // 可不传，默认 false
}

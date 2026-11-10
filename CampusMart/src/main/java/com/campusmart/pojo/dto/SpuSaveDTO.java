package com.campusmart.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SpuSaveDTO {

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称最长 200 字")
    private String name;

    @Size(max = 500, message = "卖点最长 500 字")
    private String subTitle;

    private String mainImage;

    private String detail;

    @NotEmpty(message = "至少需要一个规格")
    @Valid                                  // ← 关键
    private List<SkuSaveDTO> skus;
}

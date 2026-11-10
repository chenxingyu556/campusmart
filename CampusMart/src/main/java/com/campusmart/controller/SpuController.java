package com.campusmart.controller;

import com.campusmart.common.PageResult;
import com.campusmart.common.Result;
import com.campusmart.pojo.vo.ProductDetailVO;
import com.campusmart.pojo.vo.ProductListVO;
import com.campusmart.service.SpuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/product")
@RestController
public class SpuController {
    private final SpuService spuService;

    public SpuController(SpuService spuService){
        this.spuService = spuService;
    }


    @GetMapping("/{spuId}")
    public Result<ProductDetailVO> detail(@PathVariable Long spuId){
        return Result.ok(spuService.detail(spuId));
    }

    @GetMapping("/list")
    public Result<PageResult<ProductListVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String keyword){
        return Result.ok(spuService.list(page, size, categoryId, sort, keyword));
    }
}

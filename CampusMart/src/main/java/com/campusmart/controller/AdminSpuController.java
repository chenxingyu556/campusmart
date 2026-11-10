package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.pojo.dto.SpuSaveDTO;
import com.campusmart.service.SpuService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/admin/product")
@RestController
public class AdminSpuController {

    private final SpuService spuService;

    public AdminSpuController(SpuService spuService){
        this.spuService = spuService;
    }

    @PostMapping
    public Result<Long> save(@RequestBody @Valid SpuSaveDTO dto){
        return Result.ok(spuService.save(dto));
    }
}

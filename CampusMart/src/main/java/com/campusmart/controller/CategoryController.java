package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.pojo.vo.CategoryTreeVO;
import com.campusmart.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/api/category")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree(){
        return Result.ok(categoryService.tree());
    }
}

package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmart.common.ProductStatus;
import com.campusmart.mapper.CategoryMapper;
import com.campusmart.pojo.entity.Category;
import com.campusmart.pojo.vo.CategoryTreeVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper){
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryTreeVO> tree() {

        // 1. 一次查出所有启用的分类
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, ProductStatus.ON_SHELF)
                        .orderByAsc(Category::getSort));

        // 2. 建立"父 id → 子列表"索引
        Map<Long, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        // 3. 从一级分类（parentId = 0）开始组装
        List<CategoryTreeVO> result = new ArrayList<>();
        for (Category top : byParent.getOrDefault(0L, List.of())) {
            CategoryTreeVO vo = toVo(top);

            // 4. 从索引里取自己的孩子，没有就留空列表
            List<Category> children = byParent.getOrDefault(top.getId(), List.of());
            vo.setChildren(children.stream().map(this::toVo).toList());

            result.add(vo);
        }
        return result;
    }

    private CategoryTreeVO toVo(Category c) {
        CategoryTreeVO childrenVo = new CategoryTreeVO();
        childrenVo.setId(c.getId());
        childrenVo.setName(c.getName());
        childrenVo.setIcon(c.getIcon());
        return childrenVo;
    }

}

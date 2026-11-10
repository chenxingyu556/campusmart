package com.campusmart.service;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.common.PageResult;
import com.campusmart.pojo.vo.ProductDetailVO;
import com.campusmart.pojo.vo.ProductListVO;
import com.campusmart.pojo.vo.SkuVO;
import org.springframework.beans.BeanUtils;
import com.campusmart.common.BizException;
import com.campusmart.common.ErrorCode;
import com.campusmart.common.ProductStatus;
import com.campusmart.mapper.CategoryMapper;
import com.campusmart.mapper.SkuMapper;
import com.campusmart.mapper.SpuMapper;
import com.campusmart.pojo.dto.SkuSaveDTO;
import com.campusmart.pojo.dto.SpuSaveDTO;
import com.campusmart.pojo.entity.Category;
import com.campusmart.pojo.entity.Sku;
import com.campusmart.pojo.entity.Spu;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Service
public class SpuService {

    private static final String KEY_PREFIX = "product:detail:";
    private static final String LOCK_PREFIX = "lock:product:";

    private final SpuMapper spuMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate stringRedisTemplate;


    public SpuService(SpuMapper spuMapper, CategoryMapper categoryMapper, SkuMapper skuMapper, StringRedisTemplate stringRedisTemplate){
        this.spuMapper = spuMapper;
        this.categoryMapper = categoryMapper;
        this.skuMapper = skuMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long save(SpuSaveDTO dto) {
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null || category.getStatus() == ProductStatus.OFF_SHELF){
            throw new BizException(ErrorCode.NOT_FOUND,"分类不存在或已停用");
        }

        // 第 1 步：算最低价（来自所有 SKU）
        BigDecimal priceLow = dto.getSkus().stream()
                .map(SkuSaveDTO::getPrice)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 第 2 步：存 SPU，拿到自增 id
        Spu spu = new Spu();
        BeanUtils.copyProperties(dto,spu);
        spu.setPriceLow(priceLow);
        spu.setSales(0);
        spu.setStatus(ProductStatus.ON_SHELF);
        spuMapper.insert(spu);

        // 第 3 步：用这个 id 存所有 SKU
        for (SkuSaveDTO skuSaveDTO : dto.getSkus()){
            Sku sku = new Sku();
            BeanUtils.copyProperties(skuSaveDTO,sku);
            sku.setSpuId(spu.getId());
            sku.setStatus(ProductStatus.ON_SHELF);
            skuMapper.insert(sku);
        }
        return spu.getId();
    }

    public ProductDetailVO detail(Long spuId){
        String key = KEY_PREFIX + spuId;

        ProductDetailVO cache = readCache(key);
        if (cache != null) {
            return cache;
        }

        String lockKey = LOCK_PREFIX + spuId;
        boolean locked = false;

        try {
            for (int i = 0; i < 3; i++) {
                locked = Boolean.TRUE.equals(stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS));

                if (locked) break;

                Thread.sleep(50);
                cache = readCache(key);
                if (cache != null) return cache;
            }

            if (locked){
                cache = readCache(key);
                if (cache !=null) return cache;
            }

            ProductDetailVO vo = loadFromDb(spuId);

            if (vo == null){
                stringRedisTemplate.opsForValue()
                        .set(key,"",2,TimeUnit.MINUTES);
                throw new BizException(ErrorCode.NOT_FOUND,"商品不存在或已下架");
            }

            int ttl = 1800 + ThreadLocalRandom.current().nextInt(300);

            stringRedisTemplate.opsForValue()
                    .set(key,JSONUtil.toJsonStr(vo),ttl, TimeUnit.SECONDS);

            return vo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (locked) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    private ProductDetailVO readCache(String key){
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached == null) {return null;}

            if (cached.isEmpty()){
                throw new BizException(ErrorCode.NOT_FOUND,"商品不存在或已下架");
            }

            return JSONUtil.toBean(cached,ProductDetailVO.class);
    }

    private ProductDetailVO loadFromDb(Long spuId) {
        // 1. 查 SPU（下架的商品当不存在处理）
        Spu spu = spuMapper.selectById(spuId);
        if (spu == null || spu.getStatus() == ProductStatus.OFF_SHELF){
            return null;
        }

        // 2. 查它的所有 SKU（只要上架的）
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getSpuId, spuId)
                .eq(Sku::getStatus, ProductStatus.ON_SHELF)
                .orderByAsc(Sku::getId));

        // 3. 拼 VO
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtils.copyProperties(spu,vo);
        vo.setSkus(skus.stream().map(this::toSkuVo).toList());
        return vo;
    }

    private SkuVO toSkuVo(Sku sku) {
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            return skuVO;
    }

    public PageResult<ProductListVO> list(Integer page, Integer size, Long categoryId, String sort, String keyword){

        // 1. 参数兜底（page/size 的边界处理）
        if (page == null || page <1){
            page = 1;
        }
        if (size == null || size <1){
            size = 10;
        }
        if (size >50){
            size = 50;
        }

        // 2. 组装查询条件
        //    - .eq(Spu::getStatus, ProductStatus.ON_SHELF)   ← 只查上架
        //    - .select(...)                                   ← 排除 detail
        LambdaQueryWrapper<Spu> wrapper = new LambdaQueryWrapper<Spu>()
                .eq(Spu::getStatus, ProductStatus.ON_SHELF)
                .select(Spu::getId, Spu::getName, Spu::getSubTitle,
                        Spu::getMainImage, Spu::getPriceLow, Spu::getSales);

        //    - categoryId 不为空时：查子分类 → .in(...)
        if(categoryId != null){
            List<Long> categoryIds = new ArrayList<>();
            categoryIds.add(categoryId);
            List<Category> childCategory = categoryMapper
                    .selectList(new LambdaQueryWrapper<Category>()
                            .eq(Category::getParentId, categoryId));
            childCategory.forEach(c ->categoryIds.add(c.getId()));

            wrapper.in(Spu::getCategoryId,categoryIds);
        }

        wrapper.like(StringUtils.hasText(keyword),Spu::getName,keyword);

        //    - 排序白名单
        switch (sort == null ? "" : sort){
            case "sales" -> wrapper.orderByDesc(Spu::getSales).orderByDesc(Spu::getId);
            case "price_asc" ->wrapper.orderByAsc(Spu::getPriceLow).orderByDesc(Spu::getId);
            case "price_desc" ->wrapper.orderByDesc(Spu::getPriceLow).orderByDesc(Spu::getId);
            default -> wrapper.orderByDesc(Spu::getId);
        }

        // 3. 分页查询
        Page<Spu> p = new Page<>(page, size);
        spuMapper.selectPage(p, wrapper);

        // 4. 转 VO + 组装 PageResult
        List<ProductListVO> vos = p.getRecords().stream()
                .map(this::toListVo).toList();

        return PageResult.of(p.getTotal(), p.getPages(), p.getCurrent(), size, vos);
    }

    private ProductListVO toListVo(Spu spu) {
        ProductListVO vo = new ProductListVO();
        BeanUtils.copyProperties(spu,vo);
        return vo;
    }

}

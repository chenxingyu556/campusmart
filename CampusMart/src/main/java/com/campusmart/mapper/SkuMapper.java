package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.pojo.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    @Update("UPDATE t_sku SET stock = stock - #{num} WHERE id = #{skuId} AND stock >= #{num}")
    int deductStock(@Param("skuId") Long skuId, @Param("num") Integer num);

    @Update("UPDATE t_sku SET stock = stock + #{num} WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("num") Integer num);
}

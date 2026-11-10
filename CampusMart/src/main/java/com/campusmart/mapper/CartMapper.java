package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.pojo.entity.Cart;
import com.campusmart.pojo.vo.CartItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 加入购物车：没有就插入，有就累加数量
     * @return 影响行数（1=新插入，2=累加更新，0=值未变化）
     */
    @Insert("""
            INSERT INTO t_cart (user_id, sku_id, quantity, selected)
            VALUES (#{userId}, #{skuId}, #{quantity}, 1)
            ON DUPLICATE KEY UPDATE quantity = quantity + #{quantity}
            """)
    int addOrIncrease(@Param("userId") Long userId,
                      @Param("skuId") Long skuId,
                      @Param("quantity") Integer quantity);


    /** 购物车列表：联表带出规格和商品信息 */
    @Select("""
            SELECT c.id,
                   c.sku_id,
                   c.quantity,
                   c.selected,
                   k.sku_name,
                   k.price,
                   k.stock,
                   s.id         AS spu_id,
                   s.name       AS spu_name,
                   s.main_image,
                   (s.status = 0 OR k.status = 0 OR k.stock = 0) AS invalid
            FROM t_cart c
            JOIN t_sku k ON k.id = c.sku_id
            JOIN t_spu s ON s.id = k.spu_id
            WHERE c.user_id = #{userId}
            ORDER BY c.id DESC
            """)
    List<CartItemVO> selectCartItems(@Param("userId") Long userId);
}

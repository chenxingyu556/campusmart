package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campusmart.common.BizException;
import com.campusmart.common.ErrorCode;
import com.campusmart.common.ProductStatus;
import com.campusmart.common.UserContext;
import com.campusmart.mapper.CartMapper;
import com.campusmart.mapper.SkuMapper;
import com.campusmart.pojo.dto.CartAddDTO;
import com.campusmart.pojo.dto.CartUpdateDTO;
import com.campusmart.pojo.entity.Cart;
import com.campusmart.pojo.entity.Sku;
import com.campusmart.pojo.vo.CartItemVO;
import com.campusmart.pojo.vo.CartListVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final SkuMapper skuMapper;
    private final CartMapper cartMapper;

    public CartService(SkuMapper skuMapper,CartMapper cartMapper) {
        this.skuMapper = skuMapper;
        this.cartMapper = cartMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(CartAddDTO cartAddDTO) {

        Long userId = UserContext.get();

        // 1. 查 SKU（skuMapper.selectById）
        Sku sku = skuMapper.selectById(cartAddDTO.getSkuId());

        //    不存在 → 抛 404「商品不存在」
        if (sku == null){
            throw new BizException(ErrorCode.NOT_FOUND,"商品不存在");
        }
        //    已下架（status == OFF_SHELF）→ 抛 20001「商品已下架」
        if (sku.getStatus() == ProductStatus.OFF_SHELF){
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF,"商品已下架");
        }

        // 2. 校验库存：购物车已有 + 本次新增 不能超过库存
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, cartAddDTO.getSkuId()));
        int already = exist == null ? 0 : exist.getQuantity();
        if (already + cartAddDTO.getQuantity() >sku.getStock()){
            String msg = String.format("库存不足，购物车已有 %d 件，当前库存 %d 件", already, sku.getStock());
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, msg);
        }

        // 3. 累加：cartMapper.addOrIncrease(userId, dto.getSkuId(), dto.getQuantity())
        cartMapper.addOrIncrease(userId, cartAddDTO.getSkuId(), cartAddDTO.getQuantity());
    }

    public CartListVO list() {
        Long userId = UserContext.get();

        // 1. 调 cartMapper.selectCartItems(userId) 拿列表
        List<CartItemVO> items = cartMapper.selectCartItems(userId);

        // 2. 汇总：总件数、选中金额
        int totalQuantity = 0;
        BigDecimal selectAmount = BigDecimal.ZERO;

        for (CartItemVO item : items){
            // 总件数：所有行都计入
            totalQuantity += item.getQuantity();
            // 选中金额：只算"已勾选"且"未失效"的
            if (Boolean.TRUE.equals(item.getSelected())&& !Boolean.TRUE.equals(item.getInvalid())){
                selectAmount = selectAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        // 3. 组装
        CartListVO vo = new CartListVO();
        vo.setItems(items);
        vo.setTotalQuantity(totalQuantity);
        vo.setSelectedAmount(selectAmount);
        return vo;
    }


    public void updateCartQuantity(Long cartId, CartUpdateDTO cartUpdateDTO){

        Long userId = UserContext.get();

        // 1. 查购物车行（id + userId），查不到 → 404
        Cart cart = cartMapper.selectOne(new LambdaQueryWrapper<Cart>().eq(Cart::getId, cartId).eq(Cart::getUserId, userId));
        if (cart == null){
            throw new BizException(ErrorCode.NOT_FOUND,"购物车中没有该商品");
        }

        // 2. 用 cart.getSkuId() 查 SKU
        Sku sku = skuMapper.selectById(cart.getSkuId());
        if (sku == null){
            throw new BizException(ErrorCode.NOT_FOUND,"商品不存在");
        }

        // 3. 库存校验：dto.getQuantity() > sku.getStock() → 30002
        if (cartUpdateDTO.getQuantity() > sku.getStock()){
            String msg = String.format("库存不足，库存还剩%d件。",sku.getStock());
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,msg);
        }

        // 4. 更新：cart.setQuantity(dto.getQuantity()); cartMapper.updateById(cart);
        cartMapper.update(null, new LambdaUpdateWrapper<Cart>()
                .eq(Cart::getId, cartId)
                .eq(Cart::getUserId, userId)
                .set(Cart::getQuantity, cartUpdateDTO.getQuantity()));
    }

    public void delete(Long cartId){
        Long userId = UserContext.get();

        int row = cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getId, cartId).eq(Cart::getUserId, userId));

        if (row == 0){
            throw new BizException(ErrorCode.NOT_FOUND,"购物车中没有该商品");
        }
    }
}

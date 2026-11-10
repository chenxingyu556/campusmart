package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.pojo.dto.CartAddDTO;
import com.campusmart.pojo.dto.CartUpdateDTO;
import com.campusmart.pojo.vo.CartListVO;
import com.campusmart.service.CartService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService){
        this.cartService = cartService;
    }

    @PostMapping
    public Result<Void> add(@RequestBody @Valid CartAddDTO cartAddDTO){
        cartService.add(cartAddDTO);
        return Result.ok();
    }

    @GetMapping
    public Result<CartListVO> list(){
        return Result.ok(cartService.list());
    }

    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id,
                               @RequestBody @Valid CartUpdateDTO cartUpdateDTO){
        cartService.updateCartQuantity(id,cartUpdateDTO);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        cartService.delete(id);
        return Result.ok();
    }
}

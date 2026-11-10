package com.campusmart.controller;

import com.campusmart.common.PageResult;
import com.campusmart.common.Result;
import com.campusmart.pojo.dto.OrderFromCartDTO;
import com.campusmart.pojo.dto.OrderSubmitDTO;
import com.campusmart.pojo.vo.OrderDetailVO;
import com.campusmart.pojo.vo.OrderListVO;
import com.campusmart.pojo.vo.OrderSubmitVO;
import com.campusmart.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/order")
@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderSubmitVO> submit(@RequestBody @Valid OrderSubmitDTO dto) {
        return Result.ok(orderService.submit(dto));
    }

    @PostMapping("/from-cart")
    public Result<OrderSubmitVO> submitFromCart(@RequestBody @Valid OrderFromCartDTO dto){
        return Result.ok(orderService.submitFromCart(dto));
    }

    @PostMapping("/{orderId}/pay")
    public Result<Void> pay(@PathVariable Long orderId) {
        orderService.pay(orderId);
        return Result.ok();
    }

    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId) {
        orderService.cancel(orderId);
        return Result.ok();
    }
    @GetMapping("/list")
    public Result<PageResult<OrderListVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.list(status, page, size));
    }

    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@PathVariable Long orderId) {
        return Result.ok(orderService.detail(orderId));
    }

}

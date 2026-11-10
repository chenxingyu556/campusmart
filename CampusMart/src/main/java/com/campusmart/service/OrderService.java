package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmart.common.*;
import com.campusmart.config.RabbitMQConfig;
import com.campusmart.mapper.*;
import com.campusmart.pojo.dto.OrderFromCartDTO;
import com.campusmart.pojo.dto.OrderSubmitDTO;
import com.campusmart.pojo.entity.*;
import com.campusmart.pojo.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final SpuMapper spuMapper;
    private final CartMapper cartMapper;
    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderMapper orderMapper,OrderItemMapper orderItemMapper
            ,SkuMapper skuMapper,SpuMapper spuMapper,CartMapper cartMapper
            ,StockService stockService,RabbitTemplate rabbitTemplate){
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.skuMapper = skuMapper;
        this.spuMapper = spuMapper;
        this.cartMapper = cartMapper;
        this.stockService = stockService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrderSubmitDTO dto){
        Long userId = UserContext.get();
        // 1. 查 SKU → 不存在 404，已下架 20001
        Sku sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null){
            throw new BizException(ErrorCode.NOT_FOUND,"该商品不存在");
        }
        if (sku.getStatus() == ProductStatus.OFF_SHELF){
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF,"该商品已下架");
        }

        // 2. 查 SPU → 不存在 404，已下架 20001（拿 spuName、mainImage 做快照）
        Spu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null){
            throw new BizException(ErrorCode.NOT_FOUND,"该商品不存在");
        }
        if (spu.getStatus() == ProductStatus.OFF_SHELF){
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF,"该商品已下架");
        }

        // 3. 库存校验（查询，给友好提示）
        if (dto.getQuantity() > sku.getStock()){
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,
                    String.format("库存不足，当前库存%d",sku.getStock()));
        }

        // 4. 生成单号 + 扣库存（原子）
        //    —— 单号必须提前生成，库存流水要拿它当业务号
        String orderNo = generateOrderNo();
        stockService.deduct(dto.getSkuId(), dto.getQuantity(), orderNo);

        // 5. 组装订单对象
        BigDecimal totalAmount = sku.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(30);

        Order order = new Order();
        BeanUtils.copyProperties(dto,order);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(OrderStatus.UNPAID);
        order.setExpireTime(expireTime);

        // 6. orderMapper.insert(order)
        orderMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());            // ← insert 之后才有值
        item.setSpuId(spu.getId());
        item.setSkuId(sku.getId());
        item.setSpuName(spu.getName());
        item.setSkuName(sku.getSkuName());
        item.setImage(spu.getMainImage());
        item.setPrice(sku.getPrice());
        item.setQuantity(dto.getQuantity());
        item.setTotalAmount(totalAmount);

        // 7. orderItemMapper.insert(item)
        orderItemMapper.insert(item);

        sendDelayCloseMessage(order.getId());
        // 8. 返回 VO
        return new OrderSubmitVO(order.getId(),order.getOrderNo(),order.getPayAmount(),order.getExpireTime());
    }
    private String generateOrderNo(){
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000000);
        return time + String.format("%06d", rand);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitFromCart(OrderFromCartDTO orderFromCartDTO){

        Long userId = UserContext.get();

        // 1. 查购物车（复用 selectCartItems），过滤出 selected = 1 的行
        List<CartItemVO> all = cartMapper.selectCartItems(userId);
        List<CartItemVO> selectedItems = all.stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .toList();


        //    —— 空 → 抛 ErrorCode.CART_EMPTY「购物车中没有选中的商品」
        if (selectedItems.isEmpty()){
            throw new BizException(ErrorCode.CART_EMPTY,"购物车中没有选中的商品");
        }

        // 2. 逐行校验
        for (CartItemVO item : selectedItems) {

            //    —— invalid 为 true → 20001，提示里带上 spuName
            if (Boolean.TRUE.equals(item.getInvalid())){
                throw new BizException(ErrorCode.PRODUCT_OFF_SHELF,String.format("%s已下架",item.getSpuName()));
            }

            //    —— stock < quantity → 30002，提示里带上 spuName 和剩余库存
            if (item.getQuantity()>item.getStock()){
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,String.format("%s库存不足，仅剩%d件",item.getSpuName(),item.getStock()));
            }
        }

        // 3. 按 skuId 升序排序          ← 防死锁，别漏
        List<CartItemVO> sorted = selectedItems.stream().sorted(Comparator.comparing(CartItemVO::getSkuId)).toList();

        // 4. 生成单号（库存流水要拿它当业务号，必须提前生成）
        String orderNo = generateOrderNo();

        // 5. 逐个扣库存（内含流水 + 条件更新）
        for (CartItemVO cartItemVO : sorted) {
            stockService.deduct(cartItemVO.getSkuId(), cartItemVO.getQuantity(), orderNo);
        }

        // 6. 算总金额（BigDecimal.add 累加）

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemVO itemVO : sorted) {
            totalAmount = totalAmount.add(itemVO.getPrice().multiply(BigDecimal.valueOf(itemVO.getQuantity())));
        }

        // 7. insert t_order（收货信息来自 dto，不是 BeanUtils 拷贝）
        Order cartOrder = new Order();
        cartOrder.setOrderNo(orderNo);
        cartOrder.setUserId(userId);
        cartOrder.setTotalAmount(totalAmount);
        cartOrder.setPayAmount(totalAmount);
        cartOrder.setStatus(OrderStatus.UNPAID);
        cartOrder.setReceiverName(orderFromCartDTO.getReceiverName());
        cartOrder.setReceiverPhone(orderFromCartDTO.getReceiverPhone());
        cartOrder.setReceiverAddress(orderFromCartDTO.getReceiverAddress());
        cartOrder.setExpireTime(LocalDateTime.now().plusMinutes(30).withNano(0));
        orderMapper.insert(cartOrder);

        // 8. 循环 insert t_order_item（每轮 new 一个对象）
        for (CartItemVO itemVO : sorted) {
            OrderItem item = new OrderItem();
            item.setOrderId(cartOrder.getId());
            item.setSpuId(itemVO.getSpuId());
            item.setSkuId(itemVO.getSkuId());
            item.setSpuName(itemVO.getSpuName());
            item.setSkuName(itemVO.getSkuName());
            item.setImage(itemVO.getMainImage());
            item.setPrice(itemVO.getPrice());
            item.setQuantity(itemVO.getQuantity());
            item.setTotalAmount(itemVO.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(item);
        }

        // 9. 删除已下单 的购物车行（id IN cartIds + userId）
        List<Long> cartIds = sorted.stream().map(CartItemVO::getId).toList();
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId,userId).in(Cart::getId,cartIds));
        sendDelayCloseMessage(cartOrder.getId());
        // 10. return new OrderSubmitVO(...)
        return new OrderSubmitVO(cartOrder.getId(),cartOrder.getOrderNo(),cartOrder.getPayAmount(),cartOrder.getExpireTime());
    }

    private void sendDelayCloseMessage(Long orderId) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_DELAY,
                orderId.toString()
        );
        log.info("已投递延迟关单消息 orderId={}", orderId);
    }


    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId){

        Long userId = UserContext.get();

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getId, orderId).eq(Order::getUserId, userId));

        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        if (!order.getStatus().equals(OrderStatus.UNPAID)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单状态不允许支付");
        }

        if (order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_EXPIRED, "订单已超时");
        }

        int row = orderMapper.paySuccess(order.getId(), userId);
        if (row == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "订单状态已变更，请刷新");
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId){

        Long userId = UserContext.get();

        int row = orderMapper.cancel(orderId, userId);
        if (row == 0){
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getId, orderId).eq(Order::getUserId, userId));
            if (order == null || !order.getUserId().equals(userId)){
                throw new BizException(ErrorCode.ORDER_NOT_FOUND,"订单不存在");
            }
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR,"只有待支付订单可以取消");
        }

        Order order = orderMapper.selectById(orderId);
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : orderItems) {
            stockService.restore(item.getSkuId(), item.getQuantity(), order.getOrderNo());
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {

        List<Long> ids = orderMapper.selectExpiredOrderIds();
        int closed = 0;

        for (Long id : ids) {
            if (closeOrder(id)) {
                closed++;
            }
        }
        return closed;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(Long orderId){
        int row = orderMapper.closeById(orderId);
        if (row == 0){return false;}
        Order order = orderMapper.selectById(orderId);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            stockService.restore(item.getSkuId(), item.getQuantity(), order.getOrderNo());
        }
        return true;
    }

    public PageResult<OrderListVO> list(Integer status, int page, int size) {

        Long userId = UserContext.get();

        if (size > 50){
            size = 50;
        }

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status).orderByDesc(Order::getId));

        List<Order> orders = orderPage.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(0,0,page,size,List.of());
        }

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));

        Map<Long, List<OrderItem>> byOrder = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderListVO> vos = orders.stream()
                .map(o -> toListVo(o, byOrder.getOrDefault(o.getId(), List.of())))
                .toList();

        return PageResult.of(orderPage.getTotal(), orderPage.getPages(), orderPage.getCurrent(), size, vos);
    }

    private OrderListVO toListVo(Order order, List<OrderItem> items) {
        OrderListVO vo = new OrderListVO();

        // orderId 必须手动 set，字段名和实体不一样
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
        vo.setPayAmount(order.getPayAmount());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setExpireTime(order.getExpireTime());

        // 总件数
        vo.setTotalCount(items.stream().mapToInt(OrderItem::getQuantity).sum());

        // 缩略图取第一件
        if (!items.isEmpty()) {
            vo.setFirstImage(items.get(0).getImage());
            vo.setFirstSpuName(items.get(0).getSpuName());
        }
        return vo;
    }

    public OrderDetailVO detail(Long orderId) {
        Long userId = UserContext.get();

        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setExpireTime(order.getExpireTime());
        vo.setPayTime(order.getPayTime());
        vo.setCloseTime(order.getCloseTime());
        vo.setItems(items.stream().map(this::toItemVo).toList());
        return vo;
    }

    private OrderItemVO toItemVo(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);   // ← 这里可以拷，字段名完全一致
        return vo;
    }


    private String statusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case OrderStatus.UNPAID    -> "待支付";
            case OrderStatus.PAID      -> "已支付";
            case OrderStatus.SHIPPED   -> "已发货";
            case OrderStatus.COMPLETED -> "已完成";
            case OrderStatus.CANCELLED -> "已取消";
            case OrderStatus.CLOSED    -> "已超时关闭";
            default -> "未知";
        };
    }
}

package com.der.kitchen.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.UserContextHolder;
import com.der.kitchen.order.dto.OrderAddItemDTO;
import com.der.kitchen.order.dto.OrderCreateDTO;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.service.OrderService;
import com.der.kitchen.order.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单-用户端")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public R<Order> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(UserContextHolder.getUserId(), dto));
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public R<Page<Order>> list(OrderQueryDTO query) {
        return R.ok(orderService.pageByUser(UserContextHolder.getUserId(), query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContextHolder.getUserId(), id);
        return R.ok(null);
    }

    @Operation(summary = "加菜")
    @PostMapping("/{id}/items")
    public R<Void> addItems(@PathVariable Long id, @Valid @RequestBody OrderAddItemDTO dto) {
        orderService.addItems(UserContextHolder.getUserId(), id, dto);
        return R.ok(null);
    }
}

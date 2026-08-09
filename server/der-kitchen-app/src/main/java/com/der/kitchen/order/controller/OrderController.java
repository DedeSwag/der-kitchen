package com.der.kitchen.order.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.result.PageResult;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.order.dto.OrderAddItemDTO;
import com.der.kitchen.order.dto.OrderCreateDTO;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.service.OrderService;
import com.der.kitchen.order.vo.OrderDetailVO;
import com.der.kitchen.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单-用户端")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public R<OrderDetailVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(SecurityUtils.requireUserId(), dto));
    }

    @Operation(summary = "订单列表")
    @GetMapping
    public R<PageResult<OrderVO>> list(@Valid OrderQueryDTO query) {
        return R.ok(orderService.pageByUser(SecurityUtils.requireUserId(), query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable @Positive Long id) {
        return R.ok(orderService.getDetailForUser(SecurityUtils.requireUserId(), id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable @Positive Long id) {
        orderService.cancel(SecurityUtils.requireUserId(), id);
        return R.ok(null);
    }

    @Operation(summary = "加菜")
    @PostMapping("/{id}/items")
    public R<Void> addItems(@PathVariable @Positive Long id, @Valid @RequestBody OrderAddItemDTO dto) {
        orderService.addItems(SecurityUtils.requireUserId(), id, dto);
        return R.ok(null);
    }
}

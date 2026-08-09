package com.der.kitchen.order.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.result.PageResult;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.dto.OrderStatusRequest;
import com.der.kitchen.order.service.OrderService;
import com.der.kitchen.order.vo.OrderDetailVO;
import com.der.kitchen.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "订单-管理端")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页列表")
    @GetMapping
    public R<PageResult<OrderVO>> page(@Valid OrderQueryDTO query) {
        return R.ok(orderService.pageForAdmin(query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable @Positive Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @Operation(summary = "状态流转")
    @PutMapping("/{id}/status")
    public R<Void> transitStatus(@PathVariable @Positive Long id, @Valid @RequestBody OrderStatusRequest request) {
        orderService.transitStatus(SecurityUtils.requireUserId(), id, request.getStatus());
        return R.ok(null);
    }

    @Operation(summary = "待处理订单数")
    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        return R.ok(orderService.getPendingCount());
    }
}

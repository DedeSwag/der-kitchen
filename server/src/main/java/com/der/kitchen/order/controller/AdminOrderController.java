package com.der.kitchen.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.result.R;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.service.OrderService;
import com.der.kitchen.order.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单-管理端")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequireRole("admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单分页列表")
    @GetMapping
    public R<Page<Order>> page(OrderQueryDTO query) {
        return R.ok(orderService.pageForAdmin(query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(orderService.getDetail(id));
    }

    @Operation(summary = "状态流转")
    @PutMapping("/{id}/status")
    public R<Void> transitStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.transitStatus(id, body.get("status"));
        return R.ok(null);
    }

    @Operation(summary = "待处理订单数")
    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        return R.ok(orderService.getPendingCount());
    }
}

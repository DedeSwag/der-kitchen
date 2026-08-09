package com.der.kitchen.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderStatusRequest {

    @NotBlank(message = "订单状态不能为空")
    @Pattern(regexp = "pending|preparing|cooking|completed|cancelled", message = "订单状态不合法")
    private String status;
}

package com.der.kitchen.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderAddItemDTO {

    @NotEmpty(message = "至少选择一道菜")
    private List<OrderCreateDTO.OrderItemDTO> items;
}

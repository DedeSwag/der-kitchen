package com.der.kitchen.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class OrderAddItemDTO {

    @NotEmpty(message = "至少选择一道菜")
    @Size(max = 50, message = "单次加菜最多选择50道菜")
    @Valid
    private List<OrderCreateDTO.OrderItemDTO> items;
}

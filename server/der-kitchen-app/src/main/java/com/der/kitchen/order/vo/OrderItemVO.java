package com.der.kitchen.order.vo;

import com.der.kitchen.order.entity.OrderItem;
import lombok.Data;

@Data
public class OrderItemVO {

    private Long id;
    private Long dishId;
    private String dishName;
    private Integer quantity;
    private Boolean isExtra;

    public static OrderItemVO from(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setDishId(item.getDishId());
        vo.setDishName(item.getDishName());
        vo.setQuantity(item.getQuantity());
        vo.setIsExtra(item.getIsExtra());
        return vo;
    }
}

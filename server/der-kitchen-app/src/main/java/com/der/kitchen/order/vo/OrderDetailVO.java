package com.der.kitchen.order.vo;

import com.der.kitchen.order.entity.Order;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderVO {

    public static OrderDetailVO from(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        copyOrder(order, vo);
        return vo;
    }
}

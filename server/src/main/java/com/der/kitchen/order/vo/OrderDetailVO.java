package com.der.kitchen.order.vo;

import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.entity.OrderItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends Order {

    private List<OrderItem> items;
    private String userNickname;
}

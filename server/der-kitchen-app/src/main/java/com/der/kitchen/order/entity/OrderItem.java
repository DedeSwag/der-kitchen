package com.der.kitchen.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order_item")
public class OrderItem extends BaseEntity {

    private Long orderId;
    private Long dishId;
    private String dishName;
    private Integer quantity;
    private Boolean isExtra;
}

package com.der.kitchen.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_order_event")
public class OrderEvent extends BaseEntity {

    private Long orderId;
    private String eventType;
    private Long actorId;
    private String fromStatus;
    private String toStatus;
    private Integer itemCount;
    private String description;
}

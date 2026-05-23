package com.der.kitchen.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    private Long userId;
    private String mealType;
    private LocalDate mealDate;
    private String status;
    private String tasteTags;       // JSON string
    private String dietaryNotes;
    private String specialRequests;
}

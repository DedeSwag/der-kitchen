package com.der.kitchen.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.der.kitchen.common.entity.BaseEntity;
import com.der.kitchen.order.handler.JsonbStringListTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_order", autoResultMap = true)
public class Order extends BaseEntity {

    private Long userId;
    private String mealType;
    private LocalDate mealDate;
    private String status;
    @TableField(typeHandler = JsonbStringListTypeHandler.class)
    private List<String> tasteTags;
    private String dietaryNotes;
    private String specialRequests;
}

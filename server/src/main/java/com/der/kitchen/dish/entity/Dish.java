package com.der.kitchen.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dishes")
public class Dish extends BaseEntity {

    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private Integer cookingTime;
    private String status;
    private Boolean isListed;
}

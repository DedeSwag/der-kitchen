package com.der.kitchen.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_category")
public class Category extends BaseEntity {

    private String name;
    private Integer sortOrder;
    private String status;
}

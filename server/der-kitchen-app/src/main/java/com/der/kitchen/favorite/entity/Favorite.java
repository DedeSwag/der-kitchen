package com.der.kitchen.favorite.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_favorite")
public class Favorite extends BaseEntity {

    private Long userId;
    private Long dishId;
}

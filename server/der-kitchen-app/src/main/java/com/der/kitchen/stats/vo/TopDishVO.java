package com.der.kitchen.stats.vo;

public record TopDishVO(
        Long dishId,
        String dishName,
        long totalCount
) {
}

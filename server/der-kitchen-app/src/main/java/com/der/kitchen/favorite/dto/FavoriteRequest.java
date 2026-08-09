package com.der.kitchen.favorite.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FavoriteRequest {

    @NotNull(message = "菜品ID不能为空")
    @Positive(message = "菜品ID必须为正数")
    private Long dishId;
}

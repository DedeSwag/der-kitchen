package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class DishBatchListingRequest {

    @NotEmpty(message = "菜品ID列表不能为空")
    private List<@Positive(message = "菜品ID必须为正数") Long> ids;

    @NotNull(message = "上下架状态不能为空")
    private Boolean isListed;
}

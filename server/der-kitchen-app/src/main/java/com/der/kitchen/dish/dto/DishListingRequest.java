package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DishListingRequest {

    @NotNull(message = "上下架状态不能为空")
    private Boolean isListed;
}

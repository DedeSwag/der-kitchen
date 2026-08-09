package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DishStatusRequest {

    @NotBlank(message = "菜品状态不能为空")
    @Pattern(regexp = "normal|out_of_stock|unavailable", message = "菜品状态不合法")
    private String status;
}

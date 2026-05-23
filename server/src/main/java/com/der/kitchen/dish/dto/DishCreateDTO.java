package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DishCreateDTO {
    @NotBlank(message = "菜品名称不能为空")
    private String name;
    private String description;
    private String imageUrl;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    private Integer cookingTime;
}

package com.der.kitchen.dish.dto;

import lombok.Data;

@Data
public class DishUpdateDTO {
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private Integer cookingTime;
}

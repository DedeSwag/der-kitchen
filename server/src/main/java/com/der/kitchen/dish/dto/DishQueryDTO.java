package com.der.kitchen.dish.dto;

import lombok.Data;

@Data
public class DishQueryDTO {
    private Long categoryId;
    private String status;
    private Boolean isListed;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}

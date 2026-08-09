package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DishQueryDTO {
    @Positive(message = "分类ID必须为正数")
    private Long categoryId;
    @Pattern(regexp = "normal|out_of_stock|unavailable", message = "菜品状态不合法")
    private String status;
    private Boolean isListed;
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}

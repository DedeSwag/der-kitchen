package com.der.kitchen.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CategorySortRequest {

    @NotNull(message = "分类ID不能为空")
    @Positive(message = "分类ID必须为正数")
    private Long id;

    @NotNull(message = "排序值不能为空")
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;
}

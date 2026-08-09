package com.der.kitchen.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryUpdateRequest {

    @Size(min = 1, max = 20, message = "分类名称长度应为1到20个字符")
    @Pattern(regexp = ".*\\S.*", message = "分类名称不能为空")
    private String name;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;

    @Pattern(regexp = "active|hidden", message = "分类状态不合法")
    private String status;
}

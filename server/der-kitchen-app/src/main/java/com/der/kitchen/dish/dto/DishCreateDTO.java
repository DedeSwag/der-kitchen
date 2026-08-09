package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DishCreateDTO {
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 20, message = "菜品名称不能超过20个字符")
    private String name;
    @Size(max = 200, message = "菜品描述不能超过200个字符")
    private String description;
    /** 通过文件接口上传后返回的 fileId，优先使用此字段 */
    @NotNull(message = "菜品图片不能为空")
    @Positive(message = "文件ID必须为正数")
    private Long imageFileId;
    @NotNull(message = "分类不能为空")
    @Positive(message = "分类ID必须为正数")
    private Long categoryId;
    @Min(value = 1, message = "制作时长不能小于1分钟")
    @Max(value = 1440, message = "制作时长不能超过1440分钟")
    private Integer cookingTime;
}

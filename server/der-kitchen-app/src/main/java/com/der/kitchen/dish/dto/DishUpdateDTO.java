package com.der.kitchen.dish.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DishUpdateDTO {
    @Size(min = 1, max = 20, message = "菜品名称长度应为1到20个字符")
    @Pattern(regexp = ".*\\S.*", message = "菜品名称不能为空")
    private String name;
    @Size(max = 200, message = "菜品描述不能超过200个字符")
    private String description;
    /** 通过文件接口上传后返回的 fileId，优先使用此字段 */
    @Positive(message = "文件ID必须为正数")
    private Long imageFileId;
    @Positive(message = "分类ID必须为正数")
    private Long categoryId;
    @Min(value = 1, message = "制作时长不能小于1分钟")
    @Max(value = 1440, message = "制作时长不能超过1440分钟")
    private Integer cookingTime;
}

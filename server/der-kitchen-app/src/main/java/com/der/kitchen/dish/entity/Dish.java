package com.der.kitchen.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_info")
public class Dish extends BaseEntity {

    private String name;
    private String description;
    /** 关联 sys_file 表的文件 ID，访问地址在响应时动态解析 */
    private Long imageFileId;
    private Long categoryId;
    private Integer cookingTime;
    private String status;
    private Boolean isListed;
}

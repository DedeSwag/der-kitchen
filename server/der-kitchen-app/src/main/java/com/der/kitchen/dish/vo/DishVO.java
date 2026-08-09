package com.der.kitchen.dish.vo;

import com.der.kitchen.dish.entity.Dish;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜品响应 VO，包含解析后的图片访问地址
 */
@Data
public class DishVO {

    private Long id;
    private String name;
    private String description;
    /** 图片访问地址（预签名 URL 或存量直链） */
    private String imageUrl;
    /** 图片缩略图地址，若存在则返回 */
    private String thumbnailUrl;
    /** 关联的文件 ID；管理端可通过 /api/common/file/{fileId} 刷新地址 */
    private Long imageFileId;
    private Long categoryId;
    private Integer cookingTime;
    private String status;
    private Boolean isListed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static DishVO from(Dish dish) {
        DishVO vo = new DishVO();
        vo.setId(dish.getId());
        vo.setName(dish.getName());
        vo.setDescription(dish.getDescription());
        vo.setImageFileId(dish.getImageFileId());
        vo.setCategoryId(dish.getCategoryId());
        vo.setCookingTime(dish.getCookingTime());
        vo.setStatus(dish.getStatus());
        vo.setIsListed(dish.getIsListed());
        vo.setCreateTime(dish.getCreateTime());
        vo.setUpdateTime(dish.getUpdateTime());
        return vo;
    }
}

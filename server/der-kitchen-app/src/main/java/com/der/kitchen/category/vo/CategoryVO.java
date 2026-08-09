package com.der.kitchen.category.vo;

import com.der.kitchen.category.entity.Category;

public record CategoryVO(
        Long id,
        String name,
        Integer sortOrder,
        String status
) {
    public static CategoryVO from(Category category) {
        return new CategoryVO(
                category.getId(), category.getName(), category.getSortOrder(), category.getStatus());
    }
}

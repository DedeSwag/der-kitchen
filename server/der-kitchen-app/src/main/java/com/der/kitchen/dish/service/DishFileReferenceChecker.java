package com.der.kitchen.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import com.der.kitchen.file.service.FileReferenceChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DishFileReferenceChecker implements FileReferenceChecker {

    private final DishMapper dishMapper;

    @Override
    public boolean isReferenced(Long fileId) {
        return dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>().eq(Dish::getImageFileId, fileId)) > 0;
    }

    @Override
    public String referenceName() {
        return "菜品";
    }
}

package com.der.kitchen.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.dish.dto.DishCreateDTO;
import com.der.kitchen.dish.dto.DishQueryDTO;
import com.der.kitchen.dish.dto.DishUpdateDTO;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;

    /**
     * 用户端：已上架菜品列表
     */
    public List<Dish> listForUser(Long categoryId) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getIsListed, true)
                .eq(Dish::getStatus, "normal")
                .orderByDesc(Dish::getCreateTime);
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        return dishMapper.selectList(wrapper);
    }

    public Dish getById(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BizException("菜品不存在");
        return dish;
    }

    /**
     * 管理端：分页查询
     */
    public Page<Dish> pageForAdmin(DishQueryDTO query) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        if (query.getCategoryId() != null) {
            wrapper.eq(Dish::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Dish::getStatus, query.getStatus());
        }
        if (query.getIsListed() != null) {
            wrapper.eq(Dish::getIsListed, query.getIsListed());
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        return dishMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public Dish create(DishCreateDTO dto) {
        Dish dish = new Dish();
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setImageUrl(dto.getImageUrl());
        dish.setCategoryId(dto.getCategoryId());
        dish.setCookingTime(dto.getCookingTime());
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);
        return dish;
    }

    public Dish update(Long id, DishUpdateDTO dto) {
        Dish dish = getById(id);
        if (dto.getName() != null) dish.setName(dto.getName());
        if (dto.getDescription() != null) dish.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null) dish.setImageUrl(dto.getImageUrl());
        if (dto.getCategoryId() != null) dish.setCategoryId(dto.getCategoryId());
        if (dto.getCookingTime() != null) dish.setCookingTime(dto.getCookingTime());
        dishMapper.updateById(dish);
        return dish;
    }

    public void delete(Long id) {
        dishMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status) {
        Dish dish = getById(id);
        dish.setStatus(status);
        dishMapper.updateById(dish);
    }

    public void updateListing(Long id, boolean isListed) {
        Dish dish = getById(id);
        dish.setIsListed(isListed);
        dishMapper.updateById(dish);
    }

    public void batchListing(List<Long> ids, boolean isListed) {
        for (Long id : ids) {
            updateListing(id, isListed);
        }
    }
}

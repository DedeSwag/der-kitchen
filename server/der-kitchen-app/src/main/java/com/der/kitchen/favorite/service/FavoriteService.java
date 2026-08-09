package com.der.kitchen.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.favorite.entity.Favorite;
import com.der.kitchen.favorite.mapper.FavoriteMapper;
import com.der.kitchen.dish.service.DishService;
import com.der.kitchen.dish.vo.DishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final DishService dishService;

    public List<DishVO> listByUser(Long userId) {
        List<Long> dishIds = favoriteMapper.selectList(
                        new LambdaQueryWrapper<Favorite>()
                                .eq(Favorite::getUserId, userId)
                                .orderByDesc(Favorite::getCreateTime))
                .stream().map(Favorite::getDishId).toList();
        return dishService.listVisibleByIds(dishIds);
    }

    public void add(Long userId, Long dishId) {
        dishService.requireVisibleDish(dishId);
        favoriteMapper.insertIfAbsent(userId, dishId);
    }

    public void remove(Long userId, Long dishId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getDishId, dishId));
    }

    public boolean isFavorited(Long userId, Long dishId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getDishId, dishId)) > 0;
    }
}

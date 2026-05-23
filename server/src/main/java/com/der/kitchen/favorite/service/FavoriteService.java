package com.der.kitchen.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import com.der.kitchen.favorite.entity.Favorite;
import com.der.kitchen.favorite.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final DishMapper dishMapper;

    public List<Dish> listByUser(Long userId) {
        List<Long> dishIds = favoriteMapper.selectList(
                        new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId))
                .stream().map(Favorite::getDishId).toList();
        if (dishIds.isEmpty()) return List.of();
        return dishMapper.selectBatchIds(dishIds);
    }

    public void add(Long userId, Long dishId) {
        // 幂等：已存在则不重复添加
        Favorite exist = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getDishId, dishId));
        if (exist != null) return;

        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setDishId(dishId);
        favoriteMapper.insert(fav);
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

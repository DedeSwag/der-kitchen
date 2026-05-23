package com.der.kitchen.favorite.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.UserContextHolder;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "收藏模块")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏列表")
    @GetMapping
    public R<List<Dish>> list() {
        return R.ok(favoriteService.listByUser(UserContextHolder.getUserId()));
    }

    @Operation(summary = "添加收藏")
    @PostMapping
    public R<Void> add(@RequestBody Map<String, Long> body) {
        favoriteService.add(UserContextHolder.getUserId(), body.get("dishId"));
        return R.ok(null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{dishId}")
    public R<Void> remove(@PathVariable Long dishId) {
        favoriteService.remove(UserContextHolder.getUserId(), dishId);
        return R.ok(null);
    }
}

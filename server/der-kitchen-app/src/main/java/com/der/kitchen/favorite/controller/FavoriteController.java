package com.der.kitchen.favorite.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.dish.vo.DishVO;
import com.der.kitchen.favorite.dto.FavoriteRequest;
import com.der.kitchen.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "收藏模块")
@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Validated
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏列表")
    @GetMapping
    public R<List<DishVO>> list() {
        return R.ok(favoriteService.listByUser(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "添加收藏")
    @PostMapping
    public R<Void> add(@Valid @RequestBody FavoriteRequest request) {
        favoriteService.add(SecurityUtils.requireUserId(), request.getDishId());
        return R.ok(null);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{dishId}")
    public R<Void> remove(@PathVariable @Positive Long dishId) {
        favoriteService.remove(SecurityUtils.requireUserId(), dishId);
        return R.ok(null);
    }
}

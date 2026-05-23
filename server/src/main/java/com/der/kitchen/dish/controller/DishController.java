package com.der.kitchen.dish.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜品-用户端")
@RestController
@RequestMapping("/api/v1/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @Operation(summary = "菜品列表（已上架）")
    @GetMapping
    public R<List<Dish>> list(@RequestParam(required = false) Long categoryId) {
        return R.ok(dishService.listForUser(categoryId));
    }

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public R<Dish> detail(@PathVariable Long id) {
        return R.ok(dishService.getById(id));
    }
}

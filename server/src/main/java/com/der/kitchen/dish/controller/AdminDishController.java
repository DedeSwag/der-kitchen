package com.der.kitchen.dish.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.result.R;
import com.der.kitchen.dish.dto.DishCreateDTO;
import com.der.kitchen.dish.dto.DishQueryDTO;
import com.der.kitchen.dish.dto.DishUpdateDTO;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "菜品-管理端")
@RestController
@RequestMapping("/api/v1/admin/dishes")
@RequireRole("admin")
@RequiredArgsConstructor
public class AdminDishController {

    private final DishService dishService;

    @Operation(summary = "菜品分页列表")
    @GetMapping
    public R<Page<Dish>> page(DishQueryDTO query) {
        return R.ok(dishService.pageForAdmin(query));
    }

    @Operation(summary = "新增菜品")
    @PostMapping
    public R<Dish> create(@Valid @RequestBody DishCreateDTO dto) {
        return R.ok(dishService.create(dto));
    }

    @Operation(summary = "修改菜品")
    @PutMapping("/{id}")
    public R<Dish> update(@PathVariable Long id, @Valid @RequestBody DishUpdateDTO dto) {
        return R.ok(dishService.update(id, dto));
    }

    @Operation(summary = "删除菜品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return R.ok(null);
    }

    @Operation(summary = "修改菜品状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dishService.updateStatus(id, body.get("status"));
        return R.ok(null);
    }

    @Operation(summary = "上下架")
    @PutMapping("/{id}/listing")
    public R<Void> updateListing(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        dishService.updateListing(id, body.get("isListed"));
        return R.ok(null);
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/batch-listing")
    public R<Void> batchListing(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Number>) body.get("ids")).stream().map(Number::longValue).toList();
        Boolean isListed = (Boolean) body.get("isListed");
        dishService.batchListing(ids, isListed);
        return R.ok(null);
    }
}

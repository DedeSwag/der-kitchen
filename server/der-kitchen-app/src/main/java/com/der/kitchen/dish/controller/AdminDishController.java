package com.der.kitchen.dish.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.result.PageResult;
import com.der.kitchen.dish.dto.*;
import com.der.kitchen.dish.service.DishService;
import com.der.kitchen.dish.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "菜品-管理端")
@RestController
@RequestMapping("/api/v1/admin/dishes")
@RequiredArgsConstructor
@Validated
public class AdminDishController {

    private final DishService dishService;

    @Operation(summary = "菜品详情（含下架菜品）")
    @GetMapping("/{id}")
    public R<DishVO> detail(@PathVariable @Positive Long id) {
        return R.ok(dishService.getByIdForAdmin(id));
    }

    @Operation(summary = "菜品分页列表")
    @GetMapping
    public R<PageResult<DishVO>> page(@Valid DishQueryDTO query) {
        return R.ok(dishService.pageForAdmin(query));
    }

    @Operation(summary = "新增菜品")
    @PostMapping
    public R<DishVO> create(@Valid @RequestBody DishCreateDTO dto) {
        return R.ok(dishService.create(dto));
    }

    @Operation(summary = "修改菜品")
    @PutMapping("/{id}")
    public R<DishVO> update(@PathVariable @Positive Long id, @Valid @RequestBody DishUpdateDTO dto) {
        return R.ok(dishService.update(id, dto));
    }

    @Operation(summary = "删除菜品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable @Positive Long id) {
        dishService.delete(id);
        return R.ok(null);
    }

    @Operation(summary = "修改菜品状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable @Positive Long id, @Valid @RequestBody DishStatusRequest request) {
        dishService.updateStatus(id, request.getStatus());
        return R.ok(null);
    }

    @Operation(summary = "上下架")
    @PutMapping("/{id}/listing")
    public R<Void> updateListing(@PathVariable @Positive Long id, @Valid @RequestBody DishListingRequest request) {
        dishService.updateListing(id, request.getIsListed());
        return R.ok(null);
    }

    @Operation(summary = "批量上下架")
    @PutMapping("/batch-listing")
    public R<Void> batchListing(@Valid @RequestBody DishBatchListingRequest request) {
        dishService.batchListing(request.getIds(), request.getIsListed());
        return R.ok(null);
    }
}

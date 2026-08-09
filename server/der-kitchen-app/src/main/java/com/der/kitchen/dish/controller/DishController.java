package com.der.kitchen.dish.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.dish.service.DishService;
import com.der.kitchen.dish.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "菜品-用户端")
@RestController
@RequestMapping("/api/v1/dishes")
@RequiredArgsConstructor
@Validated
public class DishController {

    private final DishService dishService;

    @Operation(summary = "菜品列表（已上架）")
    @GetMapping
    public R<List<DishVO>> list(@RequestParam(required = false) @Positive Long categoryId) {
        return R.ok(dishService.listForUser(categoryId));
    }

    @Operation(summary = "菜品详情")
    @GetMapping("/{id}")
    public R<DishVO> detail(@PathVariable @Positive Long id) {
        return R.ok(dishService.getById(id));
    }
}

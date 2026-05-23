package com.der.kitchen.category.controller;

import com.der.kitchen.category.entity.Category;
import com.der.kitchen.category.service.CategoryService;
import com.der.kitchen.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类-用户端")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取分类列表（仅active）")
    @GetMapping
    public R<List<Category>> list() {
        return R.ok(categoryService.listActive());
    }
}

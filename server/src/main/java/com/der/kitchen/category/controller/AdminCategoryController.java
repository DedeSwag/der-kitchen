package com.der.kitchen.category.controller;

import com.der.kitchen.category.dto.CategoryRequest;
import com.der.kitchen.category.entity.Category;
import com.der.kitchen.category.service.CategoryService;
import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "分类-管理端")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequireRole("admin")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取所有分类（含hidden）")
    @GetMapping
    public R<List<Category>> listAll() {
        return R.ok(categoryService.listAll());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<Category> create(@Valid @RequestBody CategoryRequest req) {
        return R.ok(categoryService.create(req.getName(), req.getSortOrder()));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return R.ok(categoryService.update(id, req.getName(), req.getSortOrder(), req.getStatus()));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok(null);
    }

    @Operation(summary = "批量排序")
    @PutMapping("/sort")
    public R<Void> batchSort(@RequestBody List<Map<String, Object>> sortList) {
        categoryService.batchSort(sortList);
        return R.ok(null);
    }
}

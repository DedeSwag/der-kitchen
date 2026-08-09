package com.der.kitchen.category.controller;

import com.der.kitchen.category.dto.CategoryCreateRequest;
import com.der.kitchen.category.dto.CategorySortRequest;
import com.der.kitchen.category.dto.CategoryUpdateRequest;
import com.der.kitchen.category.service.CategoryService;
import com.der.kitchen.category.vo.CategoryVO;
import com.der.kitchen.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类-管理端")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Validated
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取所有分类（含hidden）")
    @GetMapping
    public R<List<CategoryVO>> listAll() {
        return R.ok(categoryService.listAll().stream().map(CategoryVO::from).toList());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<CategoryVO> create(@Valid @RequestBody CategoryCreateRequest req) {
        return R.ok(CategoryVO.from(categoryService.create(req.getName(), req.getSortOrder())));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<CategoryVO> update(@PathVariable @Positive Long id, @Valid @RequestBody CategoryUpdateRequest req) {
        return R.ok(CategoryVO.from(categoryService.update(id, req.getName(), req.getSortOrder(), req.getStatus())));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable @Positive Long id) {
        categoryService.delete(id);
        return R.ok(null);
    }

    @Operation(summary = "批量排序")
    @PutMapping("/sort")
    public R<Void> batchSort(@Valid @RequestBody @NotEmpty List<@Valid CategorySortRequest> sortList) {
        categoryService.batchSort(sortList);
        return R.ok(null);
    }
}

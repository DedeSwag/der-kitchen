package com.der.kitchen.category.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.category.entity.Category;
import com.der.kitchen.category.dto.CategorySortRequest;
import com.der.kitchen.category.mapper.CategoryMapper;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final DishMapper dishMapper;

    public List<Category> listActive() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, "active")
                        .orderByAsc(Category::getSortOrder));
    }

    public List<Category> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder));
    }

    public Category create(String name, Integer sortOrder) {
        Category c = new Category();
        c.setName(name.trim());
        c.setSortOrder(sortOrder != null ? sortOrder : 0);
        c.setStatus("active");
        categoryMapper.insert(c);
        return c;
    }

    public Category update(Long id, String name, Integer sortOrder, String status) {
        Category c = categoryMapper.selectById(id);
        if (c == null) throw new BizException(404, "分类不存在");
        if (name != null) c.setName(name.trim());
        if (sortOrder != null) c.setSortOrder(sortOrder);
        if (status != null) c.setStatus(status);
        categoryMapper.updateById(c);
        return c;
    }

    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BizException(404, "分类不存在");
        }
        Long count = dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>().eq(Dish::getCategoryId, id));
        if (count > 0) {
            throw new BizException(409, "该分类下有菜品，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    @Transactional
    public void batchSort(List<CategorySortRequest> sortList) {
        Set<Long> ids = new HashSet<>();
        for (CategorySortRequest item : sortList) {
            if (!ids.add(item.getId())) {
                throw new BizException("分类排序列表包含重复ID");
            }
            Category c = new Category();
            c.setId(item.getId());
            c.setSortOrder(item.getSortOrder());
            if (categoryMapper.updateById(c) == 0) {
                throw new BizException(404, "分类不存在: " + item.getId());
            }
        }
    }
}

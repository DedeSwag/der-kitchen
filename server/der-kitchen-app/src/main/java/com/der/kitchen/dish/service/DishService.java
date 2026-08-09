package com.der.kitchen.dish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.result.PageResult;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.category.entity.Category;
import com.der.kitchen.category.mapper.CategoryMapper;
import com.der.kitchen.dish.dto.DishCreateDTO;
import com.der.kitchen.dish.dto.DishQueryDTO;
import com.der.kitchen.dish.dto.DishUpdateDTO;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import com.der.kitchen.dish.vo.DishVO;
import com.der.kitchen.file.service.FileService;
import com.der.kitchen.file.vo.FileAccessVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishService {

    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;
    private final FileService fileService;

    public List<DishVO> listForUser(Long categoryId) {
        List<Long> activeCategoryIds = activeCategoryIds();
        if (activeCategoryIds.isEmpty() || (categoryId != null && !activeCategoryIds.contains(categoryId))) {
            return List.of();
        }
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getIsListed, true)
                .in(Dish::getCategoryId, activeCategoryIds)
                .orderByDesc(Dish::getCreateTime);
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        return dishMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    public DishVO getById(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (!isVisible(dish)) {
            throw new BizException(404, "菜品不存在");
        }
        return toVO(dish);
    }

    public DishVO getByIdForAdmin(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BizException(404, "菜品不存在");
        }
        return toVO(dish);
    }

    public PageResult<DishVO> pageForAdmin(DishQueryDTO query) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        if (query.getCategoryId() != null) {
            wrapper.eq(Dish::getCategoryId, query.getCategoryId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Dish::getStatus, query.getStatus());
        }
        if (query.getIsListed() != null) {
            wrapper.eq(Dish::getIsListed, query.getIsListed());
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        Page<Dish> dishPage = dishMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(
                dishPage.getRecords().stream().map(this::toVO).toList(),
                dishPage.getTotal(),
                Math.toIntExact(dishPage.getCurrent()),
                Math.toIntExact(dishPage.getSize()));
    }

    public DishVO create(DishCreateDTO dto) {
        validateCategory(dto.getCategoryId());
        fileService.validateOwnedImage(dto.getImageFileId(), SecurityUtils.requireUserId());
        Dish dish = new Dish();
        dish.setName(dto.getName().trim());
        dish.setDescription(dto.getDescription());
        dish.setImageFileId(dto.getImageFileId());
        dish.setCategoryId(dto.getCategoryId());
        dish.setCookingTime(dto.getCookingTime());
        dish.setStatus("normal");
        dish.setIsListed(true);
        dishMapper.insert(dish);
        return toVO(dish);
    }

    public DishVO update(Long id, DishUpdateDTO dto) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BizException(404, "菜品不存在");
        if (dto.getName() != null) dish.setName(dto.getName().trim());
        if (dto.getDescription() != null) dish.setDescription(dto.getDescription());
        if (dto.getImageFileId() != null) {
            fileService.validateOwnedImage(dto.getImageFileId(), SecurityUtils.requireUserId());
            dish.setImageFileId(dto.getImageFileId());
        }
        if (dto.getCategoryId() != null) {
            validateCategory(dto.getCategoryId());
            dish.setCategoryId(dto.getCategoryId());
        }
        if (dto.getCookingTime() != null) dish.setCookingTime(dto.getCookingTime());
        dishMapper.updateById(dish);
        return toVO(dish);
    }

    public void delete(Long id) {
        if (dishMapper.selectById(id) == null) {
            throw new BizException(404, "菜品不存在");
        }
        dishMapper.deleteById(id);
    }

    public void updateStatus(Long id, String status) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BizException(404, "菜品不存在");
        dish.setStatus(status);
        dishMapper.updateById(dish);
    }

    public void updateListing(Long id, boolean isListed) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) throw new BizException(404, "菜品不存在");
        dish.setIsListed(isListed);
        dishMapper.updateById(dish);
    }

    @Transactional
    public void batchListing(List<Long> ids, boolean isListed) {
        for (Long id : ids.stream().distinct().toList()) {
            updateListing(id, isListed);
        }
    }

    public Dish requireVisibleDish(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (!isVisible(dish)) {
            throw new BizException(404, "菜品不存在或未上架");
        }
        return dish;
    }

    public Dish requireOrderableDish(Long id) {
        Dish dish = requireVisibleDish(id);
        if (!"normal".equals(dish.getStatus())) {
            throw new BizException("菜品当前不可点: " + dish.getName());
        }
        return dish;
    }

    public List<DishVO> listVisibleByIds(List<Long> orderedIds) {
        if (orderedIds.isEmpty()) {
            return List.of();
        }
        List<Long> activeCategoryIds = activeCategoryIds();
        if (activeCategoryIds.isEmpty()) {
            return List.of();
        }
        java.util.Map<Long, Dish> dishes = dishMapper.selectList(
                        new LambdaQueryWrapper<Dish>()
                                .in(Dish::getId, orderedIds)
                                .eq(Dish::getIsListed, true)
                                .in(Dish::getCategoryId, activeCategoryIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Dish::getId, dish -> dish, (left, right) -> left, LinkedHashMap::new));
        return orderedIds.stream()
                .distinct()
                .map(dishes::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toVO)
                .toList();
    }

    private void validateCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BizException(404, "分类不存在");
        }
    }

    private boolean isVisible(Dish dish) {
        if (dish == null || !Boolean.TRUE.equals(dish.getIsListed())) {
            return false;
        }
        Category category = categoryMapper.selectById(dish.getCategoryId());
        return category != null && "active".equals(category.getStatus());
    }

    private List<Long> activeCategoryIds() {
        return categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().eq(Category::getStatus, "active"))
                .stream()
                .map(Category::getId)
                .toList();
    }

    /**
     * 转换为 VO，并解析图片访问地址。
     * 使用 imageFileId 动态解析预签名 URL，不在菜品表中持久化短期访问地址。
     */
    private DishVO toVO(Dish dish) {
        DishVO vo = DishVO.from(dish);
        if (dish.getImageFileId() != null) {
            try {
                FileAccessVO fileVO = fileService.getAccessUrl(dish.getImageFileId());
                vo.setImageUrl(fileVO.getUrl());
                vo.setThumbnailUrl(fileVO.getThumbnailUrl());
            } catch (BizException e) {
                log.warn("菜品图片访问地址解析失败，已降级: dishId={}, fileId={}, code={}",
                        dish.getId(), dish.getImageFileId(), e.getCode());
            }
        }
        return vo;
    }
}

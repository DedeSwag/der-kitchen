package com.der.kitchen.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.der.kitchen.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}

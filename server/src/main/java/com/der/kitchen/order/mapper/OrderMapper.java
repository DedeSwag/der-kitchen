package com.der.kitchen.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.der.kitchen.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}

package com.der.kitchen.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.der.kitchen.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 锁定订单主记录。写操作先获取同一把行锁，避免加菜和状态流转相互穿透。
     */
    @Select("""
            SELECT id
            FROM biz_order
            WHERE id = #{id} AND deleted = FALSE
            FOR UPDATE
            """)
    Long lockById(@Param("id") Long id);
}

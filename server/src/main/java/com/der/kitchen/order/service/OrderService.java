package com.der.kitchen.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.enums.OrderStatus;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.mapper.DishMapper;
import com.der.kitchen.order.dto.OrderAddItemDTO;
import com.der.kitchen.order.dto.OrderCreateDTO;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.entity.OrderItem;
import com.der.kitchen.order.mapper.OrderItemMapper;
import com.der.kitchen.order.mapper.OrderMapper;
import com.der.kitchen.order.vo.OrderDetailVO;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final UserMapper userMapper;

    @Transactional
    public Order create(Long userId, OrderCreateDTO dto) {
        // 创建订单
        Order order = new Order();
        order.setUserId(userId);
        order.setMealType(dto.getMealType());
        order.setMealDate(dto.getMealDate());
        order.setStatus(OrderStatus.PENDING.name().toLowerCase());
        order.setTasteTags(dto.getTasteTags());
        order.setDietaryNotes(dto.getDietaryNotes());
        order.setSpecialRequests(dto.getSpecialRequests());
        orderMapper.insert(order);

        // 创建订单项
        for (OrderCreateDTO.OrderItemDTO item : dto.getItems()) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish == null || !dish.getIsListed()) {
                throw new BizException("菜品不可用: " + item.getDishId());
            }
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setDishId(dish.getId());
            oi.setDishName(dish.getName());
            oi.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
            oi.setIsExtra(false);
            orderItemMapper.insert(oi);
        }

        return order;
    }

    public void cancel(Long userId, Long orderId) {
        Order order = getAndCheck(orderId, userId);
        if (!"pending".equals(order.getStatus())) {
            throw new BizException("只能取消待处理订单");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);
    }

    @Transactional
    public void addItems(Long userId, Long orderId, OrderAddItemDTO dto) {
        Order order = getAndCheck(orderId, userId);
        if (!"pending".equals(order.getStatus()) && !"preparing".equals(order.getStatus())) {
            throw new BizException("当前状态不支持加菜");
        }
        for (OrderCreateDTO.OrderItemDTO item : dto.getItems()) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish == null) throw new BizException("菜品不存在");
            OrderItem oi = new OrderItem();
            oi.setOrderId(orderId);
            oi.setDishId(dish.getId());
            oi.setDishName(dish.getName());
            oi.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
            oi.setIsExtra(true);
            orderItemMapper.insert(oi);
        }
    }

    public void transitStatus(Long orderId, String targetStatus) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");

        OrderStatus current = OrderStatus.valueOf(order.getStatus().toUpperCase());
        OrderStatus target = OrderStatus.valueOf(targetStatus.toUpperCase());
        if (!current.canTransitTo(target)) {
            throw new BizException("状态流转不合法: " + current + " -> " + target);
        }
        order.setStatus(targetStatus.toLowerCase());
        orderMapper.updateById(order);
    }

    public Page<Order> pageByUser(Long userId, OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        applyFilters(wrapper, query);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public Page<Order> pageForAdmin(OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreateTime);
        applyFilters(wrapper, query);
        return orderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
    }

    public OrderDetailVO getDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        vo.setItems(items);

        User user = userMapper.selectById(order.getUserId());
        if (user != null) vo.setUserNickname(user.getNickname());

        return vo;
    }

    public long getPendingCount() {
        return orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "pending"));
    }

    private Order getAndCheck(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (!order.getUserId().equals(userId)) throw new BizException(403, "无权操作");
        return order;
    }

    private void applyFilters(LambdaQueryWrapper<Order> wrapper, OrderQueryDTO query) {
        if (query.getStatus() != null) wrapper.eq(Order::getStatus, query.getStatus());
        if (query.getMealType() != null) wrapper.eq(Order::getMealType, query.getMealType());
        if (query.getStartDate() != null) wrapper.ge(Order::getMealDate, query.getStartDate());
        if (query.getEndDate() != null) wrapper.le(Order::getMealDate, query.getEndDate());
    }
}

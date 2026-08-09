package com.der.kitchen.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.der.kitchen.common.enums.OrderStatus;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.common.result.PageResult;
import com.der.kitchen.dish.entity.Dish;
import com.der.kitchen.dish.service.DishService;
import com.der.kitchen.notification.service.OrderEventService;
import com.der.kitchen.order.dto.OrderAddItemDTO;
import com.der.kitchen.order.dto.OrderCreateDTO;
import com.der.kitchen.order.dto.OrderQueryDTO;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.entity.OrderItem;
import com.der.kitchen.order.mapper.OrderItemMapper;
import com.der.kitchen.order.mapper.OrderMapper;
import com.der.kitchen.order.vo.OrderDetailVO;
import com.der.kitchen.order.vo.OrderItemVO;
import com.der.kitchen.order.vo.OrderVO;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Set<OrderStatus> EXTRA_ITEM_STATUSES =
            Set.of(OrderStatus.PENDING, OrderStatus.PREPARING);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishService dishService;
    private final UserMapper userMapper;
    private final OrderEventService orderEventService;

    @Transactional
    public OrderDetailVO create(Long userId, OrderCreateDTO dto) {
        validateMealDate(dto.getMealDate());
        List<Dish> dishes = validateAndLoadDishes(dto.getItems());

        Order order = new Order();
        order.setUserId(userId);
        order.setMealType(dto.getMealType());
        order.setMealDate(dto.getMealDate());
        order.setStatus(OrderStatus.PENDING.getValue());
        order.setTasteTags(normalizeTags(dto.getTasteTags()));
        order.setDietaryNotes(trimToNull(dto.getDietaryNotes()));
        order.setSpecialRequests(trimToNull(dto.getSpecialRequests()));
        orderMapper.insert(order);

        insertItems(order.getId(), dto.getItems(), dishes, false);
        orderEventService.recordCreated(order, userId, dto.getItems().size());
        return buildDetail(order);
    }

    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = lockAndCheck(orderId, userId);
        if (statusOf(order) != OrderStatus.PENDING) {
            throw new BizException(409, "订单已被接单，只能取消待处理订单");
        }
        String fromStatus = order.getStatus();
        transition(order, OrderStatus.CANCELLED);
        orderEventService.recordUserCancelled(order, userId, fromStatus);
    }

    @Transactional
    public void addItems(Long userId, Long orderId, OrderAddItemDTO dto) {
        validateUniqueDishIds(dto.getItems());
        Order order = lockAndCheck(orderId, userId);
        if (!EXTRA_ITEM_STATUSES.contains(statusOf(order))) {
            throw new BizException(409, "当前订单状态不支持加菜");
        }

        List<Dish> dishes = loadOrderableDishes(dto.getItems());
        insertItems(orderId, dto.getItems(), dishes, true);
        orderEventService.recordItemsAdded(order, userId, dto.getItems().size());
    }

    @Transactional
    public void transitStatus(Long actorId, Long orderId, String targetStatus) {
        Order order = lockOrder(orderId);
        String fromStatus = order.getStatus();
        transition(order, OrderStatus.fromValue(targetStatus));
        orderEventService.recordStatusChanged(order, actorId, fromStatus);
    }

    public PageResult<OrderVO> pageByUser(Long userId, OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        applyFilters(wrapper, query);
        Page<Order> page = orderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return buildPage(page);
    }

    public PageResult<OrderVO> pageForAdmin(OrderQueryDTO query) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreateTime);
        applyFilters(wrapper, query);
        Page<Order> page = orderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return buildPage(page);
    }

    public OrderDetailVO getDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return buildDetail(order);
    }

    public OrderDetailVO getDetailForUser(Long userId, Long orderId) {
        return buildDetail(getAndCheck(orderId, userId));
    }

    public long getPendingCount() {
        return orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.PENDING.getValue()));
    }

    private List<Dish> validateAndLoadDishes(List<OrderCreateDTO.OrderItemDTO> items) {
        validateUniqueDishIds(items);
        return loadOrderableDishes(items);
    }

    private List<Dish> loadOrderableDishes(List<OrderCreateDTO.OrderItemDTO> items) {
        return items.stream()
                .map(item -> dishService.requireOrderableDish(item.getDishId()))
                .toList();
    }

    private void validateUniqueDishIds(List<OrderCreateDTO.OrderItemDTO> items) {
        Set<Long> dishIds = new HashSet<>();
        for (OrderCreateDTO.OrderItemDTO item : items) {
            if (!dishIds.add(item.getDishId())) {
                throw new BizException("同一道菜不能在一次提交中重复，请合并数量");
            }
        }
    }

    private void validateMealDate(LocalDate mealDate) {
        if (mealDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new BizException("预约点餐最多提前1天");
        }
    }

    private void insertItems(
            Long orderId,
            List<OrderCreateDTO.OrderItemDTO> requestedItems,
            List<Dish> dishes,
            boolean extra
    ) {
        for (int index = 0; index < requestedItems.size(); index++) {
            OrderCreateDTO.OrderItemDTO requestedItem = requestedItems.get(index);
            Dish dish = dishes.get(index);
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setQuantity(requestedItem.getQuantity() == null ? 1 : requestedItem.getQuantity());
            item.setIsExtra(extra);
            orderItemMapper.insert(item);
        }
    }

    private void transition(Order order, OrderStatus target) {
        OrderStatus current = statusOf(order);
        if (!current.canTransitTo(target)) {
            throw new BizException(409,
                    "订单状态不能从 " + current.getValue() + " 变更为 " + target.getValue());
        }
        order.setStatus(target.getValue());
        if (orderMapper.updateById(order) != 1) {
            throw new BizException(409, "订单状态已发生变化，请刷新后重试");
        }
    }

    private OrderStatus statusOf(Order order) {
        try {
            return OrderStatus.fromValue(order.getStatus());
        } catch (IllegalArgumentException exception) {
            throw new BizException(409, "订单状态数据异常", exception);
        }
    }

    private Order lockOrder(Long orderId) {
        if (orderMapper.lockById(orderId) == null) {
            throw new BizException(404, "订单不存在");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        return order;
    }

    private Order lockAndCheck(Long orderId, Long userId) {
        Order order = lockOrder(orderId);
        checkOwner(order, userId);
        return order;
    }

    private Order getAndCheck(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        checkOwner(order, userId);
        return order;
    }

    private void checkOwner(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作该订单");
        }
    }

    private PageResult<OrderVO> buildPage(Page<Order> page) {
        if (page.getRecords().isEmpty()) {
            return PageResult.of(
                    List.of(), page.getTotal(),
                    Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
        }

        List<Long> orderIds = page.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderItemVO>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId, orderIds)
                                .orderByAsc(OrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getOrderId,
                        LinkedHashMap::new,
                        Collectors.mapping(OrderItemVO::from, Collectors.toList())));

        List<Long> userIds = page.getRecords().stream().map(Order::getUserId).distinct().toList();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toVO(order, itemsByOrder.getOrDefault(order.getId(), List.of()), users.get(order.getUserId())))
                .toList();
        return PageResult.of(
                records, page.getTotal(),
                Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
    }

    private OrderDetailVO buildDetail(Order order) {
        OrderDetailVO vo = OrderDetailVO.from(order);
        vo.setItems(orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, order.getId())
                                .orderByAsc(OrderItem::getId))
                .stream()
                .map(OrderItemVO::from)
                .toList());
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
        }
        return vo;
    }

    private OrderVO toVO(Order order, List<OrderItemVO> items, User user) {
        OrderVO vo = OrderVO.from(order);
        vo.setItems(items);
        if (user != null) {
            vo.setUserNickname(user.getNickname());
        }
        return vo;
    }

    private void applyFilters(LambdaQueryWrapper<Order> wrapper, OrderQueryDTO query) {
        if (query.getStatus() != null) {
            switch (query.getStatus()) {
                case "ongoing" -> wrapper.in(Order::getStatus,
                        OrderStatus.PENDING.getValue(),
                        OrderStatus.PREPARING.getValue(),
                        OrderStatus.COOKING.getValue());
                case "history" -> wrapper.in(Order::getStatus,
                        OrderStatus.COMPLETED.getValue(),
                        OrderStatus.CANCELLED.getValue());
                default -> wrapper.in(Order::getStatus,
                        Arrays.stream(query.getStatus().split(",")).distinct().toList());
            }
        }
        if (query.getMealType() != null) {
            wrapper.eq(Order::getMealType, query.getMealType());
        }
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new BizException("开始日期不能晚于结束日期");
        }
        if (query.getStartDate() != null) {
            wrapper.ge(Order::getMealDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(Order::getMealDate, query.getEndDate());
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream().map(String::trim).distinct().toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

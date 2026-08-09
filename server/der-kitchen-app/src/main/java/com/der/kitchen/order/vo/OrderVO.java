package com.der.kitchen.order.vo;

import com.der.kitchen.order.entity.Order;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String mealType;
    private LocalDate mealDate;
    private String status;
    private List<String> tasteTags;
    private String dietaryNotes;
    private String specialRequests;
    private List<OrderItemVO> items;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static OrderVO from(Order order) {
        OrderVO vo = new OrderVO();
        copyOrder(order, vo);
        return vo;
    }

    protected static void copyOrder(Order order, OrderVO vo) {
        vo.setId(order.getId());
        vo.setUserId(order.getUserId());
        vo.setMealType(order.getMealType());
        vo.setMealDate(order.getMealDate());
        vo.setStatus(order.getStatus());
        vo.setTasteTags(order.getTasteTags() == null ? List.of() : List.copyOf(order.getTasteTags()));
        vo.setItems(List.of());
        vo.setDietaryNotes(order.getDietaryNotes());
        vo.setSpecialRequests(order.getSpecialRequests());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
    }
}

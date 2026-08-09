package com.der.kitchen.common.enums;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public enum OrderStatus {
    PENDING("pending"),
    PREPARING("preparing"),
    COOKING("cooking"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, Set.of(PREPARING, CANCELLED),
            PREPARING, Set.of(COOKING, CANCELLED),
            COOKING, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitTo(OrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知订单状态: " + value);
    }
}

package com.der.kitchen.stats.vo;

public record StatsOverviewVO(
        long todayOrders,
        long pendingOrders,
        long totalDishes,
        long weekOrders
) {
}

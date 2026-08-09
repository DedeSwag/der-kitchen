package com.der.kitchen.stats.vo;

import java.time.LocalDate;

public record DailySummaryVO(
        LocalDate mealDate,
        long orderCount,
        long dishCount
) {
}

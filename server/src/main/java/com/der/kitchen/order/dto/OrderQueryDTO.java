package com.der.kitchen.order.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderQueryDTO {
    private String status;
    private String mealType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}

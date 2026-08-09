package com.der.kitchen.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderQueryDTO {
    @Pattern(
            regexp = "ongoing|history|(?:pending|preparing|cooking|completed|cancelled)(?:,(?:pending|preparing|cooking|completed|cancelled))*",
            message = "订单状态不合法"
    )
    private String status;
    @Pattern(regexp = "breakfast|lunch|dinner", message = "餐次不合法")
    private String mealType;
    private LocalDate startDate;
    private LocalDate endDate;
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 20;
}

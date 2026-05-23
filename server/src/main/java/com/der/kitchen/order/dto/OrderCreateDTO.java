package com.der.kitchen.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDTO {

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "用餐日期不能为空")
    private LocalDate mealDate;

    private String tasteTags;
    private String dietaryNotes;
    private String specialRequests;

    @NotEmpty(message = "至少选择一道菜")
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "菜品ID不能为空")
        private Long dishId;
        private Integer quantity = 1;
    }
}

package com.der.kitchen.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderCreateDTO {

    @NotBlank(message = "餐次不能为空")
    @Pattern(regexp = "breakfast|lunch|dinner", message = "餐次不合法")
    private String mealType;

    @NotNull(message = "用餐日期不能为空")
    @FutureOrPresent(message = "用餐日期不能早于今天")
    private LocalDate mealDate;

    @Size(max = 10, message = "口味标签不能超过10个")
    private List<@NotBlank(message = "口味标签不能为空") @Size(max = 20, message = "口味标签不能超过20个字符") String> tasteTags;
    @Size(max = 500, message = "忌口说明不能超过500个字符")
    private String dietaryNotes;
    @Size(max = 500, message = "特殊要求不能超过500个字符")
    private String specialRequests;

    @NotEmpty(message = "至少选择一道菜")
    @Size(max = 50, message = "单次订单最多选择50道菜")
    @Valid
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        @NotNull(message = "菜品ID不能为空")
        @Positive(message = "菜品ID必须为正数")
        private Long dishId;
        @Min(value = 1, message = "菜品数量不能小于1")
        @Max(value = 20, message = "单个菜品数量不能超过20")
        private Integer quantity = 1;
    }
}

package com.der.kitchen.common.enums;

import lombok.Getter;

@Getter
public enum DishStatus {
    NORMAL("normal"),
    OUT_OF_STOCK("out_of_stock"),
    UNAVAILABLE("unavailable");

    private final String value;

    DishStatus(String value) {
        this.value = value;
    }
}

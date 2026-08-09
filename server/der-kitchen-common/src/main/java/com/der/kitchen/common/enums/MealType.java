package com.der.kitchen.common.enums;

import lombok.Getter;

@Getter
public enum MealType {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner");

    private final String value;

    MealType(String value) {
        this.value = value;
    }
}

package com.der.kitchen.notification.vo;

public record SubscriptionConfigVO(
        boolean enabled,
        String templateId
) {
}

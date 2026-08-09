package com.der.kitchen.notification.service;

public record WechatDelivery(
        Long id,
        Long recipientId,
        Long orderId,
        String title,
        String content,
        int attemptCount
) {
}

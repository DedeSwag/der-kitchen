package com.der.kitchen.notification.service;

import com.der.kitchen.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WechatDeliveryWorker {

    private final WechatDeliveryStore deliveryStore;
    private final WechatSubscribeSender sender;
    private final AppConfig appConfig;

    @Scheduled(fixedDelayString = "${app.wx.subscribe.worker-delay-millis:3000}")
    public void deliver() {
        if (!appConfig.getWx().getSubscribe().isEnabled()) {
            return;
        }
        for (WechatDelivery delivery : deliveryStore.claimBatch(20)) {
            try {
                sender.send(delivery);
                deliveryStore.markSent(delivery.id());
            } catch (Exception exception) {
                log.warn("微信订阅消息投递失败: notificationId={}, attempt={}",
                        delivery.id(), delivery.attemptCount());
                deliveryStore.markFailed(delivery, exception.getMessage());
            }
        }
    }
}

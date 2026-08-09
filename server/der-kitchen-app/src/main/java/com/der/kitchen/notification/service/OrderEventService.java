package com.der.kitchen.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.config.AppConfig;
import com.der.kitchen.notification.entity.Notification;
import com.der.kitchen.notification.entity.OrderEvent;
import com.der.kitchen.notification.event.OrderEventCommitted;
import com.der.kitchen.notification.mapper.NotificationMapper;
import com.der.kitchen.notification.mapper.OrderEventMapper;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventMapper orderEventMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final AppConfig appConfig;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCreated(Order order, Long actorId, int itemCount) {
        OrderEvent event = insertEvent(
                order, "order_created", actorId, null, order.getStatus(), itemCount, "订单已创建");
        notifyAdmins(event, "new_order", "新订单 #" + order.getId(),
                mealDescription(order) + "，共 " + itemCount + " 道菜", true);
        publishAfterCommit(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordItemsAdded(Order order, Long actorId, int itemCount) {
        OrderEvent event = insertEvent(
                order, "items_added", actorId, order.getStatus(), order.getStatus(), itemCount, "用户追加菜品");
        notifyAdmins(event, "items_added", "订单加菜 #" + order.getId(),
                mealDescription(order) + "，新增 " + itemCount + " 道菜", false);
        publishAfterCommit(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUserCancelled(Order order, Long actorId, String fromStatus) {
        OrderEvent event = insertEvent(
                order, "user_cancelled", actorId, fromStatus, order.getStatus(), null, "用户取消订单");
        notifyAdmins(event, "order_cancelled", "订单已取消 #" + order.getId(),
                mealDescription(order) + "，用户已撤销订单", false);
        publishAfterCommit(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordStatusChanged(Order order, Long actorId, String fromStatus) {
        OrderEvent event = insertEvent(
                order, "status_changed", actorId, fromStatus, order.getStatus(), null,
                "订单状态由 " + fromStatus + " 变更为 " + order.getStatus());
        publishAfterCommit(event);
    }

    private OrderEvent insertEvent(
            Order order,
            String type,
            Long actorId,
            String fromStatus,
            String toStatus,
            Integer itemCount,
            String description
    ) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setEventType(type);
        event.setActorId(actorId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setItemCount(itemCount);
        event.setDescription(description);
        orderEventMapper.insert(event);
        return event;
    }

    private void notifyAdmins(
            OrderEvent event,
            String notificationType,
            String title,
            String content,
            boolean sendWechat
    ) {
        List<User> admins = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "admin")
                        .eq(User::getStatus, "active"));
        for (User admin : admins) {
            insertNotification(event, admin.getId(), "in_app", notificationType,
                    title, content, "delivered");
            if (sendWechat && wechatEnabled() && admin.getOpenid() != null && !admin.getOpenid().isBlank()) {
                insertNotification(event, admin.getId(), "wechat", notificationType,
                        title, content, "pending");
            }
        }
    }

    private void insertNotification(
            OrderEvent event,
            Long recipientId,
            String channel,
            String type,
            String title,
            String content,
            String deliveryStatus
    ) {
        Notification notification = new Notification();
        notification.setEventId(event.getId());
        notification.setOrderId(event.getOrderId());
        notification.setRecipientId(recipientId);
        notification.setChannel(channel);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setDeliveryStatus(deliveryStatus);
        notification.setAttemptCount(0);
        if ("pending".equals(deliveryStatus)) {
            notification.setNextAttemptTime(LocalDateTime.now());
        }
        notificationMapper.insert(notification);
    }

    private boolean wechatEnabled() {
        AppConfig.Subscribe subscribe = appConfig.getWx().getSubscribe();
        return subscribe.isEnabled()
                && hasText(appConfig.getWx().getAppId())
                && hasText(appConfig.getWx().getAppSecret())
                && hasText(subscribe.getTemplateId());
    }

    private void publishAfterCommit(OrderEvent event) {
        applicationEventPublisher.publishEvent(new OrderEventCommitted(event.getId()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String mealDescription(Order order) {
        return order.getMealDate() + " " + switch (order.getMealType()) {
            case "breakfast" -> "早餐";
            case "lunch" -> "午餐";
            case "dinner" -> "晚餐";
            default -> order.getMealType();
        };
    }
}

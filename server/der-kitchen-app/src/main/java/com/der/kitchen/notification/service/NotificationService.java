package com.der.kitchen.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.config.AppConfig;
import com.der.kitchen.notification.entity.Notification;
import com.der.kitchen.notification.mapper.NotificationMapper;
import com.der.kitchen.notification.vo.NotificationVO;
import com.der.kitchen.notification.vo.SubscriptionConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final AppConfig appConfig;

    public SubscriptionConfigVO subscriptionConfig() {
        AppConfig.Subscribe subscribe = appConfig.getWx().getSubscribe();
        boolean enabled = subscribe.isEnabled()
                && subscribe.getTemplateId() != null
                && !subscribe.getTemplateId().isBlank();
        return new SubscriptionConfigVO(enabled, enabled ? subscribe.getTemplateId() : null);
    }

    public List<NotificationVO> list(Long recipientId, Long afterId, int limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, recipientId)
                .eq(Notification::getChannel, "in_app")
                .last("LIMIT " + limit);
        if (afterId == null) {
            wrapper.orderByDesc(Notification::getId);
        } else {
            wrapper.gt(Notification::getId, afterId).orderByAsc(Notification::getId);
        }
        return notificationMapper.selectList(wrapper).stream()
                .map(NotificationVO::from)
                .toList();
    }

    public long unreadCount(Long recipientId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getRecipientId, recipientId)
                        .eq(Notification::getChannel, "in_app")
                        .isNull(Notification::getReadTime));
    }

    public void markRead(Long recipientId, Long notificationId) {
        int updated = notificationMapper.update(
                new Notification(),
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getRecipientId, recipientId)
                        .eq(Notification::getChannel, "in_app")
                        .isNull(Notification::getReadTime)
                        .set(Notification::getReadTime, LocalDateTime.now()));
        if (updated == 0 && notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getRecipientId, recipientId)
                        .eq(Notification::getChannel, "in_app")) == null) {
            throw new BizException(404, "通知不存在");
        }
    }

    public void markAllRead(Long recipientId) {
        notificationMapper.update(
                new Notification(),
                new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getRecipientId, recipientId)
                        .eq(Notification::getChannel, "in_app")
                        .isNull(Notification::getReadTime)
                        .set(Notification::getReadTime, LocalDateTime.now()));
    }
}

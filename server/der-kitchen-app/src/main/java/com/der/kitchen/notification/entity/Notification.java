package com.der.kitchen.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.der.kitchen.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class Notification extends BaseEntity {

    private Long eventId;
    private Long orderId;
    private Long recipientId;
    private String channel;
    private String notificationType;
    private String title;
    private String content;
    private String deliveryStatus;
    private Integer attemptCount;
    private LocalDateTime nextAttemptTime;
    private LocalDateTime processingTime;
    private LocalDateTime sentTime;
    private LocalDateTime readTime;
    private String lastError;
}

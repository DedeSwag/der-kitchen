package com.der.kitchen.notification.vo;

import com.der.kitchen.notification.entity.OrderEvent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRealtimeEventVO {

    private int schemaVersion = 1;
    private Long eventId;
    private String eventType;
    private Long orderId;
    private String fromStatus;
    private String toStatus;
    private Integer itemCount;
    private String description;
    private LocalDateTime createTime;
    private NotificationVO notification;

    public static AdminRealtimeEventVO from(OrderEvent event, NotificationVO notification) {
        AdminRealtimeEventVO vo = new AdminRealtimeEventVO();
        vo.setEventId(event.getId());
        vo.setEventType(event.getEventType());
        vo.setOrderId(event.getOrderId());
        vo.setFromStatus(event.getFromStatus());
        vo.setToStatus(event.getToStatus());
        vo.setItemCount(event.getItemCount());
        vo.setDescription(event.getDescription());
        vo.setCreateTime(event.getCreateTime());
        vo.setNotification(notification);
        return vo;
    }
}

package com.der.kitchen.notification.vo;

import com.der.kitchen.notification.entity.Notification;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;
    private Long orderId;
    private String type;
    private String title;
    private String content;
    private Boolean read;
    private LocalDateTime createTime;

    public static NotificationVO from(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setOrderId(notification.getOrderId());
        vo.setType(notification.getNotificationType());
        vo.setTitle(notification.getTitle());
        vo.setContent(notification.getContent());
        vo.setRead(notification.getReadTime() != null);
        vo.setCreateTime(notification.getCreateTime());
        return vo;
    }
}

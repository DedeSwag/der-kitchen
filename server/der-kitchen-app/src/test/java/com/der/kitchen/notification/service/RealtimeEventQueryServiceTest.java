package com.der.kitchen.notification.service;

import com.der.kitchen.notification.entity.Notification;
import com.der.kitchen.notification.entity.OrderEvent;
import com.der.kitchen.notification.mapper.NotificationMapper;
import com.der.kitchen.notification.mapper.OrderEventMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeEventQueryServiceTest {

    @Mock
    private OrderEventMapper orderEventMapper;

    @Mock
    private NotificationMapper notificationMapper;

    @Test
    void replaysOrderEventWithRecipientNotification() {
        OrderEvent event = orderEvent(11L, "order_created");
        Notification notification = new Notification();
        notification.setId(21L);
        notification.setEventId(11L);
        notification.setOrderId(101L);
        notification.setRecipientId(1L);
        notification.setChannel("in_app");
        notification.setNotificationType("new_order");
        notification.setTitle("新订单 #101");
        notification.setContent("晚餐，共 2 道菜");
        notification.setCreateTime(LocalDateTime.now());

        when(orderEventMapper.selectList(any())).thenReturn(List.of(event));
        when(notificationMapper.selectOne(any())).thenReturn(notification);

        RealtimeEventQueryService service =
                new RealtimeEventQueryService(orderEventMapper, notificationMapper);
        RealtimeReplayBatch batch = service.findAfter(1L, 10L);

        assertThat(batch.resetRequired()).isFalse();
        assertThat(batch.cursor()).isEqualTo(11L);
        assertThat(batch.events()).singleElement().satisfies(payload -> {
            assertThat(payload.getEventId()).isEqualTo(11L);
            assertThat(payload.getOrderId()).isEqualTo(101L);
            assertThat(payload.getNotification()).isNotNull();
            assertThat(payload.getNotification().getId()).isEqualTo(21L);
        });
    }

    @Test
    void requestsFullRefreshWhenReplayLimitIsExceeded() {
        List<OrderEvent> events = new ArrayList<>();
        for (long id = 1; id <= RealtimeEventQueryService.MAX_REPLAY_EVENTS + 1L; id++) {
            events.add(orderEvent(id, "status_changed"));
        }
        OrderEvent latest = orderEvent(900L, "status_changed");

        when(orderEventMapper.selectList(any())).thenReturn(events);
        when(orderEventMapper.selectOne(any())).thenReturn(latest);

        RealtimeEventQueryService service =
                new RealtimeEventQueryService(orderEventMapper, notificationMapper);
        RealtimeReplayBatch batch = service.findAfter(1L, 0L);

        assertThat(batch.resetRequired()).isTrue();
        assertThat(batch.cursor()).isEqualTo(900L);
        assertThat(batch.events()).isEmpty();
    }

    private OrderEvent orderEvent(Long id, String type) {
        OrderEvent event = new OrderEvent();
        event.setId(id);
        event.setOrderId(101L);
        event.setEventType(type);
        event.setToStatus("pending");
        event.setDescription("订单事件");
        event.setCreateTime(LocalDateTime.now());
        return event;
    }
}

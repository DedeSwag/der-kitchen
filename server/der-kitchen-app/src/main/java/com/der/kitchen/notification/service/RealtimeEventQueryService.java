package com.der.kitchen.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.notification.entity.Notification;
import com.der.kitchen.notification.entity.OrderEvent;
import com.der.kitchen.notification.mapper.NotificationMapper;
import com.der.kitchen.notification.mapper.OrderEventMapper;
import com.der.kitchen.notification.vo.AdminRealtimeEventVO;
import com.der.kitchen.notification.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeEventQueryService {

    static final int MAX_REPLAY_EVENTS = 500;

    private final OrderEventMapper orderEventMapper;
    private final NotificationMapper notificationMapper;

    public long latestCursor() {
        OrderEvent latest = orderEventMapper.selectOne(
                new LambdaQueryWrapper<OrderEvent>()
                        .orderByDesc(OrderEvent::getId)
                        .last("LIMIT 1"));
        return latest == null ? 0L : latest.getId();
    }

    public AdminRealtimeEventVO findForRecipient(Long eventId, Long recipientId) {
        OrderEvent event = orderEventMapper.selectById(eventId);
        if (event == null) {
            return null;
        }
        return toRealtimeEvent(event, recipientId);
    }

    public RealtimeReplayBatch findAfter(Long recipientId, long cursor) {
        List<OrderEvent> events = orderEventMapper.selectList(
                new LambdaQueryWrapper<OrderEvent>()
                        .gt(OrderEvent::getId, cursor)
                        .orderByAsc(OrderEvent::getId)
                        .last("LIMIT " + (MAX_REPLAY_EVENTS + 1)));
        if (events.size() > MAX_REPLAY_EVENTS) {
            return RealtimeReplayBatch.reset(latestCursor());
        }
        List<AdminRealtimeEventVO> payloads = events.stream()
                .map(event -> toRealtimeEvent(event, recipientId))
                .toList();
        long replayCursor = events.isEmpty() ? cursor : events.get(events.size() - 1).getId();
        return RealtimeReplayBatch.replay(payloads, replayCursor);
    }

    private AdminRealtimeEventVO toRealtimeEvent(OrderEvent event, Long recipientId) {
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getEventId, event.getId())
                        .eq(Notification::getRecipientId, recipientId)
                        .eq(Notification::getChannel, "in_app"));
        return AdminRealtimeEventVO.from(
                event, notification == null ? null : NotificationVO.from(notification));
    }
}

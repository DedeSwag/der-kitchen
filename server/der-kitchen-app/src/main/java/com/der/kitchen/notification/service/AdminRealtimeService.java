package com.der.kitchen.notification.service;

import com.der.kitchen.notification.vo.RealtimeBootstrapVO;
import com.der.kitchen.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class AdminRealtimeService {

    private final RealtimeEventQueryService eventQueryService;
    private final SseSessionRegistry sessionRegistry;
    private final OrderService orderService;

    public RealtimeBootstrapVO bootstrap() {
        // Cursor first: an event committed between these reads may be counted twice, but can never be skipped.
        long cursor = eventQueryService.latestCursor();
        long pendingCount = orderService.getPendingCount();
        return new RealtimeBootstrapVO(cursor, pendingCount);
    }

    public SseEmitter subscribe(Long recipientId, Long requestedCursor) {
        long latestCursor = eventQueryService.latestCursor();
        long cursor = requestedCursor == null
                ? latestCursor
                : Math.min(requestedCursor, latestCursor);
        RealtimeConnection connection = sessionRegistry.open(recipientId, cursor);
        if (!connection.sendReady(cursor)) {
            sessionRegistry.remove(recipientId, connection);
            return connection.emitter();
        }

        RealtimeReplayBatch replay = eventQueryService.findAfter(recipientId, cursor);
        if (!connection.finishInitialization(replay)) {
            sessionRegistry.remove(recipientId, connection);
        }
        return connection.emitter();
    }
}

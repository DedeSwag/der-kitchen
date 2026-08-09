package com.der.kitchen.notification.service;

import com.der.kitchen.notification.event.OrderEventCommitted;
import com.der.kitchen.notification.vo.AdminRealtimeEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeOrderEventListener {

    private final RealtimeEventQueryService eventQueryService;
    private final SseSessionRegistry sessionRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderEventCommitted(OrderEventCommitted committed) {
        for (Long recipientId : sessionRegistry.connectedRecipientIds()) {
            try {
                AdminRealtimeEventVO event = eventQueryService.findForRecipient(
                        committed.eventId(), recipientId);
                if (event != null) {
                    sessionRegistry.deliver(recipientId, event);
                }
            } catch (RuntimeException exception) {
                // Real-time delivery is best effort; the persisted event is replayed after reconnect.
                log.warn("推送订单实时事件失败，eventId={}, recipientId={}",
                        committed.eventId(), recipientId, exception);
            }
        }
    }
}

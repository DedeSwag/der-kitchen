package com.der.kitchen.notification.service;

import com.der.kitchen.notification.vo.AdminRealtimeEventVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class RealtimeConnection {

    private static final int MAX_PENDING_EVENTS = 1_000;

    private final SseEmitter emitter;
    private final TreeMap<Long, AdminRealtimeEventVO> pendingEvents = new TreeMap<>();
    private boolean initializing = true;
    private boolean closed;
    private long lastSentEventId;

    RealtimeConnection(SseEmitter emitter, long cursor) {
        this.emitter = emitter;
        this.lastSentEventId = cursor;
    }

    SseEmitter emitter() {
        return emitter;
    }

    synchronized boolean sendReady(long cursor) {
        return sendControl("realtime.ready", Map.of("cursor", cursor));
    }

    synchronized boolean deliver(AdminRealtimeEventVO event) {
        if (closed || event.getEventId() <= lastSentEventId) {
            return !closed;
        }
        if (initializing) {
            pendingEvents.put(event.getEventId(), event);
            if (pendingEvents.size() > MAX_PENDING_EVENTS) {
                closeWithError(new IllegalStateException("SSE 客户端初始化期间积压事件过多"));
                return false;
            }
            return true;
        }
        return sendOrderEvent(event);
    }

    synchronized boolean finishInitialization(RealtimeReplayBatch batch) {
        if (closed) {
            return false;
        }

        if (batch.resetRequired()) {
            if (!sendCursorControl("realtime.reset", batch.cursor(),
                    Map.of("cursor", batch.cursor(), "reason", "replay_limit_exceeded"))) {
                return false;
            }
        } else {
            TreeMap<Long, AdminRealtimeEventVO> ordered = new TreeMap<>();
            for (AdminRealtimeEventVO event : batch.events()) {
                ordered.put(event.getEventId(), event);
            }
            ordered.putAll(pendingEvents);
            for (AdminRealtimeEventVO event : ordered.values()) {
                if (!sendOrderEvent(event)) {
                    return false;
                }
            }
        }

        if (batch.resetRequired()) {
            for (AdminRealtimeEventVO event : pendingEvents.tailMap(lastSentEventId, false).values()) {
                if (!sendOrderEvent(event)) {
                    return false;
                }
            }
        }
        pendingEvents.clear();
        initializing = false;
        return true;
    }

    synchronized boolean heartbeat() {
        if (closed) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
            return true;
        } catch (IOException | IllegalStateException exception) {
            closeWithError(exception);
            return false;
        }
    }

    synchronized void close() {
        if (!closed) {
            closed = true;
            emitter.complete();
        }
    }

    private boolean sendOrderEvent(AdminRealtimeEventVO event) {
        if (event.getEventId() <= lastSentEventId) {
            return true;
        }
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(event.getEventId()))
                    .name("order.event")
                    .data(event));
            lastSentEventId = event.getEventId();
            return true;
        } catch (IOException | IllegalStateException exception) {
            closeWithError(exception);
            return false;
        }
    }

    private boolean sendControl(String name, Object data) {
        if (closed) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException | IllegalStateException exception) {
            closeWithError(exception);
            return false;
        }
    }

    private boolean sendCursorControl(String name, long cursor, Object data) {
        if (closed) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(cursor))
                    .name(name)
                    .data(data));
            lastSentEventId = cursor;
            return true;
        } catch (IOException | IllegalStateException exception) {
            closeWithError(exception);
            return false;
        }
    }

    private void closeWithError(Exception exception) {
        if (!closed) {
            closed = true;
            emitter.completeWithError(exception);
        }
    }
}

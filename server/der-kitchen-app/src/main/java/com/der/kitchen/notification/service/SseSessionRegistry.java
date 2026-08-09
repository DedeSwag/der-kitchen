package com.der.kitchen.notification.service;

import com.der.kitchen.notification.vo.AdminRealtimeEventVO;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseSessionRegistry {

    private static final long CONNECTION_TIMEOUT_MILLIS = 30L * 60L * 1_000L;

    private final ConcurrentHashMap<Long, Set<RealtimeConnection>> connections = new ConcurrentHashMap<>();

    public RealtimeConnection open(Long recipientId, long cursor) {
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MILLIS);
        RealtimeConnection connection = new RealtimeConnection(emitter, cursor);
        connections.computeIfAbsent(recipientId, ignored -> ConcurrentHashMap.newKeySet()).add(connection);

        Runnable cleanup = () -> remove(recipientId, connection);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            connection.close();
            cleanup.run();
        });
        emitter.onError(ignored -> cleanup.run());
        return connection;
    }

    public Set<Long> connectedRecipientIds() {
        return Set.copyOf(connections.keySet());
    }

    public void deliver(Long recipientId, AdminRealtimeEventVO event) {
        Set<RealtimeConnection> recipientConnections = connections.get(recipientId);
        if (recipientConnections == null) {
            return;
        }
        for (RealtimeConnection connection : List.copyOf(recipientConnections)) {
            if (!connection.deliver(event)) {
                remove(recipientId, connection);
            }
        }
    }

    public void remove(Long recipientId, RealtimeConnection connection) {
        connections.computeIfPresent(recipientId, (ignored, current) -> {
            current.remove(connection);
            return current.isEmpty() ? null : current;
        });
    }

    @Scheduled(fixedDelayString = "${app.realtime.heartbeat-millis:20000}")
    public void heartbeat() {
        connections.forEach((recipientId, recipientConnections) -> {
            for (RealtimeConnection connection : List.copyOf(recipientConnections)) {
                if (!connection.heartbeat()) {
                    remove(recipientId, connection);
                }
            }
        });
    }

    @PreDestroy
    public void closeAll() {
        connections.values().forEach(recipientConnections ->
                recipientConnections.forEach(RealtimeConnection::close));
        connections.clear();
    }
}

package com.der.kitchen.notification.event;

/**
 * Published inside the order transaction and consumed only after it commits.
 */
public record OrderEventCommitted(Long eventId) {
}

package com.der.kitchen.notification.service;

import com.der.kitchen.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WechatDeliveryStore {

    private final JdbcTemplate jdbcTemplate;
    private final AppConfig appConfig;

    @Transactional
    public List<WechatDelivery> claimBatch(int batchSize) {
        AppConfig.Subscribe config = appConfig.getWx().getSubscribe();
        String sql = """
                WITH candidates AS (
                    SELECT id
                    FROM sys_notification
                    WHERE channel = 'wechat' AND deleted = FALSE
                      AND attempt_count < ?
                      AND (
                          (delivery_status IN ('pending', 'failed')
                              AND COALESCE(next_attempt_time, clock_timestamp()) <= clock_timestamp())
                          OR
                          (delivery_status = 'processing'
                              AND processing_time <= clock_timestamp() - (? * INTERVAL '1 second'))
                      )
                    ORDER BY id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE sys_notification notification
                SET delivery_status = 'processing',
                    attempt_count = notification.attempt_count + 1,
                    processing_time = clock_timestamp(),
                    next_attempt_time = NULL,
                    last_error = NULL,
                    update_time = clock_timestamp(),
                    update_by = 'system'
                FROM candidates
                WHERE notification.id = candidates.id
                RETURNING notification.id, notification.recipient_id, notification.order_id,
                          notification.title, notification.content, notification.attempt_count
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new WechatDelivery(
                        rs.getLong("id"),
                        rs.getLong("recipient_id"),
                        rs.getLong("order_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getInt("attempt_count")),
                config.getMaxAttempts(),
                config.getProcessingTimeoutSeconds(),
                batchSize);
    }

    public void markSent(Long deliveryId) {
        int updated = jdbcTemplate.update("""
                UPDATE sys_notification
                SET delivery_status = 'sent', sent_time = clock_timestamp(),
                    processing_time = NULL, next_attempt_time = NULL, last_error = NULL,
                    update_time = clock_timestamp(), update_by = 'system'
                WHERE id = ? AND channel = 'wechat' AND delivery_status = 'processing'
                """, deliveryId);
        requireSingleUpdate(updated, deliveryId);
    }

    public void markFailed(WechatDelivery delivery, String error) {
        AppConfig.Subscribe config = appConfig.getWx().getSubscribe();
        boolean exhausted = delivery.attemptCount() >= config.getMaxAttempts();
        long multiplier = 1L << Math.min(Math.max(delivery.attemptCount() - 1, 0), 10);
        long delaySeconds = Math.min(config.getRetryBaseSeconds() * multiplier, 3600L);
        int updated = jdbcTemplate.update("""
                UPDATE sys_notification
                SET delivery_status = ?, processing_time = NULL,
                    next_attempt_time = CASE WHEN ? THEN NULL
                        ELSE clock_timestamp() + (? * INTERVAL '1 second') END,
                    last_error = ?, update_time = clock_timestamp(), update_by = 'system'
                WHERE id = ? AND channel = 'wechat' AND delivery_status = 'processing'
                """,
                exhausted ? "dead" : "failed",
                exhausted,
                delaySeconds,
                truncate(error, 500),
                delivery.id());
        requireSingleUpdate(updated, delivery.id());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "微信订阅消息发送失败";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void requireSingleUpdate(int updated, Long deliveryId) {
        if (updated != 1) {
            throw new IllegalStateException("微信通知投递状态已变化: notificationId=" + deliveryId);
        }
    }
}

package com.der.kitchen.notification.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.notification.service.NotificationService;
import com.der.kitchen.notification.vo.NotificationVO;
import com.der.kitchen.notification.vo.SubscriptionConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "通知-管理端")
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@Validated
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "查询微信订阅消息配置")
    @GetMapping("/subscription-config")
    public R<SubscriptionConfigVO> subscriptionConfig() {
        return R.ok(notificationService.subscriptionConfig());
    }

    @Operation(summary = "查询站内通知；afterId用于增量轮询")
    @GetMapping
    public R<List<NotificationVO>> list(
            @RequestParam(required = false) @PositiveOrZero Long afterId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return R.ok(notificationService.list(SecurityUtils.requireUserId(), afterId, limit));
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount(SecurityUtils.requireUserId()));
    }

    @Operation(summary = "标记单条通知已读")
    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable @Positive Long id) {
        notificationService.markRead(SecurityUtils.requireUserId(), id);
        return R.ok();
    }

    @Operation(summary = "标记全部通知已读")
    @PostMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.requireUserId());
        return R.ok();
    }
}

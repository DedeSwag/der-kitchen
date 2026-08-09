package com.der.kitchen.notification.controller;

import com.der.kitchen.common.result.R;
import com.der.kitchen.common.util.SecurityUtils;
import com.der.kitchen.notification.service.AdminRealtimeService;
import com.der.kitchen.notification.vo.RealtimeBootstrapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "实时事件-管理端")
@RestController
@RequestMapping("/api/v1/admin/realtime")
@RequiredArgsConstructor
@Validated
public class AdminRealtimeController {

    private final AdminRealtimeService realtimeService;

    @Operation(summary = "初始化实时事件游标与待处理订单数")
    @GetMapping("/bootstrap")
    public R<RealtimeBootstrapVO> bootstrap() {
        return R.ok(realtimeService.bootstrap());
    }

    @Operation(summary = "订阅管理端订单实时事件")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader(value = "Last-Event-ID", required = false)
            @PositiveOrZero Long lastEventId,
            @RequestParam(required = false)
            @PositiveOrZero Long cursor,
            HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        Long requestedCursor = lastEventId != null ? lastEventId : cursor;
        return realtimeService.subscribe(SecurityUtils.requireUserId(), requestedCursor);
    }
}

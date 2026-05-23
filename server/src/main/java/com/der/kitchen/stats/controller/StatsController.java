package com.der.kitchen.stats.controller;

import com.der.kitchen.common.annotation.RequireRole;
import com.der.kitchen.common.result.R;
import com.der.kitchen.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "统计模块")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequireRole("admin")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "工作台概览")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statsService.overview());
    }

    @Operation(summary = "高频菜品排行")
    @GetMapping("/top-dishes")
    public R<List<Map<String, Object>>> topDishes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(statsService.topDishes(startDate, endDate, limit));
    }

    @Operation(summary = "每日汇总")
    @GetMapping("/daily")
    public R<List<Map<String, Object>>> daily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(statsService.dailySummary(startDate, endDate));
    }
}

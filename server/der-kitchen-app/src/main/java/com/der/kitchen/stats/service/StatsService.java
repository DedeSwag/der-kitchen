package com.der.kitchen.stats.service;

import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.stats.vo.DailySummaryVO;
import com.der.kitchen.stats.vo.StatsOverviewVO;
import com.der.kitchen.stats.vo.TopDishVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long MAX_RANGE_DAYS = 365;

    private final JdbcTemplate jdbcTemplate;

    public StatsOverviewVO overview() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new StatsOverviewVO(
                queryLong("""
                        SELECT COUNT(*) FROM biz_order
                        WHERE meal_date = ? AND status <> 'cancelled' AND deleted = FALSE
                        """, today),
                queryLong("""
                        SELECT COUNT(*) FROM biz_order
                        WHERE status = 'pending' AND deleted = FALSE
                        """),
                queryLong("""
                        SELECT COUNT(*) FROM dish_info
                        WHERE is_listed = TRUE AND deleted = FALSE
                        """),
                queryLong("""
                        SELECT COUNT(*) FROM biz_order
                        WHERE meal_date BETWEEN ? AND ?
                          AND status <> 'cancelled' AND deleted = FALSE
                        """, weekStart, weekEnd));
    }

    public List<TopDishVO> topDishes(LocalDate startDate, LocalDate endDate, int limit) {
        validateDateRange(startDate, endDate);
        String sql = """
                SELECT oi.dish_id,
                       (ARRAY_AGG(oi.dish_name ORDER BY oi.id DESC))[1] AS dish_name,
                       SUM(oi.quantity) AS total_count
                FROM biz_order_item oi
                JOIN biz_order o ON o.id = oi.order_id
                WHERE o.deleted = FALSE AND oi.deleted = FALSE
                  AND o.status <> 'cancelled'
                  AND o.meal_date BETWEEN ? AND ?
                GROUP BY oi.dish_id
                ORDER BY total_count DESC, oi.dish_id
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new TopDishVO(
                        rs.getLong("dish_id"),
                        rs.getString("dish_name"),
                        rs.getLong("total_count")),
                startDate,
                endDate,
                limit);
    }

    public List<DailySummaryVO> dailySummary(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        String sql = """
                WITH dates AS (
                    SELECT day::date AS meal_date
                    FROM generate_series(CAST(? AS date), CAST(? AS date), INTERVAL '1 day') day
                ), order_totals AS (
                    SELECT o.id, o.meal_date, COALESCE(SUM(oi.quantity), 0) AS dish_count
                    FROM biz_order o
                    LEFT JOIN biz_order_item oi
                      ON oi.order_id = o.id AND oi.deleted = FALSE
                    WHERE o.deleted = FALSE AND o.status <> 'cancelled'
                      AND o.meal_date BETWEEN ? AND ?
                    GROUP BY o.id, o.meal_date
                )
                SELECT dates.meal_date,
                       COUNT(order_totals.id) AS order_count,
                       COALESCE(SUM(order_totals.dish_count), 0) AS dish_count
                FROM dates
                LEFT JOIN order_totals ON order_totals.meal_date = dates.meal_date
                GROUP BY dates.meal_date
                ORDER BY dates.meal_date
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DailySummaryVO(
                        rs.getObject("meal_date", LocalDate.class),
                        rs.getLong("order_count"),
                        rs.getLong("dish_count")),
                startDate,
                endDate,
                startDate,
                endDate);
    }

    private long queryLong(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BizException("开始日期不能晚于结束日期");
        }
        if (startDate.plusDays(MAX_RANGE_DAYS).isBefore(endDate)) {
            throw new BizException("统计日期范围不能超过366个自然日");
        }
    }
}

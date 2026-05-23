package com.der.kitchen.stats.service;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final SqlSessionFactory sqlSessionFactory;

    /**
     * 工作台概览
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection()) {

            // 今日订单数
            result.put("todayOrders", queryLong(conn,
                    "SELECT COUNT(*) FROM orders WHERE meal_date = CURRENT_DATE AND deleted = false"));
            // 待处理订单
            result.put("pendingOrders", queryLong(conn,
                    "SELECT COUNT(*) FROM orders WHERE status = 'pending' AND deleted = false"));
            // 菜品总数
            result.put("totalDishes", queryLong(conn,
                    "SELECT COUNT(*) FROM dishes WHERE deleted = false AND is_listed = true"));
            // 本周订单数
            result.put("weekOrders", queryLong(conn,
                    "SELECT COUNT(*) FROM orders WHERE meal_date >= date_trunc('week', CURRENT_DATE) AND deleted = false"));
        } catch (Exception e) {
            throw new RuntimeException("统计查询失败", e);
        }
        return result;
    }

    /**
     * 高频菜品 TOP N
     */
    public List<Map<String, Object>> topDishes(LocalDate startDate, LocalDate endDate, int limit) {
        String sql = """
                SELECT oi.dish_name, SUM(oi.quantity) AS total_count
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.deleted = false AND oi.deleted = false
                  AND o.meal_date >= ? AND o.meal_date <= ?
                GROUP BY oi.dish_name
                ORDER BY total_count DESC
                LIMIT ?
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, startDate);
            ps.setObject(2, endDate);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("dishName", rs.getString("dish_name"));
                row.put("totalCount", rs.getLong("total_count"));
                list.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("统计查询失败", e);
        }
        return list;
    }

    /**
     * 每日订单汇总
     */
    public List<Map<String, Object>> dailySummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT meal_date, COUNT(*) AS order_count, 
                       SUM((SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = o.id AND oi.deleted = false)) AS dish_count
                FROM orders o
                WHERE o.deleted = false AND o.meal_date >= ? AND o.meal_date <= ?
                GROUP BY meal_date
                ORDER BY meal_date
                """;
        List<Map<String, Object>> list = new ArrayList<>();
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection conn = session.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, startDate);
            ps.setObject(2, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("mealDate", rs.getObject("meal_date").toString());
                row.put("orderCount", rs.getLong("order_count"));
                row.put("dishCount", rs.getLong("dish_count"));
                list.add(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("统计查询失败", e);
        }
        return list;
    }

    private long queryLong(Connection conn, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}

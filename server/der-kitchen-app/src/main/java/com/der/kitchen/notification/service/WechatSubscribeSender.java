package com.der.kitchen.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.config.AppConfig;
import com.der.kitchen.order.entity.Order;
import com.der.kitchen.order.entity.OrderItem;
import com.der.kitchen.order.mapper.OrderItemMapper;
import com.der.kitchen.order.mapper.OrderMapper;
import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WechatSubscribeSender {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestClient.Builder restClientBuilder;
    private final AppConfig appConfig;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private volatile String cachedAccessToken;
    private volatile LocalDateTime accessTokenExpiresAt = LocalDateTime.MIN;

    public void send(WechatDelivery delivery) {
        User recipient = userMapper.selectById(delivery.recipientId());
        Order order = orderMapper.selectById(delivery.orderId());
        if (recipient == null || recipient.getOpenid() == null || order == null) {
            throw new BizException(502, "微信消息收件人或订单不存在");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .orderByAsc(OrderItem::getId));

        AppConfig.Subscribe config = appConfig.getWx().getSubscribe();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(config.getMealKey(), Map.of("value", truncate(order.getMealDate() + " " + mealName(order), 20)));
        data.put(config.getSummaryKey(), Map.of("value", truncate(itemSummary(items), 20)));
        data.put(config.getTimeKey(), Map.of("value", order.getCreateTime().format(TIME_FORMAT)));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("touser", recipient.getOpenid());
        request.put("template_id", config.getTemplateId());
        request.put("page", config.getPage().replace("{orderId}", order.getId().toString()));
        request.put("data", data);

        JsonNode response = restClientBuilder.build().post()
                .uri(SEND_URL + "?access_token={token}", accessToken())
                .body(request)
                .retrieve()
                .body(JsonNode.class);
        int errorCode = response == null ? -1 : response.path("errcode").asInt(-1);
        if (errorCode != 0) {
            throw new BizException(502,
                    "微信订阅消息发送失败: " + errorCode + " "
                            + (response == null ? "empty response" : response.path("errmsg").asText("")));
        }
    }

    private synchronized String accessToken() {
        if (cachedAccessToken != null && LocalDateTime.now().isBefore(accessTokenExpiresAt)) {
            return cachedAccessToken;
        }
        JsonNode response = restClientBuilder.build().get()
                .uri(TOKEN_URL + "?grant_type=client_credential&appid={appId}&secret={secret}",
                        appConfig.getWx().getAppId(), appConfig.getWx().getAppSecret())
                .retrieve()
                .body(JsonNode.class);
        String token = response == null ? null : response.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            int errorCode = response == null ? -1 : response.path("errcode").asInt(-1);
            throw new BizException(502, "获取微信 access_token 失败: " + errorCode);
        }
        long expiresIn = response.path("expires_in").asLong(7200);
        cachedAccessToken = token;
        accessTokenExpiresAt = LocalDateTime.now().plusSeconds(Math.max(60, expiresIn - 300));
        return cachedAccessToken;
    }

    private String mealName(Order order) {
        return switch (order.getMealType()) {
            case "breakfast" -> "早餐";
            case "lunch" -> "午餐";
            case "dinner" -> "晚餐";
            default -> order.getMealType();
        };
    }

    private String itemSummary(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "请查看订单详情";
        }
        return items.stream()
                .limit(2)
                .map(item -> item.getDishName() + "×" + item.getQuantity())
                .reduce((left, right) -> left + "、" + right)
                .orElse("请查看订单详情");
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

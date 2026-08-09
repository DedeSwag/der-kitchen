package com.der.kitchen.auth.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.der.kitchen.common.exception.BizException;
import com.der.kitchen.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 微信小程序工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxUtil {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final AppConfig appConfig;

    public String code2Openid(String code) {
        String appId = appConfig.getWx().getAppId();
        String appSecret = appConfig.getWx().getAppSecret();
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BizException(503, "微信登录服务未配置");
        }

        String url = UriComponentsBuilder.fromHttpUrl(CODE2SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUriString();
        try {
            JSONObject json = JSONUtil.parseObj(HttpUtil.get(url, 5000));
            Integer errorCode = json.getInt("errcode");
            if (errorCode != null && errorCode != 0) {
                log.warn("微信 code2session 调用失败: errorCode={}", errorCode);
                throw new BizException(502, "微信登录服务返回错误");
            }
            String openid = json.getStr("openid");
            if (openid == null || openid.isBlank()) {
                log.warn("微信 code2session 响应缺少 openid");
                throw new BizException(502, "微信登录服务返回错误");
            }
            return openid;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 code2session 调用异常: type={}", e.getClass().getSimpleName());
            throw new BizException(502, "微信登录服务暂时不可用");
        }
    }
}

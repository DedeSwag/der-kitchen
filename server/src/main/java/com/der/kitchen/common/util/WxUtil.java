package com.der.kitchen.common.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.der.kitchen.common.config.AppConfig;
import com.der.kitchen.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信小程序工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxUtil {

    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final AppConfig appConfig;

    /**
     * 通过 code 换取 openid
     */
    public String code2Openid(String code) {
        String url = String.format(CODE2SESSION_URL,
                appConfig.getWx().getAppId(),
                appConfig.getWx().getAppSecret(),
                code);

        String response = HttpUtil.get(url, 5000);
        log.debug("微信 code2session 响应: {}", response);

        JSONObject json = JSONUtil.parseObj(response);
        if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
            log.error("微信登录失败: {}", response);
            throw new BizException("微信登录失败: " + json.getStr("errmsg"));
        }

        return json.getStr("openid");
    }
}

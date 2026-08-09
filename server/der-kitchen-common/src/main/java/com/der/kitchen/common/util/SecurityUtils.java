package com.der.kitchen.common.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 当前登录用户访问入口，避免业务模块直接解析 token。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getUserId() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    public static Long requireUserId() {
        StpUtil.checkLogin();
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }
}

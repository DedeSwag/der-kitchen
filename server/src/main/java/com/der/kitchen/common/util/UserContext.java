package com.der.kitchen.common.util;

import lombok.Data;

/**
 * 当前登录用户上下文信息
 */
@Data
public class UserContext {
    private Long userId;
    private String openid;
    private String nickname;
    private String role;
}

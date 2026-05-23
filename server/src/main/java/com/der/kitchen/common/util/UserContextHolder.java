package com.der.kitchen.common.util;

/**
 * 基于 ThreadLocal 的用户上下文持有器
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserContext ctx = get();
        return ctx != null ? ctx.getUserId() : null;
    }

    public static String getRole() {
        UserContext ctx = get();
        return ctx != null ? ctx.getRole() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

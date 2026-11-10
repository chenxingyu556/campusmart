package com.campusmart.common;

/**
 * 当前登录用户上下文
 * 拦截器解析 token 后写入，业务层随时读取，请求结束时清理
 */

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new TokenException("未登录");
        }
        return id;
    }

    /** 必须由拦截器在请求结束时调用 */
    public static void clear() {
        USER_ID.remove();
    }
}

package com.github.wrx886.e2echo.server.common;

import com.github.wrx886.e2echo.server.model.entity.User;

public class LoginHold {
    private static ThreadLocal<User> threadLocal = new ThreadLocal<>();

    public static void setUser(User user) {
        threadLocal.set(user);
    }

    public static User getUser() {
        return threadLocal.get();
    }

    public static void remove() {
        threadLocal.remove();
    }

}

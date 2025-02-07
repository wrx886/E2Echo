package com.github.wrx886.e2echo.server.common;

public class RedisPrefix {

    // 登入验证码的 Redis Key 前缀
    public static final String LOGIN_CAPTCHA_PREFIX = "e2echo:login:captcha:";

    // 登入手机验证码的 Redis Key 前缀
    public static final String LOGIN_PHONE_CODE_PREFIX = "e2echo:login:phoneCode:";

    // 登入签名码的 Redis Key 前缀
    public static final String LOGIN_SIGN_CODE_PREFIX = "e2echo:login:signCode:";

    // 用户换绑业务的旧手机验证码的 Redis Key 前缀
    public static final String USER_CHANGE_OLD_CODE_PREFIX = "e2echo:user:change:oldCode:";

    // 用户换绑业务的新手机验证码的 Redis Key 前缀
    public static final String USER_CHANGE_NEW_CODE_PREFIX = "e2echo:user:change:newCode:";

    // 用户换绑逻辑的签名码的 Redis Key 前缀
    public static final String USER_CHANGE_SIGN_CODE_PREFIX = "e2echo:user:change:signCode:";

}

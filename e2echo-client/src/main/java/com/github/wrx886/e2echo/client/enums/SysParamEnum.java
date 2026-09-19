package com.github.wrx886.e2echo.client.enums;

/**
 * 系统参数名枚举。
 *
 * <p>枚举项名称就是存进 {@code sys_param} 表的参数名，因此枚举项用大写加下划线命名即可，取值时
 * 由 {@code SysParamService.find(SysParamEnum)} 等重载自动转换。</p>
 */
public enum SysParamEnum {

    /**
     * 上次拉取消息的时间。
     */
    LAST_PULL_TIME,

    /**
     * 当前配置是否正在使用，用于防止重复登入。
     */
    IS_USED

}

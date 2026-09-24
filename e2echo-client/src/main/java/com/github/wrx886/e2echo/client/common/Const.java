package com.github.wrx886.e2echo.client.common;

/**
 * 常量定义。
 */
public final class Const {

    /**
     * 服务器与客户端的最大时间差距（毫秒）。
     */
    public static final long MAX_TIME_DIFF_MS = 5 * 1000L;

    /**
     * AUTH 票据的有效时间（毫秒）。
     */
    public static final long AUTH_EXPIRED_TIME_MS = 60 * 1000L;

    /**
     * 可接收消息的时间范围（毫秒），超出视为历史或未来消息。
     */
    public static final long MAX_MESSAGE_DIFF_MS = 60 * 1000L;

    /**
     * 拉取消息时每批的条数。
     */
    public static final int MESSAGE_PULL_BATCH_SIZE = 128;

}

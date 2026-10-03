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
     * 群密钥轮换后，旧版本密钥仍被接受的容忍时间（毫秒）。
     *
     * <p>容忍是为了照顾还在用旧密钥加密的消息，以及通知、拉取的延迟；超过这个时间还用旧版本密钥
     * 加密的群聊消息会被拒收。</p>
     */
    public static final long AES_KEY_EXPIRED_TIME_MS = 60 * 1000L;

    /**
     * 拉取消息时每批的条数。
     */
    public static final int MESSAGE_PULL_BATCH_SIZE = 128;

    /**
     * 聊天文件的大小上限（字节）。
     *
     * <p>文件本体加密后上传到对象存储，这里限制的是上传前的明文大小。</p>
     */
    public static final long FILE_MAX_SIZE_BYTE = 25L * 1024 * 1024;

    /**
     * 文件大小超限时的提示文案，供各处校验统一使用。
     */
    public static final String FILE_MAX_SIZE_MESSAGE = "文件大小应小于 " + FILE_MAX_SIZE_BYTE / 1024 / 1024 + "M";

}

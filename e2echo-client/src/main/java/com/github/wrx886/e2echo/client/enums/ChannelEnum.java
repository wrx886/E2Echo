package com.github.wrx886.e2echo.client.enums;

/**
 * 消息通道枚举。
 */
public enum ChannelEnum {

    /**
     * 私聊，正文用 ECC 加解密。
     */
    CHAT_PRIVATE_ECC,

    /**
     * 群聊，正文用 AES 加解密。
     */
    CHAT_GROUP_AES,

    /**
     * 广播用户资料，正文不加密（明文存服务端）。
     */
    BROADCAST_USERINFO_NONE,

    /**
     * 广播消息与评论，正文不加密（明文存服务端）。
     */
    BROADCAST_MESSAGE_NONE

}

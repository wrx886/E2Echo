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
    CHAT_GROUP_AES

}

package com.github.wrx886.e2echo.client.enums;

/**
 * 消息类型枚举，用于区分不同类型的消息（按类型注册
 * {@link com.github.wrx886.e2echo.client.common.MessageHandler}）。
 */
public enum MessageTypeEnum {

    /**
     * 文字聊天消息，正文是 {@link com.github.wrx886.e2echo.client.vo.ChatTextMessageVo} 的 JSON。
     */
    CHAT_TEXT,

    /**
     * 群聊密钥消息，正文是
     * {@link com.github.wrx886.e2echo.client.vo.ChatGroupKeyMessageVo} 的 JSON，通过私聊通道分发。
     */
    CHAT_GROUP_KEY

}

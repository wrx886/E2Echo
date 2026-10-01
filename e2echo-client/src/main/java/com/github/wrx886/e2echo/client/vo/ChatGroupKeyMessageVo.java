package com.github.wrx886.e2echo.client.vo;

/**
 * 群聊密钥消息的正文。
 *
 * <p>群主把最新密钥通过私聊消息发给成员，成员收到后写入本地，用于收发该群的群聊消息。</p>
 *
 * @param group       群标识
 * @param publishTime 密钥签发时间（毫秒），同时充当密钥版本
 * @param aesKey      AES 密钥（HEX 格式）
 */
public record ChatGroupKeyMessageVo(
        String group,
        Long publishTime,
        String aesKey
) {
}

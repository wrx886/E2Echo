package com.github.wrx886.e2echo.client.vo;

/**
 * 会话消息的返回结构。
 *
 * @param id      消息 ID（服务端消息 ID）
 * @param from    发送者公钥
 * @param to      接收者，私聊时为对方公钥、群聊时为群聊标识
 * @param message 消息正文，已按消息类型反序列化成对象
 * @param type    消息类型
 * @param channel 消息通道
 * @param info    消息附加信息
 * @param seq     本地序号，倒序翻页时作为 {@code endSeq} 游标
 */
public record MessageVo(
        String id,
        String from,
        String to,
        Object message,
        String type,
        String channel,
        String info,
        Long seq
) {
}

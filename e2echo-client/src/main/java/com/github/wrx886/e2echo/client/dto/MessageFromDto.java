package com.github.wrx886.e2echo.client.dto;

/**
 * 消息发送者数据传输对象。
 *
 * <p>用于会话发送者列表：公钥标识发送者，别名取自当前用户与该发送者的私聊会话记录，没有对应
 * 会话或没有设置别名时为 {@code null}。</p>
 *
 * @param publicKey 发送者公钥
 * @param alias     发送者别名，可以为空
 */
public record MessageFromDto(
        String publicKey,
        String alias
) {
}

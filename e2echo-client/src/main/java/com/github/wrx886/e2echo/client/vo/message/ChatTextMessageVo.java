package com.github.wrx886.e2echo.client.vo.message;

/**
 * 文字聊天消息的正文。
 *
 * @param text  消息正文
 * @param quote 被引用消息的 ID，为空（{@code null} 或空字符串）表示未引用消息
 */
public record ChatTextMessageVo(String text, String quote) {
}

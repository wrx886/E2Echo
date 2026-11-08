package com.github.wrx886.e2echo.client.vo.req;

import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastMessageVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 发布广播消息（帖子或评论）的请求体。
 *
 * <p>{@code to} 传空字符串（或空着）就是发帖，填帖子的消息 ID 表示在该帖子下评论。一级帖子的
 * {@code replyToId} 表示被转发的一级帖子，评论的 {@code replyToId} 表示被回复的评论。</p>
 *
 * @param to      发布目标：空字符串是发帖，帖子的消息 ID 则是在该帖子下评论
 * @param message 广播正文（文本 + 附件）
 */
public record BroadcastMessageReq(
        @NotNull String to,
        @NotNull @Valid BroadcastMessageVo message
) {
}

package com.github.wrx886.e2echo.client.vo.resp;

import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastMessageVo;

/**
 * 广播消息的返回结构。
 *
 * <p>对应服务端的一条广播消息：外层是消息本身的信息，{@code message} 是解析后的正文（帖子或评论）。
 * 帖子与评论靠 {@code to} 区分（帖子是占位符 {@code *}，评论是帖子 ID），加上正文里的
 * {@code replyToId} 就能还原"回复了哪条评论"。</p>
 *
 * @param id      消息 ID（服务端生成），帖子与评论都用它被引用
 * @param from    发送者公钥，即帖子的作者或评论者
 * @param to      发布目标：占位符 {@code *} 表示帖子，帖子 ID 表示该帖子下的评论
 * @param message 广播正文（文本 + 附件 + 二级回复指向）
 * @param type    消息类型
 * @param channel 消息通道
 * @param info    消息附加信息
 */
public record BroadcastMessageRespVo(
        String id,
        String from,
        String to,
        BroadcastMessageVo message,
        String type,
        String channel,
        String info
) {
}

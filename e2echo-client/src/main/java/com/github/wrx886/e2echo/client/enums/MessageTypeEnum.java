package com.github.wrx886.e2echo.client.enums;

import com.github.wrx886.e2echo.client.vo.message.ChatGroupKeyMessageVo;
import com.github.wrx886.e2echo.client.vo.message.ChatFileMessageVo;
import com.github.wrx886.e2echo.client.vo.message.ChatTextMessageVo;

/**
 * 消息类型枚举，用于区分不同类型的消息（按类型注册
 * {@link com.github.wrx886.e2echo.client.common.MessageHandler}）。
 */
public enum MessageTypeEnum {

    /**
     * 文字聊天消息，正文是 {@link ChatTextMessageVo} 的 JSON。
     */
    CHAT_TEXT,

    /**
     * 群聊密钥消息，正文是
     * {@link ChatGroupKeyMessageVo} 的 JSON，通过私聊通道分发。
     */
    CHAT_GROUP_KEY,

    /**
     * 聊天文件消息，正文是 {@link ChatFileMessageVo} 的 JSON：只带文件信息，文件本体放在对象存储里。
     */
    CHAT_FILE,

    /**
     * 聊天图片消息：正文与文件消息相同（都是 {@link ChatFileMessageVo} 的 JSON），单独分一个类型是
     * 为了前端能把图片直接渲染出来，而不是显示成文件。
     */
    CHAT_FILE_IMAGE,

    /**
     * 聊天视频消息：正文与文件消息相同（都是 {@link ChatFileMessageVo} 的 JSON），单独分一个类型是
     * 为了前端用播放器展示，而不是显示成文件。
     */
    CHAT_FILE_VIDEO,

    /**
     * 聊天音频消息：正文与文件消息相同（都是 {@link ChatFileMessageVo} 的 JSON），单独分一个类型是
     * 为了前端用播放器展示，而不是显示成文件。
     */
    CHAT_FILE_AUDIO,

    /**
     * 广播的用户资料，正文是 {@link com.github.wrx886.e2echo.client.vo.broadcast.BroadcastUserinfo}
     * 的 JSON，按发送者取最新一条。
     */
    BROADCAST_USERINFO,

    /**
     * 广播消息，正文是
     * {@link com.github.wrx886.e2echo.client.vo.broadcast.BroadcastMessageVo} 的 JSON。
     *
     * <p>帖子和评论共用这一个类型，靠 {@code to} 区分：{@code *} 是发帖，帖子的消息 ID 是在该帖子
     * 下评论（回复某条评论时由正文里的 {@code replyToId} 指回去）。</p>
     */
    BROADCAST_MESSAGE,

}

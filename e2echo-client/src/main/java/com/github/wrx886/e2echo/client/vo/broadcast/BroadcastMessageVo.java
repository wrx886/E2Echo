package com.github.wrx886.e2echo.client.vo.broadcast;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 广播的正文：帖子（类似一条微博）和评论共用这一个结构。
 *
 * <p>两者连消息类型都一样（都是 {@code BROADCAST_MESSAGE}），唯一的区别在外层 {@code EccMessage} 的
 * {@code to}：发帖不指向任何消息（客户端会填个占位符），评论指向被评论的帖子。所以正文结构完全共用，
 * 一个 VO 就够。</p>
 *
 * <p>评论是单级的：回复某条评论（二级回复）不改变层级，只是把 {@link #replyToId} 指向被回复的那条
 * 评论；帖子本身、以及直接评论帖子的评论，这个字段都为空。</p>
 *
 * <p>广播只存服务端、不落本地库，所以这里只放业务信息：一段文本，加上若干附件。发送者不在这里
 * ——它由 {@code from}（签名者）表示；时间可以从消息 ID 前 16 位的时间戳解析出来，都不重复存。</p>
 *
 * <p>与 {@link BroadcastFileVo} 的分工：正文回答"说了什么"，附件回答"带的东西怎么取、怎么展示"
 * ——前端按附件的 {@code contentType} 决定内嵌展示（图片、播放器）还是显示成文件卡片，用户点开时再
 * 用附件的 {@code objectKey} 调广播文件下载接口。</p>
 *
 * @param text      正文文本
 * @param files     附件列表，没有附件时传空列表（这里不能为空）
 * @param replyToId 被回复评论的消息 ID：二级回复时填，帖子和直接评论帖子时为空
 */
public record BroadcastMessageVo(
        @NotBlank String text,
        @NotNull @Valid List<BroadcastFileVo> files,
        String replyToId
) {
}

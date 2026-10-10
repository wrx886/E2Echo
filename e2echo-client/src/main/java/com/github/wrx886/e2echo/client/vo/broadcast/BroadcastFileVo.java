package com.github.wrx886.e2echo.client.vo.broadcast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 广播消息里的附件（图片、视频、音频或普通文件）。
 *
 * <p>广播通道（{@code BROADCAST_*_NONE}）不做加密，文件本体也是原样存进对象存储的，所以这里只带
 * 取文件、以及前端展示它所需的信息，没有密钥；文件也不落本地磁盘，用户点开时由下载接口直接透传。</p>
 *
 * <p>附件种类放在 {@code contentType} 而不是像聊天那样放进消息类型：一条广播可以同时带图片和视频，
 * 而消息类型只有一个，放这里前端才能逐个渲染。</p>
 *
 * @param objectKey   对象存储里的对象键，形如 {@code default/文件 ID}，下载时原样传回
 * @param filename    文件名，下载时用于命名
 * @param contentType 内容类型（MIME，如 {@code image/png}），前端据此决定内嵌展示还是当文件下载
 * @param size        字节数，前端显示大小用
 */
public record BroadcastFileVo(
        @NotBlank String objectKey,
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull Long size
) {
}

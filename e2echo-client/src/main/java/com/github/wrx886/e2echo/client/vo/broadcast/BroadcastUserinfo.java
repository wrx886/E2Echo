package com.github.wrx886.e2echo.client.vo.broadcast;

import jakarta.validation.constraints.NotBlank;

/**
 * 广播的用户资料。
 *
 * <p>一条用户资料就是一条广播消息（通道 {@code BROADCAST_USERINFO_NONE}、类型
 * {@code BROADCAST_USERINFO}），取某个用户最新的一条即可；它只存服务端、不落本地库。</p>
 *
 * <p>正文里不放公钥：作者以所在广播消息的 {@code from} 为准。{@code from} 参与签名，改了会被验签
 * 发现；正文自带公钥反而给了冒充的空间，所以这里只放资料本身。</p>
 *
 * @param nickname    昵称
 * @param gender      性别，可以为空
 * @param avatarUrl   头像地址，可以为空
 * @param description 个人简介，可以为空
 */
public record BroadcastUserinfo(
        @NotBlank String nickname,
        String gender,
        String avatarUrl,
        String description
) {
}

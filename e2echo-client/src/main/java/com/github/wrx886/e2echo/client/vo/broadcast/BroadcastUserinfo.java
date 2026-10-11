package com.github.wrx886.e2echo.client.vo.broadcast;

import jakarta.validation.constraints.NotBlank;

/**
 * 广播的用户资料。
 *
 * <p>一条用户资料就是一条广播消息（通道 {@code BROADCAST_USERINFO_NONE}、类型
 * {@code BROADCAST_USERINFO}），取某个用户最新的一条即可；它只存服务端、不落本地库。</p>
 *
 * @param publicKey   用户公钥，也就是查询时用的 uid
 * @param nickname    昵称
 * @param gender      性别，可以为空
 * @param avatarUrl   头像地址，可以为空
 * @param description 个人简介，可以为空
 */
public record BroadcastUserinfo(
        @NotBlank String publicKey,
        @NotBlank String nickname,
        String gender,
        String avatarUrl,
        String description
) {
}

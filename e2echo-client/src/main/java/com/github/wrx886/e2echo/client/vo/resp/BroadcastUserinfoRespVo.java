package com.github.wrx886.e2echo.client.vo.resp;

import com.github.wrx886.e2echo.client.vo.broadcast.BroadcastUserinfo;

/**
 * 用户资料的返回结构。
 *
 * <p>对应服务端的一条用户资料广播：外层是消息本身的信息，{@code userinfo} 是解析后的资料正文。正文
 * 里不含公钥，作者以 {@code from} 为准（它参与签名，改不了）。</p>
 *
 * @param id       消息 ID（服务端生成）
 * @param from     资料所属用户的公钥，也是这条消息的签名者
 * @param to       发布目标，资料广播固定是占位符 {@code *}
 * @param userinfo 解析后的用户资料正文
 * @param type     消息类型
 * @param channel  消息通道
 * @param info     消息附加信息
 */
public record BroadcastUserinfoRespVo(
        String id,
        String from,
        String to,
        BroadcastUserinfo userinfo,
        String type,
        String channel,
        String info
) {
}

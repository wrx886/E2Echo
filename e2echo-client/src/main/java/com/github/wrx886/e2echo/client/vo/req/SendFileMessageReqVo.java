package com.github.wrx886.e2echo.client.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送文件消息的请求体。
 *
 * @param to    接收者，私聊时为对方公钥、群聊时为群聊标识
 * @param group 是否群聊
 * @param path  待发送文件的本地路径
 */
public record SendFileMessageReqVo(
        @NotBlank
        String to,

        @NotNull
        Boolean group,

        @NotBlank
        String path
) {
}

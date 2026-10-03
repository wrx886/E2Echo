package com.github.wrx886.e2echo.client.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送测试消息的请求体。
 *
 * @param to    接收者，私聊时为对方公钥、群聊时为群聊标识
 * @param group 是否群聊
 * @param text  消息正文
 */
public record SendTestMessageReqVo(

        @NotBlank
        String to,

        @NotNull
        Boolean group,

        @NotBlank
        String text

) {
}

package com.github.wrx886.e2echo.client.vo.message;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天文件消息的正文。
 *
 * <p>文件本体加密后放在对象存储里，消息里只带取回并解密它所需的信息；正文本身还会随消息一起被通道
 * 加密（私聊 ECC、群聊 AES）。</p>
 *
 * @param filename  文件名，用于展示与下载时命名
 * @param aesKey    文件加密用的一次性 AES 密钥（HEX 格式）
 * @param objectKey 对象存储里的对象键，形如 {@code default/文件 ID}
 */
public record ChatFileMessageVo(

        @NotBlank
        String filename,

        @NotBlank
        String aesKey,

        @NotBlank
        String objectKey
) {
}

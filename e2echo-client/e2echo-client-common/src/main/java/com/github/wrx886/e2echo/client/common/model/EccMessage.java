package com.github.wrx886.e2echo.client.common.model;

import lombok.Data;

@Data
// ECC 消息
public final class EccMessage {

    // UUID
    private String uuid;

    // 时间戳(int64)，表示自1970年1月1日00:00:00 UTC以来的毫秒数
    private String timestamp;

    // 发送者公钥
    private String fromPublicKeyHex;

    // 接收者公钥
    private String toPublicKeyHex;

    // 明文或密文
    private String data;

    // 签名
    private String signature;

}

package com.github.wrx886.e2echo.plugin.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "ECC 消息")
public class EccMessage {

    @Schema(description = "UUID")
    private String uuid;

    @Schema(description = "时间戳(int64)，表示自1970年1月1日00:00:00 UTC以来的毫秒数")
    private String timestamp;

    @Schema(description = "发送者公钥")
    private String fromPublicKeyHex;

    @Schema(description = "接收者公钥")
    private String toPublicKeyHex;

    @Schema(description = "明文或密文")
    private String data;

    @Schema(description = "签名")
    private String signature;

}

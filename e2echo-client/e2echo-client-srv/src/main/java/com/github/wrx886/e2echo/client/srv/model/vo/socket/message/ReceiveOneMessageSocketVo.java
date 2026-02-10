package com.github.wrx886.e2echo.client.srv.model.vo.socket.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "接收私聊消息参数")
public class ReceiveOneMessageSocketVo {

    @Schema(description = "接收者公钥")
    private String toPublicKeyHex;

    @Schema(description = "起始时间(int64)")
    private String startTimestamp;

}

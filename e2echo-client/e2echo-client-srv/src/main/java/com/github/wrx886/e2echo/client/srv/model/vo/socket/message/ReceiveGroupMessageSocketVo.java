package com.github.wrx886.e2echo.client.srv.model.vo.socket.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "接收群聊消息参数")
public class ReceiveGroupMessageSocketVo {

    @Schema(description = "群聊 UUID（格式：{群主公钥}:{群聊UUID}）")
    private String groupUuid;

    @Schema(description = "起始时间(int64)")
    private String startTimestamp;

}

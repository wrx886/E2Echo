package com.github.wrx886.e2echo.client.srv.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "群聊消息")
public class GroupMessageVo {

    @Schema(description = "发送的消息数据(SendMessageVo)")
    private String data;

    @Schema(description = "加密使用的密钥版本(int64)")
    private String timestamp;

}

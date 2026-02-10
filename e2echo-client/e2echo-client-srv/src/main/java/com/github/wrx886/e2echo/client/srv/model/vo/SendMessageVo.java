package com.github.wrx886.e2echo.client.srv.model.vo;

import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发送消息参数")
public class SendMessageVo {

    @Schema(description = "数据")
    private String data;

    @Schema(description = "消息类型")
    private MessageType type;

}

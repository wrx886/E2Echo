package com.github.wrx886.e2echo.server.model.vo.message;

import java.util.Date;

import com.github.wrx886.e2echo.server.model.enums.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "获取消息的参数")
public class MessageVo {

    @Schema(description = "起始时间")
    private Date startTime;

    @Schema(description = "接收类型（1：用户消息；2：群聊消息，null：所有类型）")
    private MessageType messageType;
}

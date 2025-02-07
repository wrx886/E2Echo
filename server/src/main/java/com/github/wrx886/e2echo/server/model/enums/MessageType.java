package com.github.wrx886.e2echo.server.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "消息类型枚举")
@Getter
public enum MessageType implements BaseEnum {
    USER(1, "用户消息"),
    GROUP(2, "群聊消息");

    @Schema(description = "消息类型代码")
    private Integer code;

    @Schema(description = "消息类型名称")
    private String name;

    private MessageType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

}

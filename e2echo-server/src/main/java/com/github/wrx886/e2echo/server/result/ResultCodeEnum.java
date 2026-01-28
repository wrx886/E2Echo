package com.github.wrx886.e2echo.server.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "返回状态码枚举")
public enum ResultCodeEnum {
    OK("0000", "OK"),
    FAIL("0001", "FAIL"),

    // 消息相关
    MESSAGE_IS_NULL("A001", "消息为空"),
    MESSAGE_UUID_IS_EMPTY("A002", "消息 UUID 为空"),
    MESSAGE_TIMESTAMP_IS_EMPTY("A003", "消息时间戳为空"),
    MESSAGE_TIMESTAMP_NOT_INT64("A004", "消息时间戳不是 int64 类型"),
    MESSAGE_FROM_PUBLIC_KEY_IS_EMPTY("A005", "ECC 消息发送者公钥为空"),
    MESSAGE_TO_PUBLIC_KEY_IS_EMPTY("A006", "ECC 消息接收者公钥为空"),
    MESSAGE_DATA_IS_NULL("A007", "消息数据为空"),
    MESSAGE_SIGNATURE_IS_EMPTY("A008", "ECC 签名为空"),
    MESSAGE_SIGNATURE_NOT_MATCH("A009", "ECC 签名不匹配"),

    // WebSocket 相关
    WEB_SOCKET_REQUEST_FARMAT_ERROR("B001", "WebSocket 请求格式错误"),
    WEB_SOCKET_REQUEST_ID_EMPTY("B002", "WebSocket 请求 ID 为空"),
    WEB_SOCKET_REQUEST_COMMAND_EMPTY("B003", "WebSocket 命令为空"),
    WEB_SOCKET_REQUEST_NOT_FOUND("B004", "WebSocket 命令未找到"),
    WEB_SOCKET_REQUEST_PARAM_ERROR("B005", "WebSocket 参数错误"),

    ;

    @Schema(description = "状态码")
    private final String code;

    @Schema(description = "状态描述")
    private final String message;

    private ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}

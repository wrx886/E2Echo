package com.github.wrx886.e2echo.server.model.socket;

import com.github.wrx886.e2echo.server.result.ResultCodeEnum;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WebSocket 响应")
public class WebSocketResult<E> {

    @Schema(description = "请求 ID（和请求一致，如果不是请求的响应则由服务端生成）")
    private String id;

    @Schema(description = "WebSocket 响应命令（和请求一致，如果不是请求的响应则由服务端生成）")
    private String command;

    @Schema(description = "响应数据")
    private E data;

    @Schema(description = "返回码")
    private String code;

    @Schema(description = "返回消息")
    private String message;

    public static <E> WebSocketResult<E> ok(String id, String command, E data) {
        return build(id, command, ResultCodeEnum.OK, data);
    }

    public static WebSocketResult<Void> ok(String id, String command) {
        return ok(id, command, null);
    }

    public static <E> WebSocketResult<E> fail(String id, String command, E data) {
        return build(id, command, ResultCodeEnum.FAIL, data);
    }

    public static WebSocketResult<Void> fail(String id, String command) {
        return fail(id, command, null);
    }

    public static <E> WebSocketResult<E> build(String id, String command, ResultCodeEnum resultCodeEnum, E data) {
        return new WebSocketResult<E>(id, command, data, resultCodeEnum.getCode(), resultCodeEnum.getMessage());
    }

    public static WebSocketResult<Void> build(String id, String command, ResultCodeEnum resultCodeEnum) {
        return build(id, command, resultCodeEnum, null);
    }

}

package com.github.wrx886.e2echo.client.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// WebSocket 响应
public class WebSocketResult<E> {

    // 请求 ID（和请求一致，如果不是请求的响应则由服务端生成）
    private String id;

    // WebSocket 响应命令（和请求一致，如果不是请求的响应则由服务端生成）
    private String command;

    // 响应数据
    private E data;

    // 返回码
    private Integer code;

    // 返回消息
    private String message;

    public static <E> WebSocketResult<E> ok(String id, String command, E data) {
        return build(id, command, data, CodeEnum.OK);
    }

    public static WebSocketResult<Void> ok(String id, String command) {
        return ok(id, command, null);
    }

    public static <E> WebSocketResult<E> fail(String id, String command, E data) {
        return build(id, command, data, CodeEnum.FAIL);
    }

    public static WebSocketResult<Void> fail(String id, String command) {
        return fail(id, command, null);
    }

    public static <E> WebSocketResult<E> build(String id, String command, Integer code, String message) {
        return new WebSocketResult<E>(id, command, null, code, message);
    }

    public static <E> WebSocketResult<E> build(String id, String command, E data, CodeEnum codeEnum) {
        return new WebSocketResult<E>(id, command, data, codeEnum.getCode(), codeEnum.getMessage());
    }

    public static WebSocketResult<Void> build(String id, String command, CodeEnum codeEnum) {
        return build(id, command, null, codeEnum);
    }
}

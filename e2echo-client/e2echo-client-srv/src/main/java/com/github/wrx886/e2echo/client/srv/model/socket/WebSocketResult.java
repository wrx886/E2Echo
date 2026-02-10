package com.github.wrx886.e2echo.client.srv.model.socket;

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

}

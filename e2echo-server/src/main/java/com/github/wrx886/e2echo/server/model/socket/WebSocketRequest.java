package com.github.wrx886.e2echo.server.model.socket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "WebSocket 请求格式")
public class WebSocketRequest<E> {

    @Schema(description = "请求 ID")
    private String id;

    @Schema(description = "WebSocket 请求命令")
    private String command;

    @Schema(description = "请求数据")
    private E data;

}
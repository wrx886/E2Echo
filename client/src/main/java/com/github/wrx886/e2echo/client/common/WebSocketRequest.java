package com.github.wrx886.e2echo.client.common;

import lombok.Data;

@Data
// WebSocket 请求格式
public class WebSocketRequest<E> {

    // 请求 ID
    private String id;

    // WebSocket 请求命令
    private String command;

    // 请求数据
    private E data;

}

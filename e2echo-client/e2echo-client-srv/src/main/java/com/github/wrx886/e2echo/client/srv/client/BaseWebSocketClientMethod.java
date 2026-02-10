package com.github.wrx886.e2echo.client.srv.client;

import com.github.wrx886.e2echo.client.srv.model.socket.WebSocketResult;

@FunctionalInterface
public interface BaseWebSocketClientMethod {

    void handle(WebSocketResult<?> result);

}

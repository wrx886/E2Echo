package com.github.wrx886.e2echo.server.test.client;

import com.github.wrx886.e2echo.server.model.socket.WebSocketResult;

@FunctionalInterface
public interface WebSocketTestClientMethod {

    void handle(WebSocketResult<?> result);

}

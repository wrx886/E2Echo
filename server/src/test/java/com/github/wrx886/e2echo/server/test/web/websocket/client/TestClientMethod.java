package com.github.wrx886.e2echo.server.test.web.websocket.client;

import com.github.wrx886.e2echo.server.web.wocket.request.WebSocketResult;

@FunctionalInterface
public interface TestClientMethod {

    void handle(WebSocketResult<?> result);

}

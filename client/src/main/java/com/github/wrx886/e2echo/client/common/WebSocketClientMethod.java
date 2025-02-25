package com.github.wrx886.e2echo.client.common;

@FunctionalInterface
public interface WebSocketClientMethod {

    void handle(WebSocketResult<?> result);

}

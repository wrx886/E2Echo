package com.github.wrx886.e2echo.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.github.wrx886.e2echo.server.web.wocket.handler.MessageHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MessageHandler messageHandler;

    // MessageHandler 对应的路径
    public static final String MessageHandlerPath = "/server/message";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 messageHandler
        registry.addHandler(messageHandler, MessageHandlerPath).setAllowedOrigins("*");
    }

}

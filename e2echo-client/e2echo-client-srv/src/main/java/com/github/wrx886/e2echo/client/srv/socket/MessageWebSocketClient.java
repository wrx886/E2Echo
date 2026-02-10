package com.github.wrx886.e2echo.client.srv.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.srv.client.BaseWebSocketClient;
import com.github.wrx886.e2echo.client.srv.model.socket.WebSocketResult;

public class MessageWebSocketClient extends BaseWebSocketClient {

    public final ObjectMapper objectMapper = BeanProvider.getBean(ObjectMapper.class);

    // 构造函数
    public MessageWebSocketClient(String url) throws Exception {
        super(url);
    }

    /**
     * 自动接收私聊消息处理
     * 
     * @param result
     */
    public void autoReveiveOne(WebSocketResult<?> result) {
        // 消息为空
        if (result.getData() == null) {
            return;
        }

        // 类型转变
        EccMessage eccMessage = objectMapper.convertValue(result.getData(), EccMessage.class);
    }

    /**
     * 自动接收群聊消息处理
     * 
     * @param result
     */
    public void autoReveiveGroup(WebSocketResult<?> result) {
        // 消息为空
        if (result.getData() == null) {
            return;
        }

        // 类型转变
        EccMessage eccMessage = objectMapper.convertValue(result.getData(), EccMessage.class);
    }

}

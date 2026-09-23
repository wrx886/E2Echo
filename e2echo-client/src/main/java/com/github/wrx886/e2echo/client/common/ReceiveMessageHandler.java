package com.github.wrx886.e2echo.client.common;

import com.github.wrx886.e2echo.client.entity.Message;

/**
 * 收到消息后的处理器，按消息类型注册，用于对特定类型的消息做额外处理。
 */
public interface ReceiveMessageHandler {

    /**
     * 处理收到的消息。
     *
     * <p>消息此时已经入库，处理器对消息内容的修改不会保存。</p>
     *
     * @param message 收到的消息
     */
    void receive(Message message);

}

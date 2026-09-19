package com.github.wrx886.e2echo.client.common;

import com.github.wrx886.e2echo.client.entity.Message;

/**
 * 收到消息后的处理器，按消息类型注册，用于对特定类型的消息做额外处理。
 */
public interface ReceiveMessageHandler {

    /**
     * 处理收到的消息。
     *
     * @param message 收到的消息，处理器可以直接修改其内容
     * @return 已处理完（消息无需入库）返回 {@code true}，还需要入库返回 {@code false}
     */
    boolean receive(Message message);

}

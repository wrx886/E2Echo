package com.github.wrx886.e2echo.client.common;

import com.github.wrx886.e2echo.client.entity.Message;

/**
 * 收到消息后的处理器，按消息类型注册，用于对特定类型的消息做额外处理。
 *
 * <p>每种消息类型对应一个处理器：{@link #receive(Message)} 在消息入库后调用；{@link #getMessageType()}
 * 给出消息正文的类型，读取会话消息时用它反序列化正文。</p>
 */
public interface MessageHandler {

    /**
     * 处理收到的消息。
     *
     * <p>消息此时已经入库，处理器对消息内容的修改不会保存。</p>
     *
     * @param message 收到的消息
     */
    void receive(Message message);

    /**
     * 消息正文的类型。
     *
     * <p>消息正文是 JSON，读取会话消息时按这个类型反序列化。</p>
     *
     * @return 消息正文反序列化后的类型
     */
    Class<?> getMessageType();
}

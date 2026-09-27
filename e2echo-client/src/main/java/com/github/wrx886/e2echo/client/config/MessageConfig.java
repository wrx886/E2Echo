package com.github.wrx886.e2echo.client.config;

import com.github.wrx886.e2echo.client.common.MessageHandler;
import com.github.wrx886.e2echo.client.enums.MessageTypeEnum;
import com.github.wrx886.e2echo.client.handler.ChatTextMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理器配置。
 *
 * <p>按消息类型注册 {@link MessageHandler}，收到消息时由业务代码按类型取出对应处理器；没有
 * 注册处理器的类型返回 {@code null}。新增一种消息类型时，除了写处理器，还要在
 * {@link #addMessageHandler} 里把它注册进来。</p>
 */
@Configuration
public class MessageConfig {

    /**
     * 消息类型与处理器的映射。
     */
    private final ConcurrentHashMap<String, MessageHandler> messageHandlerMap = new ConcurrentHashMap<>();

    /**
     * 处理器初始化标记：本 Bean 的创建过程就是处理器注册过程，注入它即可保证处理器已经注册。
     */
    public record MessageHandlerInit() {
    }

    /**
     * 注册消息处理器，并返回初始化标记。
     *
     * <p>处理器都作为参数注入进来，在这里按消息类型放进映射；容器启动时创建本 Bean，注册随之
     * 完成。新增处理器时，在这里补一个参数和一行注册。</p>
     *
     * @param chatTextMessageHandler 文字聊天消息处理器
     * @return 初始化标记
     */
    @Bean
    public MessageHandlerInit addMessageHandler(
            ChatTextMessageHandler chatTextMessageHandler
    ) {
        messageHandlerMap.put(MessageTypeEnum.CHAT_TEXT.name(), chatTextMessageHandler);

        return new MessageHandlerInit();
    }

    /**
     * 按消息类型取出处理器。
     *
     * @param type 消息类型
     * @return 对应的处理器，未注册时为 {@code null}
     */
    public MessageHandler getReceiveHandler(String type) {
        return messageHandlerMap.get(type);
    }

}

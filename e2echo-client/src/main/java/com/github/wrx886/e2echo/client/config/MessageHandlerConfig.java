package com.github.wrx886.e2echo.client.config;

import com.github.wrx886.e2echo.client.common.ReceiveMessageHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理器配置。
 *
 * <p>按消息类型注册 {@link ReceiveMessageHandler}，收到消息时由业务代码按类型取出对应处理器；没有
 * 注册处理器的类型返回 {@code null}。</p>
 */
@Configuration
public class MessageHandlerConfig {

    /**
     * 消息类型与处理器的映射。
     */
    private final ConcurrentHashMap<String, ReceiveMessageHandler> receiveMessageHandlerMap = new ConcurrentHashMap<>();

    /**
     * 处理器初始化标记：注册本 Bean 用于触发处理器注册逻辑。
     */
    public record MessageHandlerInit() {
    }

    /**
     * 创建处理器初始化标记。
     *
     * @return 初始化标记
     */
    @Bean
    public MessageHandlerInit addMessageHandler() {
        return new MessageHandlerInit();
    }

    /**
     * 按消息类型取出处理器。
     *
     * @param type 消息类型
     * @return 对应的处理器，未注册时为 {@code null}
     */
    public ReceiveMessageHandler getReceiveHandler(String type) {
        return receiveMessageHandlerMap.get(type);
    }


}

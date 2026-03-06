package com.github.wrx886.e2echo.server.config;

import java.util.UUID;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
public class RabbitMqConfig {

    // 消息分发的交换机（将消息发送给 Session）
    public static final String MESSAGE_DISTRIBUTE_EXCHANGE = "message.distribute";

    // 消息分发的队列
    public static final String MESSAGE_DISTRIBUTE_QUEUE = "message.distribute";

    // 消息发布的交换机（异步处理，将消息发送到 MESSAGE_DISTRIBUTE_EXCHANGE 交换机）
    public static final String MESSAGE_PUBLISH_EXCHANGE = "message.publish";

    // 消息发布的队列
    public static final String MESSAGE_PUBLISH_QUEUE = "message.publish";

    @Getter
    public final String uuid = UUID.randomUUID().toString();

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}

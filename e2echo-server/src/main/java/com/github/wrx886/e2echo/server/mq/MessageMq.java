package com.github.wrx886.e2echo.server.mq;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.server.config.RabbitMqConfig;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.socket.MessageWebSocketHandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class MessageMq {

    private final RabbitMqConfig rabbitMqConfig;

    @Data
    public static class MqMessage {
        private String sessionId;
        private EccMessage eccMessage;
        private Boolean isGroup;
    };

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "message.queue", durable = "false"), exchange = @Exchange(name = "message.direct", type = ExchangeTypes.DIRECT, durable = "false"), key = {
            "#{rabbitMqConfig.getUuid()}" }))
    public void directQ1Listener(MqMessage mqMessage) {
        try {
            if (!mqMessage.getIsGroup()) {
                MessageWebSocketHandler.sendMessage(
                        mqMessage.getSessionId().substring(rabbitMqConfig.getUuid().length()),
                        "autoReveiveOne",
                        mqMessage.getEccMessage());
            } else {
                MessageWebSocketHandler.sendMessage(
                        mqMessage.getSessionId().substring(rabbitMqConfig.getUuid().length()),
                        "autoReveiveGroup",
                        mqMessage.getEccMessage());
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

}

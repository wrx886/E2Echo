package com.github.wrx886.e2echo.server.mq;

import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.github.wrx886.e2echo.server.common.RedisPrefix;
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
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Data
    public static class MqMessage {
        private String uuid;
        private String sessionId;
        private EccMessage eccMessage;
        private Boolean isGroup;
    };

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = RabbitMqConfig.MESSAGE_DISTRIBUTE_QUEUE, durable = "false", autoDelete = "true"), exchange = @Exchange(name = RabbitMqConfig.MESSAGE_DISTRIBUTE_EXCHANGE, type = ExchangeTypes.DIRECT, durable = "false", autoDelete = "true"), key = {
            "#{rabbitMqConfig.getUuid()}" }))
    public void messageDistributeListener(MqMessage mqMessage) {
        try {
            // 幂等性
            if (!stringRedisTemplate.opsForValue().setIfAbsent(RedisPrefix.MQ_UUID + mqMessage.getUuid(), "1")) {
                // 重复消息
                return;
            }

            // 发送消息
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

    @Data
    public static class MqForward {
        private String uuid;
        private EccMessage eccMessage;
        private Boolean isGroup;
    }

    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = RabbitMqConfig.MESSAGE_PUBLISH_QUEUE, durable = "false", autoDelete = "true"), exchange = @Exchange(name = RabbitMqConfig.MESSAGE_PUBLISH_EXCHANGE, type = ExchangeTypes.FANOUT, durable = "false", autoDelete = "true")))
    public void messagePublishListener(MqForward mqForward) {
        // 幂等性
        if (!stringRedisTemplate.opsForValue().setIfAbsent(RedisPrefix.MQ_UUID + mqForward.getUuid(), "1")) {
            // 重复消息
            return;
        }

        // 获取 EccMessage
        EccMessage eccMessage = mqForward.getEccMessage();

        // 获取 SessionID 列表
        Set<String> sessionIds;
        if (mqForward.getIsGroup()) {
            sessionIds = stringRedisTemplate.opsForSet().members(
                    RedisPrefix.GROUP_UUID_2_SESSION_ID + eccMessage.getToPublicKeyHex());
        } else {
            sessionIds = stringRedisTemplate.opsForSet()
                    .members(RedisPrefix.PUBLIC_KEY_HEX_2_SESSION_ID + eccMessage.getToPublicKeyHex());
        }

        // 转发
        for (String sessionId : sessionIds) {
            // 提取路由键
            String routingKey = sessionId.substring(0, rabbitMqConfig.getUuid().length());
            MqMessage mqMessage = new MqMessage();
            mqMessage.setUuid(UUID.randomUUID().toString());
            mqMessage.setSessionId(sessionId);
            mqMessage.setEccMessage(eccMessage);
            mqMessage.setIsGroup(mqForward.getIsGroup());
            rabbitTemplate.convertAndSend(RabbitMqConfig.MESSAGE_DISTRIBUTE_EXCHANGE, routingKey, mqMessage);
        }

    }

}

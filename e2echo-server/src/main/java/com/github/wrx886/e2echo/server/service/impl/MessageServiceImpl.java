package com.github.wrx886.e2echo.server.service.impl;

import java.util.List;
import java.util.Set;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.server.common.RedisPrefix;
import com.github.wrx886.e2echo.server.config.RabbitMqConfig;
import com.github.wrx886.e2echo.server.mapper.MessageMapper;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.mq.MessageMq.MqMessage;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.service.MessageService;
import com.github.wrx886.e2echo.server.util.EccMessageUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitMqConfig rabbitMqConfig;

    /**
     * 发送消息
     * 
     * @param eccMessage
     */
    @Override
    public void sendOne(EccMessage eccMessage) {
        // 检查消息
        checkEccMessage(eccMessage);

        // 封装成数据库实体
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(eccMessage.getData());
        message.setSignature(eccMessage.getSignature());
        message.setGroup(false);

        // 插入数据库
        this.save(message);

        // 发送消息到 WebSocket 通道
        sendOneSocket(eccMessage);
    }

    /**
     * 接收消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param startTimestamp 起始时间
     * @return 接收到的消息
     */
    @Override
    public List<EccMessage> receiveOne(String toPublicKeyHex, String startTimestamp) {
        // 查询数据库
        List<Message> messages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getToPublicKeyHex, toPublicKeyHex)
                .ge(Message::getTimestamp, Long.valueOf(startTimestamp))
                .eq(Message::getGroup, false));

        // 转换并返回
        return messages.stream().map(message -> {
            EccMessage eccMessage = new EccMessage();
            eccMessage.setUuid(message.getUuid());
            eccMessage.setTimestamp(Long.toString(message.getTimestamp()));
            eccMessage.setFromPublicKeyHex(message.getFromPublicKeyHex());
            eccMessage.setToPublicKeyHex(message.getToPublicKeyHex());
            eccMessage.setData(message.getData());
            eccMessage.setSignature(message.getSignature());
            return eccMessage;
        }).toList();
    }

    /**
     * 发送群聊消息
     * 
     * @param eccMessage 群聊消息，toPublicKeyHex 存储群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    @Override
    public void sendGroup(EccMessage eccMessage) {
        // 检查消息
        checkEccMessage(eccMessage);

        // 封装成数据库实体
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(eccMessage.getData());
        message.setSignature(eccMessage.getSignature());
        message.setGroup(true);

        // 插入数据库
        this.save(message);

        // 发送消息到 WebSocket 通道
        sendGroupSocket(eccMessage);
    }

    /**
     * 获取群聊消息
     * 
     * @param groupUuid      群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     * @param startTimestamp 起始时间
     */
    @Override
    public List<EccMessage> receiveGroup(String groupUuid, String startTimestamp) {
        // 获取群聊消息
        List<Message> messages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getToPublicKeyHex, groupUuid)
                .ge(Message::getTimestamp, Long.valueOf(startTimestamp))
                .eq(Message::getGroup, true));

        // 转换并返回
        return messages.stream().map(message -> {
            EccMessage eccMessage = new EccMessage();
            eccMessage.setUuid(message.getUuid());
            eccMessage.setTimestamp(Long.toString(message.getTimestamp()));
            eccMessage.setFromPublicKeyHex(message.getFromPublicKeyHex());
            eccMessage.setToPublicKeyHex(message.getToPublicKeyHex());
            eccMessage.setData(message.getData());
            eccMessage.setSignature(message.getSignature());
            return eccMessage;
        }).toList();
    }

    /**
     * 检查 ECC 消息
     * 
     * @param eccMessage
     */
    private void checkEccMessage(EccMessage eccMessage) {

        // 消息为空
        if (eccMessage == null) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_IS_NULL);
        }

        // UUID
        if (eccMessage.getUuid() == null || eccMessage.getUuid().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_UUID_IS_EMPTY);
        }

        // 时间戳
        if (eccMessage.getTimestamp() == null || eccMessage.getTimestamp().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_TIMESTAMP_IS_EMPTY);
        }

        // 时间戳转换失败
        try {
            Long.valueOf(eccMessage.getTimestamp());
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_TIMESTAMP_NOT_INT64);
        }

        // 发送方公钥
        if (eccMessage.getFromPublicKeyHex() == null || eccMessage.getFromPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_FROM_PUBLIC_KEY_IS_EMPTY);
        }

        // 接收方公钥
        if (eccMessage.getToPublicKeyHex() == null || eccMessage.getToPublicKeyHex().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_TO_PUBLIC_KEY_IS_EMPTY);
        }

        // 数据
        if (eccMessage.getData() == null) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_DATA_IS_NULL);
        }

        // 签名
        if (eccMessage.getSignature() == null || eccMessage.getSignature().isBlank()) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_SIGNATURE_IS_EMPTY);
        }

        // 验证签名
        if (!EccMessageUtil.verify(eccMessage)) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_SIGNATURE_NOT_MATCH);
        }

    }

    /**
     * 订阅私聊消息
     * 
     * @param sessionId      会话 ID
     * @param toPublicKeyHex 接收者公钥
     */
    @Override
    public void subscribeOne(String sessionId, String toPublicKeyHex) {
        unsubscribeOne(sessionId); // 先取消订阅

        // 封装 SessionID
        sessionId = rabbitMqConfig.getUuid() + sessionId;

        // session id -> to public key hex
        stringRedisTemplate.opsForValue().set(
                RedisPrefix.SESSION_ID_2_PUBLIC_KEY_HEX + sessionId,
                toPublicKeyHex);
        // to public key hex -> session id
        stringRedisTemplate.opsForSet().add(
                RedisPrefix.PUBLIC_KEY_HEX_2_SESSION_ID + toPublicKeyHex,
                sessionId);
    }

    /**
     * 取消订阅私聊消息
     * 
     * @param sessionId 会话 ID
     */
    @Override
    public void unsubscribeOne(String sessionId) {
        // 封装 SessionID
        sessionId = rabbitMqConfig.getUuid() + sessionId;

        String toPublicKeyHex = stringRedisTemplate.opsForValue().get(
                RedisPrefix.SESSION_ID_2_PUBLIC_KEY_HEX + sessionId);
        // 删除 session id -> to public key hex
        stringRedisTemplate
                .delete(RedisPrefix.SESSION_ID_2_PUBLIC_KEY_HEX + sessionId);
        // 删除 to public key hex -> session id
        stringRedisTemplate.opsForSet().remove(
                RedisPrefix.PUBLIC_KEY_HEX_2_SESSION_ID + toPublicKeyHex,
                sessionId);
    }

    /**
     * 订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    @Override
    public void subscribeGroup(String sessionId, String groupUuid) {
        // 封装 SessionID
        String sessionIdPaackage = rabbitMqConfig.getUuid() + sessionId;

        // session id -> group uuid
        stringRedisTemplate.opsForSet().add(
                RedisPrefix.SESSION_ID_2_GROUP_UUID + sessionIdPaackage,
                groupUuid);

        // group uuid -> session id
        stringRedisTemplate.opsForSet().add(
                RedisPrefix.GROUP_UUID_2_SESSION_ID + groupUuid,
                sessionIdPaackage);
    }

    /**
     * 取消订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    @Override
    public void unsubscribeGroup(String sessionId, String groupUuid) {
        // 封装 SessionID
        String sessionIdPaackage = rabbitMqConfig.getUuid() + sessionId;

        // 删除 session id -> group uuid
        stringRedisTemplate.opsForSet().remove(
                RedisPrefix.SESSION_ID_2_GROUP_UUID + sessionIdPaackage,
                groupUuid);

        // 删除 group uuid -> session id
        stringRedisTemplate.opsForSet().remove(
                RedisPrefix.GROUP_UUID_2_SESSION_ID + groupUuid,
                sessionIdPaackage);
    }

    /**
     * 取消订阅会话订阅的私聊和群聊消息
     * 
     * @param sessionId 会话 ID
     */
    @Override
    public void unsubscribeAll(String sessionId) {
        // 取消私聊订阅
        unsubscribeOne(sessionId);
        // 获取订阅的群聊 UUID 列表
        Set<String> groupUuids = stringRedisTemplate.opsForSet().members(
                RedisPrefix.SESSION_ID_2_GROUP_UUID + sessionId);
        // 取消订阅
        for (String groupUuid : groupUuids) {
            unsubscribeGroup(sessionId, groupUuid);
        }
    }

    /**
     * 发送私聊消息到 WebSocket 通道，命令为：autoReveiveOne
     * 
     * @param eccMessage 私聊消息
     */
    @Async
    private void sendOneSocket(EccMessage eccMessage) {
        // 获取 SessionID 列表
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(RedisPrefix.PUBLIC_KEY_HEX_2_SESSION_ID + eccMessage.getToPublicKeyHex());

        // 发送消息
        for (String sessionId : members) {
            // 提取路由键
            String routingKey = sessionId.substring(0, rabbitMqConfig.getUuid().length());
            MqMessage mqMessage = new MqMessage();
            mqMessage.setSessionId(sessionId);
            mqMessage.setEccMessage(eccMessage);
            mqMessage.setIsGroup(false);
            rabbitTemplate.convertAndSend("message.direct", routingKey, mqMessage);
        }
    }

    /**
     * 发送群聊消息到 WebSocket 通道，命令为：autoReveiveGroup
     * 
     * @param eccMessage 群聊消息
     */
    @Async
    private void sendGroupSocket(EccMessage eccMessage) {
        // 获取 SessionID 列表
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(
                RedisPrefix.GROUP_UUID_2_SESSION_ID + eccMessage.getToPublicKeyHex());
        // 转发
        for (String sessionId : sessionIds) {
            // 提取路由键
            String routingKey = sessionId.substring(0, rabbitMqConfig.getUuid().length());
            MqMessage mqMessage = new MqMessage();
            mqMessage.setSessionId(sessionId);
            mqMessage.setEccMessage(eccMessage);
            mqMessage.setIsGroup(true);
            rabbitTemplate.convertAndSend("message.direct", routingKey, mqMessage);
        }
    }

}

package com.github.wrx886.e2echo.server.service.impl;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.server.mapper.MessageMapper;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.service.MessageService;
import com.github.wrx886.e2echo.server.util.EccMessageUtil;

public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

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
        message.setTimestamp(new Date(Long.valueOf(eccMessage.getTimestamp())));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(eccMessage.getData());
        message.setSignature(eccMessage.getSignature());
        message.setGroup(false);

        // 插入数据库
        this.save(message);
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
        // 起始时间
        Date startTime = new Date(Long.valueOf(startTimestamp));

        // 查询数据库
        List<Message> messages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getToPublicKeyHex, toPublicKeyHex)
                .ge(Message::getTimestamp, startTime)
                .eq(Message::getGroup, false));

        // 转换并返回
        return messages.stream().map(message -> {
            EccMessage eccMessage = new EccMessage();
            eccMessage.setUuid(message.getUuid());
            eccMessage.setTimestamp(Long.toString(message.getTimestamp().getTime()));
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
     * @param eccMessage 群聊消息，toPublicKeyHex 存储群聊 UUID
     */
    @Override
    public void sendGroup(EccMessage eccMessage) {
        // 检查消息
        checkEccMessage(eccMessage);

        // 封装成数据库实体
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setTimestamp(new Date(Long.valueOf(eccMessage.getTimestamp())));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(eccMessage.getData());
        message.setSignature(eccMessage.getSignature());
        message.setGroup(true);

        // 插入数据库
        this.save(message);
    }

    /**
     * 获取群聊消息
     * 
     * @param groupUuid      群聊 UUID
     * @param startTimestamp 起始时间
     */
    @Override
    public List<EccMessage> receiveGroup(String groupUuid, String startTimestamp) {
        // 起始时间
        Date startTime = new Date(Long.valueOf(startTimestamp));

        // 获取群聊消息
        List<Message> messages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getToPublicKeyHex, groupUuid)
                .ge(Message::getTimestamp, startTime)
                .eq(Message::getGroup, true));

        // 转换并返回
        return messages.stream().map(message -> {
            EccMessage eccMessage = new EccMessage();
            eccMessage.setUuid(message.getUuid());
            eccMessage.setTimestamp(Long.toString(message.getTimestamp().getTime()));
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

}

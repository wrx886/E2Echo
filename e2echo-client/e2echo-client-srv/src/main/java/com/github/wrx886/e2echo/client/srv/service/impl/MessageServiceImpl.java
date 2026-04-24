package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.gui.GuiController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.entity.GroupKey;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.entity.Session;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.store.JsonStore;
import com.github.wrx886.e2echo.client.srv.mapper.MessageMapper;
import com.github.wrx886.e2echo.client.srv.model.vo.GroupMessageVo;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.msg.BaseMessageHandler;
import com.github.wrx886.e2echo.client.srv.service.GroupKeyService;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;
import com.github.wrx886.e2echo.client.srv.store.MessageWebSocketClientStore;
import com.github.wrx886.e2echo.client.srv.util.AesUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final EccController eccController;
    private final MessageWebSocketClientStore clientStore;
    private final ObjectMapper objectMapper;
    private final JsonStore jsonStore;
    private final SessionService sessionService;
    private final GuiController guiController;
    private final GroupKeyService groupKeyService;

    /**
     * 根据会话公钥查询私聊消息
     * 
     * @param session 会话ID，也就是与用户对话的人的公钥
     * @return 私聊消息列表
     */
    @Override
    public List<Message> listOneBySession(String session) {
        // 第一种：作为发送者
        List<Message> messages1 = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getFromPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getToPublicKeyHex, session)
                .eq(Message::getGroup, false));

        // 第二种：作为接收者
        List<Message> messages2 = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getFromPublicKeyHex, session)
                .eq(Message::getToPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getGroup, false));

        // 合并
        HashSet<Message> messageSet = new HashSet<>();
        messageSet.addAll(messages1);
        messageSet.addAll(messages2);

        // 合并
        ArrayList<Message> messages = new ArrayList<>(messageSet);
        // 根据时间排序（倒序）
        messages.sort(Comparator.comparing(Message::getTimestamp).reversed());

        // 返回
        return messages;
    }

    /**
     * 根据会话公钥查询群聊消息
     * 
     * @param session 会话ID，也就是群组ID
     * @return 群聊消息列表
     */
    @Override
    public List<Message> listGroupBySession(String session) {
        // 作为接收者
        List<Message> messages = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getToPublicKeyHex, session)
                .eq(Message::getGroup, true));

        // 根据时间排序（倒序）
        messages.sort(Comparator.comparing(Message::getTimestamp).reversed());

        // 返回
        return messages;
    }

    /**
     * 发送私聊消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param data           消息内容
     * @param type           消息类型
     */
    @Override
    public void sendOne(String toPublicKeyHex, String data, MessageType type) {

        // 封装消息
        SendMessageVo sendMessageVo = new SendMessageVo();
        sendMessageVo.setData(data);
        sendMessageVo.setType(type);

        // 封装为 EccMessage
        EccMessage eccMessage = new EccMessage();
        try {
            eccMessage.setData(objectMapper.writeValueAsString(sendMessageVo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        eccMessage.setFromPublicKeyHex(eccController.getPublicKey());
        eccMessage.setToPublicKeyHex(toPublicKeyHex);

        // 加密
        eccMessage = eccController.encrypt(eccMessage);

        // 发送消息
        clientStore.getClient().sendOne(eccMessage);

        // 封装为数据库消息
        Message message = new Message();
        message.setOwnerPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setUuid(eccMessage.getUuid());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(data);
        message.setType(type);
        message.setGroup(false);

        // 插入数据库
        try {
            this.save(message);
        } catch (DuplicateKeyException e) {
            log.error("", e);
        }

        // 更新会话
        sessionService.updateSession(
                eccMessage.getToPublicKeyHex(),
                message,
                false);

        // 刷新
        guiController.flushAsync();
    }

    /**
     * 发送群聊消息
     * 
     * @param groupUuid 群聊UUID
     * @param data      消息内容
     * @param type      消息类型
     */
    @Override
    public void sendGroup(String groupUuid, String data, MessageType type) {

        // 封装消息
        SendMessageVo sendMessageVo = new SendMessageVo();
        sendMessageVo.setData(data);
        sendMessageVo.setType(type);

        // 获取群组会话
        Session session = sessionService.getSession(groupUuid);
        if (!session.getGroupEnabled()) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_SESSION_GROUP_DISABLED);
        }

        // 获取群聊的密钥
        GroupKey groupKey = groupKeyService.getById(session.getGroupKeyId());

        // 封装为 GroupMessageVo
        GroupMessageVo groupMessageVo = new GroupMessageVo();
        try {
            groupMessageVo
                    .setData(AesUtil.encrypt(objectMapper.writeValueAsString(sendMessageVo), groupKey.getAesKey()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        groupMessageVo.setTimestamp(Long.toString(groupKey.getTimestamp()));

        // 封装为 EccMessage
        EccMessage eccMessage = new EccMessage();
        try {
            eccMessage.setData(objectMapper.writeValueAsString(groupMessageVo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        eccMessage.setFromPublicKeyHex(eccController.getPublicKey());
        eccMessage.setToPublicKeyHex(groupUuid);

        // 签名
        eccMessage = eccController.sign(eccMessage);

        // 发送消息
        clientStore.getClient().sendGroup(eccMessage);
    }

    /**
     * 接受消息
     */
    @Override
    public void receiveMessage() {
        // 起始时间
        Long startTimestamp = jsonStore.getStartTimestamp();
        if (startTimestamp == null) {
            startTimestamp = System.currentTimeMillis();
            jsonStore.setStartTimestamp(startTimestamp);
        }

        // 获取消息并对返回值进行处理
        for (EccMessage eccMessage : clientStore.getClient().receiveOne(eccController.getPublicKey(),
                Long.toString(startTimestamp))) {
            try {
                receiveOneEccMessage(eccMessage);
            } catch (Exception e) {
                log.error("处理收到的单个私聊消息（未解密）异常", e);
            }
        }

        // 获取群聊列表
        List<Session> sessions = sessionService.listSession();
        for (Session session : sessions) {
            if (session.getGroup() && session.getGroupEnabled()) {
                // 获取消息并对返回值进行处理
                for (EccMessage eccMessage : clientStore.getClient().receiveGroup(session.getPublicKeyHex(),
                        Long.toString(startTimestamp))) {
                    try {
                        receiveGroupEccMessage(eccMessage);
                    } catch (Exception e) {
                        log.error("处理收到的群聊消息（未解密）异常", e);
                    }
                }
            }
        }

        // 更新时间
        jsonStore.setStartTimestamp(System.currentTimeMillis());

        // 刷新
        guiController.flushAsync();
    }

    /**
     * 自动接收单聊消息
     * 
     * @param eccMessage 群聊消息
     */
    @Override
    public void autoReceiveOne(EccMessage eccMessage) {
        try {
            receiveOneEccMessage(eccMessage);
            guiController.flushAsync();
        } catch (Exception e) {
            log.error("处理收到的单个私聊消息（未解密）异常", e);
        }
    }

    /**
     * 自动接收群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    @Override
    public void autoReceiveGroup(EccMessage eccMessage) {
        try {
            receiveGroupEccMessage(eccMessage);
            guiController.flushAsync();
        } catch (Exception e) {
            log.error("处理收到的群聊消息异常", e);
        }
    }

    /**
     * 处理结束到的单个私聊消息（未解密）
     * 
     * @param eccMessage ECC消息
     */
    private void receiveOneEccMessage(EccMessage eccMessage) {

        // 解密消息
        eccMessage = eccController.decrypt(eccMessage);

        // 时间处理，必须先解密，解密时会验证消息的签名
        Long startTimestamp = jsonStore.getStartTimestamp();
        if (startTimestamp == null) {
            startTimestamp = System.currentTimeMillis() - (1000L * 60); // 提前 1 分钟
        }

        // 消息是否过期
        if (Long.parseLong(eccMessage.getTimestamp()) <= 0 || Long.parseLong(eccMessage.getTimestamp()) < startTimestamp) {
            return;
        }

        // 获取数据
        SendMessageVo sendMessageVo;
        try {
            sendMessageVo = objectMapper.readValue(eccMessage.getData(), SendMessageVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 针对不同的消息类型进行处理
        Class<? extends BaseMessageHandler> messageHadnlerClass = BaseMessageHandler
                .getHandler(sendMessageVo.getType(), false);
        if (messageHadnlerClass == null) {
            sendMessageVo.setType(MessageType.UNSUPPORTED);
            sendMessageVo.setData("不支持的消息类型");
        } else {
            // 处理消息
            BeanProvider.getBean(messageHadnlerClass).receiveHandler(eccMessage, sendMessageVo);
        }

        // 封装为数据库消息
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setOwnerPublicKeyHex(eccController.getPublicKey());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(sendMessageVo.getData());
        message.setType(sendMessageVo.getType());
        message.setGroup(false);

        // 插入数据库
        try {
            this.save(message);
        } catch (DuplicateKeyException e) {
            log.error("", e);
        }

        // 更新会话
        sessionService.updateSession(
                eccMessage.getFromPublicKeyHex(),
                message,
                false);

        // 更新 更新时间
        jsonStore.setStartTimestamp(System.currentTimeMillis());
    }

    /**
     * 接收群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    private void receiveGroupEccMessage(EccMessage eccMessage) {

        // 验证消息
        if (!eccController.verify(eccMessage)) {
            // 验证失败
            return;
        }

        // 过期验证
        Long startTimestamp = jsonStore.getStartTimestamp();
        if (startTimestamp == null) {
            startTimestamp = System.currentTimeMillis() - (1000L * 60); // 提前 1 分钟
        }

        // 消息是否过期
        if (Long.parseLong(eccMessage.getTimestamp()) <= 0 || Long.parseLong(eccMessage.getTimestamp()) < startTimestamp) {
            return;
        }

        // 获取群聊消息
        GroupMessageVo groupMessageVo;
        try {
            groupMessageVo = objectMapper.readValue(eccMessage.getData(), GroupMessageVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 获取 Session
        Session session = sessionService.getSession(eccMessage.getToPublicKeyHex());
        if (session == null) {
            // 群聊不存在
            return;
        }

        // 群聊未启用
        if (!session.getGroupEnabled()) {
            return;
        }

        // 获取群聊 GroupKey
        GroupKey groupKey = groupKeyService.getById(session.getGroupKeyId());
        if (groupKey == null) {
            // 群聊密钥不存在
            return;
        }

        // 判读是否使用过期的密钥
        if (!groupKey.getTimestamp().equals(Long.valueOf(groupMessageVo.getTimestamp()))
                && groupKey.getTimestamp() + 1000L * 60 < Long.parseLong(eccMessage.getTimestamp())) {
            // 使用过时密钥，新密钥已经发布超过一分钟，还在使用原先的密钥，视为无效
            return;
        }

        // 获取解密的密钥
        String aesKey = groupKeyService.get(eccMessage.getToPublicKeyHex(),
                Long.valueOf(groupMessageVo.getTimestamp()));
        if (aesKey == null) {
            // 密钥不存在
            return;
        }

        // 解密消息
        String data;
        try {
            data = AesUtil.decrypt(groupMessageVo.getData(), aesKey);
        } catch (Exception e) {
            log.error(null, e);
            return;
        }

        // 获取数据
        SendMessageVo sendMessageVo;
        try {
            sendMessageVo = objectMapper.readValue(data, SendMessageVo.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 针对不同的消息类型进行处理
        Class<? extends BaseMessageHandler> messageHadnlerClass = BaseMessageHandler
                .getHandler(sendMessageVo.getType(), true);
        if (messageHadnlerClass == null) {
            sendMessageVo.setType(MessageType.UNSUPPORTED);
            sendMessageVo.setData("不支持的消息类型");
        } else {
            // 处理消息
            BeanProvider.getBean(messageHadnlerClass).receiveHandler(eccMessage, sendMessageVo);
        }

        // 封装为数据库消息
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setOwnerPublicKeyHex(eccController.getPublicKey());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(sendMessageVo.getData());
        message.setType(sendMessageVo.getType());
        message.setGroup(true);

        // 插入数据库
        try {
            this.save(message);
        } catch (DuplicateKeyException e) {
            log.error("", e);
        }

        // 更新会话
        sessionService.updateSession(
                eccMessage.getToPublicKeyHex(),
                message,
                true);

        // 为群员创建会话
        try {
            sessionService.create(eccMessage.getFromPublicKeyHex(), false);
        } catch (E2EchoException e) {
            // 重复创建的错误不需要处理
        }

        // 更新 更新时间
        jsonStore.setStartTimestamp(System.currentTimeMillis());
    }

}

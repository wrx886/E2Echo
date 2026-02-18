package com.github.wrx886.e2echo.server.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.server.mapper.MessageMapper;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.service.MessageService;
import com.github.wrx886.e2echo.server.socket.MessageWebSocketHandler;
import com.github.wrx886.e2echo.server.util.EccMessageUtil;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    // 会话 ID -> 接收者公钥
    private final ConcurrentHashMap<String, String> sessionId2ToPublicKeyHex = new ConcurrentHashMap<>();

    // 接收者公钥 -> 会话 ID 列表
    private final ConcurrentHashMap<String, Set<String>> toPublicKeyHex2SessionIds = new ConcurrentHashMap<>();

    // 会话 ID -> 群聊 UUID 列表
    private final ConcurrentHashMap<String, Set<String>> sessionId2GroupUuids = new ConcurrentHashMap<>();

    // 群聊 UUID -> 会话 ID 列表
    private final ConcurrentHashMap<String, Set<String>> groupUuid2SessionIds = new ConcurrentHashMap<>();

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
        toPublicKeyHex2SessionIds.compute(toPublicKeyHex, (k, sessionIds) -> {
            sessionId2ToPublicKeyHex.put(sessionId, toPublicKeyHex);
            sessionIds = sessionIds == null ? new HashSet<>() : sessionIds;
            sessionIds.add(sessionId);
            return sessionIds;
        });
    }

    /**
     * 取消订阅私聊消息
     * 
     * @param sessionId 会话 ID
     */
    @Override
    public void unsubscribeOne(String sessionId) {
        sessionId2ToPublicKeyHex.computeIfPresent(sessionId, (k, toPublicKeyHex) -> {
            toPublicKeyHex2SessionIds.computeIfPresent(toPublicKeyHex, (kk, sessionIds) -> {
                sessionIds.remove(sessionId);
                return sessionIds.isEmpty() ? null : sessionIds;
            });
            return null; // 删除
        });
    }

    /**
     * 订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    @Override
    public void subscribeGroup(String sessionId, String groupUuid) {
        // 先开 sessionId2GroupUuids 形成锁，再开启 groupUuid2SessionIds，下同
        sessionId2GroupUuids.compute(groupUuid, (k, groupUuids) -> {
            groupUuid2SessionIds.compute(groupUuid, (kk, sessionIds) -> {
                sessionIds = sessionIds == null ? new HashSet<>() : sessionIds;
                sessionIds.add(sessionId);
                return sessionIds;
            });

            groupUuids = groupUuids == null ? new HashSet<>() : groupUuids;
            groupUuids.add(sessionId);
            return groupUuids;
        });
    }

    /**
     * 取消订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    @Override
    public void unsubscribeGroup(String sessionId, String groupUuid) {
        sessionId2GroupUuids.computeIfPresent(groupUuid, (k, groupUuids) -> {
            groupUuid2SessionIds.computeIfPresent(groupUuid, (kk, sessionIds) -> {
                sessionIds.remove(sessionId);
                return sessionIds.isEmpty() ? null : sessionIds;
            });

            groupUuids.remove(sessionId);
            return groupUuids.isEmpty() ? null : groupUuids;
        });
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
        // 取消群聊订阅
        sessionId2GroupUuids.computeIfPresent(sessionId, (k, groupUuids) -> {
            for (String groupUuid : groupUuids) {
                groupUuid2SessionIds.computeIfPresent(groupUuid, (kk, sessionIds) -> {
                    sessionIds.remove(sessionId);
                    return sessionIds.isEmpty() ? null : sessionIds;
                });
            }
            return null; // 删除 Session
        });
    }

    /**
     * 发送私聊消息到 WebSocket 通道，命令为：autoReveiveOne
     * 
     * @param eccMessage 私聊消息
     */
    private void sendOneSocket(EccMessage eccMessage) {
        toPublicKeyHex2SessionIds.computeIfPresent(eccMessage.getToPublicKeyHex(), (k, sessionIds) -> {
            for (String sessionId : sessionIds) {
                try {
                    MessageWebSocketHandler.sendMessage(sessionId, "autoReveiveOne", eccMessage);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
            return sessionIds;
        });
    }

    /**
     * 发送群聊消息到 WebSocket 通道，命令为：autoReveiveGroup
     * 
     * @param eccMessage 群聊消息
     */
    private void sendGroupSocket(EccMessage eccMessage) {
        groupUuid2SessionIds.computeIfPresent(eccMessage.getToPublicKeyHex(), (k, sessionIds) -> {
            for (String sessionId : sessionIds) {
                try {
                    MessageWebSocketHandler.sendMessage(sessionId, "autoReveiveGroup", eccMessage);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
            return sessionIds;
        });
    }

}

package com.github.wrx886.e2echo.server.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.entity.Message;

public interface MessageService extends IService<Message> {

    /**
     * 发送消息
     * 
     * @param eccMessage
     */
    void sendOne(EccMessage eccMessage);

    /**
     * 接收消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param startTimestamp 起始时间
     * @return 接收到的消息
     */
    List<EccMessage> receiveOne(String toPublicKeyHex, String startTimestamp);

    /**
     * 发送群聊消息（格式：{群主公钥}:{群聊UUID}）
     * 
     * @param eccMessage 群聊消息，toPublicKeyHex 存储群聊 UUID
     */
    void sendGroup(EccMessage eccMessage);

    /**
     * 获取群聊消息
     * 
     * @param groupUuid      群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     * @param startTimestamp 起始时间
     * @return 群聊消息
     */
    List<EccMessage> receiveGroup(String groupUuid, String startTimestamp);

    /**
     * 订阅私聊消息
     * 
     * @param sessionId      会话 ID
     * @param toPublicKeyHex 接收者公钥
     */
    void subscribeOne(String sessionId, String toPublicKeyHex);

    /**
     * 取消订阅私聊消息
     * 
     * @param sessionId 会话 ID
     */
    void unsubscribeOne(String sessionId);

    /**
     * 订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    void subscribeGroup(String sessionId, String groupUuid);

    /**
     * 取消订阅群聊消息
     * 
     * @param sessionId 会话 ID
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    void unsubscribeGroup(String sessionId, String groupUuid);

    /**
     * 取消订阅会话订阅的私聊和群聊消息
     * 
     * @param sessionId 会话 ID
     */
    void unsubscribeAll(String sessionId);

}

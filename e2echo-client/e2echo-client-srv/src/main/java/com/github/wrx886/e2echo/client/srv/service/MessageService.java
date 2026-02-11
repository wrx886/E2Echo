package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

public interface MessageService extends IService<Message> {

    /**
     * 发送单聊消息
     * 
     * @param eccMessage 消息
     */
    void sendOne(EccMessage eccMessage);

    /**
     * 接收单聊消息
     * 
     * @param toPublicKeyHex 接收方公钥
     * @param startTimestamp 开始时间戳(int64)
     * @return 私聊消息列表
     */
    List<EccMessage> receiveOne(String toPublicKeyHex, String startTimestamp);

    /**
     * 发送群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    void sendGroup(EccMessage eccMessage);

    /**
     * 接收群聊消息
     * 
     * @param groupUuid      群组 UUID
     * @param startTimestamp 开始时间戳(int64)
     * @return
     */
    List<EccMessage> receiveGroup(String groupUuid, String startTimestamp);

    /**
     * 自动接收单聊消息
     * 
     * @param eccMessage 群聊消息
     */
    void autoReveiveOne(EccMessage eccMessage);

    /**
     * 自动接收群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    void autoReveiveGroup(EccMessage eccMessage);

    /**
     * 根据会话公钥查询私聊消息
     * 
     * @param session 会话ID，也就是与用户对话的人的公钥
     * @return 私聊消息列表
     */
    List<Message> listOneBySession(String session);

    /**
     * 根据会话公钥查询群聊消息
     * 
     * @param session 会话ID，也就是群组ID
     * @return 群聊消息列表
     */
    List<Message> listGroupBySession(String session);

    /**
     * 发送私聊消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param data           消息内容
     * @param type           消息类型
     */
    void sendOne(String toPublicKeyHex, String data, MessageType type);

    /**
     * 接收消息
     */
    void receiveMessage();

    /**
     * 订阅单聊消息
     */
    void subscribeOne();

}

package com.github.wrx886.e2echo.client.common.controller.srv;

import java.util.List;

import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;

// 消息 api
public interface MessageController {

    /**
     * 发送私聊消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param data           消息内容
     * @param type           消息类型
     */
    void sendOneMessage(String toPublicKeyHex, String data, MessageType type);

    /**
     * 接收消息
     */
    void receiveMessage();

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
     * 根据ID查询消息
     * 
     * @param id 消息ID
     * @return 消息
     */
    Message getById(Long id);

}

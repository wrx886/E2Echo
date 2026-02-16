package com.github.wrx886.e2echo.client.srv.controller.impl;

import java.util.List;

import org.springframework.stereotype.Controller;

import com.github.wrx886.e2echo.client.common.controller.srv.MessageController;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.service.MessageService;

import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class MessageControllerImpl implements MessageController {

    private final MessageService messageService;

    /**
     * 发送私聊消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param data           消息内容
     * @param type           消息类型
     */
    @Override
    public void sendOneMessage(String toPublicKeyHex, String data, MessageType type) {
        messageService.sendOne(toPublicKeyHex, data, type);
    }

    /**
     * 根据会话公钥查询私聊消息
     * 
     * @param session 会话ID，也就是与用户对话的人的公钥
     * @return 私聊消息列表
     */
    @Override
    public List<Message> listOneBySession(String session) {
        return messageService.listOneBySession(session);
    }

    /**
     * 根据会话公钥查询群聊消息
     * 
     * @param session 会话ID，也就是群组ID
     * @return 群聊消息列表
     */
    @Override
    public List<Message> listGroupBySession(String session) {
        return messageService.listGroupBySession(session);
    }

    /**
     * 接收消息
     */
    @Override
    public void receiveMessage() {
        messageService.receiveMessage();
    }

    /**
     * 根据ID查询消息
     * 
     * @param id 消息ID
     * @return 消息
     */
    @Override
    public Message getById(Long id) {
        return messageService.getById(id);
    }

    /**
     * 发送群聊消息
     * 
     * @param groupUuid 群聊 UUID
     * @param data      消息内容
     * @param type      消息类型
     */
    @Override
    public void sendGroupMessage(String groupUuid, String data, MessageType type) {
        messageService.sendGroup(groupUuid, data, type);
    }

}

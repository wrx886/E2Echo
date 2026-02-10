package com.github.wrx886.e2echo.client.srv.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.entity.Message;

public interface MessageService extends IService<Message> {

    /**
     * 发送单聊消息
     * 
     * @param eccMessage 消息
     */
    public void sendOne(EccMessage eccMessage);

    /**
     * 接收单聊消息
     * 
     * @param toPublicKeyHex 接收方公钥
     * @param startTimestamp 开始时间戳(int64)
     * @return 私聊消息列表
     */
    public List<EccMessage> receiveOne(String toPublicKeyHex, String startTimestamp);

    /**
     * 发送群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    public void sendGroup(EccMessage eccMessage);

    /**
     * 接收群聊消息
     * 
     * @param groupUuid      群组 UUID
     * @param startTimestamp 开始时间戳(int64)
     * @return
     */
    public List<EccMessage> receiveGroup(String groupUuid, String startTimestamp);

}

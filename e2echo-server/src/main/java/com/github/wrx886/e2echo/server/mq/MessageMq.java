package com.github.wrx886.e2echo.server.mq;

import com.github.wrx886.e2echo.server.model.EccMessage;

// 消息队列
@SuppressWarnings("unused")
public interface MessageMq {

    /**
     * 将消息分发到指定的 Session
     * 
     * @param sessionId  WebSocketSession id（这里是实例UUID+WebSocketSession id）
     * @param eccMessage 消息
     * @param isGroup    是否是群组消息
     */
    void messageDistribute(String sessionId, EccMessage eccMessage, Boolean isGroup);

    /**
     * 将消息发布到所有订阅者
     * 
     * @param eccMessage 消息
     * @param isGroup    是否是群组消息
     */
    void messagePublish(EccMessage eccMessage, Boolean isGroup);

}

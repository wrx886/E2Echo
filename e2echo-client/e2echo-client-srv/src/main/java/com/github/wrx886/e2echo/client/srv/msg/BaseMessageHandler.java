package com.github.wrx886.e2echo.client.srv.msg;

import java.util.concurrent.ConcurrentHashMap;

import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;

public abstract class BaseMessageHandler {

    // 记录映射
    protected static final ConcurrentHashMap<MessageType, Class<? extends BaseMessageHandler>> personMessageHandlerMap = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<MessageType, Class<? extends BaseMessageHandler>> groupMessageHandlerMap = new ConcurrentHashMap<>();

    /**
     * 获取处理器
     *
     * @param messageType 消息类型
     * @return 处理器的 Class
     */
    public static Class<? extends BaseMessageHandler> getHandler(MessageType messageType, boolean isGroup) {
        if (isGroup) {
            return groupMessageHandlerMap.get(messageType);
        } else {
            return personMessageHandlerMap.get(messageType);
        }
    }

    /**
     * 接收消息处理
     *
     * @param sendMessageVo 发送的消息
     */
    public abstract void receiveHandler(EccMessage eccMessage, SendMessageVo sendMessageVo);

}

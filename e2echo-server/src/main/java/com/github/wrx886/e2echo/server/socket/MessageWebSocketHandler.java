package com.github.wrx886.e2echo.server.socket;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.server.service.MessageService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MessageWebSocketHandler extends BaseWebSocketHandler {

    private final MessageService messageService;

    /**
     * 发送私聊消息
     * 
     * @param session    会话
     * @param eccMessage ECC 消息
     */
    public void sendOne(WebSocketSession session, EccMessage eccMessage) {
        messageService.sendOne(eccMessage);
    }

    /**
     * 接收私聊消息
     * 
     * @param session                   会话
     * @param receiveOneMessageSocketVo 接收私聊消息参数
     * @return ECC 私聊消息列表
     */
    public List<EccMessage> receiveOne(WebSocketSession session, ReceiveOneMessageSocketVo receiveOneMessageSocketVo) {
        return messageService.receiveOne(
                receiveOneMessageSocketVo.getToPublicKeyHex(),
                receiveOneMessageSocketVo.getStartTimestamp());
    }

    /**
     * 发送群聊消息
     * 
     * @param session    会话
     * @param eccMessage ECC 群聊消息，toPublicKeyHex字段填写群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    public void sendGroup(WebSocketSession session, EccMessage eccMessage) {
        messageService.sendGroup(eccMessage);
    }

    /**
     * 接收群聊消息
     * 
     * @param session                     会话
     * @param receiveGroupMessageSocketVo 接收群聊消息参数
     * @return ECC 群聊消息列表
     */
    public List<EccMessage> receiveGroup(WebSocketSession session,
            ReceiveGroupMessageSocketVo receiveGroupMessageSocketVo) {
        return messageService.receiveGroup(
                receiveGroupMessageSocketVo.getGroupUuid(),
                receiveGroupMessageSocketVo.getStartTimestamp());
    }

    /**
     * 订阅私聊消息
     * 
     * @param session        会话
     * @param toPublicKeyHex 接收者公钥
     */
    public void subscribeOne(WebSocketSession session, String toPublicKeyHex) {
        messageService.subscribeOne(session.getId(), toPublicKeyHex);
    }

    /**
     * 取消订阅私聊消息
     * 
     * @param session 会话
     */
    public void unsubscribe(WebSocketSession session) {
        messageService.unsubscribeOne(session.getId());
    }

    /**
     * 订阅群聊消息
     * 
     * @param session   会话
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    public void subscribeGroup(WebSocketSession session, String groupUuid) {
        messageService.subscribeGroup(session.getId(), groupUuid);
    }

    /**
     * 取消订阅群聊消息
     * 
     * @param session   会话
     * @param groupUuid 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
     */
    public void unsubscribeGroup(WebSocketSession session, String groupUuid) {
        messageService.unsubscribeGroup(session.getId(), groupUuid);
    }

    @Override
    protected void afterClose(WebSocketSession session) {
        // 取消所有订阅
        messageService.unsubscribeAll(session.getId());
    }

}

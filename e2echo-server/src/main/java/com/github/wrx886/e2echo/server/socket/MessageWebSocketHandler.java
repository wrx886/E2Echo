package com.github.wrx886.e2echo.server.socket;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.server.service.MessageService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
@Component
@AllArgsConstructor
public class MessageWebSocketHandler extends BaseWebSocketHandler {

    private final MessageService messageService;
    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

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
    public void unsubscribeOne(WebSocketSession session) {
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
     * 批量订阅群聊消息
     *
     * @param session    会话
     * @param groupUuids 群聊 UUID列表
     */
    public void subscribeGroups(WebSocketSession session, List<String> groupUuids) {
        for (String groupUuid : groupUuids) {
            messageService.subscribeGroup(session.getId(), groupUuid);
        }
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
    protected void afterEstablished(WebSocketSession session) {
        sessionMap.put(session.getId(), session);
    }

    @Override
    protected void afterClose(WebSocketSession session) {
        // 取消所有订阅
        try {
            messageService.unsubscribeAll(session.getId());
        } finally {
            sessionMap.remove(session.getId());
        }
    }

    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleClose() {

        // 断开所有连接
        Collection<WebSocketSession> sessions = sessionMap.values();
        for (WebSocketSession session : sessions) {
            try {
                session.close();
            } catch (Exception e) {
                log.error(null, e);
            }
        }

        // 等待所有连接关闭
        try {
            // 5s - 60s
            Thread.sleep(Long.max(5_000L, Long.min(sessions.size() * 100L, 60_000L))); // 10s
        } catch (InterruptedException e) {
            log.error(null, e);
        }

        // 未关闭提示
        if (!sessionMap.isEmpty()) {
            log.warn("{} WebSocket connection(s) still active at shutdown",
                    sessionMap.size());
        } else {
            log.info("All WebSocket connections closed successfully");
        }
    }

}

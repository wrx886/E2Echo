package com.github.wrx886.e2echo.server.web.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.wrx886.e2echo.server.config.WebSocketConfig;
import com.github.wrx886.e2echo.server.model.entity.BaseEntity;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.model.entity.User;
import com.github.wrx886.e2echo.server.model.enums.MessageType;
import com.github.wrx886.e2echo.server.model.vo.message.MessageVo;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.web.mapper.MessageMapper;
import com.github.wrx886.e2echo.server.web.wocket.handler.BaseWebSocketHandler;
import com.google.common.collect.ConcurrentHashMultiset;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginService loginService;

    // Session ID 对应的发送模式
    private ConcurrentHashMap<String, MessageType> sessionId2MessageType = new ConcurrentHashMap<>();

    // 发送消息
    public void send(User user, Message message) {
        // 判断发送时间是否存在
        if (message.getSendTime() == null) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_SEND_EXPIRED);
        }

        // 判断发送时间是否超时（发送时间必须在当前时间的 -1 min 到 +1 ms 治之间，否则超时）
        long sendTime = message.getSendTime().getTime();
        long currentTime = System.currentTimeMillis();
        if (currentTime < sendTime || sendTime + 1000 * 60 < currentTime) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_SEND_EXPIRED);
        }

        // 获取发送者的信息
        User sender = user;

        // 校验发送者
        if (!sender.getPublicKey().equals(message.getFromPublicKey())) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_SENDER_INFO_NOT_MATCH);
        }

        // 获取接收者
        User receiver = userService
                .getOne(new LambdaQueryWrapper<User>().eq(User::getPublicKey, message.getToPublicKey()));

        // 判断接收者是否存在
        if (receiver == null) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_RECEIVER_NOT_EXIST);
        }

        // 判断消息是否过长
        if (message.getData().length() >= 4096) {
            throw new E2EchoException(ResultCodeEnum.MESSAGE_DATA_TO_LONG);
        }

        // 发送消息
        message.setFromUserId(sender.getId());
        message.setToUserId(receiver.getId());
        BeanUtils.copyProperties(new BaseEntity(), message);
        save(message);

        // 自动发送
        autoSend(message);
    }

    // 接收消息
    public List<Message> receive(User user, MessageVo messageVo) {
        return list(new LambdaQueryWrapper<Message>()
                .eq(Message::getToUserId, user.getId())
                .ge(Message::getSendTime, messageVo.getStartTime())
                .eq(messageVo.getMessageType() != null, Message::getMessageType, messageVo.getMessageType()));
    }

    // 配置自动接收
    public void registryAutoReceive(String sessionId, MessageType messageType) {
        sessionId2MessageType.put(sessionId, messageType);
    }

    // 取消配置自动接收
    public void cancelRegistryAutoReceive(String sessionId) {
        sessionId2MessageType.remove(sessionId);
    }

    // 自动发送
    private void autoSend(Message message) {
        // 1. 查询用户 ID 对应的 Session ID 是否存在
        ConcurrentHashMultiset<String> sessionIds = loginService.getUserSessionMap()
                .getSessionIdsByUserId(message.getToUserId());
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        // 2. 遍历所有的 SessionId
        for (String sessionId : sessionIds) {
            // 获取 Session
            WebSocketSession session = BaseWebSocketHandler.getSessionBySessionId(sessionId);

            // 判断 session 是否属于 MessageHandler 的路径
            if (!WebSocketConfig.MessageHandlerPath.equals(session.getUri().getPath())) {
                continue;
            }

            // 查询是否配置自动发送
            if (!sessionId2MessageType.containsKey(sessionId)) {
                continue;
            }

            // 获取自动发送配置
            MessageType messageType = sessionId2MessageType.get(sessionId);
            if (messageType == null) {
                // 如果没有配置，则不发送
                continue;
            }

            // 根据发送配置决定是否发送
            if (messageType.equals(MessageType.GROUP) || messageType.equals(message.getMessageType())) {
                try {
                    BaseWebSocketHandler.sendMessage(sessionId, "autoReceive", message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}

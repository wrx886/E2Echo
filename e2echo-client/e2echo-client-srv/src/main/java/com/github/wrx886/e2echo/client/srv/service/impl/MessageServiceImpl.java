package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.common.store.JsonStore;
import com.github.wrx886.e2echo.client.srv.mapper.MessageMapper;
import com.github.wrx886.e2echo.client.srv.model.socket.WebSocketResult;
import com.github.wrx886.e2echo.client.srv.model.vo.SendMessageVo;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.result.ResultCodeEnum;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.service.SessionService;
import com.github.wrx886.e2echo.client.srv.socket.MessageWebSocketClient;
import com.github.wrx886.e2echo.client.srv.store.MessageWebSocketClientStore;

import jakarta.activation.UnsupportedDataTypeException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final EccController eccController;
    private final MessageWebSocketClientStore messageWebSocketClientStore;
    private final ObjectMapper objectMapper;
    private final JsonStore jsonStore;
    private final SessionService sessionService;

    /**
     * 发送单聊消息
     * 
     * @param eccMessage 消息
     */
    public void sendOne(EccMessage eccMessage) {
        try {
            MessageWebSocketClient client = messageWebSocketClientStore.getClient();
            WebSocketResult<?> result = client.sendMessageAndWait("sendOne", eccMessage);
            if (!ResultCodeEnum.OK.getCode().equals(result.getCode())) {
                throw new E2EchoException(result.getMessage());
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_TIMEOUT);
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.FAIL);
        }
    }

    /**
     * 接收单聊消息
     * 
     * @param toPublicKeyHex 接收方公钥
     * @param startTimestamp 开始时间戳(int64)
     * @return 私聊消息列表
     */
    public List<EccMessage> receiveOne(String toPublicKeyHex, String startTimestamp) {
        try {
            // 获取私聊消息
            MessageWebSocketClient client = messageWebSocketClientStore.getClient();
            ReceiveOneMessageSocketVo vo = new ReceiveOneMessageSocketVo();
            vo.setToPublicKeyHex(toPublicKeyHex);
            vo.setStartTimestamp(startTimestamp);
            WebSocketResult<?> result = client.sendMessageAndWait("receiveOne", vo);
            if (!ResultCodeEnum.OK.getCode().equals(result.getCode())) {
                throw new E2EchoException(result.getMessage());
            }

            // 类型转变
            ArrayList<EccMessage> eccMessages = new ArrayList<>();
            for (Object message : objectMapper.convertValue(result.getData(), List.class)) {
                EccMessage eccMessage = objectMapper.convertValue(message, EccMessage.class);
                eccMessages.add(eccMessage);
            }

            // 返回
            return eccMessages;
        } catch (E2EchoException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_TIMEOUT);
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.FAIL);
        }
    }

    /**
     * 发送群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    public void sendGroup(EccMessage eccMessage) {
        try {
            MessageWebSocketClient client = messageWebSocketClientStore.getClient();
            WebSocketResult<?> result = client.sendMessageAndWait("sendGroup", eccMessage);
            if (!ResultCodeEnum.OK.getCode().equals(result.getCode())) {
                throw new E2EchoException(result.getMessage());
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_TIMEOUT);
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.FAIL);
        }
    }

    /**
     * 接收群聊消息
     * 
     * @param groupUuid      群组 UUID
     * @param startTimestamp 开始时间戳(int64)
     * @return
     */
    public List<EccMessage> receiveGroup(String groupUuid, String startTimestamp) {
        try {
            // 获取群聊消息
            MessageWebSocketClient client = messageWebSocketClientStore.getClient();
            ReceiveGroupMessageSocketVo vo = new ReceiveGroupMessageSocketVo();
            vo.setGroupUuid(groupUuid);
            vo.setStartTimestamp(startTimestamp);
            WebSocketResult<?> result = client.sendMessageAndWait("receiveGroup", vo);
            if (!ResultCodeEnum.OK.getCode().equals(result.getCode())) {
                throw new E2EchoException(result.getMessage());
            }

            // 类型转变
            ArrayList<EccMessage> eccMessages = new ArrayList<>();
            for (Object message : objectMapper.convertValue(result.getData(), List.class)) {
                EccMessage eccMessage = objectMapper.convertValue(message, EccMessage.class);
                eccMessages.add(eccMessage);
            }

            // 返回
            return eccMessages;
        } catch (E2EchoException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_TIMEOUT);
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.FAIL);
        }
    }

    /**
     * 自动接收单聊消息
     * 
     * @param eccMessage 群聊消息
     */
    @Override
    public void autoReveiveOne(EccMessage eccMessage) {
        try {
            receiveOneEccMessage(eccMessage);
        } catch (Exception e) {
            log.error("处理收到的单个私聊消息（未解密）异常", e);
        }
    }

    /**
     * 自动接收群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    @Override
    public void autoReveiveGroup(EccMessage eccMessage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'autoReveiveGroup'");
    }

    /**
     * 根据会话公钥查询私聊消息
     * 
     * @param session 会话ID，也就是与用户对话的人的公钥
     * @return 私聊消息列表
     */
    @Override
    public List<Message> listOneBySession(String session) {
        return listBySession(session, false);
    }

    /**
     * 根据会话公钥查询群聊消息
     * 
     * @param session 会话ID，也就是群组ID
     * @return 群聊消息列表
     */
    @Override
    public List<Message> listGroupBySession(String session) {
        return listBySession(session, true);
    }

    /**
     * 根据会话公钥或群聊查询私聊消息
     * 
     * @param session 会话公钥或群组 UUID
     * @return 私聊或群聊消息列表
     */
    private List<Message> listBySession(String session, boolean isGroup) {
        // 第一种：作为发送者
        List<Message> messages1 = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getToPublicKeyHex, session)
                .eq(Message::getGroup, isGroup));

        // 第二种：作为接收者
        List<Message> messages2 = this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getOwnerPublicKeyHex, eccController.getPublicKey())
                .eq(Message::getFromPublicKeyHex, session)
                .eq(Message::getGroup, isGroup));

        // 合并
        ArrayList<Message> messages = new ArrayList<>();
        messages.addAll(messages1);
        messages.addAll(messages2);
        // 根据时间排序（倒序）
        messages.sort(Comparator.comparing(Message::getTimestamp).reversed());

        // 返回
        return messages;
    }

    /**
     * 发送私聊消息
     * 
     * @param toPublicKeyHex 接收者公钥
     * @param data           消息内容
     * @param type           消息类型
     */
    @Override
    public void sendOne(String toPublicKeyHex, String data, MessageType type) {

        // 针对不同的消息类型进行处理
        if (type == MessageType.TEXT) {
            // 不需要处理
        } else {
            // 其他消息类型暂不支持
            throw new RuntimeException(new UnsupportedDataTypeException());
        }

        // 封装消息
        SendMessageVo sendMessageVo = new SendMessageVo();
        sendMessageVo.setData(data);
        sendMessageVo.setType(type);

        // 封装为 EccMessage
        EccMessage eccMessage = new EccMessage();
        try {
            eccMessage.setData(objectMapper.writeValueAsString(sendMessageVo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        eccMessage.setFromPublicKeyHex(eccController.getPublicKey());
        eccMessage.setToPublicKeyHex(toPublicKeyHex);

        // 加密
        eccMessage = eccController.encrypt(eccMessage);

        // 发送消息
        sendOne(eccMessage);

        // 封装为数据库消息
        Message message = new Message();
        message.setOwnerPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setUuid(eccMessage.getUuid());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(data);
        message.setType(type);
        message.setGroup(false);

        // 插入数据库
        this.save(message);

        // 更新会话
        sessionService.updateSession(
                eccMessage.getFromPublicKeyHex(),
                message,
                false);
    }

    /**
     * 处理结束到的单个私聊消息（未解密）
     * 
     * @param eccMessage
     */
    private void receiveOneEccMessage(EccMessage eccMessage) {

        // 解密消息
        eccMessage = eccController.decrypt(eccMessage);

        // 时间处理，必须先解密，解密时会验证消息的签名
        Long startTimestamp = jsonStore.getStartTimestamp();
        if (startTimestamp == null) {
            startTimestamp = System.currentTimeMillis() - (1000L * 60); // 提前 1 分钟
        }

        // 消息是否过期
        if (Long.valueOf(eccMessage.getTimestamp()) <= 0 || Long.valueOf(eccMessage.getTimestamp()) < startTimestamp) {
            return;
        }

        // 获取数据
        SendMessageVo sendMessageVo = objectMapper.convertValue(eccMessage.getData(), SendMessageVo.class);

        // 针对不同的消息类型进行处理
        if (sendMessageVo.getType() == MessageType.TEXT) {
            // 不需要处理
        } else {
            // 其他消息类型暂不支持
            throw new RuntimeException(new UnsupportedDataTypeException());
        }

        // 封装为数据库消息
        Message message = new Message();
        message.setUuid(eccMessage.getUuid());
        message.setOwnerPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setTimestamp(Long.valueOf(eccMessage.getTimestamp()));
        message.setFromPublicKeyHex(eccMessage.getFromPublicKeyHex());
        message.setToPublicKeyHex(eccMessage.getToPublicKeyHex());
        message.setData(sendMessageVo.getData());
        message.setType(sendMessageVo.getType());
        message.setGroup(false);

        // 插入数据库
        this.save(message);

        // 更新会话
        sessionService.updateSession(
                eccMessage.getFromPublicKeyHex(),
                message,
                false);

        // 更新 更新时间
        jsonStore.setStartTimestamp(System.currentTimeMillis());
    }

    /**
     * 接受消息
     */
    @Override
    public void receiveMessage() {
        // 起始时间
        Long startTimestamp = jsonStore.getStartTimestamp();
        if (startTimestamp == null) {
            startTimestamp = System.currentTimeMillis();
        }

        // 获取消息并对返回值进行处理
        for (EccMessage eccMessage : receiveOne(eccController.getPublicKey(), Long.toString(startTimestamp))) {
            try {
                receiveOneEccMessage(eccMessage);
            } catch (Exception e) {
                log.error("处理收到的单个私聊消息（未解密）异常", e);
            }
        }
    }

    /**
     * 订阅单个私聊消息
     */
    @Override
    public void subscribeOne() {
        // 订阅单个私聊消息
        try {
            MessageWebSocketClient client = messageWebSocketClientStore.getClient();
            WebSocketResult<?> result = client.sendMessageAndWait("subscribeOne", eccController.getPublicKey());
            if (!ResultCodeEnum.OK.getCode().equals(result.getCode())) {
                throw new E2EchoException(result.getMessage());
            }
        } catch (E2EchoException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.SRV_WEBSOCKET_TIMEOUT);
        } catch (Exception e) {
            throw new E2EchoException(E2EchoExceptionCodeEnum.FAIL);
        }
    }

}

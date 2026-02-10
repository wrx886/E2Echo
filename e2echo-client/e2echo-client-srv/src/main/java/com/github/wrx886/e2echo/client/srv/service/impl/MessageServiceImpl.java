package com.github.wrx886.e2echo.client.srv.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.srv.mapper.MessageMapper;
import com.github.wrx886.e2echo.client.srv.model.socket.WebSocketResult;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.result.ResultCodeEnum;
import com.github.wrx886.e2echo.client.srv.service.MessageService;
import com.github.wrx886.e2echo.client.srv.socket.MessageWebSocketClient;
import com.github.wrx886.e2echo.client.srv.store.MessageWebSocketClientStore;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    private final MessageWebSocketClientStore messageWebSocketClientStore;
    private final ObjectMapper objectMapper;

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

}

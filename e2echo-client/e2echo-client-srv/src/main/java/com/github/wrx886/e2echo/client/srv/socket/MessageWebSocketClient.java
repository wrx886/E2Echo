package com.github.wrx886.e2echo.client.srv.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.github.wrx886.e2echo.client.common.common.BeanProvider;
import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.exception.E2EchoException;
import com.github.wrx886.e2echo.client.common.exception.E2EchoExceptionCodeEnum;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.srv.client.BaseWebSocketClient;
import com.github.wrx886.e2echo.client.srv.model.socket.WebSocketResult;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.client.srv.result.ResultCodeEnum;
import com.github.wrx886.e2echo.client.srv.service.MessageService;

public final class MessageWebSocketClient extends BaseWebSocketClient {

    private final EccController eccController = BeanProvider.getBean(EccController.class);
    private final MessageService messageService = BeanProvider.getBean(MessageService.class);

    // 构造函数
    public MessageWebSocketClient(String url) throws Exception {
        super(url);
    }

    /**
     * 自动接收私聊消息处理
     * 
     * @param result
     */
    public void autoReveiveOne(WebSocketResult<?> result) {
        // 消息为空
        if (result.getData() == null) {
            return;
        }

        // 类型转变
        EccMessage eccMessage = objectMapper.convertValue(result.getData(), EccMessage.class);

        // 处理
        messageService.autoReveiveOne(eccMessage);
    }

    /**
     * 自动接收群聊消息处理
     * 
     * @param result
     */
    public void autoReveiveGroup(WebSocketResult<?> result) {
        // 消息为空
        if (result.getData() == null) {
            return;
        }

        // 类型转变
        EccMessage eccMessage = objectMapper.convertValue(result.getData(), EccMessage.class);

        // 处理
        messageService.autoReveiveGroup(eccMessage);
    }

    /**
     * 发送单聊消息
     * 
     * @param eccMessage 消息
     */
    public void sendOne(EccMessage eccMessage) {
        try {
            WebSocketResult<?> result = sendMessageAndWait("sendOne", eccMessage);
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
     * 发送群聊消息
     * 
     * @param eccMessage 群聊消息
     */
    public void sendGroup(EccMessage eccMessage) {
        try {
            WebSocketResult<?> result = sendMessageAndWait("sendGroup", eccMessage);
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
            ReceiveOneMessageSocketVo vo = new ReceiveOneMessageSocketVo();
            vo.setToPublicKeyHex(toPublicKeyHex);
            vo.setStartTimestamp(startTimestamp);
            WebSocketResult<?> result = sendMessageAndWait("receiveOne", vo);
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
     * 接收群聊消息
     * 
     * @param groupUuid      群组 UUID
     * @param startTimestamp 开始时间戳(int64)
     * @return
     */
    public List<EccMessage> receiveGroup(String groupUuid, String startTimestamp) {
        try {
            // 获取群聊消息
            ReceiveGroupMessageSocketVo vo = new ReceiveGroupMessageSocketVo();
            vo.setGroupUuid(groupUuid);
            vo.setStartTimestamp(startTimestamp);
            WebSocketResult<?> result = sendMessageAndWait("receiveGroup", vo);
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
     * 订阅单个私聊消息
     */
    public void subscribeOne() {
        // 订阅单个私聊消息
        try {
            WebSocketResult<?> result = sendMessageAndWait("subscribeOne", eccController.getPublicKey());
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
     * 订阅群聊消息
     */
    public void subscribeGroup(String groupUuid) {
        // 订阅单个私聊消息
        try {
            WebSocketResult<?> result = sendMessageAndWait("subscribeGroup", groupUuid);
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
     * 批量订阅群聊消息
     * 
     * @param groupUuids 群聊UUID列表
     */
    public void subscribeGroups(List<String> groupUuids) {
        // 批量订阅私聊消息
        try {
            WebSocketResult<?> result = sendMessageAndWait("subscribeGroups", groupUuids);
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

package com.github.wrx886.e2echo.server.web.wocket.handler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketSession;

import com.github.wrx886.e2echo.server.common.LoginHold;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.model.enums.BaseEnum;
import com.github.wrx886.e2echo.server.model.enums.MessageType;
import com.github.wrx886.e2echo.server.model.vo.message.MessageVo;
import com.github.wrx886.e2echo.server.result.E2EchoException;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.web.service.MessageService;

@Controller
public class MessageHandler extends LoginHandler {

    @Autowired
    private MessageService messageService;

    // 发送消息
    public void send(WebSocketSession session, Message message) {
        messageService.send(LoginHold.getUser(), message);
    }

    // 接收消息
    public List<Message> receive(WebSocketSession session, MessageVo messageVo) {
        return messageService.receive(LoginHold.getUser(), messageVo);
    }

    // 配置自动接收，如果配置为组消息，那么用户消息也会自动接收
    public void registryAutoReceive(WebSocketSession session, Integer messageTypeCode) {
        // 将 messageTypeCode 转为 messageType
        MessageType messageType;
        try {
            messageType = convert(messageTypeCode, MessageType.class);
        } catch (Exception e) {
            throw new E2EchoException(ResultCodeEnum.WEB_SOCKET_REQUEST_PARAM_ERROR);
        }

        // 注册自动接收
        messageService.registryAutoReceive(session.getId(), messageType);
    }

    // 取消配置自动接收
    public void cancelAutoReceive(WebSocketSession session) {
        messageService.cancelRegistryAutoReceive(session.getId());
    }

    // 取消配置自动接收
    @Override
    protected void afterClose(WebSocketSession session) {
        super.afterClose(session);

        // 取消配置自动接收
        messageService.cancelRegistryAutoReceive(session.getId());
    }

    // 将字符串的值转为枚举类型
    private <T extends BaseEnum> T convert(Integer code, Class<T> targetType) {
        // 通过 Class 获取枚举类型的所有实例
        T[] ts = targetType.getEnumConstants();
        // 遍历寻找
        for (T t : ts) {
            if (t.getCode().equals(code)) {
                return t;
            }
        }
        // 没有对应的类型
        throw new IllegalArgumentException("code: " + code + " illegal");
    }

}

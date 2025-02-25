package com.github.wrx886.e2echo.client.test.service;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.wrx886.e2echo.client.api.MessageWebSocketApi;
import com.github.wrx886.e2echo.client.model.entity.User;
import com.github.wrx886.e2echo.client.model.enums.MessageApiType;
import com.github.wrx886.e2echo.client.model.enums.MessageType;
import com.github.wrx886.e2echo.client.service.MessageService;
import com.github.wrx886.e2echo.client.service.UserService;
import com.github.wrx886.e2echo.client.store.LoginUserStore;
import com.github.wrx886.e2echo.client.test.config.LoginConfig;

@SpringBootTest
public class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private LoginConfig loginConfig;

    @Autowired
    private LoginUserStore loginUserStore;

    @Autowired
    private MessageWebSocketApi messageWebSocketApi;

    // 模拟登入
    @BeforeEach
    public void login() {
        loginUserStore.setAccessToken(loginConfig.getAccessToken());
        loginUserStore.setBaseUrl(loginConfig.getBaseUrl());
        loginUserStore.setId(loginConfig.getOwnerId());
        loginUserStore.setPrivateKey(loginConfig.getPrivateKey());
        loginUserStore.setPublicKey(loginConfig.getPublicKey());

        // 配置 WebSocket
        messageWebSocketApi.setBaseUrl(loginConfig.getBaseUrl());
    }

    // 发送文本消息给自己
    @Test
    public void sendTextTest() {
        // 获取会话
        User session = userService.putPersonByPublicKey(loginUserStore.getId(), loginUserStore.getPublicKey());
        // 发送消息
        messageService.send(session.getId(), UUID.randomUUID().toString(), MessageType.TEXT);
    }

    // 接收来自自己的文本消息
    @Test
    public void receiveTest() {
        // 接收消息
        messageService.receive();

        // 查询数据库中相关的消息
        System.out.println(messageService.list());
    }

    // WebSocket-测试
    @Test
    public void webSocketTest() throws Exception {
        // 配置自动接收
        messageService.registryAutoReceive(MessageApiType.USER);

        // 发送消息
        sendTextTest();

        // 等待消息接收，手动查看是否存在输出
        Thread.sleep(5 * 1000);

        // 取消配置自动接收
        messageService.registryAutoReceive(null);

    }

}

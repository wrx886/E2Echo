package com.github.wrx886.e2echo.server.test.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.server.controller.MessageController;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.util.EccMessageTestUtil;
import com.github.wrx886.e2echo.server.util.EccMessageUtil;
import com.github.wrx886.e2echo.server.util.EccUtil.KeyPairHex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class MessageControllerTest {

    @Autowired
    private MessageController messageController;

    // 测试使用的密钥对
    private final KeyPairHex keyPairHex = new KeyPairHex(
            "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
            "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");

    // 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
    private final String groupUuid = "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5";

    @Test
    @Order(1)
    public void testSendOne() {
        // 生成数据
        String messageData = UUID.randomUUID().toString();

        // 创建消息
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFromPublicKeyHex(keyPairHex.getPublicKeyHex());
        eccMessage.setToPublicKeyHex(keyPairHex.getPublicKeyHex());
        eccMessage.setData(messageData);

        // 加密消息
        eccMessage = EccMessageTestUtil.encrypt(keyPairHex, eccMessage);

        // 发送消息
        Result<Void> result = messageController.sendOne(eccMessage);
        assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
    }

    @Test
    @Order(2)
    public void testReceiveOne() {
        // 接收消息
        Result<List<EccMessage>> result = messageController.receiveOne(keyPairHex.getPublicKeyHex(),
                Long.toString(System.currentTimeMillis() - 60 * 1000L));
        assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

        // 解析消息
        for (EccMessage eccMessage : result.getData()) {
            eccMessage = EccMessageTestUtil.decrypt(keyPairHex, eccMessage);
            log.info("收到私聊消息：{}", eccMessage.getData());
        }
    }

    @Test
    @Order(3)
    public void testSendGroup() {
        // 生成数据
        String messageData = UUID.randomUUID().toString();

        // 创建消息
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFromPublicKeyHex(keyPairHex.getPublicKeyHex());
        eccMessage.setToPublicKeyHex(groupUuid);
        eccMessage.setData(messageData);

        // 签名消息
        eccMessage = EccMessageTestUtil.sign(keyPairHex, eccMessage);

        // 发送消息
        Result<Void> result = messageController.sendGroup(eccMessage);
        assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
    }

    @Test
    @Order(4)
    public void testReceiveGroup() {
        // 接收消息
        Result<List<EccMessage>> result = messageController.receiveGroup(keyPairHex.getPublicKeyHex(),
                Long.toString(System.currentTimeMillis() - 60 * 1000L));
        assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

        // 解析消息
        for (EccMessage eccMessage : result.getData()) {
            if (EccMessageUtil.verify(eccMessage)) {
                log.info("收到群聊消息：{}", eccMessage.getData());
            }
        }

    }

}

package com.github.wrx886.e2echo.server.test.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.EccMessage;
import com.github.wrx886.e2echo.server.model.socket.WebSocketResult;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveGroupMessageSocketVo;
import com.github.wrx886.e2echo.server.model.vo.socket.message.ReceiveOneMessageSocketVo;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.client.WebSocketTestClient;
import com.github.wrx886.e2echo.server.test.util.EccMessageTestUtil;
import com.github.wrx886.e2echo.server.util.EccMessageUtil;
import com.github.wrx886.e2echo.server.util.EccUtil.KeyPairHex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class MessageWebSocketHandlerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${server.port}")
    private int port;

    // 测试使用的密钥对
    private final KeyPairHex keyPairHex = new KeyPairHex(
            "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
            "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");

    // 群聊 UUID（格式：{群主公钥}:{群聊UUID}）
    private final String groupUuid = "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5";

    @Test
    @Order(1)
    public void testSendOne() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            sendOne(client);
        }
    }

    @Test
    @Order(2)
    public void testReceiveOne() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            // 接收消息
            ReceiveOneMessageSocketVo vo = new ReceiveOneMessageSocketVo();
            vo.setToPublicKeyHex(keyPairHex.getPublicKeyHex());
            vo.setStartTimestamp(Long.toString(System.currentTimeMillis() - 10 * 60 * 1000L));
            WebSocketResult<?> result = client.sendMessageAndWait("receiveOne", vo);
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 解析消息
            for (Object message : objectMapper.convertValue(result.getData(), List.class)) {
                EccMessage eccMessage = objectMapper.convertValue(message, EccMessage.class);
                eccMessage = EccMessageTestUtil.decrypt(keyPairHex, eccMessage);
                log.info("收到私聊消息：{}", eccMessage.getData());
            }
        }
    }

    @Test
    @Order(3)
    public void testSendGroup() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            sendGroup(client);
        }
    }

    @Test
    @Order(4)
    public void testReceiveGroup() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            // 接收消息
            ReceiveGroupMessageSocketVo vo = new ReceiveGroupMessageSocketVo();
            vo.setGroupUuid(groupUuid);
            vo.setStartTimestamp(Long.toString(System.currentTimeMillis() - 10 * 60 * 1000L));
            WebSocketResult<?> result = client.sendMessageAndWait("receiveGroup", vo);
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 解析消息
            for (Object message : objectMapper.convertValue(result.getData(), List.class)) {
                EccMessage eccMessage = objectMapper.convertValue(message, EccMessage.class);
                if (EccMessageUtil.verify(eccMessage)) {
                    log.info("收到群聊消息：{}", eccMessage.getData());
                }
            }
        }
    }

    @Test
    @Order(5)
    public void testSubscribeOne() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            // 订阅消息
            WebSocketResult<?> result = client.sendMessageAndWait("subscribeOne", keyPairHex.getPublicKeyHex());
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 绑定自动接收
            int[] count = new int[] { 0 };
            client.bindCommandMethod("autoReveiveOne", (res) -> {
                assertEquals(res.getCode(), ResultCodeEnum.OK.getCode());
                // 解析消息
                EccMessage eccMessage = objectMapper.convertValue(res.getData(), EccMessage.class);
                eccMessage = EccMessageTestUtil.decrypt(keyPairHex, eccMessage);
                log.info("收到私聊消息：{}", eccMessage.getData());
                count[0]++;
            });

            // 发送消息
            sendOne(client);

            // 取消订阅
            result = client.sendMessageAndWait("unsubscribeOne");
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 发送消息
            sendOne(client);

            // 等待消息接收
            Thread.sleep(1000);

            // 断言
            assertEquals(1, count[0]);
        }
    }

    @Test
    @Order(6)
    public void testSubscribeGroup() throws Exception {
        try (WebSocketTestClient client = new WebSocketTestClient("ws://localhost:" + port + "/server/message")) {
            // 订阅消息
            WebSocketResult<?> result = client.sendMessageAndWait("subscribeGroup", groupUuid);
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 绑定自动接收
            int[] count = new int[] { 0 };
            client.bindCommandMethod("autoReveiveGroup", (res) -> {
                assertEquals(res.getCode(), ResultCodeEnum.OK.getCode());
                // 解析消息
                EccMessage eccMessage = objectMapper.convertValue(res.getData(), EccMessage.class);
                if (EccMessageUtil.verify(eccMessage)) {
                    log.info("收到群聊消息：{}", eccMessage.getData());
                }
                count[0]++;
            });

            // 发送消息
            sendGroup(client);

            // 取消订阅
            result = client.sendMessageAndWait("unsubscribeGroup", groupUuid);
            assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

            // 发送消息
            sendGroup(client);

            // 等待消息接收
            Thread.sleep(1000);

            // 断言
            assertEquals(1, count[0]);
        }
    }

    // 发送消息
    private void sendOne(WebSocketTestClient client) throws Exception {
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
        WebSocketResult<?> result = client.sendMessageAndWait("sendOne", eccMessage);
        assertEquals(ResultCodeEnum.OK.getCode(), result.getCode());
        log.info("消息发送成功");
    }

    // 发送消息
    private void sendGroup(WebSocketTestClient client) throws Exception {
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
        WebSocketResult<?> result = client.sendMessageAndWait("sendGroup", eccMessage);
        assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
        log.info("消息发送成功");
    }

}

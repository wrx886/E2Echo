package com.github.wrx886.e2echo.server.test.web.websocket.handler;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.java_websocket.enums.ReadyState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.model.enums.MessageType;
import com.github.wrx886.e2echo.server.model.vo.message.MessageVo;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.common.ResultUtil;
import com.github.wrx886.e2echo.server.test.common.TestUserInfo;
import com.github.wrx886.e2echo.server.test.web.websocket.client.MessageTestClient;
import com.github.wrx886.e2echo.server.util.EccUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class MessageHandlerTest {

    @Value("${server.port}")
    private Integer port;

    @Autowired
    private ResultUtil resultUtil;

    @Autowired
    private TestUserInfo testUserInfo;

    @Test
    public void test() {
        MessageTestClient client = null;
        try {
            // 连接 WebSocket
            client = new MessageTestClient("ws://localhost:" + port);
            client.connectBlocking();
            Assertions.assertEquals(ReadyState.OPEN, client.getReadyState());

            // 登入
            login(client);

            // 发送消息
            sendAndReceive(client);

            // 自动接收
            sendAndAutoReceive(client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    // 登入测试
    private void login(MessageTestClient client) throws Exception {
        BlockingQueue<Integer> bd = new ArrayBlockingQueue<>(1);
        // 发送登入请求并配置处理函数
        client.sendMessage("login", testUserInfo.getAccessToken(), (result) -> {
            try {
                // 打印返回值
                System.out.println(result);
                // 登入必须成功
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
                bd.put(result.getCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 等待处理完毕
        bd.take();
    }

    // 消息收发
    private void sendAndReceive(MessageTestClient client) throws Exception {
        BlockingQueue<Integer> bd = new ArrayBlockingQueue<>(1);

        // 发送消息
        // 构建要发送的消息
        Message message = new Message();
        // 填充发送者的公钥
        message.setFromPublicKey(testUserInfo.getPublicKey());
        // 填充接收者的公钥
        message.setToPublicKey(testUserInfo.getPublicKey());
        // 生成发送数据
        String sendData = UUID.randomUUID().toString();
        // 使用接收者的公钥进行加密
        message.setData(EccUtil.encrypt(sendData, testUserInfo.getPublicKey()));
        // 使用发送者的私钥进行签名
        message.setSign(EccUtil.sign(sendData, testUserInfo.getPrivateKey()));
        // 发送时间
        message.setSendTime(new Date());
        // 指定消息类型
        message.setMessageType(MessageType.USER);
        // 发送消息
        client.sendMessage("send", message, (result) -> {
            try {
                // 打印返回值
                System.out.println(result);
                // 返回成功
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
                bd.put(result.getCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // 等待服务器响应
        bd.take();

        // 接收消息
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageType(MessageType.USER);
        messageVo.setStartTime(new Date(System.currentTimeMillis() - 1000 * 60));
        // 接收消息
        client.sendMessage("receive", messageVo, (result) -> {
            try {
                // 打印返回值
                System.out.println(result);
                // 返回成功
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

                // 将数据转为 List
                List<?> messageData = resultUtil.to(result.getData(), List.class);

                // 解密数据
                ArrayList<Message> messages = new ArrayList<>();
                boolean received = false;
                for (Object obj : messageData) {
                    // 1. 把数据转为 message
                    Message tmp = resultUtil.to(obj, Message.class);

                    // 2. 解密数据
                    tmp.setData(EccUtil.decrypt(tmp.getData(), testUserInfo.getPrivateKey()));

                    // 3. 验证签名
                    assert EccUtil.verify(tmp.getData(), tmp.getSign(), testUserInfo.getPublicKey());

                    // 4. 将数据放入 message
                    messages.add(tmp);

                    // 5. 如果消息是发送出去的那条，则标记
                    if (sendData.equals(tmp.getData())) {
                        received = true;
                    }
                }

                // 接收到发送的消息
                assert received;

                // 打印收到的消息
                for (Message m : messages) {
                    System.out.println("----------------------------------------------------------------");
                    System.out.println("id: " + m.getId());
                    System.out.println("fromPublicKey: " + m.getFromPublicKey());
                    System.out.println("toPublicKey: " + m.getToPublicKey());
                    System.out.println("sign: " + m.getSign());
                    System.out.println("data: " + m.getData());
                    System.out.println("sendTime: " + m.getSendTime());
                    System.out.println("messageType: " + m.getMessageType());
                    System.out.println("----------------------------------------------------------------");
                }

                bd.put(result.getCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        bd.take();
    }

    // 配置自动接收并测试
    private void sendAndAutoReceive(MessageTestClient client) throws Exception {
        BlockingQueue<Integer> bd = new ArrayBlockingQueue<>(2);

        // 配置自动接收
        client.sendMessage("registryAutoReceive", MessageType.USER.getCode(), (result) -> {
            try {
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
                bd.put(result.getCode());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        bd.take();

        // 配置自动接收的处理方案
        client.bindCommandMethod("autoReceive", (result) -> {
            try {
                // 打印返回值
                System.out.println(result);
                // 返回成功
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());

                // 将数据转为 List
                Message message = resultUtil.to(result.getData(), Message.class);

                // 2. 解密数据
                message.setData(EccUtil.decrypt(message.getData(), testUserInfo.getPrivateKey()));

                // 3. 验证签名
                assert EccUtil.verify(message.getData(), message.getSign(), testUserInfo.getPublicKey());

                // 输出
                System.out.println("----------------------------------------------------------------");
                System.out.println("id: " + message.getId());
                System.out.println("fromPublicKey: " + message.getFromPublicKey());
                System.out.println("toPublicKey: " + message.getToPublicKey());
                System.out.println("sign: " + message.getSign());
                System.out.println("data: " + message.getData());
                System.out.println("sendTime: " + message.getSendTime());
                System.out.println("messageType: " + message.getMessageType());
                System.out.println("----------------------------------------------------------------");

                bd.put(result.getCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 发送消息
        // 构建要发送的消息
        Message message = new Message();
        // 填充发送者的公钥
        message.setFromPublicKey(testUserInfo.getPublicKey());
        // 填充接收者的公钥
        message.setToPublicKey(testUserInfo.getPublicKey());
        // 生成发送数据
        String sendData = UUID.randomUUID().toString();
        // 使用接收者的公钥进行加密
        message.setData(EccUtil.encrypt(sendData, testUserInfo.getPublicKey()));
        // 使用发送者的私钥进行签名
        message.setSign(EccUtil.sign(sendData, testUserInfo.getPrivateKey()));
        // 发送时间
        message.setSendTime(new Date());
        // 指定消息类型
        message.setMessageType(MessageType.USER);
        // 发送消息
        client.sendMessage("send", message, (result) -> {
            try {
                // 打印返回值
                System.out.println(result);
                // 返回成功
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
                bd.put(result.getCode());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // 等待服务器响应
        bd.take();

        // 等待自动接收
        bd.take();

        // 取消配置自动接收
        client.sendMessage("cancelAutoReceive", MessageType.USER.getCode(), (result) -> {
            try {
                Assertions.assertEquals(result.getCode(), ResultCodeEnum.OK.getCode());
                bd.put(result.getCode());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        bd.take();

        // 取消命令绑定
        client.unbindCommandMethod("autoReceive");
    }
}
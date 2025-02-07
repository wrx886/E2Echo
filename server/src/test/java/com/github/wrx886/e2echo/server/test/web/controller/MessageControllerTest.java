package com.github.wrx886.e2echo.server.test.web.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wrx886.e2echo.server.model.entity.Message;
import com.github.wrx886.e2echo.server.model.enums.MessageType;
import com.github.wrx886.e2echo.server.model.vo.message.MessageVo;
import com.github.wrx886.e2echo.server.result.Result;
import com.github.wrx886.e2echo.server.result.ResultCodeEnum;
import com.github.wrx886.e2echo.server.test.common.ResultUtil;
import com.github.wrx886.e2echo.server.test.common.TestUserInfo;
import com.github.wrx886.e2echo.server.util.EccUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUserInfo testUserInfo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResultUtil resultUtil;

    @Test
    public void test() throws Exception {
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
        assert resultUtil.getResultFromMockResponse(mockMvc.perform(MockMvcRequestBuilders.post("/server/message/send")
                .header("access-token", testUserInfo.getAccessToken())
                .content(objectMapper.writeValueAsString(message))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), null)
                .getCode().equals(ResultCodeEnum.OK.getCode());

        // 接收消息
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageType(MessageType.USER);
        messageVo.setStartTime(new Date(System.currentTimeMillis() - 1000 * 60));

        @SuppressWarnings("rawtypes")
        Result<ArrayList> result = resultUtil.getResultFromMockResponse(mockMvc
                .perform(MockMvcRequestBuilders.post("/server/message/receive")
                        .header("access-token", testUserInfo.getAccessToken())
                        .content(objectMapper.writeValueAsString(messageVo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn().getResponse(), ArrayList.class);

        // 成功返回
        assert result.getCode().equals(ResultCodeEnum.OK.getCode());

        // 解密数据
        ArrayList<Message> messages = new ArrayList<>();
        boolean received = false;
        for (Object obj : result.getData()) {
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

        // 这里要求必须受到发送出去的消息
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

    }

}

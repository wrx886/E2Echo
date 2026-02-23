package com.github.wrx886.e2echo.client.test.common.controller.srv;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.controller.srv.FileController;
import com.github.wrx886.e2echo.client.common.controller.srv.MessageController;
import com.github.wrx886.e2echo.client.common.model.entity.Message;
import com.github.wrx886.e2echo.client.common.model.enum_.MessageType;
import com.github.wrx886.e2echo.client.ecc.store.EccKeyStore;
import com.github.wrx886.e2echo.client.srv.store.WebUrlStore;
import com.github.wrx886.e2echo.client.starter.Main.Starter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = { Starter.class })
@ActiveProfiles("test")
public class FileControllerTest {

    @Autowired
    private EccController eccController;

    @Autowired
    private WebUrlStore webUrlStore;

    @Autowired
    private EccKeyStore eccKeyStore;

    @Autowired
    private FileController fileController;

    @Autowired
    private MessageController messageController;

    @BeforeEach
    public void init() {
        eccKeyStore.set(
                "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
                "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");
        webUrlStore.setWebUrl("http://localhost:8080");
    }

    @Test
    public void sendOneFileTest() {
        // 接收消息
        messageController.receiveMessage();
        // 发送文件
        fileController.sendOneFile(
                eccController.getPublicKey(),
                "D:\\Users\\01\\Desktop\\毕设\\##毕设表格（2023.12更新）\\附件1 毕业设计任务书.doc",
                MessageType.FILE);
        // 接收消息
        messageController.receiveMessage();
        // 获取消息
        List<Message> messages = messageController.listOneBySession(eccController.getPublicKey());
        log.info("{}", messages.stream().map(message -> message.getData()).toList());
    }

}

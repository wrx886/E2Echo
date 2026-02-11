package com.github.wrx886.e2echo.client.test.common.controller.ecc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.client.common.controller.ecc.EccController;
import com.github.wrx886.e2echo.client.common.model.EccMessage;
import com.github.wrx886.e2echo.client.ecc.store.EccKeyStore;
import com.github.wrx886.e2echo.client.ecc.util.EccUtil.KeyPairHex;
import com.github.wrx886.e2echo.client.starter.Main.Starter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = Starter.class)
public class EccControllerTest {

    @Autowired
    private EccController eccController;

    @Autowired
    private EccKeyStore eccKeyStore;

    // 测试使用的密钥对
    private final KeyPairHex keyPairHex = new KeyPairHex(
            "04d18e146436495497953b42d4fdc2bc3134b4c63214c091c7b9bd8d8d0135f4d6709795a07206f61e530e67fc4f6e7b54daefb8eb276d46cb35f84d8e05f90dd5",
            "6869bee95e46cc8b28cd9c167285ba68a6143b63f1ba371331085c9eae355bba");

    @BeforeEach
    public void init() throws Exception {
        // 生成公钥和私钥
        eccKeyStore.set(keyPairHex.getPublicKeyHex(), keyPairHex.getPrivateKeyHex());
    }

    @Test
    public void encryptAndDecryptTest() {
        // 数据
        String data = "Hello World!";

        // 封装并加密数据
        EccMessage eccMessage = new EccMessage();
        eccMessage.setData(data);
        eccMessage.setFromPublicKeyHex(eccController.getPublicKey());
        eccMessage.setToPublicKeyHex(eccController.getPublicKey());
        eccMessage = eccController.encrypt(eccMessage);

        // 解密数据
        eccMessage = eccController.decrypt(eccMessage);
        assertEquals(data, eccMessage.getData());
        log.info(eccMessage.getData());
    }

    @Test
    public void signAndVerifyTest() {
        // 数据
        String data = "Hello World!";

        // 封装并签名数据
        EccMessage eccMessage = new EccMessage();
        eccMessage.setData(data);
        eccMessage.setFromPublicKeyHex(eccController.getPublicKey());
        eccMessage.setToPublicKeyHex(eccController.getPublicKey());
        eccMessage = eccController.sign(eccMessage);

        // 验证数据
        assertEquals(eccController.verify(eccMessage), true);
    }

}

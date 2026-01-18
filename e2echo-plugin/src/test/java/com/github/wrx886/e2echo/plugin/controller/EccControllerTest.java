package com.github.wrx886.e2echo.plugin.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.wrx886.e2echo.plugin.model.entity.EccMessage;
import com.github.wrx886.e2echo.plugin.result.Result;
import com.github.wrx886.e2echo.plugin.result.ResultCodeEnum;
import com.github.wrx886.e2echo.plugin.service.EccService;
import com.github.wrx886.e2echo.plugin.util.EccUtil.KeyPairHex;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class EccControllerTest {

    @Autowired
    private EccController eccController;

    @Autowired
    private EccService eccService;

    // 密钥对
    private KeyPairHex keyPairHex;

    @BeforeEach
    public void init() {
        // 1. 生成密钥对
        keyPairHex = eccService.generateKeyPair();
        // 2. 登入
        eccService.login(keyPairHex.getPublicKeyHex(), keyPairHex.getPrivateKeyHex());
    }

    @Test
    public void testEncryptDecrypt() {
        // 1. 生成数据
        String data = UUID.randomUUID().toString();
        log.info("data: {}", data);

        // 2. 加密数据
        EccMessage fromEccMessage = new EccMessage();
        fromEccMessage.setFromPublicKeyHex(keyPairHex.getPublicKeyHex());
        fromEccMessage.setToPublicKeyHex(keyPairHex.getPublicKeyHex());
        fromEccMessage.setData(data);
        Result<EccMessage> encryptedResult = eccController.encrypt(fromEccMessage);
        assertEquals(encryptedResult.getCode(), ResultCodeEnum.OK.getCode());
        log.info("encryptedResult: {}", encryptedResult.getData());

        // 3. 解密数据
        Result<EccMessage> decryptedResult = eccController.decrypt(encryptedResult.getData());
        assertEquals(decryptedResult.getCode(), ResultCodeEnum.OK.getCode());
        log.info("decryptedResult: {}", decryptedResult.getData());

        // 4. 验证结果
        assertEquals(decryptedResult.getData().getData(), data);
    }

    @Test
    public void testSignVerify() {
        // 1. 生成数据
        String data = UUID.randomUUID().toString();
        log.info("data: {}", data);

        // 2. 签名数据
        EccMessage eccMessage = new EccMessage();
        eccMessage.setFromPublicKeyHex(keyPairHex.getPublicKeyHex());
        // eccMessage.setToPublicKeyHex(keyPairHex.getPublicKeyHex());
        eccMessage.setData(data);
        Result<EccMessage> signResult = eccController.sign(eccMessage);
        assertEquals(signResult.getCode(), ResultCodeEnum.OK.getCode());
        log.info("signResult: {}", signResult.getData());

        // 3. 验签数据
        Result<Boolean> verifyResult = eccController.verify(signResult.getData());
        assertEquals(verifyResult.getCode(), ResultCodeEnum.OK.getCode());
        log.info("verifyResult: {}", verifyResult.getData());

        // 4. 验证结果
        assertEquals(verifyResult.getData(), true);
    }

}

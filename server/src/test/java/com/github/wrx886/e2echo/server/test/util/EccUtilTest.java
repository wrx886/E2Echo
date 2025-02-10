package com.github.wrx886.e2echo.server.test.util;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.github.wrx886.e2echo.server.util.EccUtil;
import com.github.wrx886.e2echo.server.util.EccUtil.KeyPairHex;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class EccUtilTest {

    @Test
    public void test() throws Exception {
        // 生成一组密钥对
        KeyPairHex keyPair = EccUtil.generateKeyPair();

        // 将公钥转为 HEX 格式
        String publicKey = keyPair.getPublicKeyHex();
        System.out.println("publicKey: " + publicKey);

        // 将私钥转为 HEX 格式
        String privateKey = keyPair.getPrivateKeyHex();
        System.out.println("privateKey: " + privateKey);

        // 随机生成数据
        String plain = UUID.randomUUID().toString();
        System.out.println("plain: " + plain);

        // 加密数据
        String encrypt = EccUtil.encrypt(plain, publicKey);
        System.out.println("encrypt: " + encrypt);

        // 对数据进行签名
        String sign = EccUtil.sign(plain, privateKey);
        System.out.println("sign: " + sign);

        // 解密数据
        String decrypt = EccUtil.decrypt(encrypt, privateKey);
        System.out.println("decrypt: " + decrypt);
        assert plain.equals(decrypt);

        // 比较签名
        boolean verify = EccUtil.verify(decrypt, sign, publicKey);
        System.out.println("verify: " + verify);
        assert verify;
    }

}

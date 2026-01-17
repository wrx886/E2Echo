package com.github.wrx886.e2echo.plugin.util;

import org.junit.jupiter.api.Test;

import com.github.wrx886.e2echo.plugin.util.EccUtil.KeyPairHex;

public class EccUtilTest {

    /**
     * 测试 ECC 工具类的密钥生成、加密、解密、签名和验证
     * 
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        // 生成密钥对
        KeyPairHex keyPair = EccUtil.generateKeyPair();
        String publicKey = keyPair.getPublicKeyHex();
        String privateKey = keyPair.getPrivateKeyHex();

        // 输出
        System.out.println("Public Key (RAW HEX): " + publicKey);
        System.out.println("Private Key (RAW HEX): " + privateKey);

        // 加密字符串
        String plainText = "Hello, ECC!";
        String encryptedHex = EccUtil.encrypt(plainText, publicKey); // 使用 RAW HEX 公钥加密 // 公钥加密
        System.out.println("Encrypted (HEX): " + encryptedHex);

        // 解密字符串
        String decryptedText = EccUtil.decrypt(encryptedHex, privateKey); // 使用 RAW HEX 私钥解密
        System.out.println("Decrypted: " + decryptedText);

        // 签名和验证
        String signatureHex = EccUtil.sign(plainText, privateKey); // 使用 RAW HEX 私钥签名
        System.out.println("Signature (HEX): " + signatureHex);

        boolean isVerified = EccUtil.verify(plainText, signatureHex, publicKey); // 使用 RAW HEX 公钥验证
        System.out.println("Signature verified: " + isVerified);
    }

}

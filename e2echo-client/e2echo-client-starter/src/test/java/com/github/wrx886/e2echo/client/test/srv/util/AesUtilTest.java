package com.github.wrx886.e2echo.client.test.srv.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.wrx886.e2echo.client.srv.util.AesUtil;

public class AesUtilTest {

    @Test
    public void encryptAndDecrypt() {
        String key = AesUtil.generateKeyAsHex();
        String plainText = UUID.randomUUID().toString();

        String encrypted = AesUtil.encrypt(plainText, key);
        String decrypted = AesUtil.decrypt(encrypted, key);

        System.out.println("Key: " + key);
        System.out.println("PlainText: " + plainText);
        System.out.println("Encrypted: " + encrypted);
        System.out.println("Decrypted: " + decrypted);
        System.out.println("Match: " + plainText.equals(decrypted));
        assertEquals(plainText, decrypted);
    }

}
